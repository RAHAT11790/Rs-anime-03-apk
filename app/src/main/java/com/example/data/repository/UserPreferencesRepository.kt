package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DailyTask
import com.example.data.model.DownloadedEpisode
import com.example.data.model.WatchHistoryItem
import com.example.data.remote.FirebaseAnimeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserPreferencesRepository(
    context: Context,
    private val firebaseService: FirebaseAnimeService = FirebaseAnimeService()
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("rs_anime_prefs", Context.MODE_PRIVATE)

    // User Coins
    private val _coins = MutableStateFlow(prefs.getInt("user_coins", 50))
    val coins: StateFlow<Int> = _coins.asStateFlow()

    // VIP Pass Status
    private val _isVip = MutableStateFlow(prefs.getBoolean("user_is_vip", false))
    val isVip: StateFlow<Boolean> = _isVip.asStateFlow()

    private val _vipTierName = MutableStateFlow(prefs.getString("user_vip_tier", "Free Member") ?: "Free Member")
    val vipTierName: StateFlow<String> = _vipTierName.asStateFlow()

    // Watchlist / Bookmarks (Set of Anime IDs)
    private val _watchlist = MutableStateFlow(prefs.getStringSet("user_watchlist", emptySet()) ?: emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    // Unlocked Episode/Movie Keys (Format: "animeId_season_ep" or "animeId_movie")
    private val _unlockedKeys = MutableStateFlow(prefs.getStringSet("user_unlocked_keys", emptySet()) ?: emptySet())
    val unlockedKeys: StateFlow<Set<String>> = _unlockedKeys.asStateFlow()

    // Watch History
    private val _watchHistory = MutableStateFlow<List<WatchHistoryItem>>(loadWatchHistory())
    val watchHistory: StateFlow<List<WatchHistoryItem>> = _watchHistory.asStateFlow()

    // Downloaded Episodes
    private val _downloadedEpisodes = MutableStateFlow<List<DownloadedEpisode>>(loadDownloads())
    val downloadedEpisodes: StateFlow<List<DownloadedEpisode>> = _downloadedEpisodes.asStateFlow()

    // Daily Tasks List
    private val _dailyTasks = MutableStateFlow(generateTasks())
    val dailyTasks: StateFlow<List<DailyTask>> = _dailyTasks.asStateFlow()

    // Streak count
    private val _streakDays = MutableStateFlow(prefs.getInt("streak_days", 1))
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    // Watch time today in minutes
    private val _watchTimeMinutes = MutableStateFlow(prefs.getInt("watch_time_minutes", 12))
    val watchTimeMinutes: StateFlow<Int> = _watchTimeMinutes.asStateFlow()

    // User Referral Code
    val userReferralCode: String = prefs.getString("user_ref_code", null) ?: run {
        val code = "RS" + (100000..999999).random()
        prefs.edit().putString("user_ref_code", code).apply()
        code
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun triggerCloudSync() {
        CoroutineScope(Dispatchers.IO).launch {
            firebaseService.syncUserToFirebase(
                userId = userReferralCode,
                name = "Anime Explorer",
                email = "user_$userReferralCode@animeverse.app",
                coins = _coins.value,
                isVip = _isVip.value,
                vipExpiry = if (_isVip.value) System.currentTimeMillis() + 86400000L * 30 else 0L,
                vipTier = _vipTierName.value
            )
        }
    }

    suspend fun redeemPromoCodeAsync(promo: String): Pair<Boolean, String> {
        val cleaned = promo.trim().uppercase()

        // 1. First check against real Firebase Database redeemCodes
        val firebaseResult = firebaseService.redeemCodeInFirebase(cleaned, userReferralCode)
        if (firebaseResult.first) {
            activateVip("VIP Member (Redeemed)")
            addCoins(100)
            return firebaseResult
        }

        // 2. Bonus local codes fallback
        return when (cleaned) {
            "RSANIME50" -> {
                if (prefs.getBoolean("promo_rsanime50", false)) {
                    Pair(false, "You have already redeemed promo code RSANIME50!")
                } else {
                    addCoins(50)
                    prefs.edit().putBoolean("promo_rsanime50", true).apply()
                    Pair(true, "50 Coins added to your account!")
                }
            }
            "VIPFREE" -> {
                if (prefs.getBoolean("promo_vipfree", false)) {
                    Pair(false, "You have already redeemed promo code VIPFREE!")
                } else {
                    activateVip("1 Day VIP Pass")
                    prefs.edit().putBoolean("promo_vipfree", true).apply()
                    Pair(true, "1 Day Free VIP Pass successfully activated!")
                }
            }
            "RAHAT100" -> {
                if (prefs.getBoolean("promo_rahat100", false)) {
                    Pair(false, "You have already redeemed promo code RAHAT100!")
                } else {
                    addCoins(100)
                    prefs.edit().putBoolean("promo_rahat100", true).apply()
                    Pair(true, "100 Bonus Coins added to your account!")
                }
            }
            else -> firebaseResult
        }
    }

    fun redeemPromoCode(promo: String): Pair<Boolean, String> {
        val cleaned = promo.trim().uppercase()
        return when (cleaned) {
            "RSANIME50" -> {
                if (prefs.getBoolean("promo_rsanime50", false)) {
                    Pair(false, "You have already redeemed promo code RSANIME50!")
                } else {
                    addCoins(50)
                    prefs.edit().putBoolean("promo_rsanime50", true).apply()
                    Pair(true, "50 Coins added to your account!")
                }
            }
            "VIPFREE" -> {
                if (prefs.getBoolean("promo_vipfree", false)) {
                    Pair(false, "You have already redeemed promo code VIPFREE!")
                } else {
                    activateVip("1 Day VIP Pass")
                    prefs.edit().putBoolean("promo_vipfree", true).apply()
                    Pair(true, "1 Day Free VIP Pass successfully activated!")
                }
            }
            "RAHAT100" -> {
                if (prefs.getBoolean("promo_rahat100", false)) {
                    Pair(false, "You have already redeemed promo code RAHAT100!")
                } else {
                    addCoins(100)
                    prefs.edit().putBoolean("promo_rahat100", true).apply()
                    Pair(true, "100 Bonus Coins added to your account!")
                }
            }
            else -> {
                // Trigger async check in background if needed
                CoroutineScope(Dispatchers.IO).launch {
                    val res = firebaseService.redeemCodeInFirebase(cleaned, userReferralCode)
                    if (res.first) {
                        activateVip("VIP Member (Redeemed)")
                        addCoins(100)
                    }
                }
                Pair(false, "Invalid promo code! Please check and try again.")
            }
        }
    }

    private fun generateTasks(): List<DailyTask> {
        val lastCheckIn = prefs.getString("last_checkin_date", "")
        val isCheckedInToday = lastCheckIn == getTodayDate()
        val watchMins = prefs.getInt("watch_time_minutes", 12)
        val hasWatched30 = watchMins >= 30
        val isSharedToday = prefs.getBoolean("is_shared_today", false)
        val isWheelSpunToday = prefs.getBoolean("is_wheel_spun_today", false)

        return listOf(
            DailyTask(
                id = "task_daily_checkin",
                title = "Daily Check-in Reward",
                description = "Log in daily to claim free reward coins and build your streak",
                rewardCoins = 10,
                isCompleted = isCheckedInToday,
                progress = if (isCheckedInToday) 1 else 0,
                maxProgress = 1,
                iconName = "check"
            ),
            DailyTask(
                id = "task_watch_30min",
                title = "Watch 30 Minutes of Anime",
                description = "Stream your favorite anime episodes or movies today",
                rewardCoins = 15,
                isCompleted = hasWatched30,
                progress = minOf(watchMins, 30),
                maxProgress = 30,
                iconName = "play"
            ),
            DailyTask(
                id = "task_lucky_spin",
                title = "Lucky Wheel Spin",
                description = "Spin the lucky wheel to win between 5 to 50 bonus coins",
                rewardCoins = 25,
                isCompleted = isWheelSpunToday,
                progress = if (isWheelSpunToday) 1 else 0,
                maxProgress = 1,
                iconName = "casino"
            ),
            DailyTask(
                id = "task_invite_friend",
                title = "Invite Friends & Share",
                description = "Share your referral code with fellow anime fans for 25 coins",
                rewardCoins = 25,
                isCompleted = isSharedToday,
                progress = if (isSharedToday) 1 else 0,
                maxProgress = 1,
                iconName = "share"
            )
        )
    }

    fun addCoins(amount: Int) {
        val newCoins = _coins.value + amount
        _coins.value = newCoins
        prefs.edit().putInt("user_coins", newCoins).apply()
        triggerCloudSync()
    }

    fun spendCoins(amount: Int): Boolean {
        if (_coins.value >= amount) {
            val newCoins = _coins.value - amount
            _coins.value = newCoins
            prefs.edit().putInt("user_coins", newCoins).apply()
            triggerCloudSync()
            return true
        }
        return false
    }

    fun activateVip(tier: String) {
        _isVip.value = true
        _vipTierName.value = tier
        prefs.edit()
            .putBoolean("user_is_vip", true)
            .putString("user_vip_tier", tier)
            .apply()
        triggerCloudSync()
    }

    fun toggleWatchlist(animeId: String): Boolean {
        val current = _watchlist.value.toMutableSet()
        val isAdded: Boolean
        if (current.contains(animeId)) {
            current.remove(animeId)
            isAdded = false
        } else {
            current.add(animeId)
            isAdded = true
        }
        _watchlist.value = current
        prefs.edit().putStringSet("user_watchlist", current).apply()
        triggerCloudSync()
        return isAdded
    }

    fun isInWatchlist(animeId: String): Boolean {
        return _watchlist.value.contains(animeId)
    }

    fun unlockItem(key: String, coinPrice: Int = 10): Boolean {
        if (isItemUnlocked(key)) return true
        if (_isVip.value) return true
        if (spendCoins(coinPrice)) {
            val current = _unlockedKeys.value.toMutableSet()
            current.add(key)
            _unlockedKeys.value = current
            prefs.edit().putStringSet("user_unlocked_keys", current).apply()
            return true
        }
        return false
    }

    fun isItemUnlocked(key: String): Boolean {
        if (_isVip.value) return true
        return _unlockedKeys.value.contains(key)
    }

    fun claimDailyCheckIn(): Boolean {
        val today = getTodayDate()
        val lastDate = prefs.getString("last_checkin_date", "")
        if (lastDate != today) {
            val streak = prefs.getInt("streak_days", 1)
            val newStreak = if (!lastDate.isNullOrBlank()) streak + 1 else 1
            _streakDays.value = newStreak
            prefs.edit()
                .putString("last_checkin_date", today)
                .putInt("streak_days", newStreak)
                .apply()
            addCoins(10 + (newStreak * 2))
            _dailyTasks.value = generateTasks()
            return true
        }
        return false
    }

    fun recordWatchTime(minutes: Int) {
        val current = _watchTimeMinutes.value + minutes
        _watchTimeMinutes.value = current
        prefs.edit().putInt("watch_time_minutes", current).apply()
        if (current >= 30 && !prefs.getBoolean("has_claimed_watch_30", false)) {
            addCoins(15)
            prefs.edit().putBoolean("has_claimed_watch_30", true).apply()
        }
        _dailyTasks.value = generateTasks()
    }

    fun claimSpinReward(reward: Int) {
        addCoins(reward)
        prefs.edit().putBoolean("is_wheel_spun_today", true).apply()
        _dailyTasks.value = generateTasks()
    }

    fun claimInviteReward(code: String): Pair<Boolean, String> {
        if (code.isBlank() || code.equals(userReferralCode, ignoreCase = true)) {
            return Pair(false, "You cannot use your own referral code!")
        }
        if (prefs.getBoolean("used_ref_code", false)) {
            return Pair(false, "You have already redeemed a referral code!")
        }
        addCoins(30)
        prefs.edit().putBoolean("used_ref_code", true).putBoolean("is_shared_today", true).apply()
        _dailyTasks.value = generateTasks()
        return Pair(true, "Congratulations! 30 free reward coins added to your wallet!")
    }

    fun saveWatchProgress(animeId: String, episodeNumber: Int, seasonName: String, title: String, poster: String, positionMs: Long, durationMs: Long) {
        val list = _watchHistory.value.toMutableList()
        list.removeAll { it.animeId == animeId }
        list.add(0, WatchHistoryItem(animeId, episodeNumber, seasonName, title, poster, positionMs, durationMs, System.currentTimeMillis()))
        if (list.size > 20) {
            list.subList(20, list.size).clear()
        }
        _watchHistory.value = list
        saveWatchHistoryToPrefs(list)
    }

    private fun saveWatchHistoryToPrefs(list: List<WatchHistoryItem>) {
        val raw = list.joinToString(";") {
            "${it.animeId}|${it.episodeNumber}|${it.seasonName}|${it.title}|${it.poster}|${it.lastPositionMs}|${it.totalDurationMs}|${it.timestamp}"
        }
        prefs.edit().putString("watch_history_raw", raw).apply()
    }

    private fun loadWatchHistory(): List<WatchHistoryItem> {
        val raw = prefs.getString("watch_history_raw", "") ?: ""
        if (raw.isBlank()) {
            return emptyList()
        }
        return try {
            raw.split(";").mapNotNull { part ->
                val tokens = part.split("|")
                if (tokens.size >= 8) {
                    WatchHistoryItem(
                        tokens[0], tokens[1].toIntOrNull() ?: 1, tokens[2], tokens[3], tokens[4],
                        tokens[5].toLongOrNull() ?: 0, tokens[6].toLongOrNull() ?: 0, tokens[7].toLongOrNull() ?: 0
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addDownload(download: DownloadedEpisode) {
        val list = _downloadedEpisodes.value.toMutableList()
        list.removeAll { it.animeId == download.animeId && it.episodeNumber == download.episodeNumber && it.isMovie == download.isMovie }
        list.add(0, download)
        _downloadedEpisodes.value = list
        saveDownloadsToPrefs(list)
    }

    fun removeDownload(animeId: String, episodeNumber: Int, isMovie: Boolean = false) {
        val list = _downloadedEpisodes.value.toMutableList()
        val target = list.find { it.animeId == animeId && it.episodeNumber == episodeNumber && it.isMovie == isMovie }
        if (target != null && target.filePath.isNotBlank()) {
            try {
                val file = java.io.File(target.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore file delete failure
            }
        }
        list.removeAll { it.animeId == animeId && it.episodeNumber == episodeNumber && it.isMovie == isMovie }
        _downloadedEpisodes.value = list
        saveDownloadsToPrefs(list)
    }

    private fun saveDownloadsToPrefs(list: List<DownloadedEpisode>) {
        val raw = list.joinToString(";;") {
            "${it.animeId}##${it.animeTitle}##${it.episodeNumber}##${it.episodeTitle}##${it.poster}##${it.quality}##${it.sizeMb}##${it.downloadedAt}##${it.filePath}##${it.isMovie}##${it.seasonName}"
        }
        prefs.edit().putString("downloads_raw_v2", raw).apply()
    }

    private fun loadDownloads(): List<DownloadedEpisode> {
        val rawV2 = prefs.getString("downloads_raw_v2", null)
        if (!rawV2.isNullOrBlank()) {
            return try {
                rawV2.split(";;").mapNotNull { part ->
                    val tokens = part.split("##")
                    if (tokens.size >= 8) {
                        DownloadedEpisode(
                            animeId = tokens[0],
                            animeTitle = tokens[1],
                            episodeNumber = tokens[2].toIntOrNull() ?: 1,
                            episodeTitle = tokens[3],
                            poster = tokens[4],
                            quality = tokens[5],
                            sizeMb = tokens[6].toDoubleOrNull() ?: 120.0,
                            downloadedAt = tokens[7].toLongOrNull() ?: 0L,
                            filePath = if (tokens.size > 8) tokens[8] else "",
                            isMovie = if (tokens.size > 9) tokens[9].toBoolean() else false,
                            seasonName = if (tokens.size > 10) tokens[10] else "Season 1"
                        )
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Fallback for legacy v1 downloads
        val raw = prefs.getString("downloads_raw", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            raw.split(";").mapNotNull { part ->
                val tokens = part.split("|")
                if (tokens.size >= 8) {
                    DownloadedEpisode(
                        animeId = tokens[0],
                        animeTitle = tokens[1],
                        episodeNumber = tokens[2].toIntOrNull() ?: 1,
                        episodeTitle = tokens[3],
                        poster = tokens[4],
                        quality = tokens[5],
                        sizeMb = tokens[6].toDoubleOrNull() ?: 120.0,
                        downloadedAt = tokens[7].toLongOrNull() ?: 0L
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
