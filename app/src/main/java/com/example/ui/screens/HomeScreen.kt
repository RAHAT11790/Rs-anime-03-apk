package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimeItem
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    animeRepo: AnimeRepository,
    userPrefsRepo: UserPreferencesRepository,
    onAnimeClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onSeeAllSeries: () -> Unit,
    onSeeAllMovies: () -> Unit,
    onSeeAllLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animeList by animeRepo.animeList.collectAsState()
    val heroSlides by animeRepo.heroSlides.collectAsState()
    val categories by animeRepo.categories.collectAsState()
    val isLoading by animeRepo.isLoading.collectAsState()
    val watchlist by userPrefsRepo.watchlist.collectAsState()
    val watchHistory by userPrefsRepo.watchHistory.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }

    val filteredAnime = remember(selectedCategory, animeList) {
        animeRepo.getByCategory(selectedCategory)
    }

    val trendingAnime = remember(animeList) {
        animeRepo.getTrending()
    }

    val seriesAnime = remember(animeList) {
        animeRepo.getSeries()
    }

    val moviesAnime = remember(animeList) {
        animeRepo.getMovies()
    }

    if (isLoading && animeList.isEmpty()) {
        CustomLoaderScreen(
            message = "Loading high-definition anime universe...",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Featured Hero Carousel Slider (Netflix Style)
        if (heroSlides.isNotEmpty()) {
            item {
                HeroSlider(
                    slides = heroSlides,
                    onSlideClick = onAnimeClick,
                    onPlayClick = onPlayClick,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )
            }
        }

        // 2. Category Filter Chips (Action, Fantasy, Hindi Dub, etc.)
        item {
            CategoryChips(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }

        // If category is not "All", show filtered list directly
        if (selectedCategory != "All") {
            item {
                SectionHeader(
                    title = "$selectedCategory Collection",
                    subtitle = "${filteredAnime.size} titles available",
                    onSeeAll = null
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(filteredAnime, key = { it.id }) { anime ->
                        AnimeCard(
                            anime = anime,
                            isBookmarked = watchlist.contains(anime.id),
                            onCardClick = { onAnimeClick(anime.id) },
                            onBookmarkClick = { userPrefsRepo.toggleWatchlist(anime.id) }
                        )
                    }
                }
            }
        } else {
            // 3. Continue Watching (if history exists)
            if (watchHistory.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Continue Watching",
                        subtitle = "Resume your episodes in HD",
                        onSeeAll = null
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(watchHistory, key = { it.animeId + it.episodeNumber }) { item ->
                            ContinueWatchingCard(
                                historyItem = item,
                                onClick = { onPlayClick(item.animeId) }
                            )
                        }
                    }
                }
            }

            // 4. Netflix Style: Top 10 Trending Anime in Hindi & English
            item {
                SectionHeader(
                    title = "🔥 Top 10 Trending Anime",
                    subtitle = "Most watched this week with Hindi & English Dubs",
                    onSeeAll = null
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    itemsIndexed(trendingAnime.take(10), key = { _, it -> it.id }) { index, anime ->
                        NetflixTop10Card(
                            rank = index + 1,
                            anime = anime,
                            isBookmarked = watchlist.contains(anime.id),
                            onCardClick = { onAnimeClick(anime.id) },
                            onBookmarkClick = { userPrefsRepo.toggleWatchlist(anime.id) }
                        )
                    }
                }
            }

            // 5. Popular Anime Series (Full Seasons)
            item {
                SectionHeader(
                    title = "Popular Anime Series",
                    subtitle = "Complete seasons in Hindi Dub, Subbed & 1080p",
                    onSeeAll = onSeeAllSeries
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(seriesAnime, key = { it.id }) { anime ->
                        AnimeCard(
                            anime = anime,
                            isBookmarked = watchlist.contains(anime.id),
                            onCardClick = { onAnimeClick(anime.id) },
                            onBookmarkClick = { userPrefsRepo.toggleWatchlist(anime.id) }
                        )
                    }
                }
            }

            // 6. Blockbuster Movies
            item {
                SectionHeader(
                    title = "Blockbuster Anime Movies",
                    subtitle = "Full theatrical anime films in 4K & Ultra HD",
                    onSeeAll = onSeeAllMovies
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(moviesAnime, key = { it.id }) { anime ->
                        AnimeCard(
                            anime = anime,
                            isBookmarked = watchlist.contains(anime.id),
                            onCardClick = { onAnimeClick(anime.id) },
                            onBookmarkClick = { userPrefsRepo.toggleWatchlist(anime.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetflixTop10Card(
    rank: Int,
    anime: AnimeItem,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(170.dp)
            .height(210.dp)
            .clickable { onCardClick() },
        verticalAlignment = Alignment.Bottom
    ) {
        // Large Stylized Rank Number (Netflix Style)
        Box(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "$rank",
                color = AnimeNeonRed,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-4).sp,
                modifier = Modifier.offset(y = 6.dp)
            )
        }

        // Poster Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, AnimeBorder, RoundedCornerShape(12.dp))
        ) {
            AnimeImage(
                model = anime.poster,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 150f
                        )
                    )
            )

            // Rating & Dub tag
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AnimeNeonRed
                ) {
                    Text(
                        text = "★ ${anime.rating}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AnimeTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = AnimeTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        if (onSeeAll != null) {
            TextButton(
                onClick = onSeeAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "See All",
                    color = AnimeNeonRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AnimeNeonRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
