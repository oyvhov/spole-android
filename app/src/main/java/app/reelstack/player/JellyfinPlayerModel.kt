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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.DecoderReuseEvaluation
import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.network.readableMessage
import app.reelstack.data.network.serviceError
import app.reelstack.data.repository.DeviceIdentity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class PlayerScreenState(
    val title: String = "Spole", val subtitle: String = "", val busy: Boolean = true,
    val error: String? = null, val warning: String? = null,
    val choices: List<PlayableItem> = emptyList(), val browsing: Boolean = false, val hasMore: Boolean = false,
    val playing: Boolean = false, val ended: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0,
    val audio: List<PlaybackTrack> = emptyList(), val subtitles: List<PlaybackTrack> = emptyList(),
    val audioIndex: Int? = null, val subtitleIndex: Int = -1,
    val mode: PlaybackMode = PlaybackMode.DIRECT_PLAY,
    /** The server's own reason codes, mapped to a sentence only when they are shown. */
    val transcodeReasons: List<String> = emptyList(),
    val quality: Int = 0,
    /** Playback intent stays true while a seek buffers; playing alone cannot distinguish a pause. */
    val playWhenReady: Boolean = false,
    val awaitingResume: Boolean = false,
    val chapters: List<PlaybackChapter> = emptyList(),
    val itemId: String = "",
    val logoUrl: String? = null,
    /** The numbers behind [subtitle], so the header can write them out rather than show "S03 E01". */
    val season: Int? = null,
    val episode: Int? = null,
    /**
     * What follows this episode, and how long is left before it starts on its own. Null means
     * there is nothing after it — a film, the last episode, or a server that could not say.
     */
    val nextEpisode: PlayableItem? = null,
    val nextEpisodeCountdown: Int? = null,
    val nextEpisodeCountdownTotalSeconds: Int = 12,
    val nextEpisodeOfferEnabled: Boolean = true,
    val nextEpisodeLeadSeconds: Int = 60,
    val nextEpisodeDismissed: Boolean = false,
    /** Title sequences and closing credits the server has marked, if anything has marked them. */
    val segments: List<PlaybackSegment> = emptyList(),
    val source: ServiceKind = ServiceKind.JELLYFIN,
    val videoCodec: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoBitrate: Int = 0,
    val videoFrameRate: Float = 0f,
    val videoHdr: String = "SDR",
    /**
     * The audio as it arrives, which is not the audio the file holds.
     *
     * The panel used to print the source track here -- "English EAC3 5.1" -- directly under a line
     * saying the server was converting the audio because this device cannot play that format. The
     * two contradicted each other, and the one in a diagnostics panel that must be true is the one
     * describing what is actually being decoded.
     */
    val audioCodec: String? = null,
    val audioChannels: Int = 0,
    /**
     * The audio codecs this device told the server it can play, and up to how many channels.
     *
     * When a server answers "this device cannot play the audio format", the only way to tell a
     * correct refusal from a detection bug is to see what was actually claimed -- and until now
     * that was only visible by reading the source. It is one line in Stats for Nerds instead.
     */
    val advertisedAudio: String = "",
    /** The video codecs claimed, with the largest frame each was accepted at. */
    val advertisedVideo: String = "",
) {
    /** Kept so every reader that only cares whether the file is untouched still compiles. */
    val direct: Boolean get() = mode == PlaybackMode.DIRECT_PLAY
}

/**
 * The marked stretch the playhead is inside right now, if any.
 *
 * A small guard at each end keeps the offer from flickering on and off around the boundary, and
 * from appearing for the last half second of a title sequence nobody would bother skipping.
 */
fun PlayerScreenState.activeSegment(): PlaybackSegment? = segments.firstOrNull { segment ->
    positionMs >= segment.startMs && positionMs < segment.endMs - 1_500
}

/**
 * How long a stall is allowed to stay silent, try something else, and finally give up.
 *
 * The total wait is unchanged. What is new is that the first two thresholds exist at all: the
 * player used to sit on a spinner for 45 seconds, quietly switch to a compatibility stream, and sit
 * on the spinner for another 45 before saying anything.
 */
private const val STALL_NOTICE_MS = 8_000L
private const val STALL_FALLBACK_MS = 18_000L
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
    /** Read once per stream, not per frame: enumerating codecs is not free. */
    private val advertisedAudio: () -> String = {
        runCatching {
            deviceCapabilities.snapshot().audio.joinToString(" · ") { audio ->
                audio.codec + " " + audio.channels + (if (audio.passthrough) " pass" else "")
            }
        }.getOrDefault("")
    }

    private val advertisedVideo: () -> String = {
        runCatching {
            deviceCapabilities.snapshot().video.joinToString(" · ") { video ->
                video.codec + " " + video.width + "×" + video.height
            }
        }.getOrDefault("")
    }

    private var connection: ServiceConnection? = null
    private var serviceKind = ServiceKind.JELLYFIN
    private var seerrConnection: ServiceConnection? = null
    private var userId = ""
    private var rootId = ""
    private var selected: PlayableItem? = null
    private var parent: PlayableItem? = null
    private val folders = mutableListOf<PlayableItem>()
    private var offset = 0
    private var plan: PlaybackPlan? = null
    private var started = false
    private var stopped = false
    private var compatible = false
    private var networkRecoveries = 0
    private var foreground = true
    private var request: Job? = null
    private var generation = 0
    private var bufferingSince = 0L
    private var nextEpisodeJob: Job? = null
    private var countdownJob: Job? = null
    private var nextEpisodeCancelled = false
    private var preferredAudio: Int? = null
    private var preferredSubtitle: Int? = null
    private var preferredAudioKey: String? = null
    private var preferredSubtitleKey: String? = null
    private var preferredSource: String? = null
    private var subtitleCache: SubtitleMemoryCache? = null
    private var subtitleWarmJob: Job? = null
    private var subtitleWarmedSession: String? = null
    private val reporter = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reports = Channel<Report>(Channel.UNLIMITED)
    private data class Report(val connection: ServiceConnection, val plan: PlaybackPlan, val event: String, val position: Long, val paused: Boolean)

    val player: ExoPlayer = ExoPlayer.Builder(container.appContext,
        DefaultRenderersFactory(container.appContext).setEnableDecoderFallback(true)).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        setHandleAudioBecomingNoisy(true)
        setSeekBackIncrementMs(10_000)
        setSeekForwardIncrementMs(10_000)
        addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                mutable.update { it.copy(playWhenReady = playWhenReady) }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mutable.update { it.copy(playing = isPlaying) }
                if (isPlaying && !started) { started = true; report(""); warmSubtitle() }
                else if (started) report("/Progress")
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                bufferingSince = if (playbackState == Player.STATE_BUFFERING) android.os.SystemClock.elapsedRealtime() else 0L
                mutable.update { it.copy(busy = playbackState == Player.STATE_BUFFERING, ended = playbackState == Player.STATE_ENDED,
                    durationMs = duration.takeIf { value -> value > 0 } ?: it.durationMs) }
                if (playbackState == Player.STATE_ENDED) {
                    report("/Stopped"); started = false
                    startNextEpisodeCountdown()
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
                android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=stream code=${error.errorCode}")
                val position = currentPosition.coerceAtLeast(0)
                // Emby direct streams can legitimately take longer to deliver the next HLS
                // segment on a TV. Give the connection retries before treating it as a codec
                // failure, and allow the buffering watchdog below to request a server stream.
                if (networkRecoveries < 2 && recoverablePlaybackFailure(error, plan?.direct == false || serviceKind == ServiceKind.EMBY)) {
                    networkRecoveries++
                    prepare(position, autoplay = playWhenReady, retryDelayMillis = networkRecoveries * 1000L)
                    return
                }
                // A single compatible-stream fallback, never an endless retry loop or a bitrate increase.
                // Direct HLS failures are not always reported as decoder failures, especially on TV.
                if (!compatible && (error.errorCode in setOf(PlaybackException.ERROR_CODE_DECODING_FAILED,
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED)
                    || plan?.direct == true)) {
                    compatible = true
                    prepare(position, forceCompatible = true, autoplay = playWhenReady)
                } else {
                    mutable.update { it.copy(busy = false, playing = false,
                        error = container.appString(R.string.player_err_stopped)) }
                }
            }
        })
        addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
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
                    videoHeight = format.height, videoBitrate = format.bitrate, videoFrameRate = format.frameRate,
                    videoHdr = hdr) }
            }

            override fun onAudioInputFormatChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                mutable.update { it.copy(audioCodec = format.sampleMimeType, audioChannels = format.channelCount,
                    advertisedAudio = advertisedAudio(), advertisedVideo = advertisedVideo()) }
            }
        })
    }

    private fun selectTextTrack() {
        val current = plan ?: return
        val enabled = current.subtitles.any { it.index == current.subtitleIndex && it.isText }
        val group = player.currentTracks.groups.firstOrNull { group -> group.type == C.TRACK_TYPE_TEXT &&
            (0 until group.length).any { group.getTrackFormat(it).id.orEmpty().substringAfterLast(':') == "spole-subtitle-${current.subtitleIndex}" } }
        val builder = player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
        if (enabled && group != null) builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
        val next = builder.build()
        if (next != player.trackSelectionParameters) player.trackSelectionParameters = next
    }

    private fun textUrl(c: ServiceConnection, p: PlaybackPlan, index: Int) = safePlaybackUrl(c.baseUrl,
        "Videos/${enc(p.item.id)}/${enc(p.sourceId)}/Subtitles/$index/Stream.vtt")

    /** Warm one likely text track after video starts; never delay video to fetch every language. */
    private fun warmSubtitle() {
        val current = plan ?: return
        val c = connection ?: return
        val cache = subtitleCache ?: return
        if (subtitleWarmedSession == current.sessionId) return
        subtitleWarmedSession = current.sessionId
        val text = current.subtitles.filter { it.isText }
        val locale = java.util.Locale.getDefault()
        val track = text.firstOrNull { it.index == current.subtitleIndex }
            ?: text.firstOrNull { it.language in setOf(locale.language, runCatching { locale.isO3Language }.getOrNull()) }
            ?: text.firstOrNull() ?: return
        val url = textUrl(c, current, track.index)
        subtitleWarmJob = viewModelScope.launch(Dispatchers.IO) { runCatching { cache.load(url) } }
    }

    private val mediaSession = androidx.media3.session.MediaSession.Builder(container.appContext, player).build()

    init {
        reporter.launch {
            for (event in reports) {
                val failed = runCatching { client.report(event.connection, event.plan, event.event, event.position, event.paused) }.isFailure
                withContext(Dispatchers.Main) {
                    if (plan?.sessionId == event.plan.sessionId) mutable.update { it.copy(warning =
                        if (failed) container.appString(R.string.player_err_progress_not_saved) else null) }
                }
            }
            reporter.cancel()
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
                if (stalledMs > STALL_FALLBACK_MS && !compatible) {
                    compatible = true
                    stalledNotice = 2
                    val position = player.currentPosition.coerceAtLeast(0)
                    android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=buffer action=force-compatible")
                    bufferingSince = 0L
                    mutable.update { it.copy(warning = container.appString(R.string.player_stall_fallback)) }
                    prepare(position, forceCompatible = true, autoplay = player.playWhenReady)
                    continue
                }
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
                if (++ticks % 10 == 0 && started) report("/Progress")
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
        if (rootId.isNotEmpty()) return
        require(source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
        serviceKind = source
        mutable.update { it.copy(source = source, title = source.displayName) }
        rootId = id
        preferredAudio = audio
        preferredSubtitle = subtitle
        preferredSource = sourceId
        loadRoot()
    }

    private fun sameAccount(): Boolean {
        val c = connection ?: return false
        val saved = container.connectionRepository.get(serviceKind)
        val seerr = container.connectionRepository.get(ServiceKind.SEERR)
        return saved.token == c.token && saved.baseUrl == c.baseUrl && saved.userId == c.userId &&
            seerr.token == seerrConnection?.token && seerr.baseUrl == seerrConnection?.baseUrl
    }

    private fun loadRoot() {
        request?.cancel()
        mutable.update { it.copy(busy = true, error = null) }
        request = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val c = container.connectionRepository.get(serviceKind)
                    val seerr = container.connectionRepository.get(ServiceKind.SEERR)
                    val id = client.verify(c)
                    val jellyfinAccount = container.accountProfileClient.load(c)
                    val seerrAccount = seerr.takeIf { it.token.isNotBlank() }?.let {
                        runCatching { container.accountProfileClient.load(it) }.getOrNull()
                    }
                    val access = ViewerAccess(seerr.token.isNotBlank(), buildMap {
                        put(serviceKind, jellyfinAccount); seerrAccount?.let { put(ServiceKind.SEERR, it) }
                    })
                    if (access.ownMediaUser(serviceKind) != id) serviceError(R.string.player_err_pick_personal_account)
                    Triple(c, seerr, id) to client.item(c, id, rootId)
                }
                connection = result.first.first; seerrConnection = result.first.second; userId = result.first.third
                check(sameAccount())
                choose(container.localPlaybackStore.resume(result.first.first, result.second))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=open type=${error.javaClass.simpleName}")
                mutable.update { it.copy(busy = false, error = error.readableMessage(container.appContext)
                    ?: container.appString(R.string.player_err_open)) }
            }
        }
    }

    fun choose(item: PlayableItem) {
        networkRecoveries = 0
        countdownJob?.cancel()
        nextEpisodeJob?.cancel()
        nextEpisodeCancelled = false
        val options = container.preferencesRepository.personalization
        mutable.update { it.copy(nextEpisode = null, nextEpisodeCountdown = null, nextEpisodeDismissed = false,
            nextEpisodeOfferEnabled = options.showNextEpisode, nextEpisodeLeadSeconds = options.nextEpisodeLeadSeconds) }
        if (item.type in setOf("Series", "Season")) {
            if (folders.lastOrNull()?.id != item.id) folders += item
            parent = item; offset = 0; selected = null
            mutable.update { it.copy(title = item.title, subtitle = "Vel ${if (item.type == "Series") "sesong" else "episode"}",
                season = null, episode = null, logoUrl = item.logoUrl,
                choices = emptyList(), browsing = true, hasMore = false, error = null) }
            loadChildren()
        } else {
            selected = item; compatible = false
            mutable.update { it.copy(title = item.title, subtitle = item.subtitle, season = item.season,
                episode = item.episode, logoUrl = item.logoUrl, browsing = false, choices = emptyList(),
                positionMs = item.resumeMs, durationMs = item.durationMs, ended = false, error = null, chapters = item.chapters, itemId = item.id) }
            val start = playbackStartPosition(item.resumeMs, item.durationMs, item.played,
                container.preferencesRepository.personalization.autoResume)
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
        if (folders.isEmpty()) return false
        request?.cancel(); generation++; stopCurrent()
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
                mutable.update { it.copy(busy = false, choices = (it.choices + result.first).distinctBy(PlayableItem::id), hasMore = result.second) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(busy = false, error = container.appString(R.string.player_err_episodes)) } }
        }
    }

    private fun autoBitrate(): Int {
        val manager = container.appContext.getSystemService(ConnectivityManager::class.java)
        val network = manager.getNetworkCapabilities(manager.activeNetwork)
        return automaticPlaybackBitrate(network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true)
    }

    private fun prepare(position: Long, audio: Int? = null, subtitle: Int? = null, forceCompatible: Boolean = compatible, autoplay: Boolean = true,
        retryDelayMillis: Long = 0) {
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
        mutable.update { it.copy(busy = true, error = null, ended = false, positionMs = position) }
        request = viewModelScope.launch {
            try {
                if (retryDelayMillis > 0) delay(retryDelayMillis)
                val prepared = withContext(Dispatchers.IO) {
                    check(sameAccount())
                    client.prepare(c, userId, item, state.value.quality.takeIf { it > 0 } ?: autoBitrate(),
                        // The version the title page picked, until a plan exists and the player owns it.
                        audioChoice, subtitleChoice, forceCompatible, previous?.sourceId ?: preferredSource.takeIf { item.id == rootId },
                        container.preferencesRepository.personalization.preferredSubtitleLanguage,
                        container.preferencesRepository.personalization.fallbackSubtitleLanguage)
                }
                if (ticket != generation || !sameAccount()) return@launch
                plan = prepared
                stopped = false
                val http = app.reelstack.data.network.HttpTransport.sharedClient.newBuilder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(if (serviceKind == ServiceKind.EMBY) 45 else 20, TimeUnit.SECONDS).build()
                val cache = SubtitleMemoryCache(http.newBuilder().callTimeout(8, TimeUnit.SECONDS).build(), client.headers(c))
                subtitleCache = cache
                val upstream = cache.factory(OkHttpDataSource.Factory(http).setDefaultRequestProperties(client.headers(c)))
                val dataSource = ResolvingDataSource.Factory(upstream) { spec ->
                    spec.withUri(safePlaybackUrl(c.baseUrl, spec.uri.toString()).toUri())
                }
                val media = MediaItem.Builder().setUri(prepared.url).setMediaId(item.id)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).setSubtitle(item.subtitle).build())
                    .setMimeType(if (prepared.direct) null else MimeTypes.APPLICATION_M3U8)
                media.setSubtitleConfigurations(prepared.subtitles.filter { it.isText }.map { track ->
                    MediaItem.SubtitleConfiguration.Builder(textUrl(c, prepared, track.index).toUri())
                        .setId("spole-subtitle-${track.index}").setMimeType(MimeTypes.TEXT_VTT).setLanguage(track.language)
                        .setSelectionFlags(if (track.index == prepared.subtitleIndex) C.SELECTION_FLAG_DEFAULT else 0).build()
                })
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().clearOverrides()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, prepared.subtitleUrl == null).build()
                player.setMediaSource(DefaultMediaSourceFactory(dataSource).setLoadOnlySelectedTracks(true)
                    .createMediaSource(media.build()), position.coerceAtLeast(0))
                mutable.update { it.copy(audio = prepared.audio, subtitles = prepared.subtitles, audioIndex = prepared.audioIndex,
                    subtitleIndex = prepared.subtitleIndex, mode = prepared.mode, transcodeReasons = prepared.transcodeReasons) }
                prepared.audio.firstOrNull { it.index == prepared.audioIndex }?.let { preferredAudioKey = it.key }
                prepared.subtitles.firstOrNull { it.index == prepared.subtitleIndex }?.let { preferredSubtitleKey = it.key }
                player.prepare(); player.playWhenReady = foreground && autoplay
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.w("SpolePlayback", "source=${serviceKind.name} stage=prepare type=${error.javaClass.simpleName}")
                if (ticket == generation) mutable.update { it.copy(busy = false, playing = false,
                    error = error.readableMessage(container.appContext)
                        ?: container.appString(R.string.player_err_start)) }
            }
        }
    }

    /**
     * Loads what comes next while the current episode is still playing, so the offer at the end
     * appears immediately rather than after a round trip the viewer has to sit and watch.
     */
    private fun loadNextEpisode(item: PlayableItem) {
        nextEpisodeJob?.cancel()
        mutable.update { it.copy(nextEpisode = null, nextEpisodeCountdown = null, segments = emptyList()) }
        val c = connection ?: return
        if (item.type != "Episode") return
        nextEpisodeJob = viewModelScope.launch {
            val next = runCatching { withContext(Dispatchers.IO) { client.nextEpisode(c, userId, item) } }.getOrNull()
            if (!isActive || selected?.id != item.id) return@launch
            if (next != null) {
                mutable.update { if (it.itemId != item.id) it else it.copy(nextEpisode = next) }
                if (state.value.ended) startNextEpisodeCountdown()
            }
            val marked = runCatching { withContext(Dispatchers.IO) { client.segments(c, userId, item.id) } }
                .getOrDefault(emptyList())
            if (!isActive || selected?.id != item.id || marked.isEmpty()) return@launch
            mutable.update { if (it.itemId != item.id) it else it.copy(segments = marked) }
        }
    }

    /**
     * Counts from the configured offer time while playing in the foreground. Pausing also pauses
     * the countdown; seeking away from the offer window resets it.
     *
     * Long enough to read the title and decide, short enough that a series does not stop dead
     * between episodes. Anything that puts the viewer back in charge — a press of Cancel, closing
     * the player, picking a different episode — stops it, and it never starts at all when there is
     * nothing to play next.
     */
    private fun startNextEpisodeCountdown() {
        val options = container.preferencesRepository.personalization
        if (countdownJob?.isActive == true || nextEpisodeCancelled || !foreground ||
            !options.autoPlayNextEpisode || !state.value.canCountDownNextEpisode()) return
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
            if (state.value.canCountDownNextEpisode()) playNext()
            else mutable.update { it.copy(nextEpisodeCountdown = null) }
        }
    }

    /** Starts the next episode now, whether the countdown ran out or someone pressed the button. */
    fun playNext() {
        if (!foreground || state.value.busy || state.value.error != null || !sameAccount()) return
        val next = state.value.nextEpisode ?: return
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
        val oldIsImage = current.subtitles.any { it.index == current.subtitleIndex && !it.isText }
        val newIsImage = current.subtitles.any { it.index == index && !it.isText }
        if (oldIsImage || newIsImage) {
            prepare(player.currentPosition, subtitle = index, autoplay = player.playWhenReady)
        } else {
            // Text selection must not tear down the HLS session or reset its timeline.
            plan = current.copy(subtitleIndex = index, subtitleUrl = if (index == -1) null else textUrl(c, current, index))
            mutable.update { it.copy(subtitleIndex = index) }
            selectTextTrack()
            report("/Progress")
        }
    }
    fun quality(bitrate: Int) {
        if (state.value.busy || bitrate !in listOf(0, 80_000_000, 20_000_000, 4_000_000, 2_000_000)) return
        val autoplay = player.playWhenReady || state.value.error != null
        mutable.update { it.copy(quality = bitrate) }; prepare(player.currentPosition, autoplay = autoplay)
    }
    fun toggle() {
        if (state.value.error != null || state.value.busy && plan == null) return
        if (player.playbackState == Player.STATE_ENDED) { prepare(0); return }
        if (player.playWhenReady) player.pause() else player.play()
    }
    fun seek(position: Long) {
        if (plan == null) return
        val target = position.coerceIn(0, state.value.durationMs.coerceAtLeast(0))
        mutable.update { it.copy(positionMs = target, ended = false) }
        if (!state.value.canCountDownNextEpisode()) {
            countdownJob?.cancel()
            mutable.update { it.copy(nextEpisodeCountdown = null) }
        }
        player.seekTo(target)
    }
    fun background() { foreground = false; countdownJob?.cancel(); player.pause(); if (started) report("/Progress") }
    fun foreground() { foreground = true; if (state.value.ended) startNextEpisodeCountdown() }
    fun fallbackUrl(): String? = connection?.let { safePlaybackUrl(it.baseUrl, "web/index.html") + "#!/details?id=${enc(selected?.id ?: rootId)}" }

    private fun report(event: String) {
        val c = connection ?: return
        val current = plan ?: return
        if (event == "/Stopped") {
            if (stopped) return
            stopped = true
        } else if (!started) return
        if (started) container.localPlaybackStore.record(c, current.item, player.currentPosition.coerceAtLeast(0),
            player.duration.takeIf { it > 0 } ?: current.item.durationMs, player.playbackState == Player.STATE_ENDED)
        reports.trySend(Report(c, current, event, player.currentPosition.coerceAtLeast(0), !player.isPlaying))
    }
    private fun stopCurrent() {
        subtitleWarmJob?.cancel(); subtitleWarmJob = null; subtitleCache = null; subtitleWarmedSession = null
        countdownJob?.cancel()
        mutable.update { it.copy(nextEpisodeCountdown = null) }
        report("/Stopped"); started = false; plan = null
        player.stop(); player.clearMediaItems()
    }
    override fun onCleared() {
        generation++; request?.cancel(); stopCurrent(); mediaSession.release(); player.release(); reports.close()
        super.onCleared()
    }
}
