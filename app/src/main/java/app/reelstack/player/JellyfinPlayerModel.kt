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
import app.reelstack.AppContainer
import app.reelstack.data.model.*
import app.reelstack.data.repository.DeviceIdentity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class PlayerScreenState(
    val title: String = "Jellyfin", val subtitle: String = "", val busy: Boolean = true,
    val error: String? = null, val warning: String? = null,
    val choices: List<PlayableItem> = emptyList(), val browsing: Boolean = false, val hasMore: Boolean = false,
    val playing: Boolean = false, val ended: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0,
    val audio: List<PlaybackTrack> = emptyList(), val subtitles: List<PlaybackTrack> = emptyList(),
    val audioIndex: Int? = null, val subtitleIndex: Int = -1, val direct: Boolean = true,
    val quality: Int = 0,
    val awaitingResume: Boolean = false,
    val chapters: List<PlaybackChapter> = emptyList(),
    val itemId: String = "",
    /** The numbers behind [subtitle], so the header can write them out rather than show "S03 E01". */
    val season: Int? = null,
    val episode: Int? = null,
    /**
     * What follows this episode, and how long is left before it starts on its own. Null means
     * there is nothing after it — a film, the last episode, or a server that could not say.
     */
    val nextEpisode: PlayableItem? = null,
    val nextEpisodeCountdown: Int? = null,
    val nextEpisodeOfferEnabled: Boolean = true,
    val nextEpisodeLeadSeconds: Int = 60,
    val nextEpisodeDismissed: Boolean = false,
    /** Title sequences and closing credits the server has marked, if anything has marked them. */
    val segments: List<PlaybackSegment> = emptyList(),
)

/**
 * The marked stretch the playhead is inside right now, if any.
 *
 * A small guard at each end keeps the offer from flickering on and off around the boundary, and
 * from appearing for the last half second of a title sequence nobody would bother skipping.
 */
fun PlayerScreenState.activeSegment(): PlaybackSegment? = segments.firstOrNull { segment ->
    positionMs >= segment.startMs && positionMs < segment.endMs - 1_500
}

/** Owns one local player, not a remote session controller. Survives rotation; never plays in the background. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class JellyfinPlayerModel(private val container: AppContainer) : ViewModel() {
    private val mutable = MutableStateFlow(PlayerScreenState())
    val state = mutable.asStateFlow()
    private val deviceId = DeviceIdentity.get(container.appContext)
    private val deviceCapabilities = AndroidPlaybackCapabilities(container.appContext)
    private val client = JellyfinPlaybackClient(deviceId = deviceId,
        capabilities = deviceCapabilities::snapshot, sourceSupported = deviceCapabilities::canDirectPlay,
        videoSupported = deviceCapabilities::canDecodeVideo)
    private var connection: ServiceConnection? = null
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
    private var foreground = true
    private var request: Job? = null
    private var generation = 0
    private var bufferingSince = 0L
    private var nextEpisodeJob: Job? = null
    private var countdownJob: Job? = null
    private var nextEpisodeCancelled = false
    private var preferredAudio: Int? = null
    private var preferredSubtitle: Int? = null
    private var preferredSource: String? = null
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
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mutable.update { it.copy(playing = isPlaying) }
                if (isPlaying && !started) { started = true; report("") }
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
                val position = currentPosition.coerceAtLeast(0)
                // A single codec fallback, never an endless retry loop or a bitrate increase.
                if (!compatible && error.errorCode in setOf(PlaybackException.ERROR_CODE_DECODING_FAILED,
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED)) {
                    compatible = true
                    prepare(position, forceCompatible = true, autoplay = playWhenReady)
                } else {
                    mutable.update { it.copy(busy = false, playing = false,
                        error = "Avspelinga stoppa. Prøv igjen, vel lågare kvalitet eller opne i Jellyfin.") }
                }
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

    private val mediaSession = androidx.media3.session.MediaSession.Builder(container.appContext, player).build()

    init {
        reporter.launch {
            for (event in reports) {
                val failed = runCatching { client.report(event.connection, event.plan, event.event, event.position, event.paused) }.isFailure
                withContext(Dispatchers.Main) {
                    if (plan?.sessionId == event.plan.sessionId) mutable.update { it.copy(warning =
                        if (failed) "Fekk ikkje lagra framdrifta i Jellyfin. Kontroller nettet." else null) }
                }
            }
            reporter.cancel()
        }
        viewModelScope.launch {
            var ticks = 0
            while (isActive) {
                delay(1_000)
                if (bufferingSince != 0L && android.os.SystemClock.elapsedRealtime() - bufferingSince > 30_000) {
                    report("/Stopped"); started = false
                    player.stop()
                    mutable.update { it.copy(busy = false, playing = false,
                        error = "Videoen brukar for lang tid på å laste. Prøv igjen, vel lågare kvalitet eller opne i Jellyfin.") }
                }
                if (connection != null && !sameAccount()) {
                    request?.cancel(); generation++; stopCurrent()
                    mutable.update { it.copy(busy = false, playing = false, error = "Kontoen er endra. Lukk spelaren og opne tittelen på nytt.") }
                    connection = null
                }
                if (plan != null) mutable.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
                if (++ticks % 10 == 0 && started) report("/Progress")
            }
        }
    }

    /**
     * [audio] and [subtitle] are what the title page chose before playback started. They apply
     * only until a plan exists: from then on the player's own menus are the authority, so changing
     * quality or stepping to the next episode does not quietly undo a choice made inside the player.
     */
    fun open(id: String, audio: Int? = null, subtitle: Int? = null, sourceId: String? = null) {
        if (rootId.isNotEmpty()) return
        rootId = id
        preferredAudio = audio
        preferredSubtitle = subtitle
        preferredSource = sourceId
        loadRoot()
    }

    private fun sameAccount(): Boolean {
        val c = connection ?: return false
        val saved = container.connectionRepository.get(ServiceKind.JELLYFIN)
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
                    val c = container.connectionRepository.get(ServiceKind.JELLYFIN)
                    val seerr = container.connectionRepository.get(ServiceKind.SEERR)
                    val id = client.verify(c)
                    val jellyfinAccount = container.accountProfileClient.load(c)
                    val seerrAccount = seerr.takeIf { it.token.isNotBlank() }?.let { container.accountProfileClient.load(it) }
                    val access = ViewerAccess(seerr.token.isNotBlank(), buildMap {
                        put(ServiceKind.JELLYFIN, jellyfinAccount); seerrAccount?.let { put(ServiceKind.SEERR, it) }
                    })
                    check(access.ownMediaUser(ServiceKind.JELLYFIN) == id) { "Vel den personlege Jellyfin-kontoen din i innstillingane." }
                    Triple(c, seerr, id) to client.item(c, id, rootId)
                }
                connection = result.first.first; seerrConnection = result.first.second; userId = result.first.third
                check(sameAccount())
                choose(result.second)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.update { it.copy(busy = false, error = "Fekk ikkje opna tittelen. Kontroller den personlege Jellyfin-innlogginga og prøv igjen.") }
            }
        }
    }

    fun choose(item: PlayableItem) {
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
                season = null, episode = null,
                choices = emptyList(), browsing = true, hasMore = false, error = null) }
            loadChildren()
        } else {
            selected = item; compatible = false
            mutable.update { it.copy(title = item.title, subtitle = item.subtitle, season = item.season,
                episode = item.episode, browsing = false, choices = emptyList(),
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
            catch (_: Exception) { mutable.update { it.copy(busy = false, error = "Fekk ikkje henta episodane. Prøv igjen.") } }
        }
    }

    private fun autoBitrate(): Int {
        val manager = container.appContext.getSystemService(ConnectivityManager::class.java)
        val network = manager.getNetworkCapabilities(manager.activeNetwork)
        return automaticPlaybackBitrate(network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true)
    }

    private fun prepare(position: Long, audio: Int? = null, subtitle: Int? = null, forceCompatible: Boolean = compatible, autoplay: Boolean = true) {
        val c = connection ?: return
        val item = selected ?: return
        val previous = plan
        val audioChoice = audio ?: previous?.audioIndex ?: preferredAudio
        val subtitleChoice = subtitle ?: previous?.subtitleIndex ?: preferredSubtitle
        request?.cancel()
        val ticket = ++generation
        stopCurrent()
        mutable.update { it.copy(busy = true, error = null, ended = false, positionMs = position) }
        request = viewModelScope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) {
                    check(sameAccount())
                    client.prepare(c, userId, item, state.value.quality.takeIf { it > 0 } ?: autoBitrate(),
                        // The version the title page picked, until a plan exists and the player owns it.
                        audioChoice, subtitleChoice, forceCompatible, previous?.sourceId ?: preferredSource)
                }
                if (ticket != generation || !sameAccount()) return@launch
                plan = prepared
                stopped = false
                val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
                    .connectTimeout(8, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
                val dataSource = ResolvingDataSource.Factory(OkHttpDataSource.Factory(http).setDefaultRequestProperties(client.headers(c))) { spec ->
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
                    subtitleIndex = prepared.subtitleIndex, direct = prepared.direct) }
                player.prepare(); player.playWhenReady = foreground && autoplay
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (ticket == generation) mutable.update { it.copy(busy = false, playing = false,
                    error = "Fekk ikkje starta videoen. Kontroller nettet og avspelingsløyva i Jellyfin, eller prøv igjen.") }
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
     * Counts the configured delay only after playback ends and while the app is foregrounded.
     *
     * Long enough to read the title and decide, short enough that a series does not stop dead
     * between episodes. Anything that puts the viewer back in charge — a press of Cancel, closing
     * the player, picking a different episode — stops it, and it never starts at all when there is
     * nothing to play next.
     */
    private fun startNextEpisodeCountdown() {
        val options = container.preferencesRepository.personalization
        if (countdownJob?.isActive == true || nextEpisodeCancelled || !foreground ||
            !options.autoPlayNextEpisode || state.value.nextEpisode == null || !state.value.ended) return
        val episodeId = state.value.itemId
        val seconds = state.value.nextEpisodeCountdown ?: options.nextEpisodeDelaySeconds
        countdownJob = viewModelScope.launch {
            for (second in seconds.coerceIn(1, 60) downTo 1) {
                mutable.update { it.copy(nextEpisodeCountdown = second) }
                delay(1_000)
                if (!isActive || !foreground || nextEpisodeCancelled || state.value.itemId != episodeId ||
                    !state.value.ended || state.value.nextEpisode == null || !sameAccount()) return@launch
            }
            playNext()
        }
    }

    /** Starts the next episode now, whether the countdown ran out or someone pressed the button. */
    fun playNext() {
        if (!foreground || state.value.busy || state.value.error != null || !sameAccount()) return
        val next = state.value.nextEpisode ?: return
        countdownJob?.cancel()
        mutable.update { it.copy(nextEpisode = null, nextEpisodeCountdown = null) }
        report("/Stopped"); started = false
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

    fun retry() { if (connection == null || selected == null && parent == null) loadRoot()
        else if (state.value.browsing) loadChildren() else prepare(state.value.positionMs) }
    fun audio(index: Int) { if (!state.value.busy && state.value.audio.any { it.index == index }) prepare(player.currentPosition, audio = index, autoplay = player.playWhenReady) }
    fun subtitles(index: Int) {
        if (state.value.busy || index != -1 && state.value.subtitles.none { it.index == index }) return
        val current = plan ?: return
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
        if (state.value.busy || bitrate !in listOf(0, 4_000_000, 2_000_000)) return
        val autoplay = player.playWhenReady || state.value.error != null
        mutable.update { it.copy(quality = bitrate) }; prepare(player.currentPosition, autoplay = autoplay)
    }
    fun toggle() {
        if (state.value.error != null || state.value.busy) return
        if (player.playbackState == Player.STATE_ENDED) { prepare(0); return }
        if (player.isPlaying) player.pause() else player.play()
    }
    fun seek(position: Long) { if (plan != null) player.seekTo(position.coerceIn(0, state.value.durationMs.coerceAtLeast(0))) }
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
        reports.trySend(Report(c, current, event, player.currentPosition.coerceAtLeast(0), !player.isPlaying))
    }
    private fun stopCurrent() {
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
