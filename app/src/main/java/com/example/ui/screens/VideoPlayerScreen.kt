@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.*
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.theme.*
import com.example.util.DownloadHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Applies the selected streaming server domain/host to the video URL while preserving
 * the exact path, file parameters, and hash keys.
 */
fun applyServerToUrl(originalUrl: String, server: ServerItem): String {
    if (originalUrl.isBlank() || server.domain.isBlank()) return originalUrl
    return try {
        val uri = Uri.parse(originalUrl)
        val rawPath = uri.encodedPath ?: ""
        val cleanPath = if (rawPath.startsWith("/")) rawPath else "/$rawPath"
        val query = if (!uri.encodedQuery.isNullOrBlank()) "?${uri.encodedQuery}" else ""

        var targetDomain = server.domain.trim().trimEnd('/')
        if (!targetDomain.startsWith("http://") && !targetDomain.startsWith("https://")) {
            val scheme = uri.scheme ?: "http"
            targetDomain = "$scheme://$targetDomain"
        }
        "$targetDomain$cleanPath$query"
    } catch (e: Exception) {
        originalUrl
    }
}

@Composable
fun VideoPlayerScreen(
    animeId: String,
    seasonName: String,
    episodeNumber: Int,
    animeRepo: AnimeRepository,
    userPrefsRepo: UserPreferencesRepository,
    onBackClick: () -> Unit,
    onNextEpisode: ((Int) -> Unit)? = null,
    offlineFilePath: String? = null,
    customTitle: String? = null,
    customSubtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    val animeList by animeRepo.animeList.collectAsState()
    val anime = remember(animeId, animeList) { animeRepo.getAnimeById(animeId) }

    // Streaming Servers loaded directly from Firebase RTDB settings/videoServers.json
    val servers by animeRepo.servers.collectAsState()
    val availableServers = remember(servers) {
        if (servers.isNotEmpty()) servers else listOf(
            ServerItem("srv_1", "RS FR 01", domain = "http://51.75.118.79:20044", proxy = "https://jlzflmbgulievhgrunps.supabase.co/functions/v1/video-proxy", isDefault = true),
            ServerItem("srv_2", "RS FR 02", domain = "https://rahat1102-video-hosting-bot.hf.space", isDefault = false),
            ServerItem("srv_3", "RS FR 03", domain = "http://de3.bot-hosting.net:20508", proxy = "https://tucrcjmlzsdtkdqiqxxc.supabase.co/functions/v1/rs_video_proxy", isDefault = false),
            ServerItem("srv_4", "RS FR 04", domain = "http://us.monkey-network.xyz:6072", proxy = "https://tucrcjmlzsdtkdqiqxxc.supabase.co/functions/v1/rs_video_proxy", isDefault = false),
            ServerItem("srv_5", "RS PR S1", domain = "https://rsstreambot-production.up.railway.app", locked = true, isDefault = false)
        )
    }
    var selectedServer by remember(availableServers) {
        mutableStateOf<ServerItem?>(availableServers.find { it.isDefault } ?: availableServers.firstOrNull())
    }

    val currentSeason = remember(anime, seasonName) {
        anime?.seasons?.find { it.name.equals(seasonName, ignoreCase = true) } ?: anime?.seasons?.firstOrNull()
    }

    var currentEpNum by remember { mutableIntStateOf(episodeNumber) }
    val currentEpisode = remember(currentSeason, currentEpNum) {
        currentSeason?.episodes?.find { it.episodeNumber == currentEpNum }
            ?: currentSeason?.episodes?.firstOrNull()
    }

    // Playback configuration state
    var selectedQuality by remember { mutableStateOf("1080p FHD") }
    var selectedAudioTrack by remember { mutableStateOf<String?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }

    // Seeking state for smooth scrubbing
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubbedPositionMs by remember { mutableLongStateOf(0L) }

    // UI overlays & sheets
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }

    // Gesture States (MX Player style)
    var volumeLevel by remember {
        val maxV = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val curV = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        mutableFloatStateOf(curV.toFloat() / maxV.toFloat())
    }
    var brightnessLevel by remember {
        val winLp = activity?.window?.attributes
        val curB = if (winLp?.screenBrightness != null && winLp.screenBrightness >= 0) winLp.screenBrightness else 0.5f
        mutableFloatStateOf(curB)
    }

    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekTargetMs by remember { mutableLongStateOf(0L) }
    var seekDeltaSeconds by remember { mutableLongStateOf(0L) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var doubleTapSide by remember { mutableStateOf<String?>(null) }

    // Available Qualities
    val qualityOptions = remember(anime, currentEpisode) {
        if (anime?.type == AnimeType.MOVIE) {
            val list = mutableListOf<String>()
            if (!anime.movieLink1080.isNullOrBlank()) list.add("1080p FHD")
            if (!anime.movieLink720.isNullOrBlank()) list.add("720p HD")
            if (!anime.movieLink480.isNullOrBlank()) list.add("480p SD")
            if (!anime.movieLink4k.isNullOrBlank()) list.add("4K UHD")
            if (list.isEmpty()) list.add("1080p FHD")
            list
        } else {
            val list = mutableListOf<String>()
            if (!currentEpisode?.link1080.isNullOrBlank()) list.add("1080p FHD")
            if (!currentEpisode?.link720.isNullOrBlank()) list.add("720p HD")
            if (!currentEpisode?.link480.isNullOrBlank()) list.add("480p SD")
            if (!currentEpisode?.link4k.isNullOrBlank()) list.add("4K UHD")
            if (list.isEmpty()) list.add("1080p FHD")
            list
        }
    }

    // Resolve Base Video URL
    val baseVideoUrl = remember(anime, currentEpisode, selectedQuality, selectedAudioTrack, offlineFilePath) {
        if (!offlineFilePath.isNullOrBlank()) {
            return@remember offlineFilePath
        }
        val matchedAudio = currentEpisode?.audioTracks?.find { it.name.equals(selectedAudioTrack, true) || it.language.equals(selectedAudioTrack, true) }
        if (matchedAudio?.link?.isNotBlank() == true) {
            return@remember matchedAudio.link
        }

        if (anime?.type == AnimeType.MOVIE) {
            when (selectedQuality) {
                "4K UHD" -> anime.movieLink4k ?: anime.movieLink1080 ?: anime.movieLink ?: ""
                "1080p FHD" -> anime.movieLink1080 ?: anime.movieLink ?: anime.movieLink720 ?: ""
                "720p HD" -> anime.movieLink720 ?: anime.movieLink ?: anime.movieLink1080 ?: ""
                "480p SD" -> anime.movieLink480 ?: anime.movieLink ?: anime.movieLink720 ?: ""
                else -> anime.movieLink ?: anime.movieLink1080 ?: anime.movieLink720 ?: ""
            }
        } else {
            when (selectedQuality) {
                "4K UHD" -> currentEpisode?.link4k ?: currentEpisode?.link1080 ?: currentEpisode?.link ?: ""
                "1080p FHD" -> currentEpisode?.link1080 ?: currentEpisode?.link ?: currentEpisode?.link720 ?: ""
                "720p HD" -> currentEpisode?.link720 ?: currentEpisode?.link ?: currentEpisode?.link1080 ?: ""
                "480p SD" -> currentEpisode?.link480 ?: currentEpisode?.link ?: currentEpisode?.link720 ?: ""
                else -> currentEpisode?.link ?: currentEpisode?.link1080 ?: currentEpisode?.link720 ?: ""
            }
        }
    }

    // Final resolved URL with real server domain replacement
    val videoUrl = remember(baseVideoUrl, selectedServer, offlineFilePath) {
        if (!offlineFilePath.isNullOrBlank()) {
            offlineFilePath
        } else if (selectedServer != null && selectedServer?.domain?.isNotBlank() == true) {
            applyServerToUrl(baseVideoUrl, selectedServer!!)
        } else {
            baseVideoUrl
        }
    }

    // Check saved watch progress to resume
    val watchHistory by userPrefsRepo.watchHistory.collectAsState()
    val savedProgress = remember(animeId, currentEpNum) {
        watchHistory.find { it.animeId == animeId && it.episodeNumber == currentEpNum }
    }

    // Initialize ExoPlayer with HttpDataSource allowing cross protocol redirects & high performance streaming
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(25000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                playWhenReady = true
            }
    }

    // Force Landscape & Immersive Mode
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Handle MediaItem loading and initial resume
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            playbackError = null
            isBuffering = true
            val currentPos = exoPlayer.currentPosition
            val uri = if (videoUrl.startsWith("/") || videoUrl.startsWith("file://")) {
                if (videoUrl.startsWith("file://")) Uri.parse(videoUrl) else Uri.fromFile(java.io.File(videoUrl))
            } else {
                Uri.parse(videoUrl)
            }
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            val resumeMs = if (currentPos > 2000L) currentPos else (savedProgress?.lastPositionMs ?: 0L)
            if (resumeMs > 3000L && resumeMs < (savedProgress?.totalDurationMs ?: Long.MAX_VALUE) - 10000L) {
                exoPlayer.seekTo(resumeMs)
            }

            exoPlayer.play()
            isPlaying = true
        } else {
            playbackError = "No video stream URL found for this episode."
        }
    }

    // Auto-hide controls after 4.5 seconds of inactivity
    LaunchedEffect(showControls, isPlaying, isLocked, isUserScrubbing) {
        if (showControls && isPlaying && !isLocked && !isUserScrubbing) {
            delay(4500)
            showControls = false
        }
    }

    // Playback timer ticker & watch time recording
    LaunchedEffect(exoPlayer) {
        var elapsedTick = 0
        while (true) {
            if (!isUserScrubbing) {
                currentPositionMs = exoPlayer.currentPosition
            }
            val dur = exoPlayer.duration
            if (dur > 0) totalDurationMs = dur
            bufferedPositionMs = exoPlayer.bufferedPosition
            isPlaying = exoPlayer.isPlaying
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING

            if (isPlaying) {
                elapsedTick++
                if (elapsedTick >= 30) {
                    elapsedTick = 0
                    userPrefsRepo.recordWatchTime(1)
                }
            }

            delay(300)
        }
    }

    // Listener for state changes & auto-play next episode
    DisposableEffect(exoPlayer, currentEpisode, anime) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playbackError = null
                }
                if (state == Player.STATE_ENDED) {
                    val nextEpNum = currentEpNum + 1
                    val hasNext = currentSeason?.episodes?.any { it.episodeNumber == nextEpNum } == true
                    if (hasNext) {
                        Toast.makeText(context, "Playing next episode...", Toast.LENGTH_SHORT).show()
                        currentEpNum = nextEpNum
                        onNextEpisode?.invoke(nextEpNum)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = "Playback error on ${selectedServer?.name ?: "Current Server"}. Please switch server."
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            if (anime != null) {
                userPrefsRepo.saveWatchProgress(
                    animeId = anime.id,
                    episodeNumber = currentEpNum,
                    seasonName = currentSeason?.name ?: "Season 1",
                    title = anime.title,
                    poster = anime.poster,
                    positionMs = exoPlayer.currentPosition,
                    durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 1440000L
                )
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Helper functions
    fun seekRelative(seconds: Long) {
        val target = (exoPlayer.currentPosition + seconds * 1000L).coerceIn(0L, if (totalDurationMs > 0) totalDurationMs else Long.MAX_VALUE)
        exoPlayer.seekTo(target)
        currentPositionMs = target
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPlaying = false
        } else {
            exoPlayer.play()
            isPlaying = true
        }
    }

    fun playPreviousEpisode() {
        val prevEp = currentSeason?.episodes?.find { it.episodeNumber == currentEpNum - 1 }
        if (prevEp != null) {
            currentEpNum = prevEp.episodeNumber
        } else {
            Toast.makeText(context, "This is the first episode", Toast.LENGTH_SHORT).show()
        }
    }

    fun playNextEpisode() {
        val nextEp = currentSeason?.episodes?.find { it.episodeNumber == currentEpNum + 1 }
        if (nextEp != null) {
            currentEpNum = nextEp.episodeNumber
            onNextEpisode?.invoke(nextEp.episodeNumber)
        } else {
            Toast.makeText(context, "This is the latest episode", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                var gestureType: String? = null // "volume", "brightness", "seek"
                var totalDragX = 0f
                var totalDragY = 0f
                var startPosMs = 0L

                detectDragGestures(
                    onDragStart = { offset ->
                        if (isLocked) return@detectDragGestures
                        totalDragX = 0f
                        totalDragY = 0f
                        startPosMs = exoPlayer.currentPosition

                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.35f) {
                            gestureType = "volume"
                            showVolumeOverlay = true
                        } else if (offset.x > screenWidth * 0.65f) {
                            gestureType = "brightness"
                            showBrightnessOverlay = true
                        } else {
                            gestureType = "seek"
                            showSeekOverlay = true
                            seekTargetMs = startPosMs
                            seekDeltaSeconds = 0L
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isLocked) return@detectDragGestures
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        when (gestureType) {
                            "volume" -> {
                                val delta = -dragAmount.y / 450f
                                val newV = (volumeLevel + delta).coerceIn(0f, 1f)
                                volumeLevel = newV
                                audioManager?.let { am ->
                                    val maxV = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val setVal = (newV * maxV).toInt().coerceIn(0, maxV)
                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, setVal, 0)
                                }
                            }
                            "brightness" -> {
                                val delta = -dragAmount.y / 450f
                                val newB = (brightnessLevel + delta).coerceIn(0.01f, 1f)
                                brightnessLevel = newB
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = newB
                                    win.attributes = lp
                                }
                            }
                            "seek" -> {
                                val deltaSeconds = (totalDragX / 10f).toLong()
                                seekDeltaSeconds = deltaSeconds
                                val target = (startPosMs + deltaSeconds * 1000L).coerceIn(0L, if (totalDurationMs > 0) totalDurationMs else Long.MAX_VALUE)
                                seekTargetMs = target
                            }
                        }
                    },
                    onDragEnd = {
                        if (gestureType == "seek" && seekDeltaSeconds != 0L) {
                            exoPlayer.seekTo(seekTargetMs)
                            currentPositionMs = seekTargetMs
                        }
                        coroutineScope.launch {
                            delay(600)
                            showVolumeOverlay = false
                            showBrightnessOverlay = false
                            showSeekOverlay = false
                        }
                        gestureType = null
                    },
                    onDragCancel = {
                        showVolumeOverlay = false
                        showBrightnessOverlay = false
                        showSeekOverlay = false
                        gestureType = null
                    }
                )
            }
            .pointerInput(isLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.45f) {
                                seekRelative(-10)
                                doubleTapSide = "left"
                                doubleTapFeedback = "-10s"
                                coroutineScope.launch {
                                    delay(650)
                                    doubleTapFeedback = null
                                    doubleTapSide = null
                                }
                            } else if (offset.x > screenWidth * 0.55f) {
                                seekRelative(10)
                                doubleTapSide = "right"
                                doubleTapFeedback = "+10s"
                                coroutineScope.launch {
                                    delay(650)
                                    doubleTapFeedback = null
                                    doubleTapSide = null
                                }
                            }
                        }
                    },
                    onTap = {
                        if (!isLocked) {
                            showControls = !showControls
                        } else {
                            showControls = true
                        }
                    }
                )
            }
            .testTag("video_player_container")
    ) {
        // 1. ExoPlayer Surface View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Double-tap Skip Pill Animation
        if (doubleTapFeedback != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentAlignment = if (doubleTapSide == "left") Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeNeonRed.copy(alpha = 0.8f)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (doubleTapSide == "left") Icons.Default.FastRewind else Icons.Default.FastForward,
                            contentDescription = null,
                            tint = AnimeNeonRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = doubleTapFeedback ?: "",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Gesture HUD: Volume Overlay (Left Side)
        if (showVolumeOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeGold.copy(alpha = 0.5f)),
                    modifier = Modifier.width(48.dp).height(150.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = if (volumeLevel <= 0.05f) Icons.Default.VolumeMute else if (volumeLevel < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = AnimeGold,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .weight(1f)
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(volumeLevel)
                                    .background(AnimeGold)
                            )
                        }

                        Text(
                            text = "${(volumeLevel * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Gesture HUD: Brightness Overlay (Right Side)
        if (showBrightnessOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 28.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.width(48.dp).height(150.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = if (brightnessLevel < 0.5f) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh,
                            contentDescription = "Brightness",
                            tint = AnimeCyan,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .weight(1f)
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(brightnessLevel)
                                    .background(AnimeCyan)
                            )
                        }

                        Text(
                            text = "${(brightnessLevel * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Gesture HUD: Horizontal Seek Delta Overlay
        if (showSeekOverlay) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeNeonRed.copy(alpha = 0.8f)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (seekDeltaSeconds >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = AnimeNeonRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (seekDeltaSeconds >= 0) "+" else ""}${seekDeltaSeconds}s",
                                color = AnimeNeonRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatDuration(seekTargetMs)} / ${formatDuration(totalDurationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 6. Buffering Spinner
        if (isBuffering && playbackError == null && !isLocked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AnimeNeonRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // 7. Playback Error / Server Switch Dialog Overlay
        if (playbackError != null && !isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AnimeDarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeNeonRed.copy(alpha = 0.6f)),
                    modifier = Modifier.widthIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = AnimeNeonRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Stream Connection Error",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The current server could not stream the requested file. Please switch to another server.",
                            color = AnimeTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Server Switch Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableServers.take(2).forEach { srv ->
                                Button(
                                    onClick = {
                                        selectedServer = srv
                                        playbackError = null
                                        Toast.makeText(context, "Switched to ${srv.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(srv.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showServerSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View All Servers", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 8. Screen Locked Floating Unlock Pill
        if (isLocked && showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AnimeNeonRed,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            isLocked = false
                            Toast.makeText(context, "Screen Unlocked", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tap to Unlock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 9. Redesigned Main Controls HUD
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // ==================== TOP BAR ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onBackClick() }
                                .testTag("player_back_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            val resolvedTitle = customTitle ?: anime?.title ?: "RS Anime Player"
                            val resolvedSubtitle = customSubtitle ?: when {
                                !offlineFilePath.isNullOrBlank() -> "Offline Playback"
                                anime?.type == AnimeType.MOVIE -> "Full Movie"
                                else -> "${currentSeason?.name ?: "Season 1"} • Episode $currentEpNum"
                            }

                            Text(
                                text = resolvedTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!offlineFilePath.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AnimeEmeraldGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = resolvedSubtitle,
                                    color = if (!offlineFilePath.isNullOrBlank()) AnimeEmeraldGreen else Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Top Right Badges & Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Server Selector Pill (Real Servers from Firebase)
                        if (offlineFilePath.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AnimeNeonRed.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showServerSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Dns, contentDescription = "Server", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedServer?.name ?: "RS FR 01",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Episode List Toggle (Series only)
                        if (anime?.type != AnimeType.MOVIE && (currentSeason?.episodes?.size ?: 0) > 1 && offlineFilePath.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { showEpisodeDrawer = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = "Episodes", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Download Button
                        if (offlineFilePath.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val dlAnime = anime ?: AnimeItem("1", "Video")
                                        DownloadHelper.startDownload(
                                            context = context,
                                            url = videoUrl,
                                            anime = dlAnime,
                                            episode = currentEpisode,
                                            seasonNumber = currentSeason?.seasonNumber ?: 1,
                                            quality = selectedQuality,
                                            userPrefsRepo = userPrefsRepo
                                        )
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download", tint = AnimeGold, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Screen Lock Button
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isLocked = true
                                    Toast.makeText(context, "Screen Locked", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Settings Button
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showSettingsSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // ==================== CENTER CONTROLS ====================
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (anime?.type != AnimeType.MOVIE) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(enabled = currentEpNum > 1) { playPreviousEpisode() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Episode",
                                    tint = if (currentEpNum > 1) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Rewind 10s
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { seekRelative(-10) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Main Center Play / Pause Button
                    Surface(
                        shape = CircleShape,
                        color = AnimeNeonRed,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { togglePlayPause() }
                            .testTag("player_play_pause_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Forward 10s
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { seekRelative(10) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (anime?.type != AnimeType.MOVIE) {
                        val hasNext = currentSeason?.episodes?.any { it.episodeNumber == currentEpNum + 1 } == true
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(enabled = hasNext) { playNextEpisode() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Episode",
                                    tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // ==================== REDESIGNED SLEEK BOTTOM BAR ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Sleek, Compact Progress Bar with Monospace Timers
                    val activePosMs = if (isUserScrubbing) scrubbedPositionMs else currentPositionMs
                    val durMax = if (totalDurationMs > 0) totalDurationMs else 1L

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current timestamp badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = formatDuration(activePosMs),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Compact Slim Slider
                        Slider(
                            value = activePosMs.toFloat().coerceIn(0f, durMax.toFloat()),
                            onValueChange = { newValue ->
                                isUserScrubbing = true
                                scrubbedPositionMs = newValue.toLong()
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(scrubbedPositionMs)
                                currentPositionMs = scrubbedPositionMs
                                isUserScrubbing = false
                            },
                            valueRange = 0f..durMax.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = AnimeNeonRed,
                                activeTrackColor = AnimeNeonRed,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                        )

                        // Total duration badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = formatDuration(durMax),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Control Row: Quality, Audio, Speed, Aspect Ratio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quality Pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AnimeGold.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showSettingsSheet = true }
                            ) {
                                Text(
                                    text = selectedQuality,
                                    color = AnimeGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Audio Track Pill
                            val currentLang = selectedAudioTrack ?: anime?.language ?: "Hindi"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showSettingsSheet = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = currentLang.take(14),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Speed Pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        playbackSpeed = when (playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            2.0f -> 0.75f
                                            else -> 1.0f
                                        }
                                        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                        Toast.makeText(context, "Speed: ${playbackSpeed}x", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Right Action: Aspect Ratio
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    val label = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit (Default)"
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom (Full Screen)"
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch (16:9)"
                                        else -> "Original"
                                    }
                                    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                        else -> "Fit"
                                    },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================== 10. SETTINGS MODAL BOTTOM SHEET ====================
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = AnimeDarkSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Playback Settings",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Video Resolution",
                        color = AnimeGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(qualityOptions) { quality ->
                            val isSelected = selectedQuality == quality
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val currentPos = exoPlayer.currentPosition
                                    selectedQuality = quality
                                    showSettingsSheet = false
                                    Toast.makeText(context, "Quality: $quality", Toast.LENGTH_SHORT).show()
                                    exoPlayer.seekTo(currentPos)
                                },
                                label = { Text(quality, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AnimeGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val audioTracks = currentEpisode?.audioTracks ?: emptyList()
                    Text(
                        text = "Audio Track",
                        color = AnimeGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val availableLangs = if (audioTracks.isNotEmpty()) {
                            audioTracks.map { it.name }
                        } else {
                            listOf("Hindi Dub", "Japanese (Original)")
                        }

                        availableLangs.forEach { lang ->
                            val isSelected = (selectedAudioTrack == lang) || (selectedAudioTrack == null && lang.contains("Hindi", ignoreCase = true))
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAudioTrack = lang
                                    showSettingsSheet = false
                                    Toast.makeText(context, "Audio: $lang", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(lang, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AnimeGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Playback Speed",
                        color = AnimeGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        items(speeds) { speed ->
                            val isSelected = playbackSpeed == speed
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    playbackSpeed = speed
                                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                                    showSettingsSheet = false
                                    Toast.makeText(context, "Speed: ${speed}x", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("${speed}x", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AnimeGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // ==================== 11. REAL FIREBASE SERVER SELECTOR SHEET ====================
        if (showServerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showServerSheet = false },
                containerColor = AnimeDarkSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = AnimeNeonRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Streaming Server (Real Host CDNs)",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switching server changes video CDN host without losing your current timestamp.",
                        color = AnimeTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableServers) { srv ->
                            val isSelected = (selectedServer?.id == srv.id) || (selectedServer == null && srv.isDefault)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AnimeNeonRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AnimeNeonRed) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val curPos = exoPlayer.currentPosition
                                        selectedServer = srv
                                        showServerSheet = false
                                        playbackError = null
                                        Toast.makeText(context, "Switched to ${srv.name}", Toast.LENGTH_SHORT).show()
                                        exoPlayer.seekTo(curPos)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = srv.name,
                                                color = if (isSelected) AnimeNeonRed else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (srv.locked) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AnimeGold.copy(alpha = 0.2f)
                                                ) {
                                                    Text("VIP", color = AnimeGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (srv.domain.isNotBlank()) "Host: ${srv.domain}" else "Default CDN Node",
                                            color = AnimeTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active", tint = AnimeNeonRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // ==================== 12. EPISODES DRAWER ====================
        if (showEpisodeDrawer && anime?.type != AnimeType.MOVIE) {
            ModalBottomSheet(
                onDismissRequest = { showEpisodeDrawer = false },
                containerColor = AnimeDarkSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Episodes - ${currentSeason?.name ?: "Season 1"}",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val episodes = currentSeason?.episodes ?: emptyList()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(episodes) { ep ->
                            val isCurrent = ep.episodeNumber == currentEpNum
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) AnimeNeonRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, AnimeNeonRed) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentEpNum = ep.episodeNumber
                                        showEpisodeDrawer = false
                                        onNextEpisode?.invoke(ep.episodeNumber)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Episode ${ep.episodeNumber}: ${ep.title}",
                                            color = if (isCurrent) AnimeNeonRed else Color.White,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (ep.isPremium) {
                                            Text("VIP Pass", color = AnimeGold, fontSize = 10.sp)
                                        }
                                    }
                                    if (isCurrent) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Playing", tint = AnimeNeonRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
