package app.reelstack.player

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.network.readableMessage
import app.reelstack.data.network.serviceError
import app.reelstack.data.repository.DeviceIdentity
import app.reelstack.background.PlaybackProgressRuntime
import app.reelstack.background.PlaybackProgressScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


/**
 * How long a stall is allowed to stay silent, try something else, and finally give up.
 *
 * The total wait is unchanged. What is new is that the first two thresholds exist at all: the
 * player used to sit on a spinner for 45 seconds, quietly switch to a compatibility stream, and sit
 * on the spinner for another 45 before saying anything.
 */
private const val STALL_NOTICE_MS = 8_000L
private const val STALL_GIVE_UP_MS = 45_000L

/**
 * A localized string for the player's own messages.
 *
 * Top-level rather than a member: as the class's first member, an `androidx.annotation`-annotated
 * parameter made lint stop honouring the class's `@OptIn(UnstableApi)` and report every media3 call
 * in the file. Out here the annotation keeps its checking and the opt-in keeps working.
 */
private fun AppContainer.appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
    app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)

private const val MAX_KIDS_AUTOPLAY_CHAIN = 3

/** Owns one local player, not a remote session controller. Survives rotation; never plays in the background. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class JellyfinPlayerModel(private val container: AppContainer) : ViewModel() {
    private val mutable = MutableStateFlow(PlayerScreenState())
    val state = mutable.asStateFlow()
    private val deviceId = DeviceIdentity.get(container.appContext)
    private val deviceCapabilities = AndroidPlaybackCapabilities(container.appContext)
    private val client = MediaPlaybackClient(deviceId = deviceId,
        capabilities = deviceCapabilities::snapshot, sourceSupported = deviceCapabilities::canDirectPlay,
        videoSupported = deviceCapabilities::canDecodeVideo)
    private var connection: ServiceConnection? = null
    private val owner = container.connectionRepository.captureSession()
    private var serviceKind = ServiceKind.JELLYFIN
    private var seerrConnection: ServiceConnection? = null
    private var userId = ""
    private var rootId = ""
    private var selected: PlayableItem? = null
    private var parent: PlayableItem? = null
    private val folders = mutableListOf<PlayableItem>()
    private var offset = 0
    private var plan: PlaybackPlan? = null
    private var activeMediaSource: MediaSource? = null
    private val localAudioFallback = LocalAudioFallback()
    private var started = false
    private var stopped = false
    /** How far down the fallback ladder this item has been pushed. Reset when another is chosen. */
    private var compatibility = PlaybackCompatibility.DIRECT
    private var networkRecoveries = 0
    /** Where the last stream retry picked up; a minute played past it clears the count. */
    private var networkRecoveryPosition = 0L
    private var foreground = true
    private var request: Job? = null
    private var recoveryJob: Job? = null
    private var generation = 0
    private var bufferingSince = 0L
    private var nextEpisodeJob: Job? = null
    private var previewJob: Job? = null
    private var previewManifest: Pair<String, List<TrickplayGrid>>? = null
    private var countdownJob: Job? = null
    private var nextEpisodeCancelled = false

    /**
     * Kids mode caps how many episodes may start on their own.
     *
     * An app that keeps a child watching for as long as it can is working against the parent who
     * installed it, so after three in a row the offer stays put and waits for a press.
     */
    var kidsMode: Boolean = false
    private val kidsPreferences by lazy { app.reelstack.data.repository.KidsPreferencesRepository(container.appContext) }
    private fun isKidsBedtimeReached(): Boolean = kidsMode &&
        kidsPreferences.read(container.connectionRepository.activeProfileId).bedtime.isReached()
    private fun playbackOptions(): Personalization {
        val adult = container.preferencesRepository.personalization
        return if (kidsMode) kidsPreferences.read(container.connectionRepository.activeProfileId).playbackOptions(adult) else adult
    }
    private var autoplayChain = 0
    private var preferredAudio: Int? = null
    private var preferredSubtitle: Int? = null
    private var preferredAudioKey: String? = null
    private var preferredSubtitleKey: String? = null
    private var preferredSource: String? = null
    private var subtitleCache: SubtitleMemoryCache? = null
    private var subtitleLoadJob: Job? = null
    /** Text tracks of the current plan whose file is already in memory. Only these are switched on. */
    private val readyTextIndices = mutableSetOf<Int>()
    /** ASS streams whose original was too large or broken; the server's VTT of them is shown instead. */
    private val vttFallbackIndices = mutableSetOf<Int>()
    private val reporter = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reportLease = PlaybackProgressRuntime.acquire()
    private val reports = PlaybackReportQueue<PlaybackReport>({ it.plan.sessionId }, { it.event })

    private val adjustmentController = PlaybackAdjustmentController()
    private val subtitleResources = PlaybackSubtitleResources()
    val adjustments = adjustmentController.state
    fun adjust(value: PlaybackAdjustments) { if (container.connectionRepository.isCurrent(owner)) adjustmentController.apply(player, value) }

    val player: ExoPlayer = ExoPlayer.Builder(container.appContext,
        PlaybackRenderersFactory(container.appContext, localAudioFallback, adjustmentController, subtitleResources))
        .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        setHandleAudioBecomingNoisy(true)
        setSeekBackIncrementMs(10_000)
        setSeekForwardIncrementMs(10_000)
        addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    subtitleResources.width = videoSize.width
                    subtitleResources.height = videoSize.height
                }
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                mutable.update { it.copy(playWhenReady = playWhenReady) }
                // Pausing during buffering does not change isPlaying, but still needs saving.
                if (started) report("/Progress")
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mutable.update { it.copy(playing = isPlaying) }
                if (isPlaying && !started) { started = true; report("") }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                bufferingSince = if (playbackState == Player.STATE_BUFFERING) android.os.SystemClock.elapsedRealtime() else 0L
                if (playbackState == Player.STATE_READY && openStartedNanos != 0L) {
                    app.reelstack.data.network.PerfLog.milestone("player ready", openStartedNanos)
                    openStartedNanos = 0L
                }
                mutable.update { it.copy(busy = playbackState == Player.STATE_BUFFERING, ended = playbackState == Player.STATE_ENDED,
                    durationMs = duration.takeIf { value -> value > 0 } ?: it.durationMs) }
                if (playbackState == Player.STATE_READY) selectTextTrack()
                if (playbackState == Player.STATE_ENDED) {
                    report("/Stopped"); started = false
                    if (isKidsBedtimeReached()) {
                        countdownJob?.cancel()
                        mutable.update { it.copy(bedtimeReached = true, nextEpisodeCountdown = null) }
                    } else startNextEpisodeCountdown()
                }
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (started && reason == Player.DISCONTINUITY_REASON_SEEK) report("/Progress")
            }
            override fun onTracksChanged(tracks: Tracks) {
                selectTextTrack()
                val selectedPlan = plan ?: return
                val builder = trackSelectionParameters.buildUpon()
                if (selectedPlan.direct) {
                    val audioTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.flatMap { group ->
                        (0 until group.length).map { group to it }
                    }
                    val ordinal = selectedPlan.audio.indexOfFirst { it.index == selectedPlan.audioIndex }
                    if (audioTracks.size == selectedPlan.audio.size && ordinal >= 0) {
                        val (group, track) = audioTracks[ordinal]
                        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track))
                    }
                }
                val next = builder.build()
                if (next != trackSelectionParameters) trackSelectionParameters = next
            }
            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError(error)
            }
        })
        addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioDecoderInitialized(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
                mutable.update { it.copy(audioDecoder = decoderName) }
            }
            override fun onVideoDecoderInitialized(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
                mutable.update { it.copy(videoDecoder = decoderName) }
            }
            override fun onAudioUnderrun(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                mutable.update { it.copy(audioUnderruns = it.audioUnderruns + 1) }
            }
            override fun onVideoInputFormatChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                val hdr = when (format.colorInfo?.colorTransfer) {
                    C.COLOR_TRANSFER_ST2084 -> "HDR10 / HDR10+"
                    C.COLOR_TRANSFER_HLG -> "HLG"
                    else -> "SDR"
                }
                mutable.update { it.copy(videoCodec = format.sampleMimeType, videoWidth = format.width,
                    videoHeight = format.height, videoBitrate = format.bitrate,
                    videoFrameRate = format.frameRate.takeIf { rate -> rate.isFinite() && rate in 1f..240f }
                        ?: plan?.sourceFrameRate ?: 0f,
                    videoHdr = hdr) }
            }

            override fun onAudioInputFormatChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                mutable.update { it.copy(audioCodec = format.sampleMimeType, audioChannels = format.channelCount) }
            }
        })
    }

    /** Also used by integration tests to inject a renderer failure at the real recovery boundary. */
    internal fun handlePlaybackError(error: PlaybackException) {
        val playWhenReady = player.playWhenReady
        android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=stream code=${error.errorCode}")
        val position = player.currentPosition.coerceAtLeast(0)
        val source = activeMediaSource
        val currentPlan = plan
        if (currentPlan != null && currentPlan.subtitleIndex >= 0 && playbackFailureIsSubtitle(error)) {
            // A broken subtitle is not a broken episode. Disable only the text renderer and
            // prepare the same video timeline again; the viewer can choose another subtitle later.
            plan = currentPlan.copy(subtitleIndex = -1, subtitleUrl = null)
            mutable.update { it.copy(subtitleIndex = -1, subtitleUnavailable = true, error = null, busy = true) }
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).clearOverridesOfType(C.TRACK_TYPE_TEXT).build()
            player.prepare()
            player.playWhenReady = foreground && playWhenReady
            return
        }
        if (source != null && plan != null && sameAccount() && localAudioFallback.tryEnable(error)) {
            // Do not renegotiate PlaybackInfo, stop the server session or lose the chosen
            // source/tracks. Recreating periods forces track mapping onto the local decoder.
            player.stop()
            player.clearMediaItems()
            player.setMediaSource(source, position)
            mutable.update { it.copy(error = null, busy = true, audioDecoder = "",
                fallback = playbackFallbackLabel(error, "LOCAL_FFMPEG")) }
            player.prepare()
            player.playWhenReady = foreground && playWhenReady
            return
        }
        // Retry transient transport failures without changing codecs or picture quality.
        networkRecoveries = networkRecoveriesSpent(networkRecoveries, networkRecoveryPosition, position)
        if (source != null && plan != null && sameAccount() && networkRecoveries < NETWORK_RECOVERY_LIMIT &&
            recoverablePlaybackFailure(error, plan?.direct == false)) {
            networkRecoveries++
            networkRecoveryPosition = position
            val ticket = generation
            mutable.update { it.copy(busy = true, error = null,
                fallback = playbackFallbackLabel(error, "RETRY_STREAM")) }
            recoveryJob?.cancel()
            recoveryJob = viewModelScope.launch {
                delay(networkRecoveries * 1000L)
                if (ticket == generation && activeMediaSource === source && sameAccount()) {
                    // Retry Media3's existing timeline. Do not send Stopped, create another
                    // server transcode, re-extract subtitles or start the movie from zero.
                    player.prepare()
                }
            }
            return
        }
        // Conversion requires a decoder/output/container failure, never just a slow network.
        val nextMode = playbackRecoveryCompatibility(compatibility, error.errorCode,
            playbackFailureIsVideo(error), directPlay = plan?.direct == true)
        if (nextMode != null) {
            compatibility = nextMode
            val label = playbackFallbackLabel(error, compatibility)
            android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=stream action=step-down $label")
            mutable.update { it.copy(fallback = label) }
            prepare(position, mode = compatibility, autoplay = playWhenReady)
        } else {
            mutable.update { it.copy(busy = false, playing = false, fallback = playbackFallbackLabel(error, "STOPPED"),
                error = container.appString(R.string.player_err_stopped)) }
        }
    }

    private fun selectTextTrack() {
        val current = plan ?: return
        // Keep the initial embedded selection enabled while the extractor discovers its groups.
        // Disabling it here discards an early PGS cue before onTracksChanged can identify it.
        if (player.currentTracks.isEmpty && current.direct &&
            current.subtitles.any { it.index == current.subtitleIndex && it.localBitmap }) return
        // A sidecar is switched on only once its memory/file cache is ready. While Media3 loads a selected
        // text file it holds the video loader back, so an episode started, ran out of picture after
        // a megabyte and waited for the server to extract the subtitle before it went on.
        val selected = playbackSubtitleOverride(current, player.currentTracks, readyTextIndices, vttFallbackIndices)
        val builder = player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, selected == null)
        if (selected != null) builder.setOverrideForType(selected)
        val next = builder.build()
        if (next != player.trackSelectionParameters) player.trackSelectionParameters = next
        if (player.playbackState == Player.STATE_READY && current.subtitles.any { it.index == current.subtitleIndex && it.localBitmap }) {
            mutable.update { it.copy(subtitleUnavailable = selected == null) }
        }
    }

    private fun textUrl(c: ServiceConnection, p: PlaybackPlan, index: Int, vtt: Boolean = index in vttFallbackIndices) = safePlaybackUrl(c.baseUrl,
        p.subtitles.firstOrNull { it.index == index }?.externalBitmapUrl ?:
            "Videos/${enc(p.item.id)}/${enc(p.sourceId)}/Subtitles/$index/Stream.${if (!vtt && p.subtitles.any { it.index == index && it.nativeAss }) "ass" else "vtt"}")

    /**
     * Fetches the chosen text file beside the video, not through it, and switches the track on when
     * the file has arrived. The picture never waits: at worst the first lines come a moment late.
     * Nothing is fetched while subtitles are off, so a viewer who never asked for them never gets
     * a warning about a missing one.
     */
    private fun loadChosenSubtitle(ticket: Int) {
        subtitleLoadJob?.cancel()
        val current = plan ?: return
        val c = connection ?: return
        val cache = subtitleCache ?: return
        val index = current.subtitleIndex
        if (current.subtitles.none { it.index == index && it.sidecar }) {
            selectTextTrack()
            return
        }
        // The sidecar opens this address from memory or a prepared private bitmap file.
        val url = textUrl(c, current, index) // the VTT one once an ASS original fell back
        if (index in readyTextIndices && cache.contains(url)) {
            selectTextTrack()
            return
        }
        // A previously selected language may have been evicted by the bounded cache.
        readyTextIndices -= index
        subtitleLoadJob = viewModelScope.launch {
            val started = android.os.SystemClock.elapsedRealtime()
            var fallback = false
            val loaded = withContext(Dispatchers.IO) { runCatching {
                if (current.subtitles.any { it.index == index && it.externalBitmapUrl != null }) cache.loadBitmap(url)
                else if (current.subtitles.any { it.index == index && it.nativeAss } && index !in vttFallbackIndices) {
                    // libass takes a bounded original. A larger or unreadable one still has the
                    // server's VTT (beta17's only route): plain text beats no text.
                    val original = runCatching { cache.load(url) }
                    if (original.isFailure) { cache.load(textUrl(c, current, index, vtt = true)); fallback = true }
                } else cache.load(url)
                if (!fallback && index !in vttFallbackIndices && current.subtitles.any { it.index == index && it.nativeAss }) {
                    val fonts = mutableListOf<SubtitleFont>()
                    var total = 0
                    val fontDeadline = android.os.SystemClock.elapsedRealtime() + 8_000
                    for (font in current.fonts) {
                        val remaining = fontDeadline - android.os.SystemClock.elapsedRealtime()
                        if (!isActive || total >= 8 * 1024 * 1024 || remaining <= 0) {
                            break
                        }
                        // Font bytes belong to the renderer and must not evict its chosen subtitle.
                        runCatching { cache.load(font.url, cacheResult = false, timeoutMs = remaining) }.getOrNull()?.let {
                            if (total + it.size <= 8 * 1024 * 1024) { total += it.size; fonts += SubtitleFont(font.name, it) }
                        }
                    }
                    if (ticket == generation) subtitleResources.fonts = fonts
                }
                Unit
            } }
            val waited = android.os.SystemClock.elapsedRealtime() - started
            android.util.Log.i("SpolePlayback", "source=${serviceKind.name} stage=subtitle ms=$waited " +
                (loaded.exceptionOrNull()?.let { "result=failed type=${it.javaClass.simpleName}" } ?: "result=ready"))
            if (ticket != generation || plan?.subtitleIndex != index) return@launch
            if (loaded.isSuccess) {
                if (fallback) vttFallbackIndices += index
                readyTextIndices += index
                mutable.update { it.copy(subtitleUnavailable = false) }
                selectTextTrack()
            } else mutable.update { it.copy(subtitleUnavailable = true) }
        }
    }

    private val mediaSession = androidx.media3.session.MediaSession.Builder(container.appContext, player).build()

    init {
        viewModelScope.launch {
            container.connectionRepository.sessionScope.collect { if (it != owner) ensureSession() }
        }
        reporter.launch {
            val progress = LivePlaybackProgressReporter(container, owner, client, PlaybackProgressClient(deviceId))
            try {
                while (true) {
                    val event = reports.next() ?: break
                    val failed = progress.send(event) ?: continue
                    // The session exists on the server from the first report onwards, and it is the
                    // only place that says whether the picture is being copied or re-encoded. Until
                    // it answers, the plan's reading of the transcoding URL stands.
                    val serverMode = if (failed || event.event == "/Stopped" || event.plan.mode == PlaybackMode.DIRECT_PLAY) null
                        else runCatching { client.serverPlaybackMode(event.connection, event.plan.sourceId) }.getOrNull()
                    withContext(Dispatchers.Main) {
                        val current = plan
                        if (current != null && current.sessionId == event.plan.sessionId) {
                            val corrected = serverMode?.takeIf { it != current.mode && current.mode != PlaybackMode.DIRECT_PLAY }
                            // The next progress report must carry the corrected PlayMethod too,
                            // otherwise the server's dashboard keeps calling a remux a transcode.
                            if (corrected != null) plan = current.copy(mode = corrected)
                            mutable.update { state -> state.copy(
                                warning = if (failed) container.appString(R.string.player_err_progress_not_saved) else null,
                                mode = corrected ?: state.mode) }
                        }
                    }
                }
            } finally {
                PlaybackProgressRuntime.release(reportLease)
                runCatching { PlaybackProgressScheduler.schedule(container.appContext, afterPlayback = true) }
                reporter.cancel()
            }
        }
        viewModelScope.launch {
            var ticks = 0
            var stalledNotice = 0
            while (isActive) {
                delay(1_000)
                val stalledMs = if (bufferingSince == 0L) 0L else android.os.SystemClock.elapsedRealtime() - bufferingSince
                // A spinner that says nothing for three quarters of a minute, and then gives up, is
                // the worst part of a stream that will not start. The wait is the same length; what
                // changed is that it now reports what it is doing while it waits.
                if (stalledMs == 0L) {
                    if (stalledNotice != 0) { stalledNotice = 0; mutable.update { it.copy(warning = null) } }
                } else if (stalledMs > STALL_NOTICE_MS && stalledNotice == 0) {
                    stalledNotice = 1
                    mutable.update { it.copy(warning = container.appString(R.string.player_stall_waiting, serviceKind.displayName)) }
                }
                // Waiting for bytes does not prove a codec failure. In particular, do not start
                // an expensive video transcode while a slow server is still delivering a segment.
                if (stalledMs > STALL_GIVE_UP_MS) {
                    report("/Stopped"); started = false
                    player.stop()
                    // The reason the server already gave, ahead of the generic advice. "Because
                    // this device cannot play the audio format" is something a household can act
                    // on; "the video is taking too long" is not.
                    val reason = playbackReasonFor(mutable.value)
                        ?.let { container.appString(R.string.player_reason_because, container.appString(it)) }
                    mutable.update { it.copy(busy = false, playing = false, warning = null,
                        error = listOfNotNull(reason, container.appString(R.string.player_stall_failed)).joinToString(" ")) }
                }
                if (connection != null && !sameAccount()) {
                    request?.cancel(); generation++; stopCurrent()
                    mutable.update { it.copy(busy = false, playing = false, error = container.appString(R.string.player_err_account_changed)) }
                    connection = null
                }
                if (plan != null) mutable.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
                startNextEpisodeCountdown()
                // A paused screen must not keep writing an old position over another device.
                if (++ticks % 10 == 0 && started && foreground && player.playWhenReady) report("/Progress")
            }
        }
    }

    /**
     * [audio] and [subtitle] are what the title page chose before playback started. They apply
     * only until a plan exists: from then on the player's own menus are the authority, so changing
     * quality or stepping to the next episode does not quietly undo a choice made inside the player.
     */
    fun open(id: String, audio: Int? = null, subtitle: Int? = null, sourceId: String? = null,
        source: ServiceKind = ServiceKind.JELLYFIN) {
        if (!ensureSession()) return
        kidsMode = owner.kidsMode
        if (rootId.isNotEmpty()) return
        require(source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
        if (isKidsBedtimeReached()) {
            mutable.value = PlayerScreenState(busy = false, source = source, bedtimeReached = true)
            return
        }
        autoplayChain = 0
        serviceKind = source
        mutable.update { it.copy(source = source, title = source.displayName) }
        rootId = id
        preferredAudio = audio
        preferredSubtitle = subtitle
        preferredSource = sourceId
        loadRoot()
    }

    private fun sameAccount(): Boolean {
        // Main thread, every second: never queue behind the progress reporter's guarded write.
        if (!container.connectionRepository.isCurrentNow(owner)) return false
        val c = connection ?: return false
        val saved = container.connectionRepository.get(serviceKind)
        val seerr = container.connectionRepository.get(ServiceKind.SEERR)
        return saved.token == c.token && saved.baseUrl == c.baseUrl && saved.userId == c.userId &&
            seerr.token == seerrConnection?.token && seerr.baseUrl == seerrConnection?.baseUrl
    }

    /** When the viewer asked to play; cleared once the first frame is ready. Debug timing only. */
    private var openStartedNanos = 0L

    private fun loadRoot() {
        if (!ensureSession()) return
        openStartedNanos = System.nanoTime()
        request?.cancel()
        mutable.update { it.copy(busy = true, error = null) }
        request = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val c = container.connectionRepository.get(serviceKind)
                    val seerr = container.connectionRepository.get(ServiceKind.SEERR)
                    // The capability report never decides anything here; do not wait for it.
                    viewModelScope.launch(Dispatchers.IO) { runCatching { client.announceCapabilities(c) } }
                    // These used to run one after another: five round trips before PlaybackInfo.
                    // They are independent, so the wait is now the slowest of them. The rule is
                    // unchanged: nothing plays until the account is confirmed as the viewer's own.
                    coroutineScope {
                        val verified = async { client.verify(c, announce = false) }
                        val mediaAccount = async { runCatching { container.accountProfileClient.load(c) } }
                        val seerrAccount = async {
                            seerr.takeIf { it.token.isNotBlank() }?.let {
                                runCatching { container.accountProfileClient.load(it) }.getOrNull()
                            }
                        }
                        val knownUser = c.userId.takeIf(String::isNotBlank)
                        val early = knownUser?.let { user -> async { runCatching { client.item(c, user, rootId) } } }
                        val id = verified.await()
                        val access = ViewerAccess(seerr.token.isNotBlank(), buildMap {
                            put(serviceKind, mediaAccount.await().getOrThrow())
                            seerrAccount.await()?.let { put(ServiceKind.SEERR, it) }
                        })
                        if (access.ownMediaUser(serviceKind) != id) serviceError(R.string.player_err_pick_personal_account)
                        // verify() refuses a stored id that differs from the server's, so the early
                        // read was made as this same account.
                        val item = early?.takeIf { knownUser.equals(id, ignoreCase = true) }?.await()?.getOrThrow()
                            ?: client.item(c, id, rootId)
                        Triple(c, seerr, id) to item
                    }
                }
                connection = result.first.first; seerrConnection = result.first.second; userId = result.first.third
                check(sameAccount())
                choose(container.localPlaybackStore.resume(result.first.first, result.second, fromServer = true))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=open type=${error.javaClass.simpleName}")
                mutable.update { it.copy(busy = false, error = error.readableMessage(container.appContext)
                    ?: container.appString(R.string.player_err_open)) }
            }
        }
    }

    fun choose(item: PlayableItem) {
        previewJob?.cancel(); previewJob = null; previewManifest = null
        networkRecoveries = 0
        countdownJob?.cancel()
        nextEpisodeJob?.cancel()
        nextEpisodeCancelled = false
        val options = playbackOptions()
        mutable.update { it.copy(nextEpisode = null, nextEpisodeResolved = false, nextEpisodeCountdown = null, nextEpisodeDismissed = false, bedtimeReached = false,
            nextEpisodeOfferEnabled = options.showNextEpisode, nextEpisodeLeadSeconds = options.nextEpisodeLeadSeconds) }
        if (item.type in setOf("Series", "Season")) {
            if (folders.lastOrNull()?.id != item.id) folders += item
            parent = item; offset = 0; selected = null
            mutable.update { it.copy(title = item.title, subtitle = "Vel ${if (item.type == "Series") "sesong" else "episode"}",
                season = null, episode = null, logoUrl = item.logoUrl,
                choices = emptyList(), browsing = true, hasMore = false, error = null) }
            loadChildren()
        } else {
            selected = item; compatibility = PlaybackCompatibility.DIRECT
            localAudioFallback.reset()
            mutable.update { it.copy(fallback = "", audioDecoder = "", videoDecoder = "", audioUnderruns = 0) }
            mutable.update { it.copy(title = item.title, subtitle = item.subtitle, season = item.season,
                episode = item.episode, logoUrl = item.logoUrl, browsing = false, choices = emptyList(),
                positionMs = item.resumeMs, durationMs = item.durationMs, ended = false, error = null, chapters = item.chapters, itemId = item.id, trickplay = null) }
            val start = playbackStartPosition(item.resumeMs, item.durationMs, item.played,
                playbackOptions().autoResume)
            mutable.update { it.copy(awaitingResume = start == null) }
            if (start == null) mutable.update { it.copy(busy = false) }
            else prepare(start)
            loadNextEpisode(item)
        }
    }

    fun resume(fromBeginning: Boolean) {
        if (!state.value.awaitingResume) return
        mutable.update { it.copy(awaitingResume = false) }
        prepare(if (fromBeginning) 0 else selected?.resumeMs ?: 0)
    }

    fun back(): Boolean {
        request?.cancel(); generation++; stopCurrent()
        if (folders.isEmpty()) return false
        if (state.value.browsing) {
            if (folders.size <= 1) return false
            folders.removeAt(folders.lastIndex)
        }
        mutable.update { it.copy(awaitingResume = false) }
        choose(folders.last())
        return true
    }

    fun loadChildren() {
        val c = connection ?: return
        val folder = parent ?: return
        request = viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) { client.children(c, userId, folder, offset) }
                check(sameAccount())
                offset += 100
                mutable.update { current ->
                    val merged = (current.choices + result.first).distinctBy(PlayableItem::id)
                    current.copy(busy = false, choices = merged, hasMore = result.second)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(busy = false, error = container.appString(R.string.player_err_episodes)) } }
        }
    }

    private fun autoBitrate(): Int {
        val manager = container.appContext.getSystemService(ConnectivityManager::class.java)
        val network = manager.getNetworkCapabilities(manager.activeNetwork)
        return automaticPlaybackBitrate(network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true)
    }

    private fun prepare(position: Long, audio: Int? = null, subtitle: Int? = null, mode: PlaybackCompatibility = compatibility, autoplay: Boolean = true,
    ) {
        if (!ensureSession()) return
        val c = connection ?: return
        val item = selected ?: return
        val previous = plan?.takeIf { it.item.id == item.id }
        val audioChoice = audio ?: previous?.audioIndex ?: preferredAudioKey?.let { key ->
            (previous?.audio ?: plan?.audio)?.firstOrNull { it.key == key }?.index
        } ?: preferredAudio.takeIf { item.id == rootId }
        val subtitleChoice = subtitle ?: previous?.subtitleIndex ?: preferredSubtitleKey?.let { key ->
            (previous?.subtitles ?: plan?.subtitles)?.firstOrNull { it.key == key }?.index
        } ?: preferredSubtitle.takeIf { item.id == rootId }
        request?.cancel()
        val ticket = ++generation
        stopCurrent()
        mutable.update { it.copy(busy = true, error = null, ended = false, positionMs = position, subtitleUnavailable = false) }
        request = viewModelScope.launch {
            try {
                val (prepared, detected) = withContext(Dispatchers.IO) {
                    check(sameAccount())
                    client.prepare(c, userId, item, state.value.quality.takeIf { it > 0 } ?: autoBitrate(),
                        // The version the title page picked, until a plan exists and the player owns it.
                        audioChoice, subtitleChoice, mode, previous?.sourceId ?: preferredSource.takeIf { item.id == rootId },
                        playbackOptions().preferredSubtitleLanguage,
                        playbackOptions().fallbackSubtitleLanguage) to deviceCapabilities.snapshot()
                }
                if (ticket != generation || !sameAccount()) return@launch
                plan = prepared
                compatibility = prepared.compatibility
                mutable.update { it.copy(videoFrameRate = prepared.sourceFrameRate, audioDecoder = "", videoDecoder = "",
                    advertisedAudio = detected.audio.joinToString(" · ") { audio ->
                        audio.codec + " " + audio.channels + (if (audio.passthrough) " pass" else "") },
                    advertisedVideo = detected.video.joinToString(" · ") { video ->
                        video.codec + " " + video.width + "×" + video.height }) }
                stopped = false
                val http = app.reelstack.data.network.HttpTransport.sharedClient.newBuilder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(if (serviceKind == ServiceKind.EMBY) 45 else 20, TimeUnit.SECONDS).build()
                // The text file loads beside the picture now, so it may take the time the server
                // needs. Emby extracts every text track from the file on the first request and
                // keeps nothing when the client gives up; at 8 seconds a subtitle-heavy episode
                // never got its text, however many times it was chosen.
                val cache = SubtitleMemoryCache(http.newBuilder().readTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(90, TimeUnit.SECONDS).build(), client.headers(c),
                    bitmapDirectory = container.appContext.cacheDir.resolve("playback-bitmap-subtitles")) {
                    viewModelScope.launch {
                        if (ticket == generation) mutable.update { it.copy(subtitleUnavailable = true) }
                    }
                }
                subtitleCache = cache
                readyTextIndices.clear(); vttFallbackIndices.clear()
                val upstream = cache.factory(OkHttpDataSource.Factory(http).setDefaultRequestProperties(client.headers(c)))
                val dataSource = ResolvingDataSource.Factory(upstream) { spec ->
                    spec.withUri(safePlaybackUrl(c.baseUrl, spec.uri.toString()).toUri())
                }
                val media = MediaItem.Builder().setUri(prepared.url).setMediaId(item.id)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).setSubtitle(item.subtitle).build())
                    .setMimeType(if (prepared.direct) null else MimeTypes.APPLICATION_M3U8)
                media.setSubtitleConfigurations(prepared.subtitles.filter { it.sidecar }.flatMap { track ->
                    listOf(MediaItem.SubtitleConfiguration.Builder(textUrl(c, prepared, track.index, vtt = false).toUri())
                        .setId(subtitleSidecarId(track.index)).setMimeType(when {
                            track.externalBitmapUrl != null -> track.externalBitmapMime ?: SPOLE_SUP_MIME
                            track.nativeAss -> MimeTypes.TEXT_SSA
                            else -> MimeTypes.TEXT_VTT
                        }).setLanguage(track.language)
                        .setSelectionFlags(if (track.index == prepared.subtitleIndex) C.SELECTION_FLAG_DEFAULT else 0).build()) +
                        // Lazy: loads only if the original ASS cannot be used and this one is chosen.
                        listOfNotNull(MediaItem.SubtitleConfiguration.Builder(textUrl(c, prepared, track.index, vtt = true).toUri())
                            .setId(subtitleSidecarId(track.index, vtt = true)).setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage(track.language).build().takeIf { track.nativeAss && track.externalBitmapUrl == null })
                })
                // Sidecar text starts off until its bytes arrive. Embedded PGS must be enabled
                // on the first selection so an early bitmap is not discarded by the extractor.
                val initialPgs = prepared.subtitles.firstOrNull { it.index == prepared.subtitleIndex && it.localBitmap }
                // Direct play: when its language says which one it is, Media3's first pick is
                // already the planned audio. Correcting the pick afterwards enables a new track in
                // the file, and with loadOnlySelectedTracks that throws the buffer away.
                val plannedAudio = prepared.audio.firstOrNull { it.index == prepared.audioIndex }?.language
                    ?.takeIf { language -> language.isNotBlank() && prepared.direct &&
                        prepared.audio.count { it.language.equals(language, ignoreCase = true) } == 1 }
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().clearOverrides()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, initialPgs == null)
                    .setPreferredTextLanguages(*listOfNotNull(initialPgs?.language).toTypedArray())
                    .setSelectUndeterminedTextLanguage(initialPgs != null)
                    .setPreferredAudioLanguages(*listOfNotNull(plannedAudio).toTypedArray()).build()
                val source = playbackMediaSource(dataSource, media.build())
                activeMediaSource = source
                player.setMediaSource(source, position.coerceAtLeast(0))
                mutable.update { it.copy(audio = prepared.audio, subtitles = prepared.subtitles, audioIndex = prepared.audioIndex,
                    trickplay = (prepared.item.trickplay.ifEmpty { previewManifest?.takeIf { cached -> cached.first == item.id }?.second.orEmpty() })
                        .filter { grid -> grid.mediaSourceId == prepared.sourceId }.minByOrNull { grid -> grid.width },
                    subtitleIndex = prepared.subtitleIndex, mode = prepared.mode, transcodeReasons = prepared.transcodeReasons) }
                prepared.audio.firstOrNull { it.index == prepared.audioIndex }?.let { preferredAudioKey = it.key }
                prepared.subtitles.firstOrNull { it.index == prepared.subtitleIndex }?.let { preferredSubtitleKey = it.key }
                player.prepare(); player.playWhenReady = foreground && autoplay
                loadChosenSubtitle(ticket)
                loadPreviewManifest(c, item)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=prepare type=${error.javaClass.simpleName}")
                if (ticket == generation) mutable.update { it.copy(busy = false, playing = false,
                    error = error.readableMessage(container.appContext)
                        ?: container.appString(R.string.player_err_start)) }
            }
        }
    }

    /** Optional images load beside playback, once per chosen item, never for an entire shelf. */
    private fun loadPreviewManifest(c: ServiceConnection, item: PlayableItem) {
        if (kidsMode || c.kind != ServiceKind.JELLYFIN || item.trickplay.isNotEmpty() ||
            previewManifest?.first == item.id || previewJob?.isActive == true) return
        previewJob = viewModelScope.launch {
            val grids = withContext(Dispatchers.IO) { runCatching { client.trickplay(c, userId, item.id) }.getOrDefault(emptyList()) }
            if (!sameAccount() || selected?.id != item.id) return@launch
            previewManifest = item.id to grids
            val sourceId = plan?.sourceId
            mutable.update { it.copy(trickplay = grids.filter { grid -> grid.mediaSourceId == sourceId }.minByOrNull { grid -> grid.width }) }
        }
    }

    /**
     * Loads what comes next while the current episode is still playing, so the offer at the end
     * appears immediately rather than after a round trip the viewer has to sit and watch.
     */
    private fun loadNextEpisode(item: PlayableItem) {
        nextEpisodeJob?.cancel()
        mutable.update { it.copy(nextEpisode = null, nextEpisodeResolved = false, nextEpisodeCountdown = null, segments = emptyList()) }
        val c = connection ?: return
        if (item.type != "Episode") return
        nextEpisodeJob = viewModelScope.launch {
            val response = runCatching { withContext(Dispatchers.IO) { client.nextEpisode(c, userId, item) } }
            if (!isActive || selected?.id != item.id) return@launch
            if (response.isSuccess) {
                val next = response.getOrNull()
                mutable.update { if (it.itemId != item.id) it else it.copy(nextEpisode = next, nextEpisodeResolved = true) }
                if (next != null && state.value.ended) startNextEpisodeCountdown()
            }
            val marked = runCatching { withContext(Dispatchers.IO) { client.segments(c, userId, item.id) } }
                .getOrDefault(emptyList())
            if (!isActive || selected?.id != item.id || marked.isEmpty()) return@launch
            mutable.update { if (it.itemId != item.id) it else it.copy(segments = marked) }
        }
    }

    /**
     * Counts only after the current episode has ended. The offer can appear near the credits, but
     * a countdown must never cut the episode short; pausing and leaving the foreground stop it.
     *
     * Long enough to read the title and decide, short enough that a series does not stop dead
     * between episodes. Anything that puts the viewer back in charge — a press of Cancel, closing
     * the player, picking a different episode — stops it, and it never starts at all when there is
     * nothing to play next.
     */
    private fun startNextEpisodeCountdown() {
        if (isKidsBedtimeReached()) {
            countdownJob?.cancel()
            mutable.update { it.copy(bedtimeReached = true, nextEpisodeCountdown = null) }
            return
        }
        val options = playbackOptions()
        if (countdownJob?.isActive == true || nextEpisodeCancelled || !foreground ||
            !options.autoPlayNextEpisode || !state.value.canCountDownNextEpisode()) return
        // Three on the trot, then the offer waits for a hand.
        if (kidsMode && !kidsPreferences.read(container.connectionRepository.activeProfileId).canAutoplay(autoplayChain)) return
        val episodeId = state.value.itemId
        val seconds = state.value.nextEpisodeCountdown ?: options.nextEpisodeDelaySeconds
        if (state.value.nextEpisodeCountdown == null)
            mutable.update { it.copy(nextEpisodeCountdownTotalSeconds = seconds.coerceIn(1, 60)) }
        countdownJob = viewModelScope.launch {
            for (second in seconds.coerceIn(1, 60) downTo 1) {
                while (isActive && foreground && !state.value.playing && !state.value.ended) delay(200)
                if (!state.value.canCountDownNextEpisode()) {
                    mutable.update { it.copy(nextEpisodeCountdown = null) }
                    return@launch
                }
                mutable.update { it.copy(nextEpisodeCountdown = second) }
                delay(1_000)
                if (!isActive || !foreground || nextEpisodeCancelled || state.value.itemId != episodeId ||
                    state.value.nextEpisode == null || !sameAccount()) return@launch
            }
            while (isActive && foreground && !state.value.playing && !state.value.ended) delay(200)
            if (state.value.canCountDownNextEpisode()) playNext(fromCountdown = true)
            else mutable.update { it.copy(nextEpisodeCountdown = null) }
        }
    }

    /** Starts the next episode now, whether the countdown ran out or someone pressed the button. */
    fun playNext(fromCountdown: Boolean = false) {
        if (!foreground || state.value.busy || state.value.error != null || !sameAccount()) return
        if (isKidsBedtimeReached()) {
            countdownJob?.cancel()
            mutable.update { it.copy(bedtimeReached = true, nextEpisodeCountdown = null) }
            return
        }
        val next = state.value.nextEpisode ?: return
        // Only an episode that started by itself extends the chain. A press is a fresh decision.
        autoplayChain = if (fromCountdown) autoplayChain + 1 else 0
        countdownJob?.cancel()
        mutable.update { it.copy(nextEpisode = null, nextEpisodeCountdown = null) }
        report("/Stopped"); started = false
        plan?.let { current -> connection?.let { c ->
            container.localPlaybackStore.record(c, current.item, player.currentPosition, state.value.durationMs, completed = true)
        } }
        choose(next)
    }

    /** Jumps past the title sequence or into the next episode's slot at the end of the credits. */
    fun skipSegment() {
        val segment = state.value.activeSegment() ?: return
        seek(segment.endMs)
    }

    /** Keeps the player where it is. The offer stays on screen; only the clock stops. */
    fun cancelNextEpisode() {
        countdownJob?.cancel()
        nextEpisodeCancelled = true
        mutable.update { it.copy(nextEpisodeCountdown = null, nextEpisodeDismissed = !it.ended) }
    }

    fun retry() { networkRecoveries = 0; if (connection == null || selected == null && parent == null) loadRoot()
        else if (state.value.browsing) loadChildren() else prepare(state.value.positionMs) }
    fun audio(index: Int) { if (!state.value.busy && state.value.audio.any { it.index == index }) {
        preferredAudioKey = state.value.audio.first { it.index == index }.key
        prepare(player.currentPosition, audio = index, autoplay = player.playWhenReady)
    } }
    fun subtitles(index: Int) {
        if (state.value.busy || index != -1 && state.value.subtitles.none { it.index == index }) return
        val current = plan ?: return
        preferredSubtitleKey = current.subtitles.firstOrNull { it.index == index }?.key
        val c = connection ?: return
        val oldIsImage = current.subtitles.any { it.index == current.subtitleIndex && !it.sidecar && !it.localBitmap }
        val newIsImage = current.subtitles.any { it.index == index && !it.sidecar && !it.localBitmap }
        if (oldIsImage || newIsImage) {
            prepare(player.currentPosition, subtitle = index, autoplay = player.playWhenReady)
        } else {
            // Local subtitle selection must not renegotiate playback with the server.
            plan = current.copy(subtitleIndex = index, subtitleUrl = current.subtitles.firstOrNull { it.index == index && it.sidecar }
                ?.let { textUrl(c, current, index) })
            mutable.update { it.copy(subtitleIndex = index,
                subtitleUnavailable = it.subtitleUnavailable && index == current.subtitleIndex) }
            // The menu choice shows at once; the text itself follows when its file is here, so
            // switching language mid-episode no longer stalls the picture either.
            loadChosenSubtitle(generation)
            report("/Progress")
        }
    }
    fun quality(bitrate: Int) {
        if (state.value.busy || bitrate !in listOf(0, 80_000_000, 20_000_000, 4_000_000, 2_000_000)) return
        val autoplay = player.playWhenReady || state.value.error != null
        mutable.update { it.copy(quality = bitrate) }; prepare(player.currentPosition, autoplay = autoplay)
    }
    fun toggle() {
        if (!ensureSession()) return
        if (state.value.error != null || state.value.busy && plan == null) return
        if (player.playbackState == Player.STATE_ENDED) { prepare(0); return }
        if (player.playWhenReady) player.pause() else player.play()
    }
    fun seek(position: Long) {
        if (!ensureSession()) return
        if (plan == null) return
        val target = position.coerceIn(0, state.value.durationMs.coerceAtLeast(0))
        mutable.update { it.copy(positionMs = target, ended = false) }
        if (!state.value.canCountDownNextEpisode()) {
            countdownJob?.cancel()
            mutable.update { it.copy(nextEpisodeCountdown = null) }
        }
        player.seekTo(target)
    }
    fun background() {
        val wasPlaying = player.playWhenReady
        foreground = false; countdownJob?.cancel(); player.pause()
        // Save synchronously before onStop returns; Media3 dispatches its pause listener later.
        if (started && wasPlaying) report("/Progress")
    }
    fun foreground() { if (!ensureSession()) return; foreground = true; if (state.value.ended) startNextEpisodeCountdown() }
    fun fallbackUrl(): String? = if (ensureSession()) connection?.let { safePlaybackUrl(it.baseUrl, "web/index.html") + "#!/details?id=${enc(selected?.id ?: rootId)}" } else null

    private fun ensureSession(): Boolean {
        if (container.connectionRepository.isCurrentNow(owner)) return true
        request?.cancel(); generation++; stopCurrent(); connection = null
        mutable.value = PlayerScreenState(busy = false, error = container.appString(R.string.player_err_account_changed))
        return false
    }

    private fun report(event: String) {
        val c = connection ?: return
        val current = plan ?: return
        if (event == "/Stopped") {
            if (stopped) return
            stopped = true
        } else if (!started) return
        val journalId = if (started) container.localPlaybackStore.record(c, current.item, player.currentPosition.coerceAtLeast(0),
            player.duration.takeIf { it > 0 } ?: current.item.durationMs, player.playbackState == Player.STATE_ENDED) else null
        reports.offer(PlaybackReport(c, current, event, player.currentPosition.coerceAtLeast(0), !player.playWhenReady, journalId,
            player.playbackState == Player.STATE_ENDED, System.currentTimeMillis()))
    }
    private fun stopCurrent() {
        recoveryJob?.cancel(); recoveryJob = null
        subtitleLoadJob?.cancel(); subtitleLoadJob = null; subtitleCache?.close(); subtitleCache = null; readyTextIndices.clear(); vttFallbackIndices.clear()
        subtitleResources.fonts = emptyList()
        countdownJob?.cancel()
        mutable.update { it.copy(nextEpisodeCountdown = null) }
        report("/Stopped"); started = false; plan = null; activeMediaSource = null
        player.stop(); player.clearMediaItems()
    }
    override fun onCleared() {
        generation++; request?.cancel(); stopCurrent(); mediaSession.release(); player.release(); reports.close()
        super.onCleared()
    }
}
