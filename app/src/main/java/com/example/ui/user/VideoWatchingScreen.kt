package com.example.ui.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.EpisodeEntity
import com.example.ui.ads.BottomBannerAd
import com.example.ui.components.GradientButton
import com.example.ui.components.NeonCard
import com.example.ui.components.PillBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoWatchingScreen(
    episode: EpisodeEntity,
    nextEpisode: EpisodeEntity?,
    onBack: () -> Unit,
    onConfirmWatchedAndProceed: (Activity?) -> Unit,
    onNavigateToNextEpisode: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var playerState by remember { mutableIntStateOf(-1) } // -1: unstarted, 0: ended, 1: playing, 2: paused, 3: buffering
    var currentSeconds by remember { mutableFloatStateOf(0f) }
    var totalDurationSeconds by remember {
        mutableFloatStateOf(episode.durationSeconds.toFloat().coerceAtLeast(60f))
    }
    var isPlayerReady by remember { mutableStateOf(false) }
    var hasPlaybackEnded by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<Int?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Intercept hardware back button to return to home/episodes
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PRITI MARATHI UPDATES",
                                color = NeonRoseBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonAmberGlow,
                                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "LOCAL TEST MODE",
                                    color = NeonAmberBright,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = episode.serialName,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("video_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "मागे जा",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { openOfficialYouTubeChannel(context) },
                        modifier = Modifier.testTag("topbar_channel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = "चॅनेल पहा",
                            tint = NeonRoseBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "चॅनेल पहा",
                            color = NeonRoseBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        bottomBar = {
            // Reserved Space for Anchored Google Test Banner Ad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
            ) {
                BottomBannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
        },
        containerColor = BackgroundDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // ==========================================
            // 1. MAIN LARGE VIDEO PLAYER AREA (16:9)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                YouTubeEmbeddedPlayer(
                    videoId = episode.youtubeVideoId,
                    onPlayerCreated = { wv -> webViewInstance = wv },
                    onReady = { isPlayerReady = true },
                    onStateChange = { state ->
                        playerState = state
                        if (state == 0) {
                            hasPlaybackEnded = true
                        }
                    },
                    onProgressUpdate = { curr, dur ->
                        currentSeconds = curr
                        if (dur > 0f) {
                            totalDurationSeconds = dur
                        }
                    },
                    onPlaybackEnded = {
                        playerState = 0
                        hasPlaybackEnded = true
                    },
                    onError = { code ->
                        playbackError = code
                    }
                )

                // Loading spinner overlay until ready
                if (!isPlayerReady && playbackError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonRoseBright,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "व्हिडिओ प्लेअर लोड होत आहे...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. BELOW THE PLAYER: SERIAL, TITLE, DATE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Serial Name Tag + Internal Episode Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = episode.serialName,
                        color = NeonRoseBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    // Internal episode metadata (secondary)
                    PillBadge(
                        text = "भाग ${episode.episodeNumber}",
                        backgroundColor = SurfaceVariantDark,
                        textColor = TextMuted,
                        borderColor = BorderSubtle
                    )
                }

                // Dynamic Episode Title
                Text(
                    text = if (episode.isTodayEpisode) "Today's Episode" else episode.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )

                // Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = NeonAmberBright,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = episode.publishDate,
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ==========================================
            // 3. WATCH PROGRESS SECTION (Real Player State)
            // ==========================================
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                borderColor = if (hasPlaybackEnded) EmeraldGreen.copy(alpha = 0.6f) else NeonRose.copy(alpha = 0.35f),
                backgroundColor = SurfaceDark
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsing dot indicator
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAlpha"
                            )

                            val dotColor = when {
                                hasPlaybackEnded -> EmeraldGreen
                                playerState == 1 -> NeonRoseBright
                                else -> NeonAmberBright
                            }

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor.copy(alpha = if (playerState == 1) pulseAlpha else 1f))
                            )

                            Text(
                                text = when {
                                    hasPlaybackEnded -> "व्हिडिओ पूर्ण झाला आहे."
                                    playerState == 1 -> "व्हिडिओ पाहत आहात"
                                    playerState == 2 -> "व्हिडिओ थांबवला आहे (Paused)"
                                    playerState == 3 -> "व्हिडिओ बफर होत आहे..."
                                    else -> "प्लेअर सज्ज — प्ले सुरू करा"
                                },
                                color = if (hasPlaybackEnded) EmeraldGreen else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Time display
                        Text(
                            text = "${formatTimeSeconds(currentSeconds)} / ${formatTimeSeconds(totalDurationSeconds)}",
                            color = NeonAmberBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Actual Progress Bar
                    val progressFraction = (currentSeconds / totalDurationSeconds.coerceAtLeast(1f))
                        .coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { if (hasPlaybackEnded) 1f else progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (hasPlaybackEnded) EmeraldGreen else NeonRoseBright,
                        trackColor = SurfaceVariantDark
                    )

                    // Assessment Lock/Unlock Status Notice
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasPlaybackEnded) EmeraldGreenGlow else Color(0x33000000),
                        border = BorderStroke(
                            1.dp,
                            if (hasPlaybackEnded) EmeraldGreen.copy(alpha = 0.5f) else BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (hasPlaybackEnded) "🔓" else "🔒",
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (hasPlaybackEnded) {
                                    "Assessment अनलॉक करण्यास सज्ज! खालील बटनावर टॅप करा."
                                } else {
                                    "Assessment कुलूपबंद आहे. व्हिडिओ पूर्ण संपल्यावरच अनलॉक होईल."
                                },
                                color = if (hasPlaybackEnded) EmeraldGreen else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (hasPlaybackEnded) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // Fallback / Testing Trigger for local validation
                    if (!hasPlaybackEnded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "प्लेअरमध्ये पूर्ण पाहिल्यावर ऑटोमॅटिक अनलॉक होईल",
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    hasPlaybackEnded = true
                                    playerState = 0
                                    currentSeconds = totalDurationSeconds
                                },
                                modifier = Modifier.testTag("test_complete_btn")
                            ) {
                                Text(
                                    text = "Test End ▶",
                                    color = NeonAmberBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. COMPLETION CONFIRMATION CARD
            // ==========================================
            AnimatedVisibility(
                visible = hasPlaybackEnded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    borderColor = EmeraldGreen,
                    backgroundColor = SurfaceDark
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreenGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎉", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = "व्हिडिओ पूर्ण झाला आहे.",
                                    color = EmeraldGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "व्हिडिओ पूर्ण पाहिला आहे का?",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "LOCAL TEST MODE: पुष्टी केल्यावर Google Test Interstitial जाहिरात दिसेल आणि 15 प्रश्नांचा Assessment अनलॉक होईल (प्रत्येक अचूक उत्तराला ₹2).",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        // Confirmation Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // "पुन्हा पाहा" Button
                            OutlinedButton(
                                onClick = {
                                    hasPlaybackEnded = false
                                    currentSeconds = 0f
                                    playerState = 1
                                    webViewInstance?.evaluateJavascript("replayVideo();", null)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "पुन्हा पाहा",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // "हो, पुढे चला" Primary Button
                            GradientButton(
                                text = "हो, पुढे चला",
                                onClick = {
                                    val activity = context as? Activity
                                    onConfirmWatchedAndProceed(activity)
                                },
                                icon = Icons.Default.ArrowForward,
                                colors = listOf(NeonRose, NeonAmber),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                testTag = "proceed_to_quiz_btn"
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 5. ACTION ROW: SHARE, CHANNEL, NEXT EPISODE, FALLBACK
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share Official YouTube Link Button
                OutlinedButton(
                    onClick = {
                        shareYouTubeLink(context, episode.title, episode.youtubeUrl)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("share_video_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDark,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NeonRoseBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Official Channel Button: "चॅनेल पहा"
                OutlinedButton(
                    onClick = { openOfficialYouTubeChannel(context) },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                        .testTag("watch_channel_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDark,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartDisplay,
                        contentDescription = "चॅनेल पहा",
                        tint = NeonRoseBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "चॅनेल पहा",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonRoseBright
                    )
                }

                // Next Episode Button (if available)
                if (nextEpisode != null) {
                    OutlinedButton(
                        onClick = { onNavigateToNextEpisode(nextEpisode) },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp)
                            .testTag("next_episode_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceDark,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(
                            text = "Next",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmberBright
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Episode",
                            tint = NeonAmberBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Fallback Button: Open Official YouTube App/Web if embedding has issues
            TextButton(
                onClick = {
                    openOfficialYouTubeApp(context, episode.youtubeVideoId, episode.youtubeUrl)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .testTag("open_official_youtube_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "अधिकृत YouTube ॲपमध्ये उघडा",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Official YouTube IFrame Player Embedded via WebView with bidirectional Javascript interface.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeEmbeddedPlayer(
    videoId: String,
    onPlayerCreated: (WebView) -> Unit,
    onReady: () -> Unit,
    onStateChange: (Int) -> Unit,
    onProgressUpdate: (Float, Float) -> Unit,
    onPlaybackEnded: () -> Unit,
    onError: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = NeonRoseBright,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "YouTube Player (Preview Mode)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val bridge = remember {
        YouTubePlayerBridge(
            onReady = onReady,
            onState = onStateChange,
            onProgressUpdate = onProgressUpdate,
            onEnded = onPlaybackEnded,
            onErrorReceived = onError
        )
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.apply {
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = userAgentString + " PritiMarathiUpdates"
                }

                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {}

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {
                        super.onPageFinished(view, url)
                        android.util.Log.d(
                            "PritiYouTube",
                            "YouTube page loaded: $url"
                        )
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(
                            view,
                            errorCode,
                            description,
                            failingUrl
                        )
                        android.util.Log.e(
                            "PritiYouTube",
                            "WebView error $errorCode: $description URL=$failingUrl"
                        )
                    }
                }

                addJavascriptInterface(bridge, "AndroidInterface")

                val html = createYouTubePlayerHtml(videoId)

                loadDataWithBaseURL(
                    "https://www.youtube.com/",
                    html,
                    "text/html",
                    "UTF-8",
                    "https://www.youtube.com/"
                )

                onPlayerCreated(this)
            }
        },
        update = { webView ->
            // Re-bind bridge callback if needed
        }
    )
}

/**
 * Javascript Interface to receive real-time playback state and progress from YouTube IFrame API.
 */
private class YouTubePlayerBridge(
    private val onReady: () -> Unit,
    private val onState: (Int) -> Unit,
    private val onProgressUpdate: (Float, Float) -> Unit,
    private val onEnded: () -> Unit,
    private val onErrorReceived: (Int) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPlayerReady() {
        handler.post { onReady() }
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        handler.post { onState(state) }
    }

    @JavascriptInterface
    fun onProgress(curr: Float, dur: Float) {
        handler.post { onProgressUpdate(curr, dur) }
    }

    @JavascriptInterface
    fun onPlaybackEnded() {
        handler.post { onEnded() }
    }

    @JavascriptInterface
    fun onError(code: Int) {
        handler.post { onErrorReceived(code) }
    }
}

/**
 * Generates the clean YouTube IFrame API wrapper HTML.
 */
private fun createYouTubePlayerHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body, html { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var timer = null;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'playsinline': 1,
                            'autoplay': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'controls': 1,
                            'fs': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }

                function onPlayerReady(event) {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onPlayerReady();
                    }
                    try {
                        event.target.playVideo();
                    } catch(e) {}
                }

                function onPlayerStateChange(event) {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onStateChange(event.data);
                    }
                    if (event.data === 1) { // Playing
                        startTimer();
                    } else {
                        stopTimer();
                        if (event.data === 0) { // Ended
                            sendProgress();
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onPlaybackEnded();
                            }
                        }
                    }
                }

                function startTimer() {
                    stopTimer();
                    sendProgress();
                    timer = setInterval(sendProgress, 500);
                }

                function stopTimer() {
                    if (timer) {
                        clearInterval(timer);
                        timer = null;
                    }
                }

                function sendProgress() {
                    if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                        var curr = player.getCurrentTime() || 0;
                        var dur = player.getDuration() || 0;
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onProgress(curr, dur);
                        }
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onError(event.data);
                    }
                }

                function replayVideo() {
                    if (player && typeof player.seekTo === 'function') {
                        player.seekTo(0, true);
                        player.playVideo();
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Formats seconds into "MM:SS" (e.g., 06:42).
 */
private fun formatTimeSeconds(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val mins = total / 60
    val secs = total % 60
    return String.format("%02d:%02d", mins, secs)
}

/**
 * Shares the official YouTube URL via standard Android share sheet.
 */
private fun shareYouTubeLink(context: Context, title: String, url: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "🎬 $title पाहा: $url\n\nPriti Marathi Updates वर ताज्या घडामोडी जाणून घ्या!")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "व्हिडिओ शेअर करा")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        // Fallback
    }
}

/**
 * Launches the official YouTube app or browser fallback.
 */
private fun openOfficialYouTubeApp(context: Context, videoId: String, url: String) {
    try {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appIntent)
    } catch (e: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}

const val OFFICIAL_YOUTUBE_CHANNEL_URL = "https://youtube.com/@pritimarathiupdates?si=ltuhnlml04ttfGqC"

/**
 * Launches the official Priti Marathi Updates YouTube Channel via standard Android ACTION_VIEW.
 */
fun openOfficialYouTubeChannel(context: Context) {
    try {
        val channelIntent = Intent(Intent.ACTION_VIEW, Uri.parse(OFFICIAL_YOUTUBE_CHANNEL_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(channelIntent)
    } catch (e: Exception) {
        // Fallback
    }
}

data class VideoWatchingPreviewData(
    val episode: EpisodeEntity,
    val nextEpisode: EpisodeEntity?
)

class VideoWatchingPreviewParameterProvider : PreviewParameterProvider<VideoWatchingPreviewData> {
    override val values: Sequence<VideoWatchingPreviewData> = sequenceOf(
        VideoWatchingPreviewData(
            episode = EpisodeEntity(
                episodeId = "sample_ep_1",
                serialName = "पाठराखीण",
                episodeNumber = 101,
                title = "आजचा विशेष भाग - महाएपिसोड",
                youtubeVideoId = "dQw4w9WgXcQ",
                youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                thumbnailUrl = "",
                durationSeconds = 600,
                durationFormatted = "10:00",
                publishDate = "28 सप्टेंबर 2026",
                description = "पाठराखीण मालिकेचा आजचा धमाकेदार भाग.",
                isTodayEpisode = true
            ),
            nextEpisode = EpisodeEntity(
                episodeId = "sample_ep_2",
                serialName = "पाठराखीण",
                episodeNumber = 102,
                title = "पुढील वळण",
                youtubeVideoId = "dQw4w9WgXcQ",
                youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                thumbnailUrl = "",
                durationSeconds = 600,
                durationFormatted = "10:00",
                publishDate = "29 सप्टेंबर 2026",
                description = "पुढील नवीन भाग."
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun VideoWatchingScreenPreview(
    @PreviewParameter(VideoWatchingPreviewParameterProvider::class) data: VideoWatchingPreviewData
) {
    MyApplicationTheme {
        VideoWatchingScreen(
            episode = data.episode,
            nextEpisode = data.nextEpisode,
            onBack = {},
            onConfirmWatchedAndProceed = {},
            onNavigateToNextEpisode = {}
        )
    }
}

