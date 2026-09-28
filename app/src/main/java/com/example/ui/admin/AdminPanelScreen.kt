package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.GradientButton
import com.example.ui.components.NeonCard
import com.example.ui.components.PillBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminPanelScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val adminTab by viewModel.adminTab.collectAsState()
    val allEpisodes by viewModel.allEpisodes.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allWithdrawals by viewModel.allWithdrawals.collectAsState()
    val allAnnouncements by viewModel.allAnnouncements.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    AdminPanelContent(
        adminTab = adminTab,
        allEpisodes = allEpisodes,
        allQuestions = allQuestions,
        allUsers = allUsers,
        allWithdrawals = allWithdrawals,
        allAnnouncements = allAnnouncements,
        appSettings = appSettings ?: AppSettingsEntity(),
        auditLogs = auditLogs,
        allTransactions = allTransactions,
        onSelectTab = { viewModel.selectAdminTab(it) },
        onSwitchToUserRole = { viewModel.switchToUserRole() },
        onResetTestData = { viewModel.adminResetTestData() },
        onSaveEpisode = { viewModel.adminSaveEpisode(it) },
        onDeleteEpisode = { viewModel.adminDeleteEpisode(it) },
        onSetTodayEpisode = { viewModel.adminSetTodayEpisode(it) },
        onSaveQuestion = { viewModel.adminSaveQuestion(it) },
        onDeleteQuestion = { viewModel.adminDeleteQuestion(it) },
        onSaveSettings = { viewModel.adminUpdateSettings(it) },
        onToggleSuspendUser = { viewModel.adminToggleUserSuspension(it) },
        onUpdateWithdrawalStatus = { id, status, note -> viewModel.adminUpdateWithdrawal(id, status, note) },
        onSaveAnnouncement = { viewModel.adminSaveAnnouncement(it) },
        onDeleteAnnouncement = { viewModel.adminDeleteAnnouncement(it) },
        modifier = modifier
    )
}

@Composable
fun AdminPanelContent(
    adminTab: Int,
    allEpisodes: List<EpisodeEntity>,
    allQuestions: List<QuestionEntity>,
    allUsers: List<UserProfileEntity>,
    allWithdrawals: List<WithdrawalRequestEntity>,
    allAnnouncements: List<AnnouncementEntity>,
    appSettings: AppSettingsEntity,
    auditLogs: List<AdminAuditLogEntity>,
    allTransactions: List<WalletTransactionEntity>,
    onSelectTab: (Int) -> Unit,
    onSwitchToUserRole: () -> Unit,
    onResetTestData: () -> Unit,
    onSaveEpisode: (EpisodeEntity) -> Unit,
    onDeleteEpisode: (String) -> Unit,
    onSetTodayEpisode: (String) -> Unit,
    onSaveQuestion: (QuestionEntity) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onSaveSettings: (AppSettingsEntity) -> Unit,
    onToggleSuspendUser: (String) -> Unit,
    onUpdateWithdrawalStatus: (String, WithdrawalStatus, String?) -> Unit,
    onSaveAnnouncement: (AnnouncementEntity) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        "डॅशबोर्ड" to Icons.Default.Dashboard,
        "Episodes" to Icons.Default.VideoLibrary,
        "प्रश्न पेढी" to Icons.Default.Quiz,
        "क्विझ रचना" to Icons.Default.Tune,
        "रिवॉर्ड सेटिंग्ज" to Icons.Default.Paid,
        "वापरकर्ते" to Icons.Default.People,
        "पैसे काढणे" to Icons.Default.Payments,
        "सूचना" to Icons.Default.Campaign,
        "ऑडिट लॉग्स" to Icons.Default.History
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Admin Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderGlowRose)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(NeonRose, NeonAmber))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Creator Studio — Admin Panel",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonAmberGlow,
                                border = BorderStroke(1.dp, NeonAmber)
                            ) {
                                Text(
                                    text = "LOCAL TEST MODE",
                                    color = NeonAmberBright,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Priti Marathi Updates व्यवस्थापन केंद्र",
                            color = NeonAmberBright,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset Test Data Button
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonAmberBright),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_reset_test_data_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reset Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Switch back to User App Button
                    OutlinedButton(
                        onClick = onSwitchToUserRole,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRoseBright),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("exit_admin_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "User App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Reset Test Data Confirmation Dialog
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚠️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reset Test Data?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Text(
                        text = "तुम्हाला सर्व स्थानिक चाचणी डेटा पूर्ववत करायचा आहे का? यामुळे वॉलेट बॅलन्स, एपिसोड प्रोग्रेस, क्विझ अटेम्पट्स, रिवॉर्ड्स आणि पैसे काढण्याच्या विनंत्या रिसेट होऊन मूळ 32 प्रश्न व सॅम्पल भाग रिस्टोअर होतील.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            onResetTestData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("होय, रिसेट करा", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("रद्द करा", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Horizontal Category Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.indices.toList()) { index ->
                val (title, icon) = tabs[index]
                val isSelected = adminTab == index
                Surface(
                    onClick = { onSelectTab(index) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) NeonRose else SurfaceVariantDark,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) NeonRoseBright else BorderSubtle
                    ),
                    modifier = Modifier.testTag("admin_tab_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Main Content Area based on Tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (adminTab) {
                0 -> AdminOverviewTab(
                    episodes = allEpisodes,
                    questions = allQuestions,
                    users = allUsers,
                    withdrawals = allWithdrawals,
                    transactions = allTransactions,
                    onNavigateTab = onSelectTab
                )
                1 -> AdminEpisodesTab(
                    episodes = allEpisodes,
                    onSaveEpisode = onSaveEpisode,
                    onDeleteEpisode = onDeleteEpisode,
                    onSetTodayEpisode = onSetTodayEpisode
                )
                2 -> AdminQuestionsTab(
                    episodes = allEpisodes,
                    questions = allQuestions,
                    onSaveQuestion = onSaveQuestion,
                    onDeleteQuestion = onDeleteQuestion
                )
                3 -> AdminQuizConfigTab(
                    settings = appSettings,
                    onSaveSettings = onSaveSettings
                )
                4 -> AdminRewardSettingsTab(
                    settings = appSettings,
                    onSaveSettings = onSaveSettings
                )
                5 -> AdminUsersTab(
                    users = allUsers,
                    onToggleSuspend = onToggleSuspendUser
                )
                6 -> AdminWithdrawalsTab(
                    withdrawals = allWithdrawals,
                    onUpdateStatus = onUpdateWithdrawalStatus
                )
                7 -> AdminAnnouncementsTab(
                    announcements = allAnnouncements,
                    onSaveAnnouncement = onSaveAnnouncement,
                    onDeleteAnnouncement = onDeleteAnnouncement
                )
                8 -> AdminAuditLogsTab(
                    auditLogs = auditLogs
                )
            }
        }
    }
}

// Sub Tab: 0 - Overview
@Composable
fun AdminOverviewTab(
    episodes: List<EpisodeEntity>,
    questions: List<QuestionEntity>,
    users: List<UserProfileEntity>,
    withdrawals: List<WithdrawalRequestEntity>,
    transactions: List<WalletTransactionEntity>,
    onNavigateTab: (Int) -> Unit
) {
    val totalRewardsDistributed = transactions
        .filter { it.type == TransactionType.QUIZ_REWARD && (it.status == TransactionStatus.APPROVED || it.status == TransactionStatus.PAID) }
        .sumOf { it.amount }
    val pendingWithdrawalsCount = withdrawals.count { it.status == WithdrawalStatus.REQUESTED }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "डॅशबोर्ड सारांश (Performance Overview)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Metrics Grid Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "एकूण युझर्स",
                    value = "${users.size}",
                    icon = Icons.Default.Group,
                    accentColor = NeonRoseBright,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "एकूण Episodes",
                    value = "${episodes.size}",
                    icon = Icons.Default.SmartDisplay,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Grid Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "एकूण प्रश्न पेढी",
                    value = "${questions.size}",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonAmberBright,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "वाटप केलेले Rewards",
                    value = "₹${totalRewardsDistributed.toInt()}",
                    icon = Icons.Default.Paid,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Grid Row 3
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "प्रलंबित Withdrawals",
                    value = "$pendingWithdrawalsCount",
                    icon = Icons.Default.PendingActions,
                    accentColor = if (pendingWithdrawalsCount > 0) NeonRoseBright else TextMuted,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "क्विझ अचूकता दर",
                    value = "88.4%",
                    icon = Icons.Default.CheckCircle,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Fast Access Buttons
        item {
            Text(
                text = "त्वरित कृती (Quick Actions)",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(
                    text = "नवीन Episode व्यवस्थापित करा",
                    onClick = { onNavigateTab(1) },
                    icon = Icons.Default.AddCircle,
                    colors = listOf(NeonRose, NeonAmber)
                )

                GradientButton(
                    text = "पैसे काढण्याच्या विनंत्या मंजूर करा ($pendingWithdrawalsCount)",
                    onClick = { onNavigateTab(6) },
                    icon = Icons.Default.Payments,
                    colors = listOf(ElectricCyan, EmeraldGreen)
                )
            }
        }
    }
}

// Sub Tab: 1 - Episodes Management
@Composable
fun AdminEpisodesTab(
    episodes: List<EpisodeEntity>,
    onSaveEpisode: (EpisodeEntity) -> Unit,
    onDeleteEpisode: (String) -> Unit,
    onSetTodayEpisode: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "भाग व्यवस्थापन (Episodes)",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "एकूण ${episodes.size} भाग उपलब्ध", color = TextMuted, fontSize = 11.sp)
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("admin_add_ep_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("नवीन भाग", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(episodes, key = { it.episodeId }) { ep ->
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (ep.isTodayEpisode) NeonRose else BorderSubtle,
                backgroundColor = SurfaceDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${ep.serialName} — Ep ${ep.episodeNumber}",
                            color = NeonAmberBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (ep.isTodayEpisode) {
                            PillBadge(
                                text = "आजचा मुख्य भाग",
                                backgroundColor = NeonRoseGlow,
                                textColor = NeonRoseBright
                            )
                        } else {
                            OutlinedButton(
                                onClick = { onSetTodayEpisode(ep.episodeId) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text("आजचा भाग बनवा", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }

                    Text(
                        text = ep.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "वेळ: ${ep.durationFormatted} | तारीख: ${ep.publishDate}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        IconButton(
                            onClick = { onDeleteEpisode(ep.episodeId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "हटवा",
                                tint = NeonRoseBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AdminAddEpisodeDialog(
            onDismiss = { showAddDialog = false },
            onSave = { ep ->
                showAddDialog = false
                onSaveEpisode(ep)
            }
        )
    }
}

@Composable
fun AdminAddEpisodeDialog(
    onDismiss: () -> Unit,
    onSave: (EpisodeEntity) -> Unit
) {
    var serialName by remember { mutableStateOf("पाठराखीण") }
    var episodeNumber by remember { mutableStateOf("106") }
    var title by remember { mutableStateOf("") }
    var youtubeVideoId by remember { mutableStateOf("dQw4w9WgXcQ") }
    var durationFormatted by remember { mutableStateOf("09:15") }
    var description by remember { mutableStateOf("") }
    var isTodayEpisode by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नवीन भाग जोडा", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = serialName,
                        onValueChange = { serialName = it },
                        label = { Text("मालिकेचे नाव") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = episodeNumber,
                        onValueChange = { episodeNumber = it },
                        label = { Text("भाग क्रमांक (Episode No)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("शीर्षक (Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = youtubeVideoId,
                        onValueChange = { youtubeVideoId = it },
                        label = { Text("YouTube Video ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = durationFormatted,
                        onValueChange = { durationFormatted = it },
                        label = { Text("कालावधी (उदा. 08:35)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("वर्णन (Description)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isTodayEpisode,
                            onCheckedChange = { isTodayEpisode = it }
                        )
                        Text("आजचा मुख्य भाग म्हणून सेट करा", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val epNum = episodeNumber.toIntOrNull() ?: 106
                    val ep = EpisodeEntity(
                        episodeId = "ep_$epNum",
                        serialName = serialName.trim(),
                        episodeNumber = epNum,
                        title = title.ifEmpty { "$serialName — भाग $epNum चे सखोल विश्लेषण" },
                        youtubeVideoId = youtubeVideoId.trim(),
                        youtubeUrl = "https://www.youtube.com/watch?v=${youtubeVideoId.trim()}",
                        thumbnailUrl = "",
                        durationSeconds = 555,
                        durationFormatted = durationFormatted.trim(),
                        publishDate = "आजचा भाग",
                        description = description.ifEmpty { "प्रीती मराठी अपडेट्सच्या स्पेशल रिपोर्टमध्ये संपूर्ण रहस्य उलगडले." },
                        status = EpisodeStatus.PUBLISHED,
                        isTodayEpisode = isTodayEpisode,
                        createdAt = System.currentTimeMillis()
                    )
                    onSave(ep)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
            ) {
                Text("प्रसिद्ध करा (Publish)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा", color = TextMuted) }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp)
    )
}

// Sub Tab: 2 - Question Bank
@Composable
fun AdminQuestionsTab(
    episodes: List<EpisodeEntity>,
    questions: List<QuestionEntity>,
    onSaveQuestion: (QuestionEntity) -> Unit,
    onDeleteQuestion: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "प्रश्न पेढी (Question Bank)",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "एकूण ${questions.size} प्रश्न उपलब्ध", color = TextMuted, fontSize = 11.sp)
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("नवीन प्रश्न", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(questions, key = { it.questionId }) { q ->
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = BorderSubtle,
                backgroundColor = SurfaceDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PillBadge(
                            text = "भाग: ${q.episodeId}",
                            backgroundColor = SurfaceVariantDark,
                            textColor = NeonAmberBright
                        )

                        IconButton(
                            onClick = { onDeleteQuestion(q.questionId) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = NeonRoseBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = q.questionText,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val correctLabel = when (q.correctAnswer) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        else -> "D"
                    }
                    Text(
                        text = "बरोबर पर्याय: $correctLabel | स्पष्टीकरण: ${q.explanation}",
                        color = EmeraldGreen,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AdminAddQuestionDialog(
            episodes = episodes,
            onDismiss = { showAddDialog = false },
            onSave = { q ->
                showAddDialog = false
                onSaveQuestion(q)
            }
        )
    }
}

@Composable
fun AdminAddQuestionDialog(
    episodes: List<EpisodeEntity>,
    onDismiss: () -> Unit,
    onSave: (QuestionEntity) -> Unit
) {
    var episodeId by remember { mutableStateOf(episodes.firstOrNull()?.episodeId ?: "ep_105") }
    var questionText by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf(0) }
    var explanation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नवीन प्रश्न जोडा", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = episodeId,
                        onValueChange = { episodeId = it },
                        label = { Text("Episode ID (उदा. ep_105)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("प्रश्न मजकूर") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = optionA,
                        onValueChange = { optionA = it },
                        label = { Text("पर्याय A") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = optionB,
                        onValueChange = { optionB = it },
                        label = { Text("पर्याय B") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = optionC,
                        onValueChange = { optionC = it },
                        label = { Text("पर्याय C") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = optionD,
                        onValueChange = { optionD = it },
                        label = { Text("पर्याय D") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("बरोबर पर्याय निवडा (0=A, 1=B, 2=C, 3=D)", color = TextMuted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "A", 1 to "B", 2 to "C", 3 to "D").forEach { (idx, label) ->
                            FilterChip(
                                selected = correctAnswer == idx,
                                onClick = { correctAnswer = idx },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("स्पष्टीकरण") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = QuestionEntity(
                        questionId = "q_${System.currentTimeMillis()}",
                        episodeId = episodeId.trim(),
                        questionText = questionText.trim(),
                        optionA = optionA.trim(),
                        optionB = optionB.trim(),
                        optionC = optionC.trim(),
                        optionD = optionD.trim(),
                        correctAnswer = correctAnswer,
                        explanation = explanation.trim(),
                        difficulty = "Medium",
                        active = true
                    )
                    onSave(q)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
            ) {
                Text("सेव्ह करा", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा", color = TextMuted) }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp)
    )
}

// Sub Tab: 3 - Quiz Configuration
@Composable
fun AdminQuizConfigTab(
    settings: AppSettingsEntity,
    onSaveSettings: (AppSettingsEntity) -> Unit
) {
    var questionsPerQuiz by remember(settings) { mutableStateOf(settings.questionsPerQuiz.toString()) }
    var randomSelection by remember(settings) { mutableStateOf(settings.randomQuestionSelection) }
    var rewardPerAnswer by remember(settings) { mutableStateOf(settings.rewardPerCorrectAnswer.toInt().toString()) }
    var maxDailyReward by remember(settings) { mutableStateOf(settings.maxDailyReward.toInt().toString()) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "दैनिक क्विझ रचना (Daily Quiz Configuration)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            NeonCard(modifier = Modifier.fillMaxWidth(), backgroundColor = SurfaceDark) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = questionsPerQuiz,
                        onValueChange = { questionsPerQuiz = it },
                        label = { Text("प्रति क्विझ प्रश्न संख्या (Default: 15)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rewardPerAnswer,
                        onValueChange = { rewardPerAnswer = it },
                        label = { Text("प्रति बरोबर उत्तर बक्षीस (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = maxDailyReward,
                        onValueChange = { maxDailyReward = it },
                        label = { Text("कमाल दैनिक बक्षीस मर्यादा (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("प्रश्न पेढीतून यादृच्छिक (Random) निवड:", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = randomSelection,
                            onCheckedChange = { randomSelection = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GradientButton(
                        text = "क्विझ रचना सेव्ह करा",
                        onClick = {
                            val updated = settings.copy(
                                questionsPerQuiz = questionsPerQuiz.toIntOrNull() ?: 15,
                                rewardPerCorrectAnswer = rewardPerAnswer.toDoubleOrNull() ?: 2.0,
                                maxDailyReward = maxDailyReward.toDoubleOrNull() ?: 30.0,
                                randomQuestionSelection = randomSelection
                            )
                            onSaveSettings(updated)
                        },
                        colors = listOf(NeonRose, NeonAmber)
                    )
                }
            }
        }
    }
}

// Sub Tab: 4 - Reward Settings
@Composable
fun AdminRewardSettingsTab(
    settings: AppSettingsEntity,
    onSaveSettings: (AppSettingsEntity) -> Unit
) {
    var welcomeBonus by remember(settings) { mutableStateOf(settings.welcomeBonus.toInt().toString()) }
    var streakBonus by remember(settings) { mutableStateOf(settings.streakBonus.toInt().toString()) }
    var minWithdrawal by remember(settings) { mutableStateOf(settings.minWithdrawal.toInt().toString()) }
    var rewardsEnabled by remember(settings) { mutableStateOf(settings.rewardEnabled) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "बक्षीस आणि आर्थिक सेटिंग्ज (Reward Settings)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            NeonCard(modifier = Modifier.fillMaxWidth(), backgroundColor = SurfaceDark) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("संपूर्ण बक्षीस प्रणाली सक्षम ठेवा:", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = rewardsEnabled,
                            onCheckedChange = { rewardsEnabled = it }
                        )
                    }

                    Divider(color = BorderSubtle)

                    OutlinedTextField(
                        value = welcomeBonus,
                        onValueChange = { welcomeBonus = it },
                        label = { Text("नवीन युझर स्वागत बोनस (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = streakBonus,
                        onValueChange = { streakBonus = it },
                        label = { Text("7 दिवसांच्या स्ट्रीकवर बोनस (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = minWithdrawal,
                        onValueChange = { minWithdrawal = it },
                        label = { Text("किमान पैसे काढण्याची मर्यादा (Min Withdrawal ₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GradientButton(
                        text = "आर्थिक सेटिंग्ज अपडेट करा",
                        onClick = {
                            val updated = settings.copy(
                                welcomeBonus = welcomeBonus.toDoubleOrNull() ?: 5.0,
                                streakBonus = streakBonus.toDoubleOrNull() ?: 10.0,
                                minWithdrawal = minWithdrawal.toDoubleOrNull() ?: 100.0,
                                rewardEnabled = rewardsEnabled
                            )
                            onSaveSettings(updated)
                        },
                        colors = listOf(EmeraldGreen, NeonAmber)
                    )
                }
            }
        }
    }
}

// Sub Tab: 5 - Users Management
@Composable
fun AdminUsersTab(
    users: List<UserProfileEntity>,
    onToggleSuspend: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "वापरकर्ते (Registered Users)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(users, key = { it.userId }) { user ->
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (user.isSuspended) NeonRose else BorderSubtle,
                backgroundColor = SurfaceDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = user.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = user.phoneOrEmail, color = TextMuted, fontSize = 12.sp)
                        Text(
                            text = "क्विझ: ${user.totalQuizzesCompleted} | अचूक उत्तरे: ${user.totalCorrectAnswers} | स्ट्रीक: ${user.streakDays} दिवस",
                            color = NeonAmberBright,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { onToggleSuspend(user.userId) },
                        border = BorderStroke(1.dp, if (user.isSuspended) EmeraldGreen else NeonRose),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (user.isSuspended) "सक्रिय करा" else "निलंबित करा",
                            color = if (user.isSuspended) EmeraldGreen else NeonRoseBright,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Sub Tab: 6 - Withdrawals Management
@Composable
fun AdminWithdrawalsTab(
    withdrawals: List<WithdrawalRequestEntity>,
    onUpdateStatus: (String, WithdrawalStatus, String?) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "पैसे काढण्याच्या विनंत्या (Withdrawals)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (withdrawals.isEmpty()) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "सध्या कोणतीही पैसे काढण्याची विनंती प्रलंबित नाही.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(withdrawals, key = { it.withdrawalId }) { wdr ->
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = when (wdr.status) {
                        WithdrawalStatus.REQUESTED -> NeonAmber
                        WithdrawalStatus.PAID -> EmeraldGreen
                        WithdrawalStatus.REJECTED -> NeonRose
                        else -> BorderSubtle
                    },
                    backgroundColor = SurfaceDark
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = wdr.userName,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${wdr.payoutMethod}: ${wdr.payoutAddress}",
                                    color = NeonAmberBright,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = "₹${wdr.amount.toInt()}",
                                color = EmeraldGreen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "तारीख: ${dateFormat.format(Date(wdr.requestedAt))} | स्थिती: ${wdr.status}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        if (wdr.status == WithdrawalStatus.REQUESTED) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onUpdateStatus(wdr.withdrawalId, WithdrawalStatus.PAID, "Admin UPI द्वारे जमा केले") },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mark as Paid", color = Color.White, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { onUpdateStatus(wdr.withdrawalId, WithdrawalStatus.REJECTED, "चुकीचा UPI ID") },
                                    border = BorderStroke(1.dp, NeonRose),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", color = NeonRoseBright, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub Tab: 7 - Announcements
@Composable
fun AdminAnnouncementsTab(
    announcements: List<AnnouncementEntity>,
    onSaveAnnouncement: (AnnouncementEntity) -> Unit,
    onDeleteAnnouncement: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "सूचना प्रसिद्ध करा (Broadcast Announcements)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            NeonCard(modifier = Modifier.fillMaxWidth(), backgroundColor = SurfaceDark) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("सूचनेचे शीर्षक") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("सविस्तर संदेश") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    GradientButton(
                        text = "सूचना प्रसिद्ध करा",
                        onClick = {
                            if (title.isNotEmpty() && message.isNotEmpty()) {
                                val ann = AnnouncementEntity(
                                    id = "ann_${System.currentTimeMillis()}",
                                    title = title.trim(),
                                    message = message.trim(),
                                    date = "27 सप्टें 2026",
                                    active = true
                                )
                                onSaveAnnouncement(ann)
                                title = ""
                                message = ""
                            }
                        },
                        colors = listOf(NeonRose, NeonAmber)
                    )
                }
            }
        }

        item {
            Text(
                text = "प्रसिद्ध केलेल्या सूचना",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(announcements, key = { it.id }) { ann ->
            NeonCard(modifier = Modifier.fillMaxWidth(), backgroundColor = SurfaceDark) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = ann.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = ann.message, color = TextMuted, fontSize = 11.sp)
                    }

                    IconButton(onClick = { onDeleteAnnouncement(ann.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = NeonRoseBright)
                    }
                }
            }
        }
    }
}

// Sub Tab: 8 - Audit Logs
@Composable
fun AdminAuditLogsTab(
    auditLogs: List<AdminAuditLogEntity>
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "प्रणाली ऑडिट लॉग्स (System & Financial Audit Trail)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = "सर्व आर्थिक आणि प्रशासकीय बदलांची सुरक्षित नोंद", color = TextMuted, fontSize = 11.sp)
        }

        items(auditLogs, key = { it.logId }) { log ->
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = BorderSubtle,
                backgroundColor = SurfaceDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PillBadge(
                            text = log.action,
                            backgroundColor = SurfaceVariantDark,
                            textColor = NeonRoseBright
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = log.details,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "कर्ता: ${log.performedBy} | घटक: ${log.targetType}",
                        color = NeonAmberBright,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

data class AdminPanelPreviewData(
    val adminTab: Int = 0,
    val allEpisodes: List<EpisodeEntity> = emptyList(),
    val allQuestions: List<QuestionEntity> = emptyList(),
    val allUsers: List<UserProfileEntity> = emptyList(),
    val allWithdrawals: List<WithdrawalRequestEntity> = emptyList(),
    val allAnnouncements: List<AnnouncementEntity> = emptyList(),
    val appSettings: AppSettingsEntity = AppSettingsEntity(),
    val auditLogs: List<AdminAuditLogEntity> = emptyList(),
    val allTransactions: List<WalletTransactionEntity> = emptyList()
)

class AdminPanelPreviewParameterProvider : PreviewParameterProvider<AdminPanelPreviewData> {
    override val values: Sequence<AdminPanelPreviewData> = sequenceOf(
        AdminPanelPreviewData(
            adminTab = 0,
            allEpisodes = listOf(
                EpisodeEntity(
                    episodeId = "ep1",
                    serialName = "ठरलं तर मग",
                    episodeNumber = 620,
                    title = "सायली आणि अर्जुनचा मोठा निर्णय!",
                    youtubeVideoId = "dQw4w9WgXcQ",
                    youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    thumbnailUrl = "",
                    durationSeconds = 660,
                    durationFormatted = "11:00",
                    publishDate = "28 सप्टेंबर 2026",
                    description = "मालिकेचा आजचा भाग.",
                    isTodayEpisode = true
                )
            ),
            allQuestions = listOf(
                QuestionEntity(
                    questionId = "q1",
                    episodeId = "ep1",
                    questionText = "आजच्या भागात सायलीने कोणाला मदत केली?",
                    optionA = "अर्जुन",
                    optionB = "कल्पना",
                    optionC = "अस्मिता",
                    optionD = "सुप्रिया",
                    correctAnswer = 0,
                    explanation = "सायलीने कोर्टात जाऊन मदत केली."
                )
            ),
            allUsers = listOf(
                UserProfileEntity(
                    userId = "u1",
                    name = "प्रिया देशमुख",
                    phoneOrEmail = "+91 98765 43210",
                    joinedDate = "सप्टेंबर 2026",
                    streakDays = 5,
                    lastActiveDate = "28 सप्टेंबर 2026"
                )
            ),
            allWithdrawals = emptyList(),
            allAnnouncements = listOf(
                AnnouncementEntity(
                    id = "ann1",
                    title = "महत्त्वाची सूचना",
                    message = "सर्व वापरकर्त्यांसाठी नियमित क्विझ अपडेट.",
                    date = "28 सप्टें 2026",
                    active = true
                )
            ),
            appSettings = AppSettingsEntity(
                rewardPerCorrectAnswer = 2.0,
                minWithdrawal = 100.0
            ),
            auditLogs = listOf(
                AdminAuditLogEntity(
                    logId = "log1",
                    action = "SYSTEM_INIT",
                    performedBy = "Admin",
                    targetType = "System",
                    details = "प्रशासन पॅनेल लोड झाले.",
                    timestamp = System.currentTimeMillis()
                )
            ),
            allTransactions = emptyList()
        )
    )
}

@Preview(showBackground = true)
@Composable
fun AdminPanelScreenPreview(
    @PreviewParameter(AdminPanelPreviewParameterProvider::class) data: AdminPanelPreviewData
) {
    MyApplicationTheme {
        AdminPanelContent(
            adminTab = data.adminTab,
            allEpisodes = data.allEpisodes,
            allQuestions = data.allQuestions,
            allUsers = data.allUsers,
            allWithdrawals = data.allWithdrawals,
            allAnnouncements = data.allAnnouncements,
            appSettings = data.appSettings,
            auditLogs = data.auditLogs,
            allTransactions = data.allTransactions,
            onSelectTab = {},
            onSwitchToUserRole = {},
            onResetTestData = {},
            onSaveEpisode = {},
            onDeleteEpisode = {},
            onSetTodayEpisode = {},
            onSaveQuestion = {},
            onDeleteQuestion = {},
            onSaveSettings = {},
            onToggleSuspendUser = {},
            onUpdateWithdrawalStatus = { _, _, _ -> },
            onSaveAnnouncement = {},
            onDeleteAnnouncement = {}
        )
    }
}

