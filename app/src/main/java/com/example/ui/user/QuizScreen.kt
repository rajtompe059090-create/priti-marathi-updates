package com.example.ui.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EpisodeEntity
import com.example.data.model.QuestionEntity
import com.example.data.repository.QuizResult
import com.example.data.repository.WalletSummary
import com.example.ui.components.GradientButton
import com.example.ui.components.NeonCard
import com.example.ui.components.PillBadge
import com.example.ui.theme.*

@Composable
fun QuizScreen(
    episode: EpisodeEntity?,
    questions: List<QuestionEntity>,
    currentIndex: Int,
    userAnswers: Map<String, Int>,
    quizResult: QuizResult?,
    walletSummary: WalletSummary,
    onSelectAnswer: (String, Int) -> Unit,
    onNextQuestion: () -> Unit,
    onPrevQuestion: () -> Unit,
    onSubmitQuiz: () -> Unit,
    onExitQuiz: () -> Unit,
    onViewWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (quizResult != null) {
        // Result Screen
        QuizResultView(
            result = quizResult,
            walletSummary = walletSummary,
            episode = episode,
            onExit = onExitQuiz,
            onViewWallet = onViewWallet,
            modifier = modifier
        )
        return
    }

    if (questions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📝", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "सध्या क्विझ उपलब्ध नाही",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "या भागासाठी अद्याप प्रश्न जोडलेले नाहीत किंवा आधी व्हिडिओ पूर्ण पाहा.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onExitQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
                    ) {
                        Text("मुख्य पानावर जा", color = Color.White)
                    }
                }
            }
        }
        return
    }

    val currentQuestion = questions.getOrNull(currentIndex) ?: return
    val progressFraction = (currentIndex + 1).toFloat() / questions.size.toFloat()
    val selectedOption = userAnswers[currentQuestion.questionId]

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Quiz Info Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "आजचा Assessment",
                        color = NeonRoseBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${episode?.serialName ?: "मालिका"} — Ep ${episode?.episodeNumber ?: ""}",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                PillBadge(
                    text = "प्रश्न ${currentIndex + 1} / ${questions.size}",
                    backgroundColor = NeonAmberGlow,
                    textColor = NeonAmberBright,
                    borderColor = NeonAmber.copy(alpha = 0.5f)
                )
            }
        }

        // Progress Bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonRose,
                    trackColor = SurfaceVariantDark,
                )
            }
        }

        // Question Card
        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonRose.copy(alpha = 0.4f),
                backgroundColor = SurfaceDark,
                cornerRadius = 20.dp
            ) {
                Text(
                    text = "प्रश्न क्रमांक ${currentIndex + 1}",
                    color = NeonAmberBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentQuestion.questionText,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )
            }
        }

        // Options List (A, B, C, D)
        val options = listOf(
            0 to currentQuestion.optionA,
            1 to currentQuestion.optionB,
            2 to currentQuestion.optionC,
            3 to currentQuestion.optionD
        )

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { (index, text) ->
                    val isSelected = selectedOption == index
                    val optionLabel = when (index) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        else -> "D"
                    }

                    Surface(
                        onClick = { onSelectAnswer(currentQuestion.questionId, index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("option_${optionLabel}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) SurfaceVariantDark else SurfaceDark,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) NeonRose else BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) NeonRose else SurfaceVariantDark
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else BorderSubtle,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLabel,
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = text,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                lineHeight = 21.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Navigation Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev button
                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = onPrevQuestion,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("मागील")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Next or Submit
                val isLast = currentIndex == questions.size - 1
                Box(modifier = Modifier.weight(1.5f)) {
                    if (isLast) {
                        GradientButton(
                            text = "Assessment सबमिट करा",
                            onClick = onSubmitQuiz,
                            icon = Icons.Default.CheckCircle,
                            colors = listOf(EmeraldGreen, NeonAmber),
                            enabled = userAnswers.isNotEmpty(),
                            testTag = "submit_quiz_btn"
                        )
                    } else {
                        GradientButton(
                            text = "पुढचा प्रश्न",
                            onClick = onNextQuestion,
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            colors = listOf(NeonRose, NeonAmber),
                            testTag = "next_question_btn"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizResultView(
    result: QuizResult,
    walletSummary: WalletSummary,
    episode: EpisodeEntity?,
    onExit: () -> Unit,
    onViewWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonRose, NeonAmber)
                        )
                    )
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎉", fontSize = 44.sp)
            }
        }

        item {
            Text(
                text = "Assessment पूर्ण झाला!",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${episode?.serialName ?: "मालिका"} — Ep ${episode?.episodeNumber ?: ""}",
                color = NeonRoseBright,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Score Card
        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = Brush.horizontalGradient(listOf(NeonRose, NeonAmber)),
                backgroundColor = SurfaceDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "बरोबर", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${result.score}", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = BorderSubtle
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "चूक", color = NeonRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${result.incorrectCount}", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = BorderSubtle
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "एकूण प्रश्न", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${result.totalQuestions}", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // Reward Highlight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "मिळालेले Reward",
                            color = NeonAmberBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (result.alreadyClaimed) {
                            Text(
                                text = "आधीच क्लेम केलेले (सराव प्रयत्न)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "वॉलेटमध्ये जमा करण्यात आले",
                                color = EmeraldGreen,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = "+₹${result.rewardEarned.toInt()}",
                        color = if (result.rewardEarned > 0) EmeraldGreen else TextMuted,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "सध्याचे एकूण वॉलेट बॅलन्स:", color = TextMuted, fontSize = 13.sp)
                    Text(
                        text = "₹${walletSummary.currentBalance.toInt()}",
                        color = NeonAmberBright,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Actions
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GradientButton(
                    text = "माझे वॉलेट तपासा",
                    onClick = onViewWallet,
                    icon = Icons.Default.AccountBalanceWallet,
                    colors = listOf(NeonRose, NeonAmber),
                    testTag = "view_wallet_result_btn"
                )

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("मुख्य पानावर परत जा", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class QuizPreviewData(
    val episode: EpisodeEntity?,
    val questions: List<QuestionEntity>,
    val currentIndex: Int,
    val userAnswers: Map<String, Int>,
    val quizResult: QuizResult?,
    val walletSummary: WalletSummary
)

class QuizPreviewParameterProvider : PreviewParameterProvider<QuizPreviewData> {
    override val values: Sequence<QuizPreviewData> = sequenceOf(
        QuizPreviewData(
            episode = EpisodeEntity(
                episodeId = "ep1",
                serialName = "ठरलं तर मग",
                episodeNumber = 620,
                title = "दैनिक एपिसोड क्विझ",
                youtubeVideoId = "dQw4w9WgXcQ",
                youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                thumbnailUrl = "",
                durationSeconds = 600,
                durationFormatted = "10:00",
                publishDate = "28 सप्टें 2026",
                description = "एपिसोड आधारित प्रश्नमंजुषा"
            ),
            questions = listOf(
                QuestionEntity(
                    questionId = "q1",
                    episodeId = "ep1",
                    questionText = "आजच्या भागात सायलीने कोणाला मदत केली?",
                    optionA = "अर्जुन",
                    optionB = "कल्पना",
                    optionC = "अस्मिता",
                    optionD = "सुप्रिया",
                    correctAnswer = 0,
                    explanation = "सायलीने कोर्टात जाऊन अर्जुनला योग्य कागदपत्रे दिली."
                ),
                QuestionEntity(
                    questionId = "q2",
                    episodeId = "ep1",
                    questionText = "मालिकेतील कोर्ट सीन कोणत्या विषयावर होता?",
                    optionA = "कंपनी केस",
                    optionB = "घर व्यवहार",
                    optionC = "लग्न",
                    optionD = "गाडी अपघात",
                    correctAnswer = 0,
                    explanation = "कंपनीच्या गैरव्यवहाराविरुद्ध केस चालू होती."
                )
            ),
            currentIndex = 0,
            userAnswers = emptyMap(),
            quizResult = null,
            walletSummary = WalletSummary(
                currentBalance = 50.0,
                totalEarnings = 100.0,
                paidAmount = 50.0,
                pendingAmount = 0.0
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun QuizScreenPreview(
    @PreviewParameter(QuizPreviewParameterProvider::class) data: QuizPreviewData
) {
    MyApplicationTheme {
        QuizScreen(
            episode = data.episode,
            questions = data.questions,
            currentIndex = data.currentIndex,
            userAnswers = data.userAnswers,
            quizResult = data.quizResult,
            walletSummary = data.walletSummary,
            onSelectAnswer = { _, _ -> },
            onNextQuestion = {},
            onPrevQuestion = {},
            onSubmitQuiz = {},
            onExitQuiz = {},
            onViewWallet = {}
        )
    }
}

