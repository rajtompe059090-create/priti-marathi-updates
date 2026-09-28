package com.example.ui.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.WalletSummary
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun UserHomeScreen(
    todayEpisode: EpisodeEntity?,
    userProfile: UserProfileEntity?,
    walletSummary: WalletSummary,
    appSettings: AppSettingsEntity?,
    announcements: List<AnnouncementEntity>,
    userProgressList: List<UserEpisodeProgressEntity>,
    onWatchEpisode: (EpisodeEntity) -> Unit,
    onStartQuiz: (EpisodeEntity) -> Unit,
    onViewEpisodes: () -> Unit,
    onViewWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayProgress = todayEpisode?.let { ep ->
        userProgressList.find { it.episodeId == ep.episodeId }
    }
    val isEpisodeWatched = todayProgress?.status == UserProgressStatus.COMPLETED ||
            todayProgress?.status == UserProgressStatus.QUIZ_UNLOCKED ||
            todayProgress?.status == UserProgressStatus.QUIZ_COMPLETED
    val isQuizCompleted = todayProgress?.status == UserProgressStatus.QUIZ_COMPLETED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting & Welcome Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "नमस्कार! 👋",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "आजचा भाग तयार आहे!",
                        color = NeonRoseBright,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Balance Quick Pill
                Surface(
                    onClick = onViewWallet,
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceVariantDark,
                    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💰", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "₹${walletSummary.currentBalance.toInt()}",
                            color = NeonAmberBright,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Official YouTube Channel Subtle Strip
        item {
            Surface(
                onClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://youtube.com/@pritimarathiupdates?si=ltuhnlml04ttfGqC")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback
                    }
                },
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_youtube_channel_strip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = "Priti Marathi Updates",
                            tint = NeonRoseBright,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Priti Marathi Updates",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "अधिकृत YouTube चॅनेल",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "चॅनेल पहा",
                            color = NeonRoseBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = NeonRoseBright,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Announcements banner
        if (announcements.isNotEmpty()) {
            item {
                val ann = announcements.first()
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonRose.copy(alpha = 0.4f),
                    backgroundColor = SurfaceDark
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonRoseGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📢", fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ann.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ann.message,
                                color = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Main Featured Card: "आजचा नवीन Episode"
        item {
            Text(
                text = "आजचा नवीन Episode",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (todayEpisode != null) {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderBrush = Brush.horizontalGradient(listOf(NeonRose, NeonAmber)),
                    backgroundColor = SurfaceDark,
                    cornerRadius = 22.dp
                ) {
                    // Thumbnail Container with Duration Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceVariantDark)
                            .clickable { onWatchEpisode(todayEpisode) }
                    ) {
                        // Poster image
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = todayEpisode.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Dark Gradient overlay for contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC090D16))
                                    )
                                )
                        )

                        // Duration Badge top right
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xDD000000)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = NeonAmberBright,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = todayEpisode.durationFormatted,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Serial Name & Episode Tag top left
                        PillBadge(
                            text = "${todayEpisode.serialName} — Ep ${todayEpisode.episodeNumber}",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            backgroundColor = NeonRose.copy(alpha = 0.85f),
                            textColor = Color.White,
                            borderColor = Color.White.copy(alpha = 0.3f)
                        )

                        // Play Icon in Center
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonRose, NeonAmber)))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "व्हिडिओ पाहा",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = todayEpisode.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = todayEpisode.description,
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Button: "व्हिडिओ पाहा"
                    GradientButton(
                        text = if (isEpisodeWatched) "व्हिडिओ पुन्हा पाहा" else "व्हिडिओ पाहा",
                        onClick = { onWatchEpisode(todayEpisode) },
                        icon = Icons.Default.PlayCircle,
                        colors = listOf(NeonRose, NeonAmber),
                        testTag = "watch_video_btn"
                    )
                }
            } else {
                NeonCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "सध्या आजचा नवीन भाग उपलब्ध नाही. लवकरच अपडेट केला जाईल.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Dual Highlight Cards: "आजचे Reward" and "आजचा Quiz"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reward Card
                NeonCard(
                    modifier = Modifier.weight(1f),
                    borderColor = NeonAmber.copy(alpha = 0.5f),
                    backgroundColor = SurfaceDark,
                    onClick = onViewWallet
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "आजचे Reward",
                            color = NeonAmberBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "🎁", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${appSettings?.maxDailyReward?.toInt() ?: 30}",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "प्रत्येक बरोबर उत्तरासाठी ₹${appSettings?.rewardPerCorrectAnswer?.toInt() ?: 2}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                // Quiz Card
                NeonCard(
                    modifier = Modifier.weight(1f),
                    borderColor = if (isEpisodeWatched) NeonRose.copy(alpha = 0.7f) else BorderSubtle,
                    backgroundColor = SurfaceDark,
                    onClick = {
                        if (todayEpisode != null && isEpisodeWatched) {
                            onStartQuiz(todayEpisode)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "आजचा Quiz",
                            color = NeonRoseBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = if (isEpisodeWatched) "📝" else "🔒", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "15 प्रश्न",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isQuizCompleted -> "✅ पूर्ण झाला!"
                            isEpisodeWatched -> "🚀 सुरू करा!"
                            else -> "व्हिडिओ पाहिल्यावर अनलॉक"
                        },
                        color = if (isEpisodeWatched) NeonRoseBright else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Daily Streak Card
        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonAmber.copy(alpha = 0.35f),
                backgroundColor = SurfaceDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonAmberGlow)
                                .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🔥", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${userProfile?.streakDays ?: 1} दिवसांचा Daily Streak!",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "सलग 7 दिवस क्विझ सोडवा आणि मिळवा ₹10 बोनस",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Navigation to Previous Episodes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "पूर्वीचे Episodes",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewEpisodes) {
                    Text(
                        text = "सर्व पाहा >",
                        color = NeonRoseBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class UserHomePreviewData(
    val todayEpisode: EpisodeEntity?,
    val userProfile: UserProfileEntity?,
    val walletSummary: WalletSummary,
    val appSettings: AppSettingsEntity?,
    val announcements: List<AnnouncementEntity>,
    val userProgressList: List<UserEpisodeProgressEntity>
)

class UserHomePreviewParameterProvider : PreviewParameterProvider<UserHomePreviewData> {
    override val values: Sequence<UserHomePreviewData> = sequenceOf(
        UserHomePreviewData(
            todayEpisode = EpisodeEntity(
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
            ),
            userProfile = UserProfileEntity(
                userId = "u1",
                name = "प्रिया देशमुख",
                phoneOrEmail = "+91 98765 43210",
                joinedDate = "सप्टेंबर 2026",
                streakDays = 3,
                lastActiveDate = "28 सप्टेंबर 2026"
            ),
            walletSummary = WalletSummary(
                currentBalance = 80.0,
                totalEarnings = 150.0,
                paidAmount = 70.0,
                pendingAmount = 0.0
            ),
            appSettings = AppSettingsEntity(
                rewardPerCorrectAnswer = 2.0,
                minWithdrawal = 100.0
            ),
            announcements = listOf(
                AnnouncementEntity(
                    id = "ann_1",
                    title = "स्वागत ऑफर!",
                    message = "दररोज व्हिडिओ पहा आणि रिवॉर्ड्स जिंका.",
                    date = "28 सप्टें 2026",
                    active = true
                )
            ),
            userProgressList = emptyList()
        )
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview(
    @PreviewParameter(UserHomePreviewParameterProvider::class) data: UserHomePreviewData
) {
    MyApplicationTheme {
        UserHomeScreen(
            todayEpisode = data.todayEpisode,
            userProfile = data.userProfile,
            walletSummary = data.walletSummary,
            appSettings = data.appSettings,
            announcements = data.announcements,
            userProgressList = data.userProgressList,
            onWatchEpisode = {},
            onStartQuiz = {},
            onViewEpisodes = {},
            onViewWallet = {}
        )
    }
}

