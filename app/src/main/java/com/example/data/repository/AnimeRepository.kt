package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.example.data.remote.FirebaseAnimeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnimeRepository(
    val firebaseService: FirebaseAnimeService = FirebaseAnimeService()
) {
    companion object {
        private const val TAG = "AnimeRepository"
    }

    private val _animeList = MutableStateFlow<List<AnimeItem>>(emptyList())
    val animeList: StateFlow<List<AnimeItem>> = _animeList.asStateFlow()

    private val _heroSlides = MutableStateFlow<List<HeroSlide>>(emptyList())
    val heroSlides: StateFlow<List<HeroSlide>> = _heroSlides.asStateFlow()

    private val _liveChannels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val liveChannels: StateFlow<List<LiveChannel>> = _liveChannels.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _servers = MutableStateFlow<List<ServerItem>>(emptyList())
    val servers: StateFlow<List<ServerItem>> = _servers.asStateFlow()

    private val _weeklySchedule = MutableStateFlow<List<WeeklyScheduleItem>>(emptyList())
    val weeklySchedule: StateFlow<List<WeeklyScheduleItem>> = _weeklySchedule.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val vipTiers = listOf(
        VipTier(
            id = "tier_1day",
            name = "1 Day VIP Pass",
            durationText = "24 Hours Full Access",
            coinPrice = 30,
            bdtPrice = 20,
            perks = listOf("100% Ad-Free Fast Streaming", "Instant 1080p & 4K UHD Unlock", "Dedicated Ultra Fast CDN Servers")
        ),
        VipTier(
            id = "tier_7day",
            name = "7 Days Super Pass",
            durationText = "1 Week Unlimited Access",
            coinPrice = 120,
            bdtPrice = 60,
            perks = listOf("All 1-Day VIP Perks", "Unlimited Offline Downloads", "Exclusive Golden VIP Badge"),
            isPopular = true
        ),
        VipTier(
            id = "tier_30day",
            name = "Monthly Mega Pass",
            durationText = "30 Days Unlimited Access",
            coinPrice = 350,
            bdtPrice = 150,
            perks = listOf("All VIP Benefits Included", "Early Access to Simulcast Premieres", "Custom Anime Profile Icons & Themes")
        ),
        VipTier(
            id = "tier_lifetime",
            name = "Lifetime Legend VIP",
            durationText = "Permanent Lifetime Access",
            coinPrice = 1500,
            bdtPrice = 500,
            perks = listOf("Permanent VIP Legend Status", "Zero Ads Forever", "All Future Anime & Movies Unlocked")
        )
    )

    init {
        loadAllDataFromFirebase()
    }

    fun loadAllDataFromFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            _isLoading.value = true
            try {
                // Fetch all real data in parallel from Firebase Realtime Database
                val seriesDeferred = async { firebaseService.fetchWebseries() }
                val moviesDeferred = async { firebaseService.fetchMovies() }
                val liveDeferred = async { firebaseService.fetchLiveTvChannels() }
                val categoriesDeferred = async { firebaseService.fetchCategories() }
                val scheduleDeferred = async { firebaseService.fetchWeeklySchedule() }
                val serversDeferred = async { firebaseService.fetchServers() }

                val series = seriesDeferred.await()
                val movies = moviesDeferred.await()
                val live = liveDeferred.await()
                val cats = categoriesDeferred.await()
                val sched = scheduleDeferred.await()
                val srvs = serversDeferred.await()

                val combinedCatalog = mutableListOf<AnimeItem>()
                combinedCatalog.addAll(series)
                combinedCatalog.addAll(movies)

                _animeList.value = combinedCatalog
                _heroSlides.value = buildHeroSlides(combinedCatalog)
                _liveChannels.value = live
                if (cats.isNotEmpty()) {
                    _categories.value = cats
                }
                _weeklySchedule.value = sched
                if (srvs.isNotEmpty()) {
                    _servers.value = srvs
                }

                Log.d(TAG, "Successfully loaded ${combinedCatalog.size} items from Firebase (Series: ${series.size}, Movies: ${movies.size}, Live: ${live.size})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading data from Firebase: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildHeroSlides(catalog: List<AnimeItem>): List<HeroSlide> {
        val topPicks = catalog.filter { it.backdrop.isNotBlank() || it.poster.isNotBlank() }
            .sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            .take(6)

        return topPicks.mapIndexed { idx, item ->
            HeroSlide(
                id = "hero_${item.id}_$idx",
                animeId = item.id,
                title = item.title,
                subtitle = item.storyline.take(80) + if (item.storyline.length > 80) "..." else "",
                backdrop = if (item.backdrop.isNotBlank()) item.backdrop else item.poster,
                rating = item.rating,
                year = item.year,
                category = item.category
            )
        }
    }

    fun getAnimeById(id: String): AnimeItem? {
        return _animeList.value.find { it.id == id }
    }

    fun getTrending(): List<AnimeItem> {
        val trending = _animeList.value.filter { it.isTrending }
        return if (trending.isNotEmpty()) trending else _animeList.value.take(10)
    }

    fun getSeries(): List<AnimeItem> {
        return _animeList.value.filter { it.type == AnimeType.SERIES }
    }

    fun getMovies(): List<AnimeItem> {
        return _animeList.value.filter { it.type == AnimeType.MOVIE }
    }

    fun getByCategory(category: String): List<AnimeItem> {
        if (category == "All" || category.isBlank()) return _animeList.value
        return _animeList.value.filter {
            it.category.contains(category, ignoreCase = true) ||
            it.genres.any { g -> g.contains(category, ignoreCase = true) }
        }
    }

    fun searchAnime(query: String, category: String? = null, type: AnimeType? = null): List<AnimeItem> {
        return _animeList.value.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.storyline.contains(query, ignoreCase = true) ||
                    item.genres.any { it.contains(query, ignoreCase = true) }

            val matchesCategory = category == null || category == "All" ||
                    item.category.contains(category, ignoreCase = true) ||
                    item.genres.any { it.contains(category, ignoreCase = true) }

            val matchesType = type == null || item.type == type

            matchesQuery && matchesCategory && matchesType
        }
    }
}
