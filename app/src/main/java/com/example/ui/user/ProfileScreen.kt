package com.example.ui.user

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfileEntity
import com.example.data.repository.WalletSummary
import com.example.ui.components.NeonCard
import com.example.ui.components.PillBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity?,
    walletSummary: WalletSummary,
    onOpenAdminMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header Card
        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = Brush.horizontalGradient(listOf(NeonRose, NeonAmber)),
                backgroundColor = SurfaceDark,
                cornerRadius = 22.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonRose, NeonAmber)))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile?.name?.take(2) ?: "PM",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = userProfile?.name ?: "प्रिया देशमुख",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile?.phoneOrEmail ?: "+91 98765 43210",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PillBadge(
                                text = "सभासद: ${userProfile?.joinedDate ?: "सप्टेंबर 2026"}",
                                backgroundColor = SurfaceVariantDark,
                                textColor = NeonAmberBright,
                                borderColor = BorderSubtle
                            )
                        }
                    }
                }
            }
        }

        // Official YouTube Channel Card
        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonRose.copy(alpha = 0.6f),
                backgroundColor = SurfaceDark,
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(NeonRose.copy(alpha = 0.2f))
                                .border(1.dp, NeonRoseBright, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartDisplay,
                                contentDescription = "YouTube Channel",
                                tint = NeonRoseBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "YouTube Channel",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Priti Marathi Updates",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                val channelIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://youtube.com/@pritimarathiupdates?si=ltuhnlml04ttfGqC")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(channelIntent)
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileScreen", "Failed to open YouTube channel", e)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonRose
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("profile_view_channel_btn")
                    ) {
                        Text(
                            text = "चॅनेल पहा",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Lifetime Statistics
        item {
            Text(
                text = "माझी कामगिरी (My Activity)",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "क्विझ सोडवले",
                    value = "${userProfile?.totalQuizzesCompleted ?: 1}",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonRoseBright,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "बरोबर उत्तरे",
                    value = "${userProfile?.totalCorrectAnswers ?: 15}",
                    icon = Icons.Default.TaskAlt,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Badges & Achievements
        item {
            Text(
                text = "बॅजेस आणि पुरस्कार (Achievements)",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val achievements = listOf(
                Triple("🎬", "पहिला Episode", "मालिकेचा पहिला भाग पूर्ण पाहिला"),
                Triple("📝", "पहिली Quiz", "पहिली अचूक क्विझ पूर्ण केली"),
                Triple("🔥", "7 Day Streak", "सलग 7 दिवस चॅनेलवर सक्रिय"),
                Triple("💯", "शतकवीर", "100 हून अधिक अचूक उत्तरे"),
                Triple("👑", "Super Viewer", "प्रीती मराठी अपडेट्सचे निष्ठावंत प्रेक्षक")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                achievements.forEach { (emoji, title, desc) ->
                    NeonCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderSubtle,
                        backgroundColor = SurfaceDark
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = emoji, fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Support & Info Links
        item {
            Text(
                text = "मदत आणि धोरणे (Help & Policies)",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileLinkItem(
                        icon = Icons.Default.HelpOutline,
                        title = "मदत आणि साहाय्यता (Help & Support)",
                        onClick = { showHelpDialog = true }
                    )
                    Divider(color = BorderSubtle)
                    ProfileLinkItem(
                        icon = Icons.Default.Shield,
                        title = "गोपनीयता धोरण (Privacy Policy)",
                        onClick = { showPrivacyDialog = true }
                    )
                    Divider(color = BorderSubtle)
                    ProfileLinkItem(
                        icon = Icons.Default.Description,
                        title = "नियम आणि अटी (Terms & Conditions)",
                        onClick = { showTermsDialog = true }
                    )
                    Divider(color = BorderSubtle)
                    ProfileLinkItem(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "ॲडमिन पॅनेल (Creator Studio Login)",
                        accentColor = NeonRoseBright,
                        onClick = onOpenAdminMode
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Priti Marathi Updates v1.0.0",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "मराठी मालिकांचे वेगवान, सखोल आणि ताज्या घडामोडींचे केंद्र",
                    color = NeonAmberBright,
                    fontSize = 10.sp
                )
            }
        }
    }

    if (showHelpDialog) {
        PolicyDialog(
            title = "प्रेक्षक साहाय्यता (Help & Support)",
            content = "नमस्कार! 'प्रीती मराठी अपडेट्स' चॅनेलच्या प्रेक्षकांसाठी ही अधिकृत ॲप आहे.\n\n• दररोज नवीन भागाचे विश्लेषण आल्यावर नोटिफिकेशन मिळते.\n• पूर्ण व्हिडिओ पाहिल्यानंतर 15 प्रश्नांची दैनिक क्विझ अनलॉक होते.\n• प्रत्येक बरोबर उत्तरासाठी निश्चित रोख बक्षीस थेट वॉलेटमध्ये जमा होते.\n• वॉलेटमध्ये ₹100 पेक्षा जास्त रक्कम झाल्यावर तुम्ही UPI द्वारे थेट पैसे काढू शकता.\n• संपर्क ईमेल: support@pritimarathiupdates.com",
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PolicyDialog(
            title = "गोपनीयता धोरण (Privacy Policy)",
            content = "आम्ही युझरच्या गोपनीयतेचा आदर करतो.\n\n१. ॲपमध्ये केवळ आवश्यक माहिती (नाव, फोन/UPI) सुरक्षित ठेवली जाते.\n२. YouTube व्हिडिओ पाहण्यासाठी अधिकृत YouTube लिंक/ॲपचा वापर केला जातो; कोणताही गैरवापर किंवा डेटा चोरी केली जात नाही.\n३. आर्थिक देवाणघेवाण फक्त ॲडमिन मंजुरीनंतर सुरक्षित UPI प्रणालीद्वारे केली जाते.\n४. युझरचा कोणताही खाजगी डेटा तृतीय पक्षाला विकला जात नाही.",
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showTermsDialog) {
        PolicyDialog(
            title = "नियम आणि अटी (Terms & Conditions)",
            content = "१. एका भागाचे बक्षीस एका युझरला एकदाच दिले जाते.\n२. जुने भाग पुन्हा पाहता येतात, परंतु जुन्या भागांवर वारंवार बक्षीस दिले जात नाही.\n३. कोणताही बॉट, गैरप्रकार किंवा खोटे व्ह्यूज नोंदवल्यास खाते निलंबित केले जाऊ शकते.\n४. बक्षीस आणि पैसे काढण्याची मर्यादा ॲडमिनच्या धोरणानुसार बदलू शकते.\n५. हे ॲप प्रेक्षकांच्या करमणूक आणि ज्ञानासाठी आहे.",
            onDismiss = { showTermsDialog = false }
        )
    }
}

@Composable
fun ProfileLinkItem(
    icon: ImageVector,
    title: String,
    accentColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun PolicyDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Text(text = content, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
            ) {
                Text("समजले", color = Color.White)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp)
    )
}

data class ProfilePreviewData(
    val userProfile: UserProfileEntity?,
    val walletSummary: WalletSummary
)

class ProfilePreviewParameterProvider : PreviewParameterProvider<ProfilePreviewData> {
    override val values: Sequence<ProfilePreviewData> = sequenceOf(
        ProfilePreviewData(
            userProfile = UserProfileEntity(
                userId = "u1",
                name = "प्रिया देशमुख",
                phoneOrEmail = "+91 98765 43210",
                joinedDate = "सप्टेंबर 2026",
                streakDays = 5,
                lastActiveDate = "28 सप्टेंबर 2026",
                totalQuizzesCompleted = 8,
                totalCorrectAnswers = 92
            ),
            walletSummary = WalletSummary(
                currentBalance = 120.0,
                totalEarnings = 184.0,
                paidAmount = 64.0,
                pendingAmount = 0.0
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) data: ProfilePreviewData
) {
    MyApplicationTheme {
        ProfileScreen(
            userProfile = data.userProfile,
            walletSummary = data.walletSummary,
            onOpenAdminMode = {}
        )
    }
}

