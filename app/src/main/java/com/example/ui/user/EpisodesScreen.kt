package com.example.ui.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
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
import com.example.data.model.EpisodeEntity
import com.example.data.model.UserEpisodeProgressEntity
import com.example.data.model.UserProgressStatus
import com.example.ui.components.NeonCard
import com.example.ui.components.PillBadge
import com.example.ui.theme.*

@Composable
fun EpisodesScreen(
    episodes: List<EpisodeEntity>,
    userProgressList: List<UserEpisodeProgressEntity>,
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSelectEpisode: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSerial by remember { mutableStateOf("सर्व") }

    val serials = listOf("सर्व", "पाठराखीण", "ठरलं तर मग", "सुख म्हणजे नक्की काय असतं")

    val filteredEpisodes = episodes.filter { ep ->
        val matchesSearch = ep.title.contains(searchQuery, ignoreCase = true) ||
                ep.serialName.contains(searchQuery, ignoreCase = true) ||
                ep.episodeNumber.toString().contains(searchQuery)
        val matchesSerial = selectedSerial == "सर्व" || ep.serialName.contains(selectedSerial, ignoreCase = true)
        matchesSearch && matchesSerial
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "सर्व Episodes",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "YouTube चॅनेलवरील ताज्या भागांचे सखोल विश्लेषण",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("refresh_episodes_btn")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = NeonRoseBright,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "नवीन व्हिडिओ रिफ्रेश करा",
                            tint = NeonRoseBright
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Episode शोधा (उदा. पाठराखीण, 105)...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = NeonRoseBright
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "हटवा",
                                tint = TextMuted
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonRose,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        // Serial Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(serials) { serial ->
                    val isSelected = selectedSerial == serial
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSerial = serial },
                        label = {
                            Text(
                                text = serial,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceDark,
                            labelColor = TextMuted,
                            selectedContainerColor = NeonRose,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) NeonRose else BorderSubtle,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // Episode List Items
        if (filteredEpisodes.isEmpty()) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDark
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔍", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "कोणताही Episode सापडला नाही",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "कृपया वेगळा शब्द किंवा मालिका निवडून पुन्हा शोधा.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredEpisodes, key = { it.episodeId }) { episode ->
                val progress = userProgressList.find { it.episodeId == episode.episodeId }
                val isWatched = progress?.status == UserProgressStatus.COMPLETED ||
                        progress?.status == UserProgressStatus.QUIZ_UNLOCKED ||
                        progress?.status == UserProgressStatus.QUIZ_COMPLETED
                val isQuizDone = progress?.status == UserProgressStatus.QUIZ_COMPLETED

                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (episode.isTodayEpisode) NeonRose.copy(alpha = 0.6f) else BorderSubtle,
                    backgroundColor = SurfaceDark,
                    onClick = { onSelectEpisode(episode) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thumbnail Container
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(85.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceVariantDark)
                        ) {
                            val thumb = episode.thumbnailUrl.ifEmpty {
                                "https://img.youtube.com/vi/${episode.youtubeVideoId}/hqdefault.jpg"
                            }
                            AsyncImage(
                                model = thumb,
                                contentDescription = episode.title,
                                placeholder = painterResource(id = R.drawable.img_hero_banner),
                                error = painterResource(id = R.drawable.img_hero_banner),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Duration Pill at bottom right
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xCC000000)
                            ) {
                                Text(
                                    text = episode.durationFormatted,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Episode Info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${episode.serialName} — Ep ${episode.episodeNumber}",
                                    color = NeonAmberBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (episode.isTodayEpisode) {
                                    PillBadge(
                                        text = "आजचा भाग",
                                        backgroundColor = NeonRoseGlow,
                                        textColor = NeonRoseBright
                                    )
                                } else if (isWatched) {
                                    PillBadge(
                                        text = if (isQuizDone) "क्विझ पूर्ण" else "पाहिला",
                                        backgroundColor = EmeraldGreenGlow,
                                        textColor = EmeraldGreen,
                                        borderColor = EmeraldGreen.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = episode.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = episode.publishDate,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )

                                Text(
                                    text = "पाहा >",
                                    color = NeonRoseBright,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class EpisodesPreviewData(
    val episodes: List<EpisodeEntity>,
    val userProgressList: List<UserEpisodeProgressEntity>
)

class EpisodesPreviewParameterProvider : PreviewParameterProvider<EpisodesPreviewData> {
    override val values: Sequence<EpisodesPreviewData> = sequenceOf(
        EpisodesPreviewData(
            episodes = listOf(
                EpisodeEntity(
                    episodeId = "ep1",
                    serialName = "पाठराखीण",
                    episodeNumber = 106,
                    title = "Pathrakhin Today Episode 29 September 2026",
                    youtubeVideoId = "-GS0owKJqlQ",
                    youtubeUrl = "https://www.youtube.com/watch?v=-GS0owKJqlQ",
                    thumbnailUrl = "https://i.ytimg.com/vi/-GS0owKJqlQ/hqdefault.jpg",
                    durationSeconds = 540,
                    durationFormatted = "09:00",
                    publishDate = "29 सप्टेंबर 2026",
                    description = "पाठराखीण मालिकेचा नवीन भाग."
                ),
                EpisodeEntity(
                    episodeId = "ep2",
                    serialName = "पाठराखीण",
                    episodeNumber = 105,
                    title = "Pathrakhin Today's Episode 27 September 2026",
                    youtubeVideoId = "KcysA63NQG0",
                    youtubeUrl = "https://www.youtube.com/watch?v=KcysA63NQG0",
                    thumbnailUrl = "https://i.ytimg.com/vi/KcysA63NQG0/hqdefault.jpg",
                    durationSeconds = 515,
                    durationFormatted = "08:35",
                    publishDate = "27 सप्टेंबर 2026",
                    description = "पाठराखीण मालिकेचा उत्कंठावर्धक भाग."
                )
            ),
            userProgressList = listOf(
                UserEpisodeProgressEntity(
                    progressId = "prog1",
                    userId = "u1",
                    episodeId = "ep1",
                    status = UserProgressStatus.COMPLETED,
                    watchedSeconds = 600,
                    completedAt = System.currentTimeMillis()
                )
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun EpisodesScreenPreview(
    @PreviewParameter(EpisodesPreviewParameterProvider::class) data: EpisodesPreviewData
) {
    MyApplicationTheme {
        EpisodesScreen(
            episodes = data.episodes,
            userProgressList = data.userProgressList,
            onSelectEpisode = {}
        )
    }
}

