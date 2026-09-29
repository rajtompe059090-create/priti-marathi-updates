package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class PritiRepository(
    private val db: PritiDatabase
) {
    private val episodeDao = db.episodeDao()
    private val questionDao = db.questionDao()
    private val userProgressDao = db.userProgressDao()
    private val quizAttemptDao = db.quizAttemptDao()
    private val walletTransactionDao = db.walletTransactionDao()
    private val withdrawalDao = db.withdrawalDao()
    private val userProfileDao = db.userProfileDao()
    private val announcementDao = db.announcementDao()
    private val appSettingsDao = db.appSettingsDao()
    private val adminAuditLogDao = db.adminAuditLogDao()

    // Episodes
    val publishedEpisodes: Flow<List<EpisodeEntity>> = episodeDao.getPublishedEpisodes()
    val allEpisodes: Flow<List<EpisodeEntity>> = episodeDao.getAllEpisodes()
    val todayEpisode: Flow<EpisodeEntity?> = episodeDao.getTodayEpisode()

    suspend fun getEpisode(id: String): EpisodeEntity? = episodeDao.getEpisodeById(id)

    suspend fun saveEpisode(episode: EpisodeEntity, adminName: String) {
        if (episode.isTodayEpisode) {
            episodeDao.clearTodayEpisode()
        }
        episodeDao.insertEpisode(episode)
        logAudit("SAVE_EPISODE", adminName, "EPISODE", "Episode '${episode.title}' (Ep ${episode.episodeNumber}) saved/updated")
    }

    suspend fun deleteEpisode(id: String, adminName: String) {
        episodeDao.deleteEpisode(id)
        logAudit("DELETE_EPISODE", adminName, "EPISODE", "Episode ID '$id' deleted")
    }

    suspend fun setTodayEpisode(id: String, adminName: String) {
        episodeDao.clearTodayEpisode()
        episodeDao.setTodayEpisode(id)
        logAudit("SET_TODAY_EPISODE", adminName, "EPISODE", "Episode ID '$id' set as Today's featured episode")
    }

    // User Progress & Video Completion
    fun getUserProgress(userId: String): Flow<List<UserEpisodeProgressEntity>> =
        userProgressDao.getUserProgress(userId)

    suspend fun getProgressForEpisode(userId: String, episodeId: String): UserEpisodeProgressEntity? =
        userProgressDao.getProgressById("${userId}_$episodeId")

    suspend fun markVideoCompleted(userId: String, episodeId: String, watchedSeconds: Int): UserEpisodeProgressEntity {
        val progressId = "${userId}_$episodeId"
        val existing = userProgressDao.getProgressById(progressId)
        val updated = UserEpisodeProgressEntity(
            progressId = progressId,
            userId = userId,
            episodeId = episodeId,
            status = UserProgressStatus.COMPLETED,
            watchedSeconds = watchedSeconds,
            completedAt = existing?.completedAt ?: System.currentTimeMillis(),
            quizAttempted = existing?.quizAttempted ?: false,
            rewardClaimed = existing?.rewardClaimed ?: false
        )
        userProgressDao.insertProgress(updated)
        return updated
    }

    suspend fun updateWatchedProgress(userId: String, episodeId: String, watchedSeconds: Int) {
        val progressId = "${userId}_$episodeId"
        val existing = userProgressDao.getProgressById(progressId)
        val status = if (existing?.status == UserProgressStatus.COMPLETED ||
            existing?.status == UserProgressStatus.QUIZ_UNLOCKED ||
            existing?.status == UserProgressStatus.QUIZ_COMPLETED
        ) {
            existing.status
        } else {
            UserProgressStatus.IN_PROGRESS
        }
        val updated = UserEpisodeProgressEntity(
            progressId = progressId,
            userId = userId,
            episodeId = episodeId,
            status = status,
            watchedSeconds = maxOf(existing?.watchedSeconds ?: 0, watchedSeconds),
            completedAt = existing?.completedAt,
            quizAttempted = existing?.quizAttempted ?: false,
            rewardClaimed = existing?.rewardClaimed ?: false
        )
        userProgressDao.insertProgress(updated)
    }

    // Question Bank
    fun getQuestionsForEpisode(episodeId: String): Flow<List<QuestionEntity>> =
        questionDao.getQuestionsForEpisode(episodeId)

    suspend fun getQuestionsForQuiz(episodeId: String): List<QuestionEntity> {
        var allQuestions = questionDao.getQuestionsListForEpisode(episodeId)
        if (allQuestions.isEmpty()) {
            val generalQuestions = questionDao.getAllQuestions().first().filter { it.active }
            if (generalQuestions.isNotEmpty()) {
                allQuestions = generalQuestions
            }
        }
        val settings = appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()
        val needed = settings.questionsPerQuiz
        return if (settings.randomQuestionSelection && allQuestions.size > needed) {
            allQuestions.shuffled().take(needed)
        } else if (allQuestions.size > needed) {
            allQuestions.take(needed)
        } else {
            allQuestions
        }
    }

    val youTubeSyncService = YouTubeSyncService(db)

    suspend fun syncYouTubeUploads(apiKey: String? = null): Result<Int> {
        val result = youTubeSyncService.syncLatestUploads(apiKey)
        if (result.isSuccess) {
            logAudit("SYNC_YOUTUBE", "System", "YOUTUBE", "Synced ${result.getOrNull()} uploads from @pritimarathiupdates")
        }
        return result
    }

    fun getAllQuestions(): Flow<List<QuestionEntity>> =
        questionDao.getAllQuestions()

    suspend fun saveQuestion(question: QuestionEntity, adminName: String) {
        questionDao.insertQuestion(question)
        logAudit("SAVE_QUESTION", adminName, "QUESTION_BANK", "Question '${question.questionId}' saved for episode ${question.episodeId}")
    }

    suspend fun deleteQuestion(questionId: String, adminName: String) {
        questionDao.deleteQuestion(questionId)
        logAudit("DELETE_QUESTION", adminName, "QUESTION_BANK", "Question '$questionId' deleted")
    }

    // Deterministic Quiz evaluation & Server-side Reward calculation
    suspend fun submitQuiz(
        userId: String,
        episodeId: String,
        submittedAnswers: Map<String, Int> // questionId -> selectedOptionIndex (0..3)
    ): QuizResult {
        val allEpisodeQuestions = questionDao.getQuestionsListForEpisode(episodeId)
        val questions = allEpisodeQuestions.filter { submittedAnswers.containsKey(it.questionId) }
            .ifEmpty { allEpisodeQuestions.take(15) }
        val settings = appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()
        val progress = userProgressDao.getProgressById("${userId}_$episodeId")

        var correctCount = 0
        var incorrectCount = 0

        for (q in questions) {
            val userChoice = submittedAnswers[q.questionId]
            if (userChoice != null && userChoice == q.correctAnswer) {
                correctCount++
            } else {
                incorrectCount++
            }
        }

        // Check if reward was already claimed for this episode (One reward per episode rule)
        val alreadyClaimed = progress?.rewardClaimed == true ||
                walletTransactionDao.findTransactionByRef(userId, "quiz_$episodeId") != null

        val rewardPerAnswer = settings.rewardPerCorrectAnswer
        val calculatedReward = if (settings.rewardEnabled && !alreadyClaimed) {
            val potential = correctCount * rewardPerAnswer
            minOf(potential, settings.maxDailyReward)
        } else {
            0.0
        }

        val attemptId = "attempt_${userId}_${episodeId}_${System.currentTimeMillis()}"
        val attempt = QuizAttemptEntity(
            attemptId = attemptId,
            userId = userId,
            episodeId = episodeId,
            score = correctCount,
            totalQuestions = questions.size,
            rewardEarned = calculatedReward,
            completedAt = System.currentTimeMillis(),
            eligibleForReward = !alreadyClaimed && settings.rewardEnabled
        )
        quizAttemptDao.insertAttempt(attempt)

        // Update User Progress
        val updatedProgress = UserEpisodeProgressEntity(
            progressId = "${userId}_$episodeId",
            userId = userId,
            episodeId = episodeId,
            status = UserProgressStatus.QUIZ_COMPLETED,
            watchedSeconds = progress?.watchedSeconds ?: 300,
            completedAt = progress?.completedAt ?: System.currentTimeMillis(),
            quizAttempted = true,
            rewardClaimed = alreadyClaimed || calculatedReward > 0.0
        )
        userProgressDao.insertProgress(updatedProgress)

        // If eligible reward earned, create ledger transaction
        if (calculatedReward > 0.0) {
            val tx = WalletTransactionEntity(
                transactionId = "tx_quiz_${episodeId}_$userId",
                userId = userId,
                type = TransactionType.QUIZ_REWARD,
                amount = calculatedReward,
                description = "📝 Episode ${episodeId.replace("ep_", "")} क्विझ बक्षीस ($correctCount बरोबर उत्तर)",
                status = TransactionStatus.APPROVED,
                timestamp = System.currentTimeMillis(),
                referenceId = "quiz_$episodeId"
            )
            walletTransactionDao.insertTransaction(tx)
        }

        // Update User profile stats
        val user = userProfileDao.getUserProfileOnce(userId)
        if (user != null) {
            val updatedUser = user.copy(
                totalQuizzesCompleted = user.totalQuizzesCompleted + 1,
                totalCorrectAnswers = user.totalCorrectAnswers + correctCount
            )
            userProfileDao.updateUser(updatedUser)
        }

        return QuizResult(
            score = correctCount,
            totalQuestions = questions.size,
            incorrectCount = incorrectCount,
            rewardEarned = calculatedReward,
            alreadyClaimed = alreadyClaimed
        )
    }

    // Wallet Ledger & Balance
    fun getUserTransactions(userId: String): Flow<List<WalletTransactionEntity>> =
        walletTransactionDao.getUserTransactions(userId)

    fun getAllTransactions(): Flow<List<WalletTransactionEntity>> =
        walletTransactionDao.getAllTransactions()

    suspend fun calculateWalletBalance(userId: String): WalletSummary {
        val txs = walletTransactionDao.getUserTransactions(userId).first()
        var totalEarnings = 0.0
        var totalWithdrawn = 0.0
        var pendingWithdrawals = 0.0

        for (tx in txs) {
            when (tx.type) {
                TransactionType.WELCOME_BONUS,
                TransactionType.QUIZ_REWARD,
                TransactionType.STREAK_BONUS,
                TransactionType.ADMIN_ADJUSTMENT -> {
                    if (tx.status == TransactionStatus.APPROVED || tx.status == TransactionStatus.PAID) {
                        totalEarnings += tx.amount
                    }
                }
                TransactionType.WITHDRAWAL -> {
                    if (tx.status == TransactionStatus.APPROVED || tx.status == TransactionStatus.PAID) {
                        totalWithdrawn += tx.amount
                    } else if (tx.status == TransactionStatus.PENDING) {
                        pendingWithdrawals += tx.amount
                    }
                }
            }
        }

        val currentBalance = maxOf(0.0, totalEarnings - totalWithdrawn - pendingWithdrawals)
        return WalletSummary(
            currentBalance = currentBalance,
            totalEarnings = totalEarnings,
            paidAmount = totalWithdrawn,
            pendingAmount = pendingWithdrawals
        )
    }

    // Withdrawals
    fun getUserWithdrawals(userId: String): Flow<List<WithdrawalRequestEntity>> =
        withdrawalDao.getUserWithdrawals(userId)

    fun getAllWithdrawals(): Flow<List<WithdrawalRequestEntity>> =
        withdrawalDao.getAllWithdrawals()

    suspend fun requestWithdrawal(
        userId: String,
        userName: String,
        userPhone: String,
        amount: Double,
        payoutMethod: String,
        payoutAddress: String
    ): Result<WithdrawalRequestEntity> {
        val settings = appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()
        if (amount < settings.minWithdrawal) {
            return Result.failure(Exception("किमान पैसे काढण्याची मर्यादा ₹${settings.minWithdrawal.toInt()} आहे."))
        }
        val summary = calculateWalletBalance(userId)
        if (amount > summary.currentBalance) {
            return Result.failure(Exception("वॉलेटमध्ये पुरेसे बॅलन्स नाही. उपलब्ध बॅलन्स: ₹${summary.currentBalance}"))
        }

        val withdrawalId = "wdr_${System.currentTimeMillis()}_$userId"
        val request = WithdrawalRequestEntity(
            withdrawalId = withdrawalId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            amount = amount,
            payoutMethod = payoutMethod,
            payoutAddress = payoutAddress,
            status = WithdrawalStatus.REQUESTED,
            requestedAt = System.currentTimeMillis()
        )
        withdrawalDao.insertWithdrawal(request)

        // Create a pending ledger entry to lock the amount from double-spending
        val tx = WalletTransactionEntity(
            transactionId = "tx_$withdrawalId",
            userId = userId,
            type = TransactionType.WITHDRAWAL,
            amount = amount,
            description = "💸 पैसे काढण्याची विनंती ($payoutMethod: $payoutAddress)",
            status = TransactionStatus.PENDING,
            timestamp = System.currentTimeMillis(),
            referenceId = withdrawalId
        )
        walletTransactionDao.insertTransaction(tx)

        return Result.success(request)
    }

    suspend fun updateWithdrawalStatus(
        withdrawalId: String,
        newStatus: WithdrawalStatus,
        adminName: String,
        adminNote: String?
    ) {
        val withdrawals = withdrawalDao.getAllWithdrawals().first()
        val target = withdrawals.find { it.withdrawalId == withdrawalId } ?: return

        val updated = target.copy(
            status = newStatus,
            processedAt = System.currentTimeMillis(),
            adminNote = adminNote
        )
        withdrawalDao.updateWithdrawal(updated)

        // Update corresponding wallet transaction
        val txId = "tx_$withdrawalId"
        val txs = walletTransactionDao.getUserTransactions(target.userId).first()
        val tx = txs.find { it.transactionId == txId }
        if (tx != null) {
            val newTxStatus = when (newStatus) {
                WithdrawalStatus.APPROVED -> TransactionStatus.APPROVED
                WithdrawalStatus.PAID -> TransactionStatus.PAID
                WithdrawalStatus.REJECTED -> TransactionStatus.REJECTED
                else -> TransactionStatus.PENDING
            }
            walletTransactionDao.insertTransaction(tx.copy(status = newTxStatus))
        }

        logAudit("UPDATE_WITHDRAWAL", adminName, "WITHDRAWAL", "Withdrawal $withdrawalId set to $newStatus by $adminName")
    }

    // User Profiles
    fun getUserProfile(userId: String): Flow<UserProfileEntity?> =
        userProfileDao.getUserProfile(userId)

    fun getAllUsers(): Flow<List<UserProfileEntity>> =
        userProfileDao.getAllUsers()

    suspend fun updateUser(user: UserProfileEntity) = userProfileDao.updateUser(user)

    suspend fun saveUser(user: UserProfileEntity) = userProfileDao.insertUser(user)

    suspend fun toggleUserSuspension(userId: String, adminName: String) {
        val user = userProfileDao.getUserProfileOnce(userId) ?: return
        val updated = user.copy(isSuspended = !user.isSuspended)
        userProfileDao.updateUser(updated)
        logAudit("TOGGLE_SUSPEND_USER", adminName, "USER", "User ${user.name} (${user.userId}) suspended state set to ${updated.isSuspended}")
    }

    // Announcements
    val activeAnnouncements: Flow<List<AnnouncementEntity>> = announcementDao.getActiveAnnouncements()
    val allAnnouncements: Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncements()

    suspend fun saveAnnouncement(announcement: AnnouncementEntity, adminName: String) {
        announcementDao.insertAnnouncement(announcement)
        logAudit("SAVE_ANNOUNCEMENT", adminName, "ANNOUNCEMENT", "Announcement '${announcement.title}' published")
    }

    suspend fun deleteAnnouncement(id: String, adminName: String) {
        announcementDao.deleteAnnouncement(id)
        logAudit("DELETE_ANNOUNCEMENT", adminName, "ANNOUNCEMENT", "Announcement ID '$id' deleted")
    }

    // Settings
    val appSettings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()

    suspend fun updateSettings(settings: AppSettingsEntity, adminName: String) {
        appSettingsDao.insertSettings(settings)
        logAudit("UPDATE_SETTINGS", adminName, "SETTINGS", "Settings updated: Reward ₹${settings.rewardPerCorrectAnswer}, Min Wdr: ₹${settings.minWithdrawal}")
    }

    // Audit Logs
    val auditLogs: Flow<List<AdminAuditLogEntity>> = adminAuditLogDao.getAllLogs()

    suspend fun logAudit(action: String, performedBy: String, targetType: String, details: String) {
        val log = AdminAuditLogEntity(
            logId = "log_${System.currentTimeMillis()}_${(1000..9999).random()}",
            action = action,
            performedBy = performedBy,
            targetType = targetType,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        adminAuditLogDao.insertLog(log)
    }

    suspend fun resetTestData(adminName: String) {
        PritiDatabase.resetAndRepopulateData(db)
        logAudit("RESET_TEST_DATA", adminName, "DATABASE", "All test data reset and initial seed data re-populated.")
    }
}

data class QuizResult(
    val score: Int,
    val totalQuestions: Int,
    val incorrectCount: Int,
    val rewardEarned: Double,
    val alreadyClaimed: Boolean
)

data class WalletSummary(
    val currentBalance: Double,
    val totalEarnings: Double,
    val paidAmount: Double,
    val pendingAmount: Double
)
