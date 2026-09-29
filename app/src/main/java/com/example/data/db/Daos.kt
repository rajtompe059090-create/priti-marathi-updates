package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE status = 'PUBLISHED' ORDER BY isTodayEpisode DESC, createdAt DESC")
    fun getPublishedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes ORDER BY createdAt DESC")
    fun getAllEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE episodeId = :id LIMIT 1")
    suspend fun getEpisodeById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE isTodayEpisode = 1 LIMIT 1")
    fun getTodayEpisode(): Flow<EpisodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)

    @Query("UPDATE episodes SET isTodayEpisode = 0 WHERE isTodayEpisode = 1")
    suspend fun clearTodayEpisode()

    @Query("UPDATE episodes SET isTodayEpisode = 1 WHERE episodeId = :id")
    suspend fun setTodayEpisode(id: String)

    @Query("DELETE FROM episodes WHERE episodeId = :id")
    suspend fun deleteEpisode(id: String)

    @Query("DELETE FROM episodes")
    suspend fun clearAllEpisodes()
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE episodeId = :episodeId AND active = 1")
    fun getQuestionsForEpisode(episodeId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE episodeId = :episodeId AND active = 1")
    suspend fun getQuestionsListForEpisode(episodeId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions ORDER BY questionId DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE questionId = :id")
    suspend fun deleteQuestion(id: String)

    @Query("DELETE FROM questions")
    suspend fun clearAllQuestions()
}

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_episode_progress WHERE userId = :userId")
    fun getUserProgress(userId: String): Flow<List<UserEpisodeProgressEntity>>

    @Query("SELECT * FROM user_episode_progress WHERE progressId = :progressId LIMIT 1")
    suspend fun getProgressById(progressId: String): UserEpisodeProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserEpisodeProgressEntity)

    @Query("DELETE FROM user_episode_progress")
    suspend fun clearAllProgress()
}

@Dao
interface QuizAttemptDao {
    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY completedAt DESC")
    fun getUserQuizAttempts(userId: String): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND episodeId = :episodeId LIMIT 1")
    suspend fun getAttempt(userId: String, episodeId: String): QuizAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)

    @Query("SELECT COUNT(*) FROM quiz_attempts")
    fun getTotalQuizAttemptsCount(): Flow<Int>

    @Query("DELETE FROM quiz_attempts")
    suspend fun clearAllAttempts()
}

@Dao
interface WalletTransactionDao {
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserTransactions(userId: String): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity)

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId AND referenceId = :referenceId LIMIT 1")
    suspend fun findTransactionByRef(userId: String, referenceId: String): WalletTransactionEntity?

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getUserWithdrawals(userId: String): Flow<List<WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawals ORDER BY requestedAt DESC")
    fun getAllWithdrawals(): Flow<List<WithdrawalRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequestEntity)

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalRequestEntity)

    @Query("DELETE FROM withdrawals")
    suspend fun clearAllWithdrawals()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :id LIMIT 1")
    fun getUserProfile(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :id LIMIT 1")
    suspend fun getUserProfileOnce(id: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY joinedDate DESC")
    fun getAllUsers(): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfileEntity)

    @Update
    suspend fun updateUser(user: UserProfileEntity)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAllUsers()
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements WHERE active = 1 ORDER BY date DESC")
    fun getActiveAnnouncements(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements ORDER BY date DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)

    @Query("DELETE FROM announcements")
    suspend fun clearAllAnnouncements()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()
}

@Dao
interface AdminAuditLogDao {
    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AdminAuditLogEntity)

    @Query("DELETE FROM admin_audit_logs")
    suspend fun clearAllLogs()
}
