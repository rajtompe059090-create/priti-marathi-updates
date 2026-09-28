package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.PritiDatabase
import com.example.data.model.*
import com.example.data.repository.PritiRepository
import com.example.data.repository.QuizResult
import com.example.data.repository.WalletSummary
import com.example.ui.ads.InterstitialAdManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PritiDatabase.getInstance(application, viewModelScope)
    val repository = PritiRepository(db)
    val interstitialAdManager = InterstitialAdManager(application)

    // Current active user
    private val _currentUserId = MutableStateFlow("user_default_1")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // Role state
    private val _currentRole = MutableStateFlow("USER") // "USER" or "ADMIN"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _showAdminPinPrompt = MutableStateFlow(false)
    val showAdminPinPrompt: StateFlow<Boolean> = _showAdminPinPrompt.asStateFlow()

    // User navigation tabs: 0: Home, 1: Episodes, 2: Quiz, 3: Wallet, 4: Profile
    private val _userTab = MutableStateFlow(0)
    val userTab: StateFlow<Int> = _userTab.asStateFlow()

    // Admin navigation tabs: 0: Dashboard, 1: Episodes, 2: QuestionBank, 3: QuizConfig, 4: Rewards, 5: Users, 6: Withdrawals, 7: Announcements, 8: Audit
    private val _adminTab = MutableStateFlow(0)
    val adminTab: StateFlow<Int> = _adminTab.asStateFlow()

    // Observables from DB
    val publishedEpisodes: StateFlow<List<EpisodeEntity>> = repository.publishedEpisodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEpisodes: StateFlow<List<EpisodeEntity>> = repository.allEpisodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEpisode: StateFlow<EpisodeEntity?> = repository.todayEpisode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userProfile: StateFlow<UserProfileEntity?> = _currentUserId
        .flatMapLatest { repository.getUserProfile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userProgressList: StateFlow<List<UserEpisodeProgressEntity>> = _currentUserId
        .flatMapLatest { repository.getUserProgress(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userTransactions: StateFlow<List<WalletTransactionEntity>> = _currentUserId
        .flatMapLatest { repository.getUserTransactions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<WalletTransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = _currentUserId
        .flatMapLatest { repository.getUserWithdrawals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = repository.getAllWithdrawals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserProfileEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<QuestionEntity>> = repository.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAnnouncements: StateFlow<List<AnnouncementEntity>> = repository.activeAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnnouncements: StateFlow<List<AnnouncementEntity>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AdminAuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wallet Summary State
    private val _walletSummary = MutableStateFlow(WalletSummary(0.0, 0.0, 0.0, 0.0))
    val walletSummary: StateFlow<WalletSummary> = _walletSummary.asStateFlow()

    // Episode Detail & YouTube Watch Dialog
    private val _selectedEpisode = MutableStateFlow<EpisodeEntity?>(null)
    val selectedEpisode: StateFlow<EpisodeEntity?> = _selectedEpisode.asStateFlow()

    private val _watchCompletionPrompt = MutableStateFlow<EpisodeEntity?>(null)
    val watchCompletionPrompt: StateFlow<EpisodeEntity?> = _watchCompletionPrompt.asStateFlow()

    // Dedicated Full-Screen Video Watching Screen State
    private val _currentWatchingEpisode = MutableStateFlow<EpisodeEntity?>(null)
    val currentWatchingEpisode: StateFlow<EpisodeEntity?> = _currentWatchingEpisode.asStateFlow()

    // Quiz Session State
    private val _quizQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val quizQuestions: StateFlow<List<QuestionEntity>> = _quizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userQuizAnswers = MutableStateFlow<Map<String, Int>>(emptyMap())
    val userQuizAnswers: StateFlow<Map<String, Int>> = _userQuizAnswers.asStateFlow()

    private val _quizResult = MutableStateFlow<QuizResult?>(null)
    val quizResult: StateFlow<QuizResult?> = _quizResult.asStateFlow()

    private val _activeQuizEpisode = MutableStateFlow<EpisodeEntity?>(null)
    val activeQuizEpisode: StateFlow<EpisodeEntity?> = _activeQuizEpisode.asStateFlow()

    // Status Message / Toast
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        refreshWalletSummary()
    }

    fun selectUserTab(tab: Int) {
        _userTab.value = tab
    }

    fun selectAdminTab(tab: Int) {
        _adminTab.value = tab
    }

    fun openAdminPrompt() {
        if (_isAdminAuthenticated.value) {
            _currentRole.value = "ADMIN"
        } else {
            _showAdminPinPrompt.value = true
        }
    }

    fun dismissAdminPrompt() {
        _showAdminPinPrompt.value = false
    }

    fun verifyAdminPin(pin: String): Boolean {
        return if (pin == "7788" || pin == "1234") {
            _isAdminAuthenticated.value = true
            _showAdminPinPrompt.value = false
            _currentRole.value = "ADMIN"
            _snackbarMessage.value = "ॲडमिन मोड सक्रिय केला गेला आहे (LOCAL TEST MODE)."
            true
        } else {
            _snackbarMessage.value = "चुकीचा PIN! कृपया 7788 टाका."
            false
        }
    }

    fun switchToUserRole() {
        _currentRole.value = "USER"
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun openEpisodeDetail(episode: EpisodeEntity) {
        _selectedEpisode.value = episode
    }

    fun closeEpisodeDetail() {
        _selectedEpisode.value = null
    }

    fun startWatchingEpisode(episode: EpisodeEntity) {
        interstitialAdManager.loadAd()
        _currentWatchingEpisode.value = episode
    }

    fun stopWatchingEpisode() {
        _currentWatchingEpisode.value = null
    }

    fun confirmVideoWatchedFromWatchingScreen(activity: Activity?, episode: EpisodeEntity) {
        if (activity != null) {
            interstitialAdManager.showAd(activity) {
                stopWatchingEpisode()
                completeEpisodeAndUnlockQuiz(episode)
            }
        } else {
            stopWatchingEpisode()
            completeEpisodeAndUnlockQuiz(episode)
        }
    }

    fun getNextEpisode(current: EpisodeEntity): EpisodeEntity? {
        val list = publishedEpisodes.value
        val index = list.indexOfFirst { it.episodeId == current.episodeId }
        return if (index != -1 && index + 1 < list.size) {
            list[index + 1]
        } else null
    }

    fun promptWatchCompletion(episode: EpisodeEntity) {
        interstitialAdManager.loadAd()
        _watchCompletionPrompt.value = episode
    }

    fun dismissWatchCompletion() {
        _watchCompletionPrompt.value = null
    }

    fun confirmVideoWatched(activity: Activity?, episode: EpisodeEntity) {
        _watchCompletionPrompt.value = null
        if (activity != null) {
            // Flow: Completion confirmation -> Interstitial TEST AD -> Ad dismissed -> Assessment unlock
            interstitialAdManager.showAd(activity) {
                completeEpisodeAndUnlockQuiz(episode)
            }
        } else {
            completeEpisodeAndUnlockQuiz(episode)
        }
    }

    private fun completeEpisodeAndUnlockQuiz(episode: EpisodeEntity) {
        viewModelScope.launch {
            repository.markVideoCompleted(
                userId = _currentUserId.value,
                episodeId = episode.episodeId,
                watchedSeconds = episode.durationSeconds
            )
            _snackbarMessage.value = "🎉 अभिनंदन! Episode पूर्ण झाला. आजचा Assessment अनलॉक झाला आहे!"
            // Automatically select Quiz tab and load 15 randomized questions
            prepareQuiz(episode)
        }
    }

    fun prepareQuiz(episode: EpisodeEntity) {
        viewModelScope.launch {
            val questions = repository.getQuestionsForQuiz(episode.episodeId)
            _activeQuizEpisode.value = episode
            _quizQuestions.value = questions
            _currentQuestionIndex.value = 0
            _userQuizAnswers.value = emptyMap()
            _quizResult.value = null
            _userTab.value = 2 // Switch to Quiz tab
        }
    }

    fun selectQuizAnswer(questionId: String, optionIndex: Int) {
        val current = _userQuizAnswers.value.toMutableMap()
        current[questionId] = optionIndex
        _userQuizAnswers.value = current
    }

    fun nextQuizQuestion() {
        if (_currentQuestionIndex.value < _quizQuestions.value.size - 1) {
            _currentQuestionIndex.value += 1
        }
    }

    fun previousQuizQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
        }
    }

    fun submitQuiz() {
        val episode = _activeQuizEpisode.value ?: return
        viewModelScope.launch {
            val result = repository.submitQuiz(
                userId = _currentUserId.value,
                episodeId = episode.episodeId,
                submittedAnswers = _userQuizAnswers.value
            )
            _quizResult.value = result
            refreshWalletSummary()
            if (result.rewardEarned > 0) {
                _snackbarMessage.value = "🎉 शाब्बास! ₹${result.rewardEarned.toInt()} बक्षीस तुमच्या वॉलेटमध्ये जमा झाले!"
            } else if (result.alreadyClaimed) {
                _snackbarMessage.value = "या भागाचे बक्षीस तुम्ही आधीच मिळवले आहे. सराव क्विझ पूर्ण झाली!"
            }
        }
    }

    fun exitQuiz() {
        _quizResult.value = null
        _activeQuizEpisode.value = null
        _quizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _userQuizAnswers.value = emptyMap()
        _userTab.value = 0
    }

    fun refreshWalletSummary() {
        viewModelScope.launch {
            val summary = repository.calculateWalletBalance(_currentUserId.value)
            _walletSummary.value = summary
        }
    }

    fun submitWithdrawalRequest(
        amount: Double,
        payoutMethod: String,
        payoutAddress: String,
        userName: String,
        userPhone: String
    ) {
        viewModelScope.launch {
            val result = repository.requestWithdrawal(
                userId = _currentUserId.value,
                userName = userName,
                userPhone = userPhone,
                amount = amount,
                payoutMethod = payoutMethod,
                payoutAddress = payoutAddress
            )
            if (result.isSuccess) {
                _snackbarMessage.value = "विनंती यशस्वी! ॲडमिन मंजुरीनंतर पैसे खात्यात जमा होतील."
                refreshWalletSummary()
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "त्रुटी आली."
            }
        }
    }

    // Admin Operations
    fun adminSaveEpisode(episode: EpisodeEntity) {
        viewModelScope.launch {
            repository.saveEpisode(episode, "Admin")
            _snackbarMessage.value = "भाग '${episode.title}' यशस्वीरीत्या सेव्ह झाला."
        }
    }

    fun adminDeleteEpisode(id: String) {
        viewModelScope.launch {
            repository.deleteEpisode(id, "Admin")
            _snackbarMessage.value = "भाग हटवला गेला."
        }
    }

    fun adminSetTodayEpisode(id: String) {
        viewModelScope.launch {
            repository.setTodayEpisode(id, "Admin")
            _snackbarMessage.value = "आजचा मुख्य भाग अपडेट केला गेला."
        }
    }

    fun adminSaveQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            repository.saveQuestion(question, "Admin")
            _snackbarMessage.value = "प्रश्न सेव्ह झाला."
        }
    }

    fun adminDeleteQuestion(id: String) {
        viewModelScope.launch {
            repository.deleteQuestion(id, "Admin")
            _snackbarMessage.value = "प्रश्न हटवला गेला."
        }
    }

    fun adminUpdateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings, "Admin")
            _snackbarMessage.value = "रिवॉर्ड सेटिंग्ज अपडेट झाल्या."
        }
    }

    fun adminUpdateWithdrawal(id: String, newStatus: WithdrawalStatus, note: String?) {
        viewModelScope.launch {
            repository.updateWithdrawalStatus(id, newStatus, "Admin", note)
            _snackbarMessage.value = "पैसे काढण्याची विनंती $newStatus केली गेली."
            refreshWalletSummary()
        }
    }

    fun adminToggleUserSuspension(userId: String) {
        viewModelScope.launch {
            repository.toggleUserSuspension(userId, "Admin")
            _snackbarMessage.value = "युझर स्टेटस बदलले."
        }
    }

    fun adminSaveAnnouncement(announcement: AnnouncementEntity) {
        viewModelScope.launch {
            repository.saveAnnouncement(announcement, "Admin")
            _snackbarMessage.value = "सूचना प्रसिद्ध झाली."
        }
    }

    fun adminDeleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id, "Admin")
            _snackbarMessage.value = "सूचना हटवली गेली."
        }
    }

    fun adminResetTestData() {
        viewModelScope.launch {
            repository.resetTestData("Admin")
            _snackbarMessage.value = "सर्व टेस्ट डेटा पूर्ववत (Reset) करण्यात आला आहे."
            refreshWalletSummary()
        }
    }
}
