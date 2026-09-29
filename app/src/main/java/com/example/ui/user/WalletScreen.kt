package com.example.ui.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.WalletTransactionEntity
import com.example.data.repository.WalletSummary
import com.example.ui.components.GradientButton
import com.example.ui.components.NeonCard
import com.example.ui.components.PillBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WalletScreen(
    walletSummary: WalletSummary,
    transactions: List<WalletTransactionEntity>,
    appSettings: AppSettingsEntity?,
    userName: String,
    userPhone: String,
    onRequestWithdrawal: (Double, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWithdrawDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "माझे वॉलेट (Wallet)",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "दैनंदिन क्विझ आणि चॅनेल सहभागातून मिळवलेली खरी बक्षिसे",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Main Balance Card
        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = Brush.horizontalGradient(listOf(NeonRose, NeonAmber)),
                backgroundColor = SurfaceDark,
                cornerRadius = 22.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "उपलब्ध वॉलेट बॅलन्स",
                            color = NeonAmberBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", walletSummary.currentBalance)}",
                            color = TextPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonAmberGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💰", fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Withdraw Button
                val minWithdrawal = appSettings?.minWithdrawal ?: 100.0
                GradientButton(
                    text = "पैसे काढा (किमान ₹${minWithdrawal.toInt()})",
                    onClick = { showWithdrawDialog = true },
                    icon = Icons.Default.Payments,
                    colors = listOf(NeonRose, NeonAmber),
                    enabled = walletSummary.currentBalance >= minWithdrawal,
                    testTag = "withdraw_btn"
                )

                if (walletSummary.currentBalance < minWithdrawal) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "पैसे काढण्यासाठी किमान ₹${minWithdrawal.toInt()} बॅलन्स आवश्यक आहे.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Summary Statistics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "एकूण कमाई",
                    value = "₹${walletSummary.totalEarnings.toInt()}",
                    icon = Icons.Default.TrendingUp,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "जमा झालेले",
                    value = "₹${walletSummary.paidAmount.toInt()}",
                    icon = Icons.Default.CheckCircle,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pending and Bonus Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "प्रक्रियेत (Pending)",
                    value = "₹${walletSummary.pendingAmount.toInt()}",
                    icon = Icons.Default.HourglassEmpty,
                    accentColor = NeonAmber,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "प्रति उत्तर बक्षीस",
                    value = "₹${appSettings?.rewardPerCorrectAnswer?.toInt() ?: 2}",
                    icon = Icons.Default.CardGiftcard,
                    accentColor = NeonRoseBright,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Transaction History Header
        item {
            Text(
                text = "व्यवहार इतिहास (Transaction History)",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Transaction Ledger Items
        if (transactions.isEmpty()) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📜", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "अद्याप कोणतेही व्यवहार नाहीत",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "आजचा व्हिडिओ पाहून क्विझ सोडवा आणि पहिले बक्षीस मिळवा!",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            items(transactions, key = { it.transactionId }) { tx ->
                val isCredit = tx.type != TransactionType.WITHDRAWAL
                val sign = if (isCredit) "+" else "-"
                val amountColor = if (isCredit) EmeraldGreen else NeonRoseBright

                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderSubtle,
                    backgroundColor = SurfaceDark
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCredit) EmeraldGreenGlow else NeonRoseGlow
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isCredit) EmeraldGreen else NeonRoseBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = tx.description,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(Date(tx.timestamp)),
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$sign₹${tx.amount.toInt()}",
                                color = amountColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val (badgeBg, badgeText, badgeColor) = when (tx.status) {
                                TransactionStatus.APPROVED -> Triple(EmeraldGreenGlow, "मंजूर", EmeraldGreen)
                                TransactionStatus.PAID -> Triple(EmeraldGreenGlow, "जमा झाले", EmeraldGreen)
                                TransactionStatus.PENDING -> Triple(NeonAmberGlow, "प्रलंबित", NeonAmberBright)
                                TransactionStatus.REJECTED -> Triple(NeonRoseGlow, "नाकारले", NeonRoseBright)
                            }
                            PillBadge(
                                text = badgeText,
                                backgroundColor = badgeBg,
                                textColor = badgeColor,
                                borderColor = badgeColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Withdrawal Request Modal Dialog
    if (showWithdrawDialog) {
        WithdrawalDialog(
            currentBalance = walletSummary.currentBalance,
            minWithdrawal = appSettings?.minWithdrawal ?: 100.0,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { amount, method, address ->
                showWithdrawDialog = false
                onRequestWithdrawal(amount, method, address)
            }
        )
    }
}

@Composable
fun WithdrawalDialog(
    currentBalance: Double,
    minWithdrawal: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(minWithdrawal.toInt().toString()) }
    var selectedMethod by remember { mutableStateOf("UPI") }
    var payoutAddress by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NeonRoseGlow,
                    border = BorderStroke(1.dp, NeonRose)
                ) {
                    Text(
                        text = "TEST MODE — हा वास्तविक payment नाही",
                        color = NeonRoseBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "पैसे काढण्याची विनंती",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "उपलब्ध बॅलन्स: ₹${currentBalance.toInt()}",
                    color = NeonAmberBright,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("रक्कम (₹)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonRose,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                // Payout Method Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "Bank Transfer").forEach { method ->
                        val isSelected = selectedMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMethod = method },
                            label = { Text(method, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonRose,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Payout address input
                OutlinedTextField(
                    value = payoutAddress,
                    onValueChange = { payoutAddress = it },
                    label = {
                        Text(
                            if (selectedMethod == "UPI") "UPI ID (उदा. 9876543210@paytm)" else "बँक खाते क्रमांक आणि IFSC",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonRose,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = NeonRose, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount < minWithdrawal) {
                        errorMessage = "किमान ₹${minWithdrawal.toInt()} रक्कम प्रविष्ट करा."
                    } else if (amount > currentBalance) {
                        errorMessage = "वॉलेटमध्ये पुरेशी रक्कम नाही."
                    } else if (payoutAddress.trim().isEmpty()) {
                        errorMessage = "कृपया वैध पत्ता/UPI ID टाका."
                    } else {
                        onSubmit(amount, selectedMethod, payoutAddress.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                modifier = Modifier.testTag("confirm_withdraw_btn")
            ) {
                Text("विनंती पाठवा", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करा", color = TextMuted)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp)
    )
}

data class WalletPreviewData(
    val walletSummary: WalletSummary,
    val transactions: List<WalletTransactionEntity>,
    val appSettings: AppSettingsEntity?,
    val userName: String,
    val userPhone: String
)

class WalletPreviewParameterProvider : PreviewParameterProvider<WalletPreviewData> {
    override val values: Sequence<WalletPreviewData> = sequenceOf(
        WalletPreviewData(
            walletSummary = WalletSummary(
                currentBalance = 135.0,
                totalEarnings = 135.0,
                paidAmount = 0.0,
                pendingAmount = 0.0
            ),
            transactions = listOf(
                WalletTransactionEntity(
                    transactionId = "tx1",
                    userId = "u1",
                    type = TransactionType.WELCOME_BONUS,
                    amount = 5.0,
                    description = "वेलकम बोनस",
                    status = TransactionStatus.APPROVED,
                    timestamp = System.currentTimeMillis() - 86400000
                ),
                WalletTransactionEntity(
                    transactionId = "tx2",
                    userId = "u1",
                    type = TransactionType.QUIZ_REWARD,
                    amount = 30.0,
                    description = "दैनिक क्विझ बक्षीस",
                    status = TransactionStatus.APPROVED,
                    timestamp = System.currentTimeMillis()
                )
            ),
            appSettings = AppSettingsEntity(
                minWithdrawal = 100.0,
                maxDailyReward = 30.0
            ),
            userName = "प्रिया देशमुख",
            userPhone = "+91 98765 43210"
        )
    )
}

@Preview(showBackground = true)
@Composable
fun WalletScreenPreview(
    @PreviewParameter(WalletPreviewParameterProvider::class) data: WalletPreviewData
) {
    MyApplicationTheme {
        WalletScreen(
            walletSummary = data.walletSummary,
            transactions = data.transactions,
            appSettings = data.appSettings,
            userName = data.userName,
            userPhone = data.userPhone,
            onRequestWithdrawal = { _, _, _ -> }
        )
    }
}

