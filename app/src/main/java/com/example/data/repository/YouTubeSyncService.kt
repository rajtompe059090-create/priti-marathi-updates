package com.example.data.repository

import android.util.Log
import android.util.Xml
import com.example.data.db.PritiDatabase
import com.example.data.model.EpisodeEntity
import com.example.data.model.EpisodeStatus
import com.example.data.model.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object YouTubeConstants {
    const val CHANNEL_ID = "UCZgV5iGbc4NJxfjz7-OuCGg"
    const val UPLOADS_PLAYLIST_ID = "UUZgV5iGbc4NJxfjz7-OuCGg"
    const val CHANNEL_NAME = "Priti Marathi Updates"
    const val CHANNEL_URL = "https://youtube.com/@pritimarathiupdates"
    const val RSS_FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=$CHANNEL_ID"
}

class YouTubeSyncService(
    private val db: PritiDatabase
) {
    private val TAG = "YouTubeSyncService"

    /**
     * Fetches latest uploaded videos from the official YouTube channel feed,
     * parses metadata, formats published date in Marathi, and stores into Room DB.
     */
    suspend fun syncLatestUploads(apiKey: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync for channel: ${YouTubeConstants.CHANNEL_ID}")
            val episodes = fetchEpisodesFromRss(YouTubeConstants.RSS_FEED_URL)
            if (episodes.isEmpty()) {
                Log.w(TAG, "No episodes parsed from RSS feed.")
                return@withContext Result.failure(Exception("व्हिडिओ अपडेट्स लोड करता आले नाहीत. कृपया इंटरनेट तपासा."))
            }

            // Clear previous today episode flag
            db.episodeDao().clearTodayEpisode()

            // Mark the newest episode as today's episode
            val sorted = episodes.sortedByDescending { it.createdAt }
            val marked = sorted.mapIndexed { index, ep ->
                if (index == 0) ep.copy(isTodayEpisode = true) else ep.copy(isTodayEpisode = false)
            }

            db.episodeDao().insertEpisodes(marked)

            // Ensure questions exist for each episode so assessment can always be attempted
            val existingQuestions = db.questionDao().getAllQuestions().first().filter { it.active }
            for (ep in marked) {
                val epQuestions = db.questionDao().getQuestionsListForEpisode(ep.episodeId)
                if (epQuestions.isEmpty() && existingQuestions.isNotEmpty()) {
                    val cloned = existingQuestions.take(15).mapIndexed { idx, q ->
                        QuestionEntity(
                            questionId = "q_${ep.episodeId}_${idx + 1}",
                            episodeId = ep.episodeId,
                            questionText = q.questionText,
                            optionA = q.optionA,
                            optionB = q.optionB,
                            optionC = q.optionC,
                            optionD = q.optionD,
                            correctAnswer = q.correctAnswer,
                            explanation = q.explanation,
                            difficulty = q.difficulty,
                            active = true
                        )
                    }
                    db.questionDao().insertQuestions(cloned)
                }
            }

            Log.d(TAG, "Successfully synced ${marked.size} episodes from YouTube channel.")
            Result.success(marked.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing YouTube uploads: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fetchEpisodesFromRss(feedUrl: String): List<EpisodeEntity> {
        val url = URL(feedUrl)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) PritiMarathiUpdates/1.0")
        }

        connection.inputStream.use { stream ->
            return parseAtomFeed(stream)
        }
    }

    private fun parseAtomFeed(inputStream: InputStream): List<EpisodeEntity> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(inputStream, "UTF-8")

        val episodes = mutableListOf<EpisodeEntity>()
        var eventType = parser.eventType

        var currentVideoId: String? = null
        var currentTitle: String? = null
        var currentPublished: String? = null
        var currentThumbnail: String? = null
        var currentDescription: String? = null

        var insideEntry = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("entry", ignoreCase = true)) {
                        insideEntry = true
                        currentVideoId = null
                        currentTitle = null
                        currentPublished = null
                        currentThumbnail = null
                        currentDescription = null
                    } else if (insideEntry) {
                        when {
                            tagName.equals("videoId", ignoreCase = true) -> {
                                currentVideoId = parser.nextText()
                            }
                            tagName.equals("title", ignoreCase = true) && currentTitle == null -> {
                                currentTitle = parser.nextText()
                            }
                            tagName.equals("published", ignoreCase = true) -> {
                                currentPublished = parser.nextText()
                            }
                            tagName.equals("thumbnail", ignoreCase = true) -> {
                                val urlAttr = parser.getAttributeValue(null, "url")
                                if (!urlAttr.isNullOrEmpty()) {
                                    currentThumbnail = urlAttr
                                }
                            }
                            tagName.equals("description", ignoreCase = true) -> {
                                currentDescription = parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("entry", ignoreCase = true) && insideEntry) {
                        insideEntry = false
                        if (!currentVideoId.isNullOrEmpty() && !currentTitle.isNullOrEmpty()) {
                            val vid = currentVideoId
                            val title = currentTitle.trim()
                            val thumb = currentThumbnail ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                            val marathiDate = formatPublishedDateToMarathi(currentPublished)
                            val serial = detectSerialName(title)
                            val epNum = extractEpisodeNumber(title)
                            val createdAtMillis = parseIso8601ToMillis(currentPublished)

                            episodes.add(
                                EpisodeEntity(
                                    episodeId = "yt_$vid",
                                    serialName = serial,
                                    episodeNumber = epNum,
                                    title = title,
                                    youtubeVideoId = vid,
                                    youtubeUrl = "https://www.youtube.com/watch?v=$vid",
                                    thumbnailUrl = thumb,
                                    durationSeconds = 600, // standard review length
                                    durationFormatted = "10:00",
                                    publishDate = marathiDate,
                                    description = currentDescription?.trim() ?: "प्रीती मराठी अपडेट्स — अधिकृत विश्लेषण आणि ताज्या घडामोडी.",
                                    status = EpisodeStatus.PUBLISHED,
                                    isTodayEpisode = false,
                                    createdAt = createdAtMillis
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return episodes
    }

    private fun detectSerialName(title: String): String {
        return when {
            title.contains("पाठराखीण", ignoreCase = true) || title.contains("Pathrakhin", ignoreCase = true) -> "पाठराखीण"
            title.contains("ठरलं तर मग", ignoreCase = true) || title.contains("Tharla", ignoreCase = true) -> "ठरलं तर मग"
            title.contains("सुख म्हणजे", ignoreCase = true) || title.contains("Sukh", ignoreCase = true) -> "सुख म्हणजे नक्की काय असतं"
            else -> "पाठराखीण"
        }
    }

    private fun extractEpisodeNumber(title: String): Int {
        val pattern = Pattern.compile("(?i)(?:episode|भाग|ep)\\s*(\\d+)")
        val matcher = pattern.matcher(title)
        if (matcher.find()) {
            val numStr = matcher.group(1)
            return numStr?.toIntOrNull() ?: 105
        }
        // Extract day as fallback secondary metadata
        val dayPattern = Pattern.compile("(\\d{1,2})\\s*(?:सप्टेंबर|सप्टें|sep|september|ऑक्टोबर|ऑक्टो|oct)", Pattern.CASE_INSENSITIVE)
        val dayMatcher = dayPattern.matcher(title)
        if (dayMatcher.find()) {
            return dayMatcher.group(1)?.toIntOrNull() ?: 100
        }
        return 100
    }

    private fun formatPublishedDateToMarathi(isoDate: String?): String {
        if (isoDate.isNullOrEmpty()) return "आजचा भाग"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoDate.substringBefore('+').substringBefore('Z')) ?: Date()

            val cal = Calendar.getInstance()
            cal.time = date

            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) // 0-based
            val year = cal.get(Calendar.YEAR)

            val marathiMonths = listOf(
                "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल", "मे", "जून",
                "जुलै", "ऑगस्ट", "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर"
            )
            val monthName = marathiMonths.getOrElse(month) { "सप्टेंबर" }
            "$day $monthName $year"
        } catch (e: Exception) {
            "आजचा भाग"
        }
    }

    private fun parseIso8601ToMillis(isoDate: String?): Long {
        if (isoDate.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoDate.substringBefore('+').substringBefore('Z'))
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
