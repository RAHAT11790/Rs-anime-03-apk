package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimeType
import com.example.data.model.DownloadedEpisode
import com.example.data.model.Episode
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.AnimeImage
import com.example.ui.theme.*
import com.example.util.DownloadHelper

@Composable
fun AnimeDetailsScreen(
    animeId: String,
    animeRepo: AnimeRepository,
    userPrefsRepo: UserPreferencesRepository,
    onBackClick: () -> Unit,
    onPlayEpisode: (animeId: String, seasonName: String, episodeNumber: Int) -> Unit,
    onPlayMovie: (animeId: String) -> Unit,
    onOpenVipScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animeList by animeRepo.animeList.collectAsState()
    val anime = remember(animeId, animeList) { animeRepo.getAnimeById(animeId) }
    val watchlist by userPrefsRepo.watchlist.collectAsState()
    val unlockedKeys by userPrefsRepo.unlockedKeys.collectAsState()
    val isVip by userPrefsRepo.isVip.collectAsState()
    val coins by userPrefsRepo.coins.collectAsState()

    var selectedSeasonIndex by remember { mutableIntStateOf(0) }
    var unlockTargetEpisode by remember { mutableStateOf<Episode?>(null) }
    var showUnlockDialog by remember { mutableStateOf(false) }

    if (anime == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AnimeDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Anime not found", color = AnimeTextPrimary)
        }
        return
    }

    val isBookmarked = watchlist.contains(anime.id)
    val seasons = anime.seasons ?: emptyList()
    val currentSeason = seasons.getOrNull(selectedSeasonIndex)

    // Unlock Dialog
    if (showUnlockDialog && unlockTargetEpisode != null) {
        val ep = unlockTargetEpisode!!
        val epKey = "${anime.id}_${currentSeason?.name ?: "S1"}_${ep.episodeNumber}"
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = {
                Text(
                    text = "🔒 Unlock Episode",
                    fontWeight = FontWeight.Bold,
                    color = AnimeTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "${anime.title} - ${ep.title}",
                        color = AnimeCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlocking this premium episode costs 10 reward coins. Your current balance: $coins coins.",
                        color = AnimeTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userPrefsRepo.unlockItem(epKey, 10)) {
                            Toast.makeText(context, "Episode unlocked successfully!", Toast.LENGTH_SHORT).show()
                            showUnlockDialog = false
                            onPlayEpisode(anime.id, currentSeason?.name ?: "Season 1", ep.episodeNumber)
                        } else {
                            Toast.makeText(context, "Insufficient coins! Complete daily tasks to earn coins.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed)
                ) {
                    Text("Unlock (10 Coins)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnlockDialog = false
                    onOpenVipScreen()
                }) {
                    Text("Get VIP Pass", color = AnimeGold)
                }
            },
            containerColor = AnimeDarkSurfaceCard
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AnimeDarkBackground)
            .testTag("anime_details_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Hero Backdrop Header with Back, Bookmark, Share icons
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AnimeImage(
                    model = anime.backdrop,
                    contentDescription = anime.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    AnimeDarkBackground.copy(alpha = 0.8f),
                                    AnimeDarkBackground
                                )
                            )
                        )
                )

                // Top Navigation Icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("details_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { userPrefsRepo.toggleWatchlist(anime.id) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .testTag("details_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Watchlist",
                                tint = if (isBookmarked) AnimeNeonRed else Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Watch ${anime.title} on RS Anime App!")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Anime"))
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Play Floating Button in Center of Backdrop
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(AnimeNeonRed)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable {
                            if (anime.type == AnimeType.MOVIE) {
                                onPlayMovie(anime.id)
                            } else {
                                onPlayEpisode(anime.id, currentSeason?.name ?: "Season 1", 1)
                            }
                        }
                        .testTag("details_play_hero_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 2. Title & Metadata
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = anime.title,
                            color = AnimeTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = AnimeDarkSurfaceCard,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = AnimeGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = anime.rating,
                                        color = AnimeTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = anime.year,
                                color = AnimeTextSecondary,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "•",
                                color = AnimeTextMuted,
                                fontSize = 12.sp
                            )

                            Text(
                                text = anime.language,
                                color = AnimeCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row: Play Now, Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (anime.type == AnimeType.MOVIE) {
                                onPlayMovie(anime.id)
                            } else {
                                onPlayEpisode(anime.id, currentSeason?.name ?: "Season 1", 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("details_watch_now_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (anime.type == AnimeType.MOVIE) "Play Movie" else "Watch Episode 1",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val ep = currentSeason?.episodes?.firstOrNull()
                            val dlUrl = if (anime.type == AnimeType.MOVIE) {
                                anime.downloadLink ?: anime.movieLink1080 ?: anime.movieLink ?: ""
                            } else {
                                ep?.downloadLink ?: ep?.link1080 ?: ep?.link ?: ""
                            }
                            val id = DownloadHelper.startDownload(
                                context = context,
                                url = dlUrl,
                                anime = anime,
                                episode = ep,
                                seasonNumber = currentSeason?.seasonNumber ?: 1,
                                quality = "1080p"
                            )
                            if (id > 0) {
                                userPrefsRepo.addDownload(
                                    DownloadedEpisode(
                                        animeId = anime.id,
                                        animeTitle = anime.title,
                                        episodeNumber = ep?.episodeNumber ?: 1,
                                        episodeTitle = ep?.title ?: "Episode 1",
                                        poster = anime.poster,
                                        quality = "1080p FHD"
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AnimeTextPrimary),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Genres chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(anime.genres) { genre ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AnimeDarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder)
                        ) {
                            Text(
                                text = genre,
                                color = AnimeTextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Storyline
                Text(
                    text = "Storyline",
                    color = AnimeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = anime.storyline,
                    color = AnimeTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // 3. Episodes Section (for Series)
        if (anime.type == AnimeType.SERIES && seasons.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes",
                            color = AnimeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Season Selector Tabs
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(seasons.size) { index ->
                                val isSeasonSelected = selectedSeasonIndex == index
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSeasonSelected) AnimeNeonRed else AnimeDarkSurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSeasonSelected) AnimeNeonRed else AnimeBorder
                                    ),
                                    modifier = Modifier.clickable { selectedSeasonIndex = index }
                                ) {
                                    Text(
                                        text = seasons[index].name,
                                        color = if (isSeasonSelected) Color.White else AnimeTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (currentSeason != null) {
                items(currentSeason.episodes, key = { it.episodeNumber }) { ep ->
                    val epKey = "${anime.id}_${currentSeason.name}_${ep.episodeNumber}"
                    val isUnlocked = isVip || !ep.isPremium || unlockedKeys.contains(epKey)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clickable {
                                if (isUnlocked) {
                                    onPlayEpisode(anime.id, currentSeason.name, ep.episodeNumber)
                                } else {
                                    unlockTargetEpisode = ep
                                    showUnlockDialog = true
                                }
                            }
                            .testTag("episode_item_${ep.episodeNumber}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Episode index badge
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isUnlocked) AnimeNeonRed.copy(alpha = 0.2f) else AnimeDarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = AnimeGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${ep.episodeNumber}",
                                        color = AnimeNeonRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ep.title,
                                    color = AnimeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "1080p FHD",
                                        color = AnimeCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (ep.isPremium && !isUnlocked) {
                                        Text(
                                            text = "• VIP / 10 Coins",
                                            color = AnimeGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Download Episode Button
                            IconButton(
                                onClick = {
                                    val dlUrl = ep.downloadLink ?: ep.link1080 ?: ep.link
                                    val id = DownloadHelper.startDownload(
                                        context = context,
                                        url = dlUrl,
                                        anime = anime,
                                        episode = ep,
                                        seasonNumber = currentSeason.seasonNumber,
                                        quality = "1080p"
                                    )
                                    if (id > 0) {
                                        userPrefsRepo.addDownload(
                                            DownloadedEpisode(
                                                animeId = anime.id,
                                                animeTitle = anime.title,
                                                episodeNumber = ep.episodeNumber,
                                                episodeTitle = ep.title,
                                                poster = anime.poster,
                                                quality = "1080p FHD"
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Episode",
                                    tint = AnimeCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Play or Unlock Button
                            IconButton(
                                onClick = {
                                    if (isUnlocked) {
                                        onPlayEpisode(anime.id, currentSeason.name, ep.episodeNumber)
                                    } else {
                                        unlockTargetEpisode = ep
                                        showUnlockDialog = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.PlayCircle else Icons.Default.Lock,
                                    contentDescription = "Play Episode",
                                    tint = if (isUnlocked) AnimeNeonRed else AnimeGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Movie Parts / Quality options (for Movies)
        if (anime.type == AnimeType.MOVIE) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Movie Server & Resolution",
                        color = AnimeTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val parts = if (anime.parts.isNotEmpty()) anime.parts else listOf(
                        com.example.data.model.MoviePart(1, "Full HD Complete Movie (1080p)", anime.movieLink ?: "")
                    )

                    parts.forEach { part ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onPlayMovie(anime.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = AnimeCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = part.title ?: "Part ${part.partNumber}",
                                        color = AnimeTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Fast Server • 4K / 1080p / 720p",
                                        color = AnimeTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Play Part",
                                    tint = AnimeNeonRed,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
