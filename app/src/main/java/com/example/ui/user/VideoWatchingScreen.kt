package com.example.ui.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
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
    initialWatchedSeconds: Int = 0,
    isAlreadyCompleted: Boolean = false,
    onBack: () -> Unit,
    onSaveProgress: (Int) -> Unit = {},
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

    // Anti-skip continuous watch tracking
    var actualWatchedSeconds by remember(episode.episodeId) {
        mutableFloatStateOf(initialWatchedSeconds.toFloat())
    }
    var lastReportedPosition by remember { mutableFloatStateOf(-1f) }
    var userSkippedForwardNotice by remember { mutableStateOf(false) }

    var isPlayerReady by remember { mutableStateOf(false) }
    var isVideoStarted by remember { mutableStateOf(false) }
    var hasPlaybackEnded by remember { mutableStateOf(false) }
    var isAssessmentUnlocked by remember(episode.episodeId) {
        mutableStateOf(isAlreadyCompleted)
    }
    var playbackError by remember { mutableStateOf<Int?>(null) }
    var useAlternativeHost by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Save watch progress locally when leaving screen
    DisposableEffect(episode.episodeId) {
        onDispose {
            onSaveProgress(actualWatchedSeconds.toInt())
        }
    }

    // Intercept hardware back button to return to home/episodes after saving progress
    BackHandler {
        onSaveProgress(actualWatchedSeconds.toInt())
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
                                    text = "OFFICIAL EPISODE",
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
                        onClick = {
                            onSaveProgress(actualWatchedSeconds.toInt())
                            onBack()
                        },
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
            // Reserved Space for Anchored Banner Ad
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
                // Official YouTube embedded player via WebView
                YouTubeEmbeddedPlayer(
                    videoId = episode.youtubeVideoId,
                    useAlternativeHost = useAlternativeHost,
                    onPlayerCreated = { wv -> webViewInstance = wv },
                    onReady = {
                        Log.d("PritiYouTube", "Player reached READY state")
                        isPlayerReady = true
                        playbackError = null
                    },
                    onStateChange = { state ->
                        Log.d("PritiYouTube", "Player state changed: $state")
                        playerState = state
                        if (state == 1) { // Playing
                            isVideoStarted = true
                            playbackError = null
                        } else if (state == 0) { // Ended
                            hasPlaybackEnded = true
                            val required = totalDurationSeconds * 0.95f
                            if (actualWatchedSeconds >= required) {
                                isAssessmentUnlocked = true
                            } else {
                                userSkippedForwardNotice = true
                            }
                        } else {
                            // Paused or buffering — reset jump delta reference
                            lastReportedPosition = -1f
                        }
                    },
                    onProgressUpdate = { curr, dur ->
                        currentSeconds = curr
                        if (dur > 0f) {
                            totalDurationSeconds = dur
                        }
                        if (playerState == 1) { // Only while actively playing
                            if (lastReportedPosition >= 0f) {
                                val delta = curr - lastReportedPosition
                                if (delta in 0.05f..2.5f) {
                                    // Legitimate continuous playback: add only real played seconds
                                    actualWatchedSeconds += delta
                                    userSkippedForwardNotice = false
                                } else if (delta > 2.5f) {
                                    // User seeked/jumped forward — do NOT count skipped time!
                                    userSkippedForwardNotice = true
                                }
                            }
                            lastReportedPosition = curr

                            // Check legitimate completion (95% genuine watch requirement)
                            if (actualWatchedSeconds >= (totalDurationSeconds * 0.95f) && hasPlaybackEnded) {
                                isAssessmentUnlocked = true
                            }
                        }
                    },
                    onPlaybackEnded = {
                        playerState = 0
                        hasPlaybackEnded = true
                        val required = totalDurationSeconds * 0.95f
                        if (actualWatchedSeconds >= required) {
                            isAssessmentUnlocked = true
                        } else {
                            userSkippedForwardNotice = true
                        }
                    },
                    onError = { code ->
                        Log.e("PritiYouTube", "YouTube player error: $code")
                        if (playerState != 1) {
                            playbackError = code
                        }
                    }
                )

                // Real YouTube Poster Thumbnail before playback starts (prevents black screen)
                if (!isVideoStarted && playbackError == null) {
                    val realThumbUrl = episode.thumbnailUrl.ifEmpty {
                        "https://img.youtube.com/vi/${episode.youtubeVideoId}/hqdefault.jpg"
                    }
                    AsyncImage(
                        model = realThumbUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Translucent dark scrim + clear Play button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isVideoStarted = true
                                    webViewInstance?.evaluateJavascript("startPlayVideo();", null)
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(NeonRose)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "व्हिडिओ सुरू करा",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Text(
                                text = "व्हिडिओ सुरू करण्यासाठी टॅप करा",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // In-App Error Banner with visible error code and recovery options
                if (playbackError != null && playerState != 1) {
                    val errorCode = playbackError!!
                    val errorTitle = when (errorCode) {
                        2 -> "अवैध व्हिडिओ पॅरामीटर (Invalid Parameter)"
                        5 -> "HTML5 प्लेअर एरर (HTML5 Player Error)"
                        100 -> "व्हिडिओ उपलब्ध नाही (Video Removed / Private)"
                        101 -> "या व्हिडिओचे एम्बेडिंग मर्यादित आहे (Embedding Restricted)"
                        150 -> "व्हिडिओ एम्बेडिंग निर्बंध (Error 150: Embedding Restricted)"
                        152 -> "YouTube प्लेअर त्रुटी (Error Code: 152)"
                        153 -> "एम्बेडर पडताळणी अयशस्वी (Error 153: Verification Failed)"
                        else -> "YouTube प्लेअर त्रुटी (Error Code: $errorCode)"
                    }
                    val errorDescription = when (errorCode) {
                        152 -> "YouTube सुरक्षा पडताळणी अडचण. 'पुन्हा प्रयत्न करा' किंवा थेट YouTube वर पहा."
                        101, 150 -> "YouTube च्या सुरक्षा नियमांनुसार एम्बेड प्लेबॅकमध्ये मर्यादा आहे. आपण थेट YouTube ॲपमध्ये पाहू शकता."
                        100 -> "हा व्हिडिओ YouTube वरून काढला गेला आहे किंवा खाजगी करण्यात आला आहे."
                        else -> "व्हिडिओ प्ले करताना तांत्रिक अडचण आली आहे. इंटरनेट कनेक्शन तपासा किंवा पुन्हा लोड करा."
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceDark.copy(alpha = 0.95f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NeonAmberBright,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = errorTitle,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "त्रुटी कोड: $errorCode — $errorDescription",
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        playbackError = null
                                        webViewInstance?.reload()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("पुन्हा प्रयत्न करा (Retry)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        openOfficialYouTubeApp(context, episode.youtubeVideoId, episode.youtubeUrl)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("YouTube वर पहा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        playbackError = null
                                        useAlternativeHost = !useAlternativeHost
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonAmberBright),
                                    border = BorderStroke(1.dp, NeonAmberBright.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        if (useAlternativeHost) "Standard" else "No-Cookie",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (errorCode == 101 || errorCode == 150) {
                                Button(
                                    onClick = {
                                        playbackError = null
                                        hasPlaybackEnded = true
                                        actualWatchedSeconds = totalDurationSeconds
                                        isAssessmentUnlocked = true
                                        onSaveProgress(totalDurationSeconds.toInt())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("पाहिल्याची नोंद करा (+५० नाणी)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(
                                onClick = { playbackError = null }
                            ) {
                                Text("लपवा (Dismiss)", color = TextMuted, fontSize = 11.sp)
                            }
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
            // 3. WATCH PROGRESS SECTION (Anti-Skip Watch Tracking)
            // ==========================================
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                borderColor = if (isAssessmentUnlocked) EmeraldGreen.copy(alpha = 0.6f) else NeonRose.copy(alpha = 0.35f),
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
                                isAssessmentUnlocked -> EmeraldGreen
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
                                    isAssessmentUnlocked -> "व्हिडिओ पूर्ण पाहिला आहे"
                                    playerState == 1 -> "व्हिडिओ पाहत आहात..."
                                    playerState == 2 -> "व्हिडिओ थांबवला आहे (Paused)"
                                    playerState == 3 -> "व्हिडिओ बफर होत आहे..."
                                    else -> "व्हिडिओ सुरू करा"
                                },
                                color = if (isAssessmentUnlocked) EmeraldGreen else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Real Continuous Watch Time Display
                        val watchedPercentage = ((actualWatchedSeconds / totalDurationSeconds.coerceAtLeast(1f)) * 100).toInt().coerceIn(0, 100)
                        Text(
                            text = "${formatTimeSeconds(actualWatchedSeconds)} / ${formatTimeSeconds(totalDurationSeconds)} ($watchedPercentage%)",
                            color = if (isAssessmentUnlocked) EmeraldGreen else NeonAmberBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Anti-Skip Continuous Progress Bar
                    val progressFraction = (actualWatchedSeconds / totalDurationSeconds.coerceAtLeast(1f)).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { if (isAssessmentUnlocked) 1f else progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isAssessmentUnlocked) EmeraldGreen else NeonRoseBright,
                        trackColor = SurfaceVariantDark
                    )

                    // Skip Warning if user seeked forward
                    if (userSkippedForwardNotice && !isAssessmentUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonAmberGlow,
                            border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "⚠️", fontSize = 15.sp)
                                Text(
                                    text = "व्हिडिओ पुढे ढकलला गेला (Skip) आहे. Assessment अनलॉक करण्यासाठी व्हिडिओ अखंड पाहणे आवश्यक आहे.",
                                    color = NeonAmberBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Assessment Lock/Unlock Status Notice
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAssessmentUnlocked) EmeraldGreenGlow else Color(0x33000000),
                        border = BorderStroke(
                            1.dp,
                            if (isAssessmentUnlocked) EmeraldGreen.copy(alpha = 0.5f) else BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isAssessmentUnlocked) "✅" else "🔒",
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isAssessmentUnlocked) {
                                    "Assessment Unlocked! खालील बटनावर टॅप करा."
                                } else {
                                    "Assessment Locked — पूर्ण व्हिडिओ पाहिल्यानंतर Assessment सुरू होईल."
                                },
                                color = if (isAssessmentUnlocked) EmeraldGreen else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isAssessmentUnlocked) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. COMPLETION CONFIRMATION CARD
            // ==========================================
            AnimatedVisibility(
                visible = isAssessmentUnlocked,
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
                                    text = "Assessment सुरू करण्यास सज्ज!",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "पुष्टी केल्यावर जाहिरात दिसेल आणि आजच्या 15 प्रश्नांचा Assessment अनलॉक होईल (प्रत्येक अचूक उत्तराला ₹2).",
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
    useAlternativeHost: Boolean = false,
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

    val embedHost = if (useAlternativeHost) "https://www.youtube-nocookie.com" else "https://www.youtube.com"
    val appOrigin = embedHost

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
                tag = embedHost
                setBackgroundColor(android.graphics.Color.BLACK)

                // Ensure WebView code cache directory exists to prevent Chromium simple_file_enumerator error
                try {
                    val codeCacheDir = java.io.File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
                    if (!codeCacheDir.exists()) {
                        codeCacheDir.mkdirs()
                    }
                } catch (e: Exception) {
                    // Ignore cache dir setup
                }

                // Enable cookies and third-party cookies (essential for YouTube embed auth/session)
                try {
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                } catch (e: Exception) {
                    Log.w("PritiYouTube", "CookieManager init warning: ${e.message}")
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = true

                    // Remove "; wv" from user agent to allow seamless YouTube embedding without WebView blocks
                    val defaultUa = userAgentString ?: ""
                    userAgentString = if (defaultUa.contains("; wv")) {
                        defaultUa.replace("; wv", "")
                    } else if (defaultUa.isEmpty()) {
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    } else {
                        defaultUa
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("PritiYouTube", "Console [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("favicon.ico")) {
                            return WebResourceResponse("image/x-icon", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                        }
                        val headers = request?.requestHeaders
                        val referer = headers?.get("Referer") ?: headers?.get("referer") ?: "none"
                        val origin = headers?.get("Origin") ?: headers?.get("origin") ?: "none"
                        if (request?.isForMainFrame == true || url.contains("youtube.com") || url.contains("youtube-nocookie.com")) {
                            Log.d("PritiYouTube", "Request: isMainFrame=${request?.isForMainFrame} url=$url | Referer=$referer | Origin=$origin")
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("favicon.ico")) return
                        super.onReceivedError(view, request, error)
                        Log.e("PritiYouTube", "WebView Error on URL: ${request?.url} code: ${error?.errorCode} desc: ${error?.description}")
                    }

                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("favicon.ico")) return
                        super.onReceivedHttpError(view, request, errorResponse)
                        Log.e("PritiYouTube", "WebView HTTP Error on URL: ${request?.url} status: ${errorResponse?.statusCode}")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("PritiYouTube", "onPageFinished: $url")
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        Log.d("PritiYouTube", "shouldOverrideUrlLoading: $url")
                        if (url.contains("youtube.com") || url.contains("youtube-nocookie.com") || url.contains("googlevideo.com") || url.contains("ytimg.com")) {
                            return false
                        }
                        return true
                    }
                }

                addJavascriptInterface(bridge, "AndroidInterface")

                val html = createYouTubePlayerHtml(videoId, embedHost, appOrigin)
                Log.d("PritiYouTube", "Loading local HTML with BaseURL: $appOrigin | embedHost: $embedHost | videoId: $videoId")
                loadDataWithBaseURL(appOrigin, html, "text/html", "UTF-8", null)
                onPlayerCreated(this)
            }
        },
        update = { webView ->
            val currentLoadedHost = webView.tag as? String
            if (currentLoadedHost != null && currentLoadedHost != embedHost) {
                webView.tag = embedHost
                val updatedHtml = createYouTubePlayerHtml(videoId, embedHost, embedHost)
                Log.d("PritiYouTube", "Host switched to: $embedHost with BaseURL: $embedHost")
                webView.loadDataWithBaseURL(embedHost, updatedHtml, "text/html", "UTF-8", null)
            }
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
        Log.d("PritiYouTube", "Bridge: onPlayerReady received from JavaScript")
        handler.post { onReady() }
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        Log.d("PritiYouTube", "Bridge: onStateChange: $state")
        handler.post { onState(state) }
    }

    @JavascriptInterface
    fun onProgress(curr: Float, dur: Float) {
        handler.post { onProgressUpdate(curr, dur) }
    }

    @JavascriptInterface
    fun onPlaybackEnded() {
        Log.d("PritiYouTube", "Bridge: onPlaybackEnded received")
        handler.post { onEnded() }
    }

    @JavascriptInterface
    fun onError(code: Int) {
        Log.e("PritiYouTube", "Bridge: onError received code: $code")
        handler.post { onErrorReceived(code) }
    }

    @JavascriptInterface
    fun onLog(msg: String) {
        Log.d("PritiYouTube", "Bridge JS: $msg")
    }
}

/**
 * Generates the clean YouTube IFrame API wrapper HTML.
 * Both the base URL and YouTube iframe origin/widget_referrer strictly match appOrigin,
 * preventing cross-origin mismatch and Error 152/153.
 */
private fun createYouTubePlayerHtml(videoId: String, embedHost: String, appOrigin: String): String {
    val embedUrl = "$embedHost/embed/$videoId?enablejsapi=1&autoplay=1&playsinline=1&controls=1&rel=0&fs=1&origin=$appOrigin&widget_referrer=$appOrigin"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <meta http-equiv="Referrer-Policy" content="strict-origin-when-cross-origin">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body, html { width: 100%; height: 100%; background: #000000; overflow: hidden; }
                #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; }
                iframe { width: 100% !important; height: 100% !important; border: 0 !important; }
            </style>
        </head>
        <body>
            <div id="player"></div>

            <script>
                var player = null;
                var progressTimer = null;
                var isPlayerReady = false;
                var appOrigin = "$appOrigin";
                var embedHost = "$embedHost";
                var videoId = "$videoId";

                function log(msg) {
                    console.log("[PritiYouTube] " + msg);
                    if (window.AndroidInterface && window.AndroidInterface.onLog) {
                        window.AndroidInterface.onLog(msg);
                    }
                }

                // Dual message listener: captures postMessage from embedded YouTube iframe
                window.addEventListener('message', function(event) {
                    try {
                        var data = event.data;
                        if (typeof data === 'string') {
                            try { data = JSON.parse(data); } catch(e) { return; }
                        }
                        if (!data) return;

                        if (data.event === 'onReady' || data.event === 'ready') {
                            handleReady();
                        } else if (data.event === 'onStateChange') {
                            handleState(data.info);
                        } else if (data.event === 'infoDelivery' && data.info) {
                            if (data.info.currentTime !== undefined) {
                                var curr = data.info.currentTime || 0;
                                var dur = data.info.duration || 0;
                                if (window.AndroidInterface && window.AndroidInterface.onProgress) {
                                    window.AndroidInterface.onProgress(curr, dur);
                                }
                            }
                            if (data.info.playerState !== undefined) {
                                handleState(data.info.playerState);
                            }
                        } else if (data.event === 'onError') {
                            handleError(data.info);
                        }
                    } catch(e) {
                        log("Message parse error: " + e.message);
                    }
                });

                // Load official YouTube IFrame API
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                function onYouTubeIframeAPIReady() {
                    log("onYouTubeIframeAPIReady fired. Initializing YT.Player with origin: " + appOrigin);
                    try {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: videoId,
                            host: embedHost,
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'controls': 1,
                                'rel': 0,
                                'fs': 1,
                                'enablejsapi': 1,
                                'origin': appOrigin,
                                'widget_referrer': appOrigin
                            },
                            events: {
                                'onReady': function(e) {
                                    log("YT.Player onReady");
                                    handleReady();
                                    try { e.target.playVideo(); } catch(err) {}
                                },
                                'onStateChange': function(e) {
                                    log("YT.Player state: " + e.data);
                                    handleState(e.data);
                                },
                                'onError': function(e) {
                                    log("YT.Player error: " + e.data);
                                    handleError(e.data);
                                }
                            }
                        });
                    } catch (err) {
                        log("YT.Player init exception: " + err.message);
                        createIframeFallback();
                    }
                }

                // Fallback: If YT.Player doesn't initialize within 3s, inject direct iframe with matching origin
                setTimeout(function() {
                    if (!isPlayerReady && !player) {
                        log("API timeout fallback: injecting iframe with matching origin");
                        createIframeFallback();
                    }
                }, 3000);

                function createIframeFallback() {
                    var container = document.getElementById('player');
                    if (!container || container.querySelector('iframe')) return;
                    var iframeUrl = "$embedUrl";
                    log("Fallback iframe URL: " + iframeUrl);
                    container.innerHTML = '<iframe id="yt_iframe" width="100%" height="100%" src="' + iframeUrl + '" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>';
                }

                function handleReady() {
                    if (isPlayerReady) return;
                    isPlayerReady = true;
                    log("Player is READY");
                    if (window.AndroidInterface && window.AndroidInterface.onPlayerReady) {
                        window.AndroidInterface.onPlayerReady();
                    }
                }

                function handleState(state) {
                    log("Handle state: " + state);
                    if (window.AndroidInterface && window.AndroidInterface.onStateChange) {
                        window.AndroidInterface.onStateChange(state);
                    }
                    if (state === 1) { // Playing
                        startProgressTimer();
                    } else {
                        stopProgressTimer();
                        if (state === 0) { // Ended
                            sendProgress();
                            if (window.AndroidInterface && window.AndroidInterface.onPlaybackEnded) {
                                window.AndroidInterface.onPlaybackEnded();
                            }
                        }
                    }
                }

                function handleError(code) {
                    log("Handle error: " + code);
                    if (window.AndroidInterface && window.AndroidInterface.onError) {
                        window.AndroidInterface.onError(code);
                    }
                }

                function startProgressTimer() {
                    stopProgressTimer();
                    sendProgress();
                    progressTimer = setInterval(sendProgress, 500);
                }

                function stopProgressTimer() {
                    if (progressTimer) {
                        clearInterval(progressTimer);
                        progressTimer = null;
                    }
                }

                function sendProgress() {
                    try {
                        if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                            var curr = player.getCurrentTime() || 0;
                            var dur = player.getDuration() || 0;
                            if (window.AndroidInterface && window.AndroidInterface.onProgress) {
                                window.AndroidInterface.onProgress(curr, dur);
                            }
                        }
                    } catch(e) {}
                }

                function startPlayVideo() {
                    log("startPlayVideo called");
                    try {
                        if (player && typeof player.playVideo === 'function') {
                            player.playVideo();
                        } else {
                            var iframe = document.getElementById('yt_iframe') || document.querySelector('iframe');
                            if (iframe && iframe.contentWindow) {
                                iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                            }
                        }
                    } catch(e) {
                        log("startPlayVideo err: " + e.message);
                    }
                }

                function replayVideo() {
                    try {
                        if (player && typeof player.seekTo === 'function') {
                            player.seekTo(0, true);
                            player.playVideo();
                        } else {
                            var iframe = document.getElementById('yt_iframe') || document.querySelector('iframe');
                            if (iframe && iframe.contentWindow) {
                                iframe.contentWindow.postMessage('{"event":"command","func":"seekTo","args":[0, true]}', '*');
                                iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                            }
                        }
                    } catch(e) {}
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
                episodeNumber = 106,
                title = "Pathrakhin Today Episode 29 September 2026",
                youtubeVideoId = "-GS0owKJqlQ",
                youtubeUrl = "https://www.youtube.com/watch?v=-GS0owKJqlQ",
                thumbnailUrl = "https://i.ytimg.com/vi/-GS0owKJqlQ/hqdefault.jpg",
                durationSeconds = 540,
                durationFormatted = "09:00",
                publishDate = "29 सप्टेंबर 2026",
                description = "पाठराखीण मालिकेचा आजचा धमाकेदार भाग.",
                isTodayEpisode = true
            ),
            nextEpisode = EpisodeEntity(
                episodeId = "sample_ep_2",
                serialName = "पाठराखीण",
                episodeNumber = 105,
                title = "Pathrakhin Today's Episode 27 September 2026",
                youtubeVideoId = "KcysA63NQG0",
                youtubeUrl = "https://www.youtube.com/watch?v=KcysA63NQG0",
                thumbnailUrl = "https://i.ytimg.com/vi/KcysA63NQG0/hqdefault.jpg",
                durationSeconds = 515,
                durationFormatted = "08:35",
                publishDate = "27 सप्टेंबर 2026",
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

