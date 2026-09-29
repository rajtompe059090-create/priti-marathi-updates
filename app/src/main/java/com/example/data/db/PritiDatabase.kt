package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EpisodeEntity::class,
        QuestionEntity::class,
        UserEpisodeProgressEntity::class,
        QuizAttemptEntity::class,
        WalletTransactionEntity::class,
        WithdrawalRequestEntity::class,
        UserProfileEntity::class,
        AnnouncementEntity::class,
        AppSettingsEntity::class,
        AdminAuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PritiDatabase : RoomDatabase() {

    abstract fun episodeDao(): EpisodeDao
    abstract fun questionDao(): QuestionDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    abstract fun walletTransactionDao(): WalletTransactionDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun adminAuditLogDao(): AdminAuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: PritiDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): PritiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PritiDatabase::class.java,
                    "priti_marathi_updates.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun resetAndRepopulateData(db: PritiDatabase) {
            db.userProgressDao().clearAllProgress()
            db.quizAttemptDao().clearAllAttempts()
            db.walletTransactionDao().clearAllTransactions()
            db.withdrawalDao().clearAllWithdrawals()
            db.userProfileDao().clearAllUsers()
            db.announcementDao().clearAllAnnouncements()
            db.appSettingsDao().clearSettings()
            db.adminAuditLogDao().clearAllLogs()
            db.questionDao().clearAllQuestions()
            db.episodeDao().clearAllEpisodes()

            populateInitialData(db)
        }

        suspend fun populateInitialData(db: PritiDatabase) {
            // 1. App Settings
            val initialSettings = AppSettingsEntity(
                id = 1,
                rewardPerCorrectAnswer = 2.0,
                maxDailyReward = 30.0,
                welcomeBonus = 5.0,
                streakBonus = 10.0,
                minWithdrawal = 100.0,
                maxWithdrawalPerRequest = 5000.0,
                rewardEnabled = true,
                questionsPerQuiz = 15,
                randomQuestionSelection = true
            )
            db.appSettingsDao().insertSettings(initialSettings)

            // 2. Default Local User
            val defaultUser = UserProfileEntity(
                userId = "user_default_1",
                name = "प्रिया देशमुख",
                phoneOrEmail = "+91 98765 43210",
                joinedDate = "27 सप्टें 2026",
                streakDays = 3,
                lastActiveDate = "आज",
                isSuspended = false,
                role = "USER",
                avatarColorIndex = 0,
                totalQuizzesCompleted = 0,
                totalCorrectAnswers = 0
            )
            db.userProfileDao().insertUser(defaultUser)

            // 3. Welcome Bonus Transaction (+₹5)
            val welcomeBonusTx = WalletTransactionEntity(
                transactionId = "tx_welcome_1",
                userId = "user_default_1",
                type = TransactionType.WELCOME_BONUS,
                amount = 5.0,
                description = "🎉 Welcome Bonus (नवीन युझर स्वागत बोनस)",
                status = TransactionStatus.APPROVED,
                timestamp = System.currentTimeMillis() - 86400000L,
                referenceId = "welcome"
            )
            db.walletTransactionDao().insertTransaction(welcomeBonusTx)

            // 4. Real Episodes from @pritimarathiupdates channel
            val episodes = listOf(
                EpisodeEntity(
                    episodeId = "ep_gs0ow",
                    serialName = "पाठराखीण",
                    episodeNumber = 106,
                    title = "Pathrakhin Today Episode 29 September 2026 | पाठराखीण आजचा भाग | प्राजक्ताने सासर सोडण्यास नकार दिला",
                    youtubeVideoId = "-GS0owKJqlQ",
                    youtubeUrl = "https://www.youtube.com/watch?v=-GS0owKJqlQ",
                    thumbnailUrl = "https://i.ytimg.com/vi/-GS0owKJqlQ/hqdefault.jpg",
                    durationSeconds = 540,
                    durationFormatted = "09:00",
                    publishDate = "29 सप्टेंबर 2026",
                    description = "पाठराखीण आजचा भाग 29 सप्टेंबर 2026 मध्ये प्राजक्ताने घेतलेला मोठा आणि धाडसी निर्णय पाहायला मिळणार आहे. ती सूर्यवंशी घराची सून आणि आलोकची पत्नी आहे, त्यामुळे सासर सोडणार नाही!",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = true,
                    createdAt = System.currentTimeMillis()
                ),
                EpisodeEntity(
                    episodeId = "ep_kcys",
                    serialName = "पाठराखीण",
                    episodeNumber = 105,
                    title = "Pathrakhin Today's Episode 27 September 2026 | पाठराखीण आजचा भाग | प्राजक्ताचा मोठा निर्णय",
                    youtubeVideoId = "KcysA63NQG0",
                    youtubeUrl = "https://www.youtube.com/watch?v=KcysA63NQG0",
                    thumbnailUrl = "https://i.ytimg.com/vi/KcysA63NQG0/hqdefault.jpg",
                    durationSeconds = 515,
                    durationFormatted = "08:35",
                    publishDate = "27 सप्टेंबर 2026",
                    description = "पाठराखीण मालिकेतील आजचा एपिसोड अत्यंत रोमांचक ठरला. प्राजक्ताच्या आयुष्यात नव्या संकटाची चाहूल लागली असून सत्याचा खरा चेहरा समोर येणार का? सविस्तर विश्लेषण!",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = false,
                    createdAt = System.currentTimeMillis() - (86400000L * 2)
                ),
                EpisodeEntity(
                    episodeId = "ep_uqq6",
                    serialName = "पाठराखीण",
                    episodeNumber = 104,
                    title = "Pathrakhin Today's Episode 26 September 2026 | पाठराखीण आजचा भाग | प्राजक्ताने घेतला मोठा निर्णय!",
                    youtubeVideoId = "uQq6KWEtnTE",
                    youtubeUrl = "https://www.youtube.com/watch?v=uQq6KWEtnTE",
                    thumbnailUrl = "https://i.ytimg.com/vi/uQq6KWEtnTE/hqdefault.jpg",
                    durationSeconds = 480,
                    durationFormatted = "08:00",
                    publishDate = "26 सप्टेंबर 2026",
                    description = "प्राजक्ताने वाड्यावरील रहस्य उलगडण्यासाठी मोठे पाऊल उचलले आहे. काय घडले आजच्या एपिसोडमध्ये? जाणून घ्या संपूर्ण विश्लेषण.",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = false,
                    createdAt = System.currentTimeMillis() - (86400000L * 3)
                ),
                EpisodeEntity(
                    episodeId = "ep_pt0w",
                    serialName = "पाठराखीण",
                    episodeNumber = 103,
                    title = "Pathrakhin Today's Episode 25 September | आलोक-प्राजक्ताचं नातं बदलणार?",
                    youtubeVideoId = "PT0wyERgwF0",
                    youtubeUrl = "https://www.youtube.com/watch?v=PT0wyERgwF0",
                    thumbnailUrl = "https://i.ytimg.com/vi/PT0wyERgwF0/hqdefault.jpg",
                    durationSeconds = 465,
                    durationFormatted = "07:45",
                    publishDate = "25 सप्टेंबर 2026",
                    description = "आलोक आणि प्राजक्ताच्या नात्यातील नवीन वळण, वाड्यातील विरोध आणि सत्याची बाजू. प्रीती मराठी अपडेट्स खास रिपोर्ट.",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = false,
                    createdAt = System.currentTimeMillis() - (86400000L * 4)
                ),
                EpisodeEntity(
                    episodeId = "ep_firj",
                    serialName = "पाठराखीण",
                    episodeNumber = 102,
                    title = "Pathrakhin Todays Episode 23 September 2026 | पाठराखीण आजचा भाग | आलोकने प्राजक्ताला घराबाहेर",
                    youtubeVideoId = "FirjQ5sKz2U",
                    youtubeUrl = "https://www.youtube.com/watch?v=FirjQ5sKz2U",
                    thumbnailUrl = "https://i.ytimg.com/vi/FirjQ5sKz2U/hqdefault.jpg",
                    durationSeconds = 500,
                    durationFormatted = "08:20",
                    publishDate = "23 सप्टेंबर 2026",
                    description = "आलोकने प्राजक्ताला घराबाहेर काढण्याचा प्रयत्न केला का? मालिकेतील सर्वात नाट्यमय वळण.",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = false,
                    createdAt = System.currentTimeMillis() - (86400000L * 6)
                ),
                EpisodeEntity(
                    episodeId = "ep_oqfx",
                    serialName = "पाठराखीण",
                    episodeNumber = 101,
                    title = "Pathrakhin Today's Episode 22 Sep 2026 | पाठराखिण आजचा भाग | आलोकने प्राजक्ताला घराबाहेर काढलं?",
                    youtubeVideoId = "OqfXiu9yVgk",
                    youtubeUrl = "https://www.youtube.com/watch?v=OqfXiu9yVgk",
                    thumbnailUrl = "https://i.ytimg.com/vi/OqfXiu9yVgk/hqdefault.jpg",
                    durationSeconds = 450,
                    durationFormatted = "07:30",
                    publishDate = "22 सप्टेंबर 2026",
                    description = "22 सप्टेंबरचा महाएपिसोड — प्राजक्ताच्या आयुष्यात काय घडले? संपूर्ण महाएपिसोड विश्लेषण.",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = false,
                    createdAt = System.currentTimeMillis() - (86400000L * 7)
                ),
                EpisodeEntity(
                    episodeId = "ep_jjlq",
                    serialName = "पाठराखीण",
                    episodeNumber = 100,
                    title = "Pathrakhin Today's Episode 22 Sep 2026 | पाठराखिण आजचा भाग | प्राजक्ताच्या आयुष्यात मोठं वळण? 😱",
                    youtubeVideoId = "JJlQWkJwnVA",
                    youtubeUrl = "https://www.youtube.com/watch?v=JJlQWkJwnVA",
                    thumbnailUrl = "https://i.ytimg.com/vi/JJlQWkJwnVA/hqdefault.jpg",
                    durationSeconds = 480,
                    durationFormatted = "08:00",
                    publishDate = "22 सप्टेंबर 2026",
                    description = "प्राजक्ताच्या आयुष्यातील महाट्विस्ट — प्रेक्षकांच्या प्रतिक्रिया आणि पुढील अंदाज.",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = false,
                    createdAt = System.currentTimeMillis() - (86400000L * 7) - 3600000L
                ),
                EpisodeEntity(
                    episodeId = "ep_l7j8",
                    serialName = "पाठराखीण",
                    episodeNumber = 107,
                    title = "Pathrakhin Today Episode 29 September 2026 | पाठराखीण आजचा भाग 2 | प्राजक्ताचा निर्धार",
                    youtubeVideoId = "L7J84Xjaw54",
                    youtubeUrl = "https://www.youtube.com/watch?v=L7J84Xjaw54",
                    thumbnailUrl = "https://i.ytimg.com/vi/L7J84Xjaw54/hqdefault.jpg",
                    durationSeconds = 520,
                    durationFormatted = "08:40",
                    publishDate = "29 सप्टेंबर 2026",
                    description = "प्राजक्ताचा सूर्यवंशी वाड्यातील नवा निर्धार आणि आलोकला दिलेली खंबीर साथ.",
                    status = EpisodeStatus.PUBLISHED,
                    isTodayEpisode = false,
                    createdAt = System.currentTimeMillis() - 7200000L
                )
            )
            db.episodeDao().insertEpisodes(episodes)

            // 5. 32 Sample Questions for the Question Bank
            val questions = listOf(
                QuestionEntity("q_105_1", "ep_105", "आजच्या एपिसोडमध्ये अनुराधाने कोणता सर्वात महत्त्वाचा निर्णय घेतला?", "घर सोडून जाण्याचा", "सत्याचा शोध स्वतः घेण्याचा", "वाड्याची चावी परत करण्याचा", "पोलिसांत तक्रार नोंदवण्याचा", 1, "अनुराधाने कोणाच्याही दबावाला बळी न पडता सत्याचा शोध स्वतः घेण्याचा ठाम निर्धार केला.", "Medium"),
                QuestionEntity("q_105_2", "ep_105", "वाड्यावरील जुनी पेटी कोणाच्या खोलीत लपवून ठेवली होती?", "दिगंबरराव यांच्या जुन्या अभ्यासिकेत", "स्वयंपाकघरातील तळघरात", "अतिथी कक्षातील कपाटात", "बागेतील जुन्या खोलीत", 0, "दिगंबररावांच्या जुन्या अभ्यासिकेतील गुप्त कपाटात ती लाकडी पेटी सापडली.", "Hard"),
                QuestionEntity("q_105_3", "ep_105", "मालिकेच्या आजच्या ट्विस्टमध्ये संशयितांच्या यादीत कोणाचे नाव नव्याने आले?", "राघव", "शारदा काकू", "वकील साळवी", "इन्स्पेक्टर शिंदे", 2, "वकील साळवींची संशयास्पद वागणूक आणि फोन कॉल्समुळे त्यांच्यावर संशय बळावला.", "Medium"),
                QuestionEntity("q_105_4", "ep_105", "प्रीती ताईंच्या आजच्या व्हिडिओ विश्लेषणानुसार पुढील भागात काय घडू शकते?", "मालिका संपणार आहे", "अनुराधाला संपत्तीचा कायदेशीर वारसदार घोषित केले जाईल", "नव्या पात्राची रहस्यमय एंट्री होणार", "सर्व वाद सामोपचाराने मिटणार", 2, "प्रीती ताईंनी प्रोमोच्या आधारे स्पष्ट केले की वाड्यावर एका नव्या रहस्यमय पात्राची एंट्री होणार आहे.", "Easy"),
                QuestionEntity("q_105_5", "ep_105", "अनुराधाच्या पाठीशी ठामपणे कोण उभे राहिले?", "तिची लहान बहीण मृणाल", "गावचे सरपंच", "वृद्ध रखवालदार तात्या", "शेजारचे काका", 2, "तात्यांनी पूर्वीच्या घटनांची खरी माहिती देत अनुराधाला धीर दिला.", "Medium"),
                QuestionEntity("q_105_6", "ep_105", "मालिकेतील 'पाठराखीण' या शीर्षकाचा खरा अर्थ आजच्या दृश्यात कसा स्पष्ट झाला?", "कुटुंबाचे रक्षण करणारी अदृश्य ताकद", "फक्त एक जुनी लोककथा", "वाड्याचे नाव", "एक पारंपरिक अलंकार", 0, "वाड्यावर आणि कुटुंबावर संकट आल्यास रक्षण करणारी पाठराखीण ही संकल्पना स्पष्ट झाली.", "Easy"),
                QuestionEntity("q_105_7", "ep_105", "आजच्या भागात कोणते गूढ पत्र वाचताना पार्श्वसंगीत तीव्र झाले?", "1995 चे मृत्युपत्र", "आईने लिहिलेले शेवटचे पत्र", "धमकीचे निनावी पत्र", "सरकारी नोटीस", 1, "आईने लिहिलेल्या शेवटच्या पत्रातील ओळी वाचताना सत्य उघड झाले.", "Hard"),
                QuestionEntity("q_105_8", "ep_105", "अनुराधाने मंदिरात जाऊन कोणता नवस केला?", "सत्य बाहेर येईपर्यंत अनवाणी चालण्याचा", "सोन्याचा हार अर्पण करण्याचा", "उपवास करण्याचा", "तीर्थयात्रेला जाण्याचा", 0, "सत्य निष्पन्न होईपर्यंत तिने अनवाणी चालण्याचा संकल्प केला.", "Medium"),
                QuestionEntity("q_105_9", "ep_105", "आजच्या एपिसोडची एकूण वेळ किती मिनिटांची होती?", "05:00", "08:35", "12:15", "15:00", 1, "प्रीती मराठी अपडेट्सच्या विश्लेषणाचा आजचा भाग 8 मिनिटे 35 सेकंदांचा होता.", "Easy"),
                QuestionEntity("q_105_10", "ep_105", "दिगंबररावांच्या हातातील अंगठीवर कोणते चिन्ह कोरलेले होते?", "सूर्याचे", "सिंहाचे", "कमळाचे", "रुद्राक्षाचे", 1, "खानदानी अंगठीवर सिंहाची मुद्रा कोरलेली आहे जी महत्त्वाची खूण ठरली.", "Hard"),
                QuestionEntity("q_105_11", "ep_105", "राघवने अनुराधाला सावध करण्यासाठी काय सल्ला दिला?", "घरातल्या प्रत्येकावर आंधळा विश्वास ठेवू नकोस", "गाव सोडून मुंबईला निघून जा", "सगळी संपत्ती विकून टाक", "कोणाशीही बोलू नकोस", 0, "राघवने घरातील जवळच्या व्यक्तींवरही आंधळा विश्वास न ठेवण्याचा सल्ला दिला.", "Medium"),
                QuestionEntity("q_105_12", "ep_105", "आजच्या भागात कोणत्या जुन्या आठवणींचा फ्लॅशबॅक दाखवला गेला?", "दहा वर्षांपूर्वीच्या दसऱ्याच्या रात्रीचा", "शाळेतील गॅदरिंगचा", "लग्न समारंभाचा", "परदेश प्रवासाचा", 0, "दहा वर्षांपूर्वी दसऱ्याच्या रात्री वाड्यात घडलेल्या गूढ घटनेचा फ्लॅशबॅक आला.", "Medium"),
                QuestionEntity("q_105_13", "ep_105", "प्रीती मराठी अपडेट्स चॅनेलच्या विश्लेषणाचा मुख्य उद्देश काय आहे?", "फेक अफवा पसरवणे", "सखोल आणि वेगवान मालिका विश्लेषण प्रेक्षकांपर्यंत पोहोचवणे", "फक्त जाहिराती दाखवणे", "मालिकेवर बंदी घालणे", 1, "मराठी मालिकांचे वेगवान, सखोल आणि ताज्या घडामोडींचे केंद्र हे चॅनेलचे ध्येय आहे.", "Easy"),
                QuestionEntity("q_105_14", "ep_105", "वाड्याच्या तळघराची गुप्त किल्ली कोठे लपवलेली होती?", "काकूंच्या पूजेच्या ताटात", "जुन्या विंटेज घड्याळाच्या मागे", "तुळशी वृंदावनाजवळ", "विहिरीच्या दगडात", 1, "दिवाणखान्यातील विंटेज घड्याळाच्या पाठीमागे ती पितळी किल्ली लपवलेली होती.", "Hard"),
                QuestionEntity("q_105_15", "ep_105", "उद्याच्या महाएपिसोडमध्ये कोणता मोठा खुलासा अपेक्षित आहे?", "सत्य कोण आणि कपटी कोण हे सर्वांसमोर येणार", "वाडा पाडला जाणार", "सर्वजण सहलीला जाणार", "लग्न रद्द होणार", 0, "प्रोमो आणि विश्लेषणावरून स्पष्ट होते की खऱ्या कपटी व्यक्तीचा बुरखा फाटणार आहे.", "Medium"),
                QuestionEntity("q_105_16", "ep_105", "अनुराधाच्या हातातील जुन्या छायाचित्रात कोण दिसत होते?", "तिची आई आणि अनोळखी महिला", "फक्त दिगंबरराव", "गावचे ठाकूर", "शाळेचे शिक्षक", 0, "छायाचित्रात तिची आई आणि एक गूढ अनोळखी महिला सोबत उभी होती.", "Medium"),
                QuestionEntity("q_105_17", "ep_105", "दिगंबररावांच्या वाड्यात कोणत्या सणाला मोठा उत्सव साजरा केला जायचा?", "दिवाळी पाडवा", "गुढीपाडवा आणि दसरा", "होळी", "संक्रांत", 1, "वाड्यात पूर्वापार गुढीपाडवा आणि दसऱ्याला मोठी पूजा होत असे.", "Easy"),
                QuestionEntity("q_105_18", "ep_105", "शारदा काकूंच्या चेहऱ्यावरील भीतीचे मुख्य कारण काय होते?", "अनुराधाला जुनी डायरी सापडल्याचे समजल्याने", "पाहुणे आल्याने", "घरात चोरी झाल्याने", "पाऊस पडल्याने", 0, "अनुराधाला ती जुनी डायरी मिळाल्याने काकू अत्यंत अस्वस्थ झाल्या.", "Medium"),
                QuestionEntity("q_105_19", "ep_105", "मालिकेचे शीर्षकगीत कोणत्या वाद्याच्या स्वरांनी सुरू होते?", "बासरी आणि तुतारी", "सतार", "पियानो", "ड्रम्स", 0, "मालिकेचे पारंपरिक मराठमोळे संगीत बासरी आणि तुतारीने सुरू होते.", "Easy"),
                QuestionEntity("q_105_20", "ep_105", "राघवने कोर्टात सादर केलेले पुरावे कोणाकडून गोळा केले होते?", "सायबर सेल आणि बँक मॅनेजर", "शेजारच्या दुकानातून", "सोशल मीडियावरून", "वृत्तपत्रातून", 0, "अधिकृत बँक व्यवहार आणि सायबर नोंदी कोर्टात सादर करण्यात आल्या.", "Hard"),
                QuestionEntity("q_105_21", "ep_105", "आजच्या भागात अनुराधाने नेसलेल्या साडीचा रंग कोणता होता?", "गडद जांभळा आणि सोनेरी जरी", "पांढरा", "हिरवा", "काळा", 0, "अनुराधाने पारंपारिक जांभळ्या रंगाची नऊवारी साडी नेसली होती.", "Easy"),
                QuestionEntity("q_105_22", "ep_105", "इन्स्पेक्टर शिंदेंनी कोणाला 24 तासांची मुदत दिली?", "संशयित ड्रायव्हरला", "राघवला", "सरपंचांना", "वाचमनला", 0, "गाडी चालवणाऱ्या ड्रायव्हरला सत्य सांगण्यासाठी 24 तास मिळाले.", "Medium"),
                QuestionEntity("q_105_23", "ep_105", "प्रीती मराठी अपडेट्सच्या मते या आठवड्यातील सर्वोत्कृष्ट अभिनय कोणाचा राहिला?", "अनुराधाची भूमिका करणाऱ्या अभिनेत्रीचा", "खलनायकाचा", "रखवालदाराचा", "पोलिसांचा", 0, "भावुक आणि कणखर दोन्ही बाजू लीलया साकारल्याबद्दल अनुराधाचे कौतुक झाले.", "Easy"),
                QuestionEntity("q_105_24", "ep_105", "वाड्याच्या जुन्या दारात कोरलेली वेलबुट्टी कोणत्या लाकडाची आहे?", "सागवान", "बांबू", "कडुनिंब", "नारळ", 0, "अतिशय पुरातन सागवानी लाकडाचे मजबूत नक्षीदार दार वाड्याला आहे.", "Medium"),
                QuestionEntity("q_105_25", "ep_105", "अनुराधाच्या गळ्यातील मंगळसूत्राची डिझाईन कोणती आहे?", "पारंपरिक दोन वाट्या आणि काळे मणी", "डायमंड पेंडंट", "फुलांची माळ", "साखळी", 0, "महाराष्ट्रीयन परंपरेनुसार दोन सोन्याच्या वाट्या असलेले मंगळसूत्र.", "Easy"),
                QuestionEntity("q_105_26", "ep_105", "वकील साळवींच्या ऑफिसमधील फाईलचा कोड नंबर काय होता?", "FILE-95-B", "FILE-01", "CASE-99", "DOC-007", 0, "वसीयतीशी संबंधित संचिकेवर FILE-95-B असा सांकेतिक क्रमांक होता.", "Hard"),
                QuestionEntity("q_105_27", "ep_105", "आजच्या भागात कोणता भावनिक संवाद प्रेक्षकांच्या पसंतीस उतरला?", "'सत्याचा मार्ग कठीण असेल पण शेवट विजयाचाच होतो'", "'मी कोणाला सोडणार नाही'", "'माझे पैसे मला हवेत'", "'मी काहीच बोलणार नाही'", 0, "अनुराधाचा हा संवाद प्रेक्षकांना अत्यंत भावला.", "Easy"),
                QuestionEntity("q_105_28", "ep_105", "वाड्याच्या बागेत असलेल्या जुन्या विहिरीचे नाव काय होते?", "अमृत विहीर", "देवबाव", "गंगा विहीर", "मोठी विहीर", 1, "गावकरी त्या जुन्या पाषाणी विहिरीला 'देवबाव' म्हणून ओळखत.", "Medium"),
                QuestionEntity("q_105_29", "ep_105", "आजच्या भागात दाखवलेला चहाचा कप कोणत्या धातूचा होता?", "पितळी कप आणि बशी", "काचेचा कप", "प्लॅस्टिक कप", "थर्माकोल", 0, "दिगंबररावांच्या काळात वापरला जाणारा पारंपरिक पितळी कप दाखवला गेला.", "Medium"),
                QuestionEntity("q_105_30", "ep_105", "मालिकेच्या आजच्या शेवटास कोणता ध्वनी वाजून सस्पेन्स निर्माण झाला?", "जुना मोठा घंटा आणि वारा", "गाडीचा हॉर्न", "फोनची रिंग", "कुत्र्याचे भुंकणे", 0, "वाड्यावरील जुनी घंटा वाऱ्याने वाजून गूढ सस्पेन्स निर्माण झाला.", "Easy"),
                QuestionEntity("q_105_31", "ep_105", "अनुराधाला मदत करणाऱ्या लहान मुलीचे नाव काय आहे?", "चिऊ", "मृण्मयी", "सायली", "पिंकी", 0, "चिऊने गुप्त चावी अनुराधाच्या हातात आणून दिली.", "Medium"),
                QuestionEntity("q_105_32", "ep_105", "उद्याच्या एपिसोडचा महाट्विस्ट पाहण्यासाठी काय करणे आवश्यक आहे?", "प्रीती मराठी अपडेट्स चॅनेल सबस्क्राईब करून बेल आयकॉन दाबणे", "काहीच नाही", "टीव्ही बंद ठेवणे", "दुसरे चॅनेल पाहणे", 0, "नियमित ताज्या अपडेट्स मिळवण्यासाठी चॅनेलशी जोडलेले राहणे आवश्यक आहे.", "Easy")
            )
            db.questionDao().insertQuestions(questions)

            // 6. Announcements
            val announcements = listOf(
                AnnouncementEntity(
                    id = "ann_1",
                    title = "🎬 आजचा नवीन Episode 105 लाइव्ह झाला आहे!",
                    message = "पाठराखीण मालिकेचे आजचे सर्वात धमाकेदार विश्लेषण चॅनेलवर आले आहे. व्हिडिओ पाहा आणि आजच्या 15 प्रश्नांच्या Quiz चे बक्षीस जिंका!",
                    date = "27 सप्टें 2026",
                    active = true
                ),
                AnnouncementEntity(
                    id = "ann_2",
                    title = "🔥 7 दिवसांच्या Daily Streak वर विशेष बोनस!",
                    message = "दररोज व्हिडिओ पाहून क्विझ पूर्ण करणाऱ्या प्रेक्षकांना ₹10 चा अतिरिक्त स्ट्रीक बोनस वॉलेटमध्ये दिला जात आहे.",
                    date = "25 सप्टें 2026",
                    active = true
                )
            )
            announcements.forEach { db.announcementDao().insertAnnouncement(it) }

            // 7. Audit Log
            val initialLog = AdminAuditLogEntity(
                logId = "log_init",
                action = "SYSTEM_INITIALIZATION",
                performedBy = "Admin (Priti Marathi Updates)",
                targetType = "DATABASE",
                details = "प्रणाली यशस्वीरीत्या सुरू झाली. 32 प्रश्न आणि भाग 105 सह टेस्ट डेटा लोड झाला.",
                timestamp = System.currentTimeMillis()
            )
            db.adminAuditLogDao().insertLog(initialLog)
        }
    }
}
