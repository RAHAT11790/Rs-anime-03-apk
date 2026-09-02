package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimeType
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.AnimeCard
import com.example.ui.theme.*

@Composable
fun SearchScreen(
    animeRepo: AnimeRepository,
    userPrefsRepo: UserPreferencesRepository,
    onAnimeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<AnimeType?>(null) }
    var selectedGenre by remember { mutableStateOf("All") }
    val animeList by animeRepo.animeList.collectAsState()
    val watchlist by userPrefsRepo.watchlist.collectAsState()

    val searchHistory = remember {
        mutableStateListOf("Solo Leveling", "Demon Slayer", "Jujutsu Kaisen", "Attack on Titan", "Cyberpunk")
    }

    val searchResults = remember(searchQuery, selectedType, selectedGenre, animeList) {
        animeRepo.searchAnime(
            query = searchQuery,
            category = if (selectedGenre == "All") null else selectedGenre,
            type = selectedType
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        // Search Header Bar with Back button, text field and clear icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AnimeDarkSurfaceCard)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AnimeTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by title, genre, or character...",
                        color = AnimeTextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AnimeNeonRed,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = AnimeTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AnimeDarkSurfaceCard,
                    unfocusedContainerColor = AnimeDarkSurfaceCard,
                    focusedTextColor = AnimeTextPrimary,
                    unfocusedTextColor = AnimeTextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(1.dp, AnimeBorder, RoundedCornerShape(12.dp))
                    .testTag("search_input_field")
            )
        }

        // Type Selector Chips (All, Series, Movies)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { selectedType = null },
                label = { Text("All Types") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AnimeNeonRed,
                    selectedLabelColor = Color.White,
                    containerColor = AnimeDarkSurfaceCard,
                    labelColor = AnimeTextSecondary
                )
            )

            FilterChip(
                selected = selectedType == AnimeType.SERIES,
                onClick = { selectedType = AnimeType.SERIES },
                label = { Text("Series") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AnimeNeonRed,
                    selectedLabelColor = Color.White,
                    containerColor = AnimeDarkSurfaceCard,
                    labelColor = AnimeTextSecondary
                )
            )

            FilterChip(
                selected = selectedType == AnimeType.MOVIE,
                onClick = { selectedType = AnimeType.MOVIE },
                label = { Text("Movies") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AnimeNeonRed,
                    selectedLabelColor = Color.White,
                    containerColor = AnimeDarkSurfaceCard,
                    labelColor = AnimeTextSecondary
                )
            )
        }

        // Recent searches history tags (when query is empty)
        if (searchQuery.isBlank()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Popular Searches",
                    color = AnimeTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchHistory) { tag ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AnimeDarkSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                            modifier = Modifier.clickable { searchQuery = tag }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = AnimeCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tag,
                                    color = AnimeTextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Results Count Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isBlank()) "All Anime Titles" else "Results for '$searchQuery'",
                color = AnimeTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${searchResults.size} found",
                color = AnimeCyan,
                fontSize = 12.sp
            )
        }

        // Results Grid
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AnimeTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No anime found",
                        color = AnimeTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try searching for a different keyword or genre",
                        color = AnimeTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 135.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults, key = { it.id }) { anime ->
                    AnimeCard(
                        anime = anime,
                        isBookmarked = watchlist.contains(anime.id),
                        onCardClick = { onAnimeClick(anime.id) },
                        onBookmarkClick = { userPrefsRepo.toggleWatchlist(anime.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
