package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.AnimeCard
import com.example.ui.components.CategoryChips
import com.example.ui.theme.AnimeTextPrimary
import com.example.ui.theme.AnimeTextSecondary

@Composable
fun MoviesScreen(
    animeRepo: AnimeRepository,
    userPrefsRepo: UserPreferencesRepository,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animeList by animeRepo.animeList.collectAsState()
    val categories by animeRepo.categories.collectAsState()
    val moviesList = remember(animeList) { animeRepo.getMovies() }
    val watchlist by userPrefsRepo.watchlist.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredMovies = remember(selectedCategory, moviesList) {
        if (selectedCategory == "All") moviesList
        else moviesList.filter {
            it.category.contains(selectedCategory, ignoreCase = true) ||
                    it.genres.any { g -> g.contains(selectedCategory, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("movies_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Anime Movies",
                color = AnimeTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Feature-length anime films in 1080p and 4K Ultra HD",
                color = AnimeTextSecondary,
                fontSize = 12.sp
            )
        }

        CategoryChips(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 135.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredMovies, key = { it.id }) { anime ->
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
