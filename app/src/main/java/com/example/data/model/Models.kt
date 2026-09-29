package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EpisodeStatus {
    DRAFT, SCHEDULED, PUBLISHED, ARCHIVED
}

enum class UserProgressStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, QUIZ_UNLOCKED, QUIZ_COMPLETED
}

enum class TransactionType {
    WELCOME_BONUS, QUIZ_REWARD, STREAK_BONUS, WITHDRAWAL, ADMIN_ADJUSTMENT
}

enum class TransactionStatus {
    PENDING, APPROVED, PAID, REJECTED
}

enum class WithdrawalStatus {
    REQUESTED, UNDER_REVIEW, APPROVED, PAID, REJECTED
}

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val episodeId: String,
    val serialName: String,
    val episodeNumber: Int,
    val title: String,
    val youtubeVideoId: String,
    val youtubeUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val durationFormatted: String,
    val publishDate: String,
    val description: String,
    val status: EpisodeStatus = EpisodeStatus.PUBLISHED,
    val isTodayEpisode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val questionId: String,
    val episodeId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: Int, // 0 for A, 1 for B, 2 for C, 3 for D
    val explanation: String,
    val difficulty: String = "Medium",
    val active: Boolean = true
)

@Entity(tableName = "user_episode_progress")
data class UserEpisodeProgressEntity(
    @PrimaryKey val progressId: String, // userId_episodeId
    val userId: String,
    val episodeId: String,
    val status: UserProgressStatus,
    val watchedSeconds: Int,
    val completedAt: Long?,
    val quizAttempted: Boolean = false,
    val rewardClaimed: Boolean = false
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val attemptId: String,
    val userId: String,
    val episodeId: String,
    val score: Int,
    val totalQuestions: Int,
    val rewardEarned: Double,
    val completedAt: Long,
    val eligibleForReward: Boolean
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val transactionId: String,
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val status: TransactionStatus,
    val timestamp: Long,
    val referenceId: String? = null
)

@Entity(tableName = "withdrawals")
data class WithdrawalRequestEntity(
    @PrimaryKey val withdrawalId: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val amount: Double,
    val payoutMethod: String, // e.g., "UPI" or "Bank Transfer"
    val payoutAddress: String, // UPI ID or Account Number
    val status: WithdrawalStatus,
    val requestedAt: Long,
    val processedAt: Long? = null,
    val adminNote: String? = null
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val phoneOrEmail: String,
    val joinedDate: String,
    val streakDays: Int = 1,
    val lastActiveDate: String,
    val isSuspended: Boolean = false,
    val role: String = "USER", // "USER" or "ADMIN"
    val avatarColorIndex: Int = 0,
    val totalQuizzesCompleted: Int = 0,
    val totalCorrectAnswers: Int = 0
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val date: String,
    val active: Boolean = true
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val rewardPerCorrectAnswer: Double = 2.0,
    val maxDailyReward: Double = 30.0,
    val welcomeBonus: Double = 5.0,
    val streakBonus: Double = 10.0,
    val minWithdrawal: Double = 100.0,
    val maxWithdrawalPerRequest: Double = 5000.0,
    val rewardEnabled: Boolean = true,
    val questionsPerQuiz: Int = 15,
    val randomQuestionSelection: Boolean = true
)

@Entity(tableName = "admin_audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey val logId: String,
    val action: String,
    val performedBy: String,
    val targetType: String,
    val details: String,
    val timestamp: Long
)
