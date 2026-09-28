package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.admin.AdminPanelScreen
import com.example.ui.ads.BottomBannerAd
import com.example.ui.components.BrandHeader
import com.example.ui.theme.*
import com.example.ui.user.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "MobileAds initialization error: ${e.message}")
        }
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel? = if (androidx.compose.ui.platform.LocalInspectionMode.current) null else viewModel()
) {
    if (viewModel == null) {
        MainAppPreviewContent()
        return
    }

    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val userTab by viewModel.userTab.collectAsState()
    val todayEpisode by viewModel.todayEpisode.collectAsState()
    val publishedEpisodes by viewModel.publishedEpisodes.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val walletSummary by viewModel.walletSummary.collectAsState()
    val userProgressList by viewModel.userProgressList.collectAsState()
    val userTransactions by viewModel.userTransactions.collectAsState()
    val activeAnnouncements by viewModel.activeAnnouncements.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val showAdminPinPrompt by viewModel.showAdminPinPrompt.collectAsState()
    val watchCompletionPrompt by viewModel.watchCompletionPrompt.collectAsState()
    val currentWatchingEpisode by viewModel.currentWatchingEpisode.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val userQuizAnswers by viewModel.userQuizAnswers.collectAsState()
    val quizResult by viewModel.quizResult.collectAsState()
    val activeQuizEpisode by viewModel.activeQuizEpisode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Full-Screen Dedicated Video Watching Experience
    if (currentWatchingEpisode != null) {
        val watchingEp = currentWatchingEpisode!!
        val nextEp = viewModel.getNextEpisode(watchingEp)
        VideoWatchingScreen(
            episode = watchingEp,
            nextEpisode = nextEp,
            onBack = { viewModel.stopWatchingEpisode() },
            onConfirmWatchedAndProceed = { act ->
                viewModel.confirmVideoWatchedFromWatchingScreen(act, watchingEp)
            },
            onNavigateToNextEpisode = { nEp ->
                viewModel.startWatchingEpisode(nEp)
            }
        )
    } else if (currentRole == "ADMIN") {
        BackHandler {
            viewModel.switchToUserRole()
        }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            AdminPanelScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    } else {
        // Normal User App Flow
        BackHandler(enabled = userTab != 0) {
            viewModel.selectUserTab(0)
        }

        Scaffold(
            topBar = {
                BrandHeader(
                    currentRole = currentRole,
                    streakDays = userProfile?.streakDays ?: 1,
                    onToggleRole = { viewModel.openAdminPrompt() }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                ) {
                    NavigationBar(
                        containerColor = SurfaceDark,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        val navItems = listOf(
                            Triple(0, "मुख्य", Icons.Filled.Home to Icons.Outlined.Home),
                            Triple(1, "Episodes", Icons.Filled.VideoLibrary to Icons.Outlined.VideoLibrary),
                            Triple(2, "Assessment", Icons.Filled.Quiz to Icons.Outlined.Quiz),
                            Triple(3, "वॉलेट", Icons.Filled.AccountBalanceWallet to Icons.Outlined.AccountBalanceWallet),
                            Triple(4, "प्रोफाइल", Icons.Filled.Person to Icons.Outlined.Person)
                        )

                        navItems.forEach { (tabIndex, label, icons) ->
                            val isSelected = userTab == tabIndex
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectUserTab(tabIndex) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) icons.first else icons.second,
                                        contentDescription = label,
                                        tint = if (isSelected) NeonRoseBright else TextMuted
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        color = if (isSelected) NeonRoseBright else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = NeonRoseGlow
                                ),
                                modifier = Modifier.testTag("nav_item_$tabIndex")
                            )
                        }
                    }

                    // Anchored Bottom Banner Test Ad with Reserved Space & Navigation Bar Insets
                    BottomBannerAd(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = BackgroundDark,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = userTab, label = "UserTabCrossfade") { targetTab ->
                    when (targetTab) {
                        0 -> UserHomeScreen(
                            todayEpisode = todayEpisode,
                            userProfile = userProfile,
                            walletSummary = walletSummary,
                            appSettings = appSettings,
                            announcements = activeAnnouncements,
                            userProgressList = userProgressList,
                            onWatchEpisode = { ep ->
                                viewModel.startWatchingEpisode(ep)
                            },
                            onStartQuiz = { ep ->
                                viewModel.prepareQuiz(ep)
                            },
                            onViewEpisodes = { viewModel.selectUserTab(1) },
                            onViewWallet = { viewModel.selectUserTab(3) }
                        )

                        1 -> EpisodesScreen(
                            episodes = publishedEpisodes,
                            userProgressList = userProgressList,
                            onSelectEpisode = { ep ->
                                viewModel.startWatchingEpisode(ep)
                            }
                        )

                        2 -> QuizScreen(
                            episode = activeQuizEpisode ?: todayEpisode,
                            questions = quizQuestions,
                            currentIndex = currentQuestionIndex,
                            userAnswers = userQuizAnswers,
                            quizResult = quizResult,
                            walletSummary = walletSummary,
                            onSelectAnswer = { qId, opt -> viewModel.selectQuizAnswer(qId, opt) },
                            onNextQuestion = { viewModel.nextQuizQuestion() },
                            onPrevQuestion = { viewModel.previousQuizQuestion() },
                            onSubmitQuiz = { viewModel.submitQuiz() },
                            onExitQuiz = { viewModel.exitQuiz() },
                            onViewWallet = { viewModel.selectUserTab(3) }
                        )

                        3 -> WalletScreen(
                            walletSummary = walletSummary,
                            transactions = userTransactions,
                            appSettings = appSettings,
                            userName = userProfile?.name ?: "प्रिया देशमुख",
                            userPhone = userProfile?.phoneOrEmail ?: "+91 98765 43210",
                            onRequestWithdrawal = { amount, method, address ->
                                viewModel.submitWithdrawalRequest(
                                    amount = amount,
                                    payoutMethod = method,
                                    payoutAddress = address,
                                    userName = userProfile?.name ?: "प्रिया देशमुख",
                                    userPhone = userProfile?.phoneOrEmail ?: "+91 98765 43210"
                                )
                            }
                        )

                        4 -> ProfileScreen(
                            userProfile = userProfile,
                            walletSummary = walletSummary,
                            onOpenAdminMode = { viewModel.openAdminPrompt() }
                        )
                    }
                }
            }
        }
    }

    // YouTube Watch Completion Confirmation Dialog (TEST MODE)
    if (watchCompletionPrompt != null) {
        WatchCompletionDialog(
            episode = watchCompletionPrompt!!,
            onDismiss = { viewModel.dismissWatchCompletion() },
            onConfirmWatched = {
                val activity = context as? Activity
                viewModel.confirmVideoWatched(activity, watchCompletionPrompt!!)
            }
        )
    }

    // Admin PIN Verification Dialog
    if (showAdminPinPrompt) {
        AdminPinDialog(
            onDismiss = { viewModel.dismissAdminPrompt() },
            onVerifyPin = { pin -> viewModel.verifyAdminPin(pin) }
        )
    }
}

@Composable
fun MainAppPreviewContent(
    initialTab: Int = 0
) {
    val sampleEpisode = com.example.data.model.EpisodeEntity(
        episodeId = "ep_today",
        serialName = "ठरलं तर मग",
        episodeNumber = 620,
        title = "सायली आणि अर्जुनचा मोठा निर्णय!",
        youtubeVideoId = "dQw4w9WgXcQ",
        youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        thumbnailUrl = "",
        durationSeconds = 660,
        durationFormatted = "11:00",
        publishDate = "28 सप्टेंबर 2026",
        description = "मालिकेचा आजचा अत्यंत भावनिक आणि रंजक भाग.",
        isTodayEpisode = true
    )
    val sampleProfile = com.example.data.model.UserProfileEntity(
        userId = "u1",
        name = "प्रिया देशमुख",
        phoneOrEmail = "+91 98765 43210",
        joinedDate = "सप्टेंबर 2026",
        streakDays = 3,
        lastActiveDate = "28 सप्टेंबर 2026"
    )
    val sampleWallet = com.example.data.repository.WalletSummary(
        currentBalance = 80.0,
        totalEarnings = 150.0,
        paidAmount = 70.0,
        pendingAmount = 0.0
    )

    var currentTab by remember { mutableIntStateOf(initialTab) }

    Scaffold(
        topBar = {
            BrandHeader(
                currentRole = "USER",
                streakDays = sampleProfile.streakDays,
                onToggleRole = {}
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
            ) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    val navItems = listOf(
                        Triple(0, "मुख्य", Icons.Filled.Home to Icons.Outlined.Home),
                        Triple(1, "Episodes", Icons.Filled.VideoLibrary to Icons.Outlined.VideoLibrary),
                        Triple(2, "Assessment", Icons.Filled.Quiz to Icons.Outlined.Quiz),
                        Triple(3, "वॉलेट", Icons.Filled.AccountBalanceWallet to Icons.Outlined.AccountBalanceWallet),
                        Triple(4, "प्रोफाइल", Icons.Filled.Person to Icons.Outlined.Person)
                    )

                    navItems.forEach { (tabIndex, label, icons) ->
                        val isSelected = currentTab == tabIndex
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tabIndex },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) icons.first else icons.second,
                                    contentDescription = label,
                                    tint = if (isSelected) NeonRoseBright else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (isSelected) NeonRoseBright else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = NeonRoseGlow
                            )
                        )
                    }
                }

                BottomBannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
        },
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> UserHomeScreen(
                    todayEpisode = sampleEpisode,
                    userProfile = sampleProfile,
                    walletSummary = sampleWallet,
                    appSettings = com.example.data.model.AppSettingsEntity(),
                    announcements = emptyList(),
                    userProgressList = emptyList(),
                    onWatchEpisode = {},
                    onStartQuiz = {},
                    onViewEpisodes = { currentTab = 1 },
                    onViewWallet = { currentTab = 3 }
                )
                1 -> EpisodesScreen(
                    episodes = listOf(sampleEpisode),
                    userProgressList = emptyList(),
                    onSelectEpisode = {}
                )
                2 -> QuizScreen(
                    episode = sampleEpisode,
                    questions = listOf(
                        com.example.data.model.QuestionEntity(
                            questionId = "q1",
                            episodeId = "ep1",
                            questionText = "आजच्या भागात काय घडले?",
                            optionA = "पर्याय १",
                            optionB = "पर्याय २",
                            optionC = "पर्याय ३",
                            optionD = "पर्याय ४",
                            correctAnswer = 0,
                            explanation = ""
                        )
                    ),
                    currentIndex = 0,
                    userAnswers = emptyMap(),
                    quizResult = null,
                    walletSummary = sampleWallet,
                    onSelectAnswer = { _, _ -> },
                    onNextQuestion = {},
                    onPrevQuestion = {},
                    onSubmitQuiz = {},
                    onExitQuiz = {},
                    onViewWallet = { currentTab = 3 }
                )
                3 -> WalletScreen(
                    walletSummary = sampleWallet,
                    transactions = emptyList(),
                    appSettings = com.example.data.model.AppSettingsEntity(),
                    userName = sampleProfile.name,
                    userPhone = sampleProfile.phoneOrEmail,
                    onRequestWithdrawal = { _, _, _ -> }
                )
                4 -> ProfileScreen(
                    userProfile = sampleProfile,
                    walletSummary = sampleWallet,
                    onOpenAdminMode = {}
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    MyApplicationTheme {
        MainAppPreviewContent()
    }
}

