@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package app.reelstack.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.reelstack.R
import app.reelstack.offline.OfflineDownloadRuntime
import app.reelstack.offline.OfflinePlaybackSource
import app.reelstack.offline.offlineServiceHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class OfflinePlayerModel(private val container: app.reelstack.AppContainer) : ViewModel() {
    private val owner = container.connectionRepository.captureSession()
    private val progressLease = app.reelstack.background.PlaybackProgressRuntime.acquire()
    private val mutable = MutableStateFlow(PlayerScreenState())
    val state = mutable.asStateFlow()
    private var source: OfflinePlaybackSource? = null
    private val progress = OfflinePlaybackProgress(owner.profileId, container.localPlaybackStore, container.playbackProgressOutbox)
    private data class Checkpoint(val source: OfflinePlaybackSource, val position: Long, val duration: Long, val completed: Boolean)
    private val checkpoints = kotlinx.coroutines.channels.Channel<Checkpoint>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private val recorder = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val recordingJob = recorder.launch {
        for (checkpoint in checkpoints) {
            val result = runCatching {
                container.connectionRepository.withCurrentSession(owner) {
                    connection(checkpoint.source)?.let { account ->
                        progress.record(account, checkpoint.source, checkpoint.position, checkpoint.duration, checkpoint.completed)
                    }
                }
            }
            container.connectionRepository.withCurrentSession(owner) {
                mutable.update { it.copy(warning = if (result.isFailure) appString(R.string.player_err_local_progress) else null) }
            }
        }
    }
    // A download is accepted with the same FFmpeg audio the online player uses, so the local player
    // needs the same renderers. Without them an EAC3 or DTS film played with no sound.
    private val localAudioFallback = LocalAudioFallback()

    private val adjustmentController = PlaybackAdjustmentController()
    private val subtitleResources = PlaybackSubtitleResources()
    val adjustments = adjustmentController.state
    fun adjust(value: PlaybackAdjustments) { if (ensureSession()) adjustmentController.apply(player, value) }

    val player: ExoPlayer = ExoPlayer.Builder(container.appContext,
        PlaybackRenderersFactory(container.appContext, localAudioFallback, adjustmentController, subtitleResources))
        .setMediaSourceFactory(DefaultMediaSourceFactory(OfflineDownloadRuntime.offlineDataSourceFactory(container.appContext))
            .setSubtitleParserFactory(PlaybackSubtitleParserFactory()))
        .build()
        .also { player ->
            player.setAudioAttributes(androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            player.setHandleAudioBecomingNoisy(true)
            player.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        subtitleResources.width = videoSize.width
                        subtitleResources.height = videoSize.height
                    }
                }
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    mutable.update { it.copy(
                        audio = localPlaybackTracks(tracks, androidx.media3.common.C.TRACK_TYPE_AUDIO) { appString(R.string.player_audio) },
                        subtitles = localPlaybackTracks(tracks, androidx.media3.common.C.TRACK_TYPE_TEXT) { appString(R.string.player_subtitles) },
                        audioIndex = selectedLocalTrack(tracks, androidx.media3.common.C.TRACK_TYPE_AUDIO),
                        subtitleIndex = selectedLocalTrack(tracks, androidx.media3.common.C.TRACK_TYPE_TEXT) ?: -1,
                    ) }
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    mutable.update { it.copy(playing = isPlaying, playWhenReady = player.playWhenReady) }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    mutable.update { it.copy(playWhenReady = playWhenReady) }
                    if (!playWhenReady) record(player.playbackState == Player.STATE_ENDED)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val duration = player.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: 0L
                    mutable.update {
                        it.copy(
                            busy = playbackState == Player.STATE_BUFFERING,
                            ended = playbackState == Player.STATE_ENDED,
                            durationMs = duration,
                            positionMs = player.currentPosition.coerceAtLeast(0),
                            playWhenReady = player.playWhenReady,
                        )
                    }
                    if (playbackState == Player.STATE_ENDED) record(completed = true)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (!ensureSession()) return
                    if (localAudioFallback.tryEnable(error)) {
                        // A platform decoder that claimed the track and then failed: hand the same
                        // local file to the bundled decoder, from the same position.
                        val position = player.currentPosition.coerceAtLeast(0)
                        val playWhenReady = player.playWhenReady
                        val item = player.currentMediaItem
                        if (item != null) {
                            player.stop()
                            player.setMediaItem(item, position)
                            mutable.update { it.copy(error = null, busy = true) }
                            player.prepare()
                            player.playWhenReady = playWhenReady
                            return
                        }
                    }
                    mutable.update { it.copy(busy = false, error = appString(R.string.offline_playback_failed)) }
                }
            })
        }

    init {
        viewModelScope.launch {
            container.connectionRepository.sessionScope.collect { if (it != owner) ensureSession() }
        }
        viewModelScope.launch {
            var checkpointAt = 0L
            while (isActive) {
                if (source != null) mutable.update { current ->
                    current.copy(positionMs = player.currentPosition.coerceAtLeast(0),
                        durationMs = player.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: current.durationMs)
                }
                val now = android.os.SystemClock.elapsedRealtime()
                if (player.isPlaying && now - checkpointAt >= 10_000) {
                    record(completed = false)
                    checkpointAt = now
                }
                delay(500)
            }
        }
    }

    fun open(downloadId: String) {
        if (!ensureSession()) return
        viewModelScope.launch {
            val activeProfile = container.connectionRepository.activeProfileId
            val resolved = withContext(Dispatchers.IO) { container.offlineDownloads.playbackSource(activeProfile, downloadId) }
            // The profile may have changed while the local database was read. Refuse rather than
            // briefly opening an adult file after a switch to a child profile.
            if (resolved == null || !container.connectionRepository.isCurrent(owner)) {
                mutable.update { it.copy(busy = false, error = appString(R.string.offline_playback_unavailable)) }
                return@launch
            }
            source = resolved
            subtitleResources.fonts = withContext(Dispatchers.IO) {
                matroskaFonts(OfflineDownloadRuntime.offlineDataSourceFactory(container.appContext), resolved.uri, resolved.cacheKey)
            }
            if (!ensureSession()) return@launch
            localAudioFallback.reset()
            val item = MediaItem.Builder()
                .setMediaId(resolved.id)
                .setUri(resolved.uri)
                .setCustomCacheKey(resolved.cacheKey)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(resolved.title).setSubtitle(resolved.subtitle).build())
                .build()
            mutable.value = PlayerScreenState(
                title = resolved.title,
                subtitle = resolved.subtitle,
                busy = true,
                source = resolved.service,
                itemId = resolved.itemId,
                mode = PlaybackMode.DIRECT_PLAY,
            )
            val resume = withContext(Dispatchers.IO) { connection(resolved)?.let { progress.resumePosition(it, resolved) } ?: 0L }
            if (!ensureSession()) return@launch
            player.setMediaItem(item, resume)
            player.prepare()
            player.play()
        }
    }

    fun toggle() {
        if (!ensureSession()) return
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.play()
        } else if (player.playWhenReady) player.pause() else player.play()
    }

    fun seek(positionMs: Long) {
        if (!ensureSession()) return
        player.seekTo(positionMs.coerceIn(0, state.value.durationMs.coerceAtLeast(0)))
        mutable.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0), ended = false) }
        record(completed = false)
    }

    fun retry() = source?.let { open(it.id) }
    fun audio(index: Int) { if (ensureSession()) selectLocalTrack(player, androidx.media3.common.C.TRACK_TYPE_AUDIO, index) }
    fun subtitles(index: Int) { if (ensureSession()) selectLocalTrack(player, androidx.media3.common.C.TRACK_TYPE_TEXT, index) }
    fun foreground() { ensureSession() }

    fun background() {
        player.pause()
        record(completed = player.playbackState == Player.STATE_ENDED)
    }

    private fun record(completed: Boolean) {
        if (!container.connectionRepository.isCurrentNow(owner)) return
        val offline = source ?: return
        checkpoints.trySend(Checkpoint(offline,
            player.currentPosition.coerceAtLeast(0),
            player.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: state.value.durationMs,
            completed))
    }

    private fun connection(offline: OfflinePlaybackSource) = container.connectionRepository.list(owner.profileId).firstOrNull {
        it.kind == offline.service && it.token.isNotBlank() && offlineServiceHash(it) == offline.serviceScope
    }

    private fun appString(@androidx.annotation.StringRes id: Int): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(id)

    private fun ensureSession(): Boolean {
        if (container.connectionRepository.isCurrentNow(owner)) return true
        source = null
        player.stop(); player.clearMediaItems()
        mutable.value = PlayerScreenState(busy = false, error = appString(R.string.offline_playback_unavailable))
        return false
    }

    override fun onCleared() {
        record(completed = player.playbackState == Player.STATE_ENDED)
        player.release()
        checkpoints.close()
        recorder.launch {
            recordingJob.join()
            app.reelstack.background.PlaybackProgressRuntime.release(progressLease)
            runCatching { app.reelstack.background.PlaybackProgressScheduler.schedule(container.appContext, afterPlayback = true) }
            recorder.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        }
        super.onCleared()
    }
}
