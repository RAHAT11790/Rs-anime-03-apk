package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppHeader
import com.example.ui.components.BottomTab
import com.example.ui.screens.*
import com.example.ui.theme.AnimeDarkBackground

@Composable
fun RSAnimeApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animeRepo = remember { AnimeRepository() }
    val userPrefsRepo = remember { UserPreferencesRepository(context) }
    val navController = rememberNavController()

    val coins by userPrefsRepo.coins.collectAsState()
    val isVip by userPrefsRepo.isVip.collectAsState()

    var currentTab by remember { mutableStateOf(BottomTab.HOME) }

    NavHost(
        navController = navController,
        startDestination = "main_flow",
        modifier = modifier
            .fillMaxSize()
            .background(AnimeDarkBackground)
    ) {
        // Main Tab Flow with Persistent Bottom Bar & Header (Home, Series, Movies, Profile)
        composable("main_flow") {
            Scaffold(
                containerColor = AnimeDarkBackground,
                topBar = {
                    AppHeader(
                        coins = coins,
                        isVip = isVip,
                        onSearchClick = { navController.navigate("search") },
                        onCoinsClick = { navController.navigate("tasks") },
                        onVipClick = { navController.navigate("vip") }
                    )
                },
                bottomBar = {
                    AppBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        BottomTab.HOME -> {
                            HomeScreen(
                                animeRepo = animeRepo,
                                userPrefsRepo = userPrefsRepo,
                                onAnimeClick = { animeId -> navController.navigate("details/$animeId") },
                                onPlayClick = { animeId -> navController.navigate("player/$animeId/Season 1/1") },
                                onSeeAllSeries = { currentTab = BottomTab.SERIES },
                                onSeeAllMovies = { currentTab = BottomTab.MOVIES },
                                onSeeAllLive = { currentTab = BottomTab.SERIES }
                            )
                        }
                        BottomTab.SERIES -> {
                            SeriesScreen(
                                animeRepo = animeRepo,
                                userPrefsRepo = userPrefsRepo,
                                onAnimeClick = { animeId -> navController.navigate("details/$animeId") }
                            )
                        }
                        BottomTab.MOVIES -> {
                            MoviesScreen(
                                animeRepo = animeRepo,
                                userPrefsRepo = userPrefsRepo,
                                onAnimeClick = { animeId -> navController.navigate("details/$animeId") }
                            )
                        }
                        BottomTab.PROFILE -> {
                            ProfileScreen(
                                animeRepo = animeRepo,
                                userPrefsRepo = userPrefsRepo,
                                onAnimeClick = { animeId -> navController.navigate("details/$animeId") },
                                onPlayClick = { animeId -> navController.navigate("player/$animeId/Season 1/1") },
                                onOpenVipScreen = { navController.navigate("vip") },
                                onPlayOffline = { dl ->
                                    val safePath = if (dl.filePath.isNotBlank()) dl.filePath else ""
                                    val encodedPath = java.net.URLEncoder.encode(safePath, "UTF-8")
                                    val encodedTitle = java.net.URLEncoder.encode(dl.animeTitle, "UTF-8")
                                    val encodedEpTitle = java.net.URLEncoder.encode(if (dl.isMovie) "Full Movie" else "${dl.seasonName} - ${dl.episodeTitle}", "UTF-8")
                                    navController.navigate("player_offline?filePath=$encodedPath&title=$encodedTitle&episodeTitle=$encodedEpTitle&animeId=${dl.animeId}&episodeNumber=${dl.episodeNumber}")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Search Screen
        composable("search") {
            SearchScreen(
                animeRepo = animeRepo,
                userPrefsRepo = userPrefsRepo,
                onAnimeClick = { animeId -> navController.navigate("details/$animeId") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Daily Tasks & Reward Coins Screen
        composable("tasks") {
            DailyTasksScreen(
                userPrefsRepo = userPrefsRepo,
                onOpenVipScreen = { navController.navigate("vip") }
            )
        }

        // Anime Details Screen
        composable(
            route = "details/{animeId}",
            arguments = listOf(navArgument("animeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: "1"
            AnimeDetailsScreen(
                animeId = animeId,
                animeRepo = animeRepo,
                userPrefsRepo = userPrefsRepo,
                onBackClick = { navController.popBackStack() },
                onPlayEpisode = { aId, season, epNum ->
                    navController.navigate("player/$aId/$season/$epNum")
                },
                onPlayMovie = { aId ->
                    navController.navigate("player/$aId/Movie/1")
                },
                onOpenVipScreen = { navController.navigate("vip") }
            )
        }

        // Fullscreen Video Player Screen (Online Streaming)
        composable(
            route = "player/{animeId}/{seasonName}/{episodeNumber}",
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("seasonName") { type = NavType.StringType },
                navArgument("episodeNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: "1"
            val seasonName = backStackEntry.arguments?.getString("seasonName") ?: "Season 1"
            val episodeNumber = backStackEntry.arguments?.getInt("episodeNumber") ?: 1

            VideoPlayerScreen(
                animeId = animeId,
                seasonName = seasonName,
                episodeNumber = episodeNumber,
                animeRepo = animeRepo,
                userPrefsRepo = userPrefsRepo,
                onBackClick = { navController.popBackStack() },
                onNextEpisode = { nextEp ->
                    navController.navigate("player/$animeId/$seasonName/$nextEp") {
                        popUpTo("player/$animeId/$seasonName/$episodeNumber") { inclusive = true }
                    }
                }
            )
        }

        // Fullscreen Video Player Screen (Offline Download Playback)
        composable(
            route = "player_offline?filePath={filePath}&title={title}&episodeTitle={episodeTitle}&animeId={animeId}&episodeNumber={episodeNumber}",
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "Downloaded Video" },
                navArgument("episodeTitle") { type = NavType.StringType; defaultValue = "Offline Playback" },
                navArgument("animeId") { type = NavType.StringType; defaultValue = "1" },
                navArgument("episodeNumber") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: "Downloaded Video"
            val rawEpTitle = backStackEntry.arguments?.getString("episodeTitle") ?: "Offline Playback"
            val animeId = backStackEntry.arguments?.getString("animeId") ?: "1"
            val episodeNumber = backStackEntry.arguments?.getInt("episodeNumber") ?: 1

            val decodedPath = try { java.net.URLDecoder.decode(rawPath, "UTF-8") } catch (e: Exception) { rawPath }
            val decodedTitle = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch (e: Exception) { rawTitle }
            val decodedEpTitle = try { java.net.URLDecoder.decode(rawEpTitle, "UTF-8") } catch (e: Exception) { rawEpTitle }

            VideoPlayerScreen(
                animeId = animeId,
                seasonName = "Offline",
                episodeNumber = episodeNumber,
                animeRepo = animeRepo,
                userPrefsRepo = userPrefsRepo,
                onBackClick = { navController.popBackStack() },
                offlineFilePath = decodedPath,
                customTitle = decodedTitle,
                customSubtitle = decodedEpTitle
            )
        }

        // VIP Pass Screen
        composable("vip") {
            PremiumVipScreen(
                animeRepo = animeRepo,
                userPrefsRepo = userPrefsRepo,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
