package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeImage
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    animeRepo: AnimeRepository,
    userPrefsRepo: UserPreferencesRepository,
    onAnimeClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onOpenVipScreen: () -> Unit,
    onPlayOffline: ((com.example.data.model.DownloadedEpisode) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVip by userPrefsRepo.isVip.collectAsState()
    val vipTierName by userPrefsRepo.vipTierName.collectAsState()
    val coins by userPrefsRepo.coins.collectAsState()
    val watchlist by userPrefsRepo.watchlist.collectAsState()
    val watchHistory by userPrefsRepo.watchHistory.collectAsState()
    val downloads by userPrefsRepo.downloadedEpisodes.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val watchlistAnime = remember(watchlist) {
        watchlist.mapNotNull { animeRepo.getAnimeById(it) }
    }
    val totalDownloadedMb = remember(downloads) { downloads.sumOf { it.sizeMb } }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About RS Anime", fontWeight = FontWeight.Bold, color = AnimeTextPrimary) },
            text = {
                Column {
                    Text(
                        text = "RS Anime - Cloud Launch Pad",
                        color = AnimeNeonRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Version: 2.4.0 (Pro Edition)\n\nFast and premium anime streaming app with Hindi & English audio, multi-language dubs, and Ultra HD 4K playback.",
                        color = AnimeTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed)) {
                    Text("OK")
                }
            },
            containerColor = AnimeDarkSurfaceCard
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = AnimeTextPrimary) },
            text = {
                Text(
                    text = "RS Anime respects your privacy. All your history, bookmarks, and coin rewards are securely synchronized with Firebase and stored on your device.",
                    color = AnimeTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed)) {
                    Text("Got It")
                }
            },
            containerColor = AnimeDarkSurfaceCard
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AnimeDarkSurfaceCard,
                                    AnimeDarkSurfaceVariant
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, AnimeNeonRed, CircleShape)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Rahat Anime Fan",
                                        color = AnimeTextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isVip) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = AnimePurple,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "VIP",
                                                color = AnimeCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (isVip) vipTierName else "Free Member",
                                    color = if (isVip) AnimeGold else AnimeTextSecondary,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = AnimeGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$coins Reward Coins",
                                        color = AnimeGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onOpenVipScreen,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVip) AnimeDarkSurfaceVariant else AnimeNeonRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isVip) AnimeCyan else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isVip) "Manage VIP Membership" else "Upgrade to VIP Pass",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Tab Selector (Watchlist, History, Offline Library, Settings)
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AnimeNeonRed,
                edgePadding = 0.dp,
                divider = {}
            ) {
                listOf("Watchlist (${watchlist.size})", "History (${watchHistory.size})", "Offline Library (${downloads.size})", "Settings").forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == idx) AnimeNeonRed else AnimeTextSecondary,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }

        // Tab 0: Watchlist
        if (selectedTab == 0) {
            if (watchlistAnime.isEmpty()) {
                item {
                    EmptyStateCard("Watchlist is Empty", "Bookmark your favorite anime series or movies to access them quickly.")
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 135.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 700.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(watchlistAnime, key = { it.id }) { anime ->
                            AnimeCard(
                                anime = anime,
                                isBookmarked = true,
                                onCardClick = { onAnimeClick(anime.id) },
                                onBookmarkClick = { userPrefsRepo.toggleWatchlist(anime.id) }
                            )
                        }
                    }
                }
            }
        }

        // Tab 1: Watch History
        if (selectedTab == 1) {
            if (watchHistory.isEmpty()) {
                item {
                    EmptyStateCard("No Watch History", "Anime you start watching will automatically appear here.")
                }
            } else {
                items(watchHistory, key = { it.animeId + it.episodeNumber }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayClick(item.animeId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 55.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AnimeImage(model = item.poster, contentDescription = item.title, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = AnimeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${item.seasonName} • Ep ${item.episodeNumber}", color = AnimeCyan, fontSize = 12.sp)
                            }
                            IconButton(onClick = { onPlayClick(item.animeId) }) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = AnimeNeonRed)
                            }
                        }
                    }
                }
            }
        }

        // Tab 2: Offline Library
        if (selectedTab == 2) {
            if (downloads.isEmpty()) {
                item {
                    EmptyStateCard(
                        "No Offline Videos Saved",
                        "Download your favorite anime episodes or movies to watch anytime anywhere without an active internet connection."
                    )
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeEmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AnimeEmeraldGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Offline Storage Active",
                                        color = AnimeEmeraldGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${downloads.size} video(s) ready • ${String.format(java.util.Locale.US, "%.1f", totalDownloadedMb)} MB",
                                    color = AnimeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AnimeEmeraldGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "No Net Needed",
                                    color = AnimeEmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                items(downloads, key = { it.animeId + it.episodeNumber }) { dl ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (onPlayOffline != null) {
                                    onPlayOffline(dl)
                                } else {
                                    onPlayClick(dl.animeId)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 85.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AnimeImage(model = dl.poster, contentDescription = dl.animeTitle, modifier = Modifier.fillMaxSize())
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = dl.quality,
                                        color = AnimeGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dl.animeTitle,
                                    color = AnimeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (dl.isMovie) "Full Movie" else "${dl.seasonName.ifBlank { "Season 1" }} • ${dl.episodeTitle}",
                                    color = AnimeCyan,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", dl.sizeMb)} MB • Saved on Device",
                                    color = AnimeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Play Offline Button
                            FilledTonalIconButton(
                                onClick = {
                                    if (onPlayOffline != null) {
                                        onPlayOffline(dl)
                                    } else {
                                        onPlayClick(dl.animeId)
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = AnimeNeonRed.copy(alpha = 0.2f),
                                    contentColor = AnimeNeonRed
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Watch Offline")
                            }

                            IconButton(
                                onClick = {
                                    userPrefsRepo.removeDownload(dl.animeId, dl.episodeNumber)
                                    Toast.makeText(context, "Deleted from offline storage", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AnimeTextMuted)
                            }
                        }
                    }
                }
            }
        }

        // Tab 3: Settings & About
        if (selectedTab == 3) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingsRowItem(
                            icon = Icons.Default.Language,
                            title = "App Language",
                            subtitle = "English (US)",
                            onClick = { Toast.makeText(context, "English language active", Toast.LENGTH_SHORT).show() }
                        )

                        SettingsRowItem(
                            icon = Icons.Default.CleaningServices,
                            title = "Clear Cache",
                            subtitle = "Free up device storage",
                            onClick = { Toast.makeText(context, "Cache cleared successfully!", Toast.LENGTH_SHORT).show() }
                        )

                        SettingsRowItem(
                            icon = Icons.Default.Send,
                            title = "Official Community Channel",
                            subtitle = "Join for latest releases & promo codes",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/rsanime"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Community: @rsanime", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        SettingsRowItem(
                            icon = Icons.Default.Info,
                            title = "About Application",
                            subtitle = "Version & developer details",
                            onClick = { showAboutDialog = true }
                        )

                        SettingsRowItem(
                            icon = Icons.Default.Policy,
                            title = "Privacy Policy",
                            subtitle = "User data security & terms",
                            onClick = { showPrivacyDialog = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AnimeDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AnimeCyan, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AnimeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AnimeTextSecondary, fontSize = 11.sp)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AnimeTextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun EmptyStateCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = AnimeTextMuted, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = AnimeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = AnimeTextSecondary, fontSize = 12.sp)
        }
    }
}
