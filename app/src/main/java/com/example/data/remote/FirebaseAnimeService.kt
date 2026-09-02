package com.example.data.remote

import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FirebaseAnimeService {

    companion object {
        private const val TAG = "FirebaseAnimeService"
        const val FIREBASE_DATABASE_URL = "https://animeverse-d7b79-default-rtdb.asia-southeast1.firebasedatabase.app"
        const val FIREBASE_API_KEY = "AIzaSyASjrQM27mfAbHXA9ZqYv3YbubZPUxOR50"
        const val FIREBASE_PROJECT_ID = "animeverse-d7b79"
        const val FIREBASE_AUTH_DOMAIN = "animeverse-d7b79.firebaseapp.com"
        const val FIREBASE_STORAGE_BUCKET = "animeverse-d7b79.firebasestorage.app"
        const val FIREBASE_MESSAGING_SENDER_ID = "1050779978318"
        const val FIREBASE_APP_ID = "1:1050779978318:web:8bc00ed477bec7a14f511f"

        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // 1. Fetch All Webseries from Firebase RTDB
    suspend fun fetchWebseries(): List<AnimeItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AnimeItem>()
        try {
            val url = "$FIREBASE_DATABASE_URL/webseries.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank() && bodyString != "null") {
                        val jsonObject = JSONObject(bodyString)
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val id = keys.next()
                            val itemObj = jsonObject.optJSONObject(id) ?: continue
                            val animeItem = parseAnimeSeries(id, itemObj)
                            result.add(animeItem)
                        }
                    }
                }
            }
            Log.d(TAG, "Successfully fetched ${result.size} series from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching webseries from Firebase: ${e.message}", e)
        }
        result
    }

    // 2. Fetch All Movies from Firebase RTDB
    suspend fun fetchMovies(): List<AnimeItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AnimeItem>()
        try {
            val url = "$FIREBASE_DATABASE_URL/movies.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank() && bodyString != "null") {
                        val jsonObject = JSONObject(bodyString)
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val id = keys.next()
                            val itemObj = jsonObject.optJSONObject(id) ?: continue
                            val animeMovie = parseAnimeMovie(id, itemObj)
                            result.add(animeMovie)
                        }
                    }
                }
            }
            Log.d(TAG, "Successfully fetched ${result.size} movies from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching movies from Firebase: ${e.message}", e)
        }
        result
    }

    // 3. Fetch Live TV Channels from Firebase RTDB
    suspend fun fetchLiveTvChannels(): List<LiveChannel> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LiveChannel>()
        try {
            val url = "$FIREBASE_DATABASE_URL/liveTvChannels.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank() && bodyString != "null") {
                        val jsonObject = JSONObject(bodyString)
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val id = keys.next()
                            val chObj = jsonObject.optJSONObject(id) ?: continue
                            val name = chObj.optString("name", "Live TV")
                            val category = chObj.optString("category", "General")
                            val streamUrl = chObj.optString("streamUrl", "")
                            val logo = chObj.optString("logo", "")
                            val banner = chObj.optString("banner", "")
                            val order = chObj.optInt("order", 1)

                            if (streamUrl.isNotBlank()) {
                                result.add(
                                    LiveChannel(
                                        id = id,
                                        name = name,
                                        category = category,
                                        streamUrl = streamUrl,
                                        logo = logo,
                                        banner = banner,
                                        posterUrl = if (banner.isNotBlank()) banner else logo,
                                        order = order
                                    )
                                )
                            }
                        }
                    }
                }
            }
            result.sortBy { it.order }
            Log.d(TAG, "Successfully fetched ${result.size} Live Channels from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live TV channels: ${e.message}", e)
        }
        result
    }

    // 4. Fetch Categories from Firebase RTDB
    suspend fun fetchCategories(): List<String> = withContext(Dispatchers.IO) {
        val categories = linkedSetOf("All")
        try {
            val url = "$FIREBASE_DATABASE_URL/categories.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank() && bodyString != "null") {
                        if (bodyString.trim().startsWith("{")) {
                            val jsonObject = JSONObject(bodyString)
                            val keys = jsonObject.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val catObj = jsonObject.optJSONObject(key)
                                val name = catObj?.optString("name") ?: jsonObject.optString(key)
                                if (!name.isNullOrBlank()) categories.add(name)
                            }
                        } else if (bodyString.trim().startsWith("[")) {
                            val jsonArray = JSONArray(bodyString)
                            for (i in 0 until jsonArray.length()) {
                                val name = jsonArray.optString(i)
                                if (name.isNotBlank()) categories.add(name)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories: ${e.message}")
        }
        if (categories.size <= 1) {
            categories.addAll(listOf("Action", "Fantasy", "Romance", "Isekai", "Sci-Fi", "Comedy", "Adventure", "Drama", "Horror"))
        }
        categories.toList()
    }

    // 4b. Fetch Real Streaming Servers from Firebase RTDB
    suspend fun fetchServers(): List<ServerItem> = withContext(Dispatchers.IO) {
        val servers = mutableListOf<ServerItem>()
        try {
            val urlsToTry = listOf(
                "$FIREBASE_DATABASE_URL/settings/videoServers.json",
                "$FIREBASE_DATABASE_URL/videoServers.json",
                "$FIREBASE_DATABASE_URL/servers.json"
            )
            for (url in urlsToTry) {
                if (servers.isNotEmpty()) break
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank() && bodyString != "null") {
                            val trimmed = bodyString.trim()
                            if (trimmed.startsWith("[")) {
                                val jsonArray = JSONArray(trimmed)
                                for (i in 0 until jsonArray.length()) {
                                    val sObj = jsonArray.optJSONObject(i)
                                    if (sObj != null) {
                                        val name = sObj.optString("name", "Server ${i + 1}")
                                        val domain = sObj.optString("domain", sObj.optString("host", ""))
                                        val proxy = sObj.optString("proxy", "")
                                        val locked = sObj.optBoolean("locked", false)
                                        val isDefault = i == 0 || name.contains("01", true) || name.contains("03", true)
                                        servers.add(ServerItem(id = "srv_${i + 1}", name = name, domain = domain, proxy = proxy, isDefault = isDefault, locked = locked))
                                    }
                                }
                            } else if (trimmed.startsWith("{")) {
                                val jsonObject = JSONObject(trimmed)
                                val keys = jsonObject.keys()
                                var idx = 1
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    val sObj = jsonObject.optJSONObject(key)
                                    if (sObj != null) {
                                        val name = sObj.optString("name", "Server $idx")
                                        val domain = sObj.optString("domain", sObj.optString("host", ""))
                                        val proxy = sObj.optString("proxy", "")
                                        val locked = sObj.optBoolean("locked", false)
                                        val isDefault = idx == 1
                                        servers.add(ServerItem(id = key, name = name, domain = domain, proxy = proxy, isDefault = isDefault, locked = locked))
                                    }
                                    idx++
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching servers from Firebase: ${e.message}")
        }

        // Real Firebase servers fallback if offline/error
        if (servers.isEmpty()) {
            servers.addAll(
                listOf(
                    ServerItem(id = "srv_1", name = "RS FR 01", domain = "http://51.75.118.79:20044", proxy = "https://jlzflmbgulievhgrunps.supabase.co/functions/v1/video-proxy", isDefault = true),
                    ServerItem(id = "srv_2", name = "RS FR 02", domain = "https://rahat1102-video-hosting-bot.hf.space", isDefault = false),
                    ServerItem(id = "srv_3", name = "RS FR 03", domain = "http://de3.bot-hosting.net:20508", proxy = "https://tucrcjmlzsdtkdqiqxxc.supabase.co/functions/v1/rs_video_proxy", isDefault = false),
                    ServerItem(id = "srv_4", name = "RS FR 04", domain = "http://us.monkey-network.xyz:6072", proxy = "https://tucrcjmlzsdtkdqiqxxc.supabase.co/functions/v1/rs_video_proxy", isDefault = false),
                    ServerItem(id = "srv_5", name = "RS PR S1", domain = "https://rsstreambot-production.up.railway.app", locked = true, isDefault = false)
                )
            )
        }
        servers
    }

    // 5. Fetch Weekly Schedule
    suspend fun fetchWeeklySchedule(): List<WeeklyScheduleItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WeeklyScheduleItem>()
        try {
            val url = "$FIREBASE_DATABASE_URL/weeklySchedule.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body != "null") {
                        val obj = JSONObject(body)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val id = keys.next()
                            val item = obj.optJSONObject(id) ?: continue
                            list.add(
                                WeeklyScheduleItem(
                                    id = id,
                                    seriesId = item.optString("seriesId", id),
                                    title = item.optString("title", ""),
                                    day = item.optString("day", ""),
                                    poster = item.optString("poster", ""),
                                    updatedAt = item.optLong("updatedAt", 0L)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weekly schedule: ${e.message}")
        }
        list
    }

    // 6. Redeem Promo Code from Firebase
    suspend fun redeemCodeInFirebase(code: String, userId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val cleanCode = code.trim().uppercase()
            val url = "$FIREBASE_DATABASE_URL/redeemCodes.json"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Pair(false, "Network error. Please try again.")

            val body = response.body?.string() ?: return@withContext Pair(false, "No codes available.")
            if (body == "null" || body.isBlank()) return@withContext Pair(false, "Invalid promo code.")

            val jsonObject = JSONObject(body)
            val keys = jsonObject.keys()
            var matchedKey: String? = null
            var codeDays = 1

            while (keys.hasNext()) {
                val key = keys.next()
                val codeObj = jsonObject.optJSONObject(key) ?: continue
                val existingCode = codeObj.optString("code", "").trim().uppercase()
                val used = codeObj.optBoolean("used", false)

                if (existingCode == cleanCode) {
                    if (used) {
                        return@withContext Pair(false, "This code has already been redeemed.")
                    }
                    matchedKey = key
                    codeDays = codeObj.optInt("days", 1)
                    break
                }
            }

            if (matchedKey == null) {
                return@withContext Pair(false, "Invalid promo code. Please check and try again.")
            }

            // Mark as used in Firebase
            val patchUrl = "$FIREBASE_DATABASE_URL/redeemCodes/$matchedKey.json"
            val patchPayload = JSONObject().apply {
                put("used", true)
                put("usedAt", System.currentTimeMillis())
                put("usedBy", userId)
            }
            val patchRequest = Request.Builder()
                .url(patchUrl)
                .patch(patchPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val patchResponse = client.newCall(patchRequest).execute()
            if (patchResponse.isSuccessful) {
                Pair(true, "Promo code redeemed successfully! Enjoy $codeDays days of VIP Pass.")
            } else {
                Pair(false, "Failed to update code status. Please try again.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error redeeming code: ${e.message}")
            Pair(false, "Error: ${e.message}")
        }
    }

    // 7. Sync User State with Firebase RTDB
    suspend fun syncUserToFirebase(
        userId: String,
        name: String,
        email: String,
        coins: Int,
        isVip: Boolean,
        vipExpiry: Long = 0L,
        vipTier: String = "Free Member"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sanitizedKey = userId.replace(".", ",").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
            val url = "$FIREBASE_DATABASE_URL/users/$sanitizedKey.json"
            val payload = JSONObject().apply {
                put("id", userId)
                put("name", name)
                put("email", email)
                put("coins", coins)
                put("isVip", isVip)
                put("vipExpiry", vipExpiry)
                put("vipTier", vipTier)
                put("lastSeen", System.currentTimeMillis())
                put("online", true)
            }
            val request = Request.Builder()
                .url(url)
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user to Firebase: ${e.message}")
            false
        }
    }

    // 8. Fetch Comments for an Anime or Movie
    suspend fun fetchComments(contentId: String): List<CommentItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<CommentItem>()
        try {
            val url = "$FIREBASE_DATABASE_URL/comments/$contentId.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body != "null") {
                        val obj = JSONObject(body)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val id = keys.next()
                            val commentObj = obj.optJSONObject(id) ?: continue
                            result.add(
                                CommentItem(
                                    id = id,
                                    text = commentObj.optString("text", ""),
                                    timestamp = commentObj.optLong("timestamp", 0L),
                                    userId = commentObj.optString("userId", ""),
                                    userName = commentObj.optString("userName", "Anonymous")
                                )
                            )
                        }
                    }
                }
            }
            result.sortByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments: ${e.message}")
        }
        result
    }

    // 9. Post Comment
    suspend fun postComment(contentId: String, text: String, userId: String, userName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$FIREBASE_DATABASE_URL/comments/$contentId.json"
            val payload = JSONObject().apply {
                put("text", text)
                put("timestamp", System.currentTimeMillis())
                put("userId", userId)
                put("userName", userName)
            }
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting comment: ${e.message}")
            false
        }
    }

    // 10. Like / Engagement
    suspend fun fetchLikesCount(contentId: String): Int = withContext(Dispatchers.IO) {
        try {
            val url = "$FIREBASE_DATABASE_URL/engagement/$contentId/likes.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body != "null") {
                        val obj = JSONObject(body)
                        return@withContext obj.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching likes count: ${e.message}")
        }
        0
    }

    suspend fun toggleLike(contentId: String, userId: String, isLiked: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val sanitizedUserId = userId.replace(".", "_")
            val url = "$FIREBASE_DATABASE_URL/engagement/$contentId/likes/$sanitizedUserId.json"
            val request = if (isLiked) {
                val payload = JSONObject().apply { put("ts", System.currentTimeMillis()) }
                Request.Builder().url(url).put(payload.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
            } else {
                Request.Builder().url(url).delete().build()
            }
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling like: ${e.message}")
            false
        }
    }

    // Parser for Webseries
    private fun parseAnimeSeries(id: String, json: JSONObject): AnimeItem {
        val title = json.optString("title", "Untitled Series")
        val poster = json.optString("poster", "")
        val backdrop = json.optString("backdrop", poster)
        val logo = json.optString("logo", "")
        val year = parseStringOrNumber(json, "year", "2024")
        val rating = parseStringOrNumber(json, "rating", "8.5")
        val language = json.optString("language", "Japanese (Sub/Dub)")
        val category = json.optString("category", "Action")
        val storyline = json.optString("storyline", "")
        val trailer = json.optString("trailer", "")
        val dubType = json.optString("dubType", "")
        val tmdbId = parseStringOrNumber(json, "tmdbId", "")
        val createdAt = json.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = json.optLong("updatedAt", System.currentTimeMillis())

        // Parse genres
        val genres = parseGenres(json)

        // Parse cast
        val cast = parseCast(json)

        // Parse seasons and episodes
        val seasons = parseSeasons(json)

        return AnimeItem(
            id = id,
            title = title,
            poster = poster,
            backdrop = backdrop,
            logo = logo,
            year = year,
            rating = rating,
            language = language,
            category = category,
            type = AnimeType.SERIES,
            storyline = storyline,
            seasons = seasons,
            trailer = trailer,
            genres = genres,
            cast = cast,
            dubType = dubType,
            tmdbId = tmdbId,
            isTrending = rating.toDoubleOrNull()?.let { it >= 8.0 } ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // Parser for Movie
    private fun parseAnimeMovie(id: String, json: JSONObject): AnimeItem {
        val title = json.optString("title", "Untitled Movie")
        val poster = json.optString("poster", "")
        val backdrop = json.optString("backdrop", poster)
        val logo = json.optString("logo", "")
        val year = parseStringOrNumber(json, "year", "2024")
        val rating = parseStringOrNumber(json, "rating", "8.5")
        val language = json.optString("language", "Japanese (Sub/Dub)")
        val category = json.optString("category", "Action")
        val storyline = json.optString("storyline", "")
        val trailer = json.optString("trailer", "")
        val dubType = json.optString("dubType", "")
        val tmdbId = parseStringOrNumber(json, "tmdbId", "")
        val downloadLink = json.optString("downloadLink", "")

        val movieLink = json.optString("movieLink", "")
        val movieLink480 = json.optString("movieLink480", movieLink)
        val movieLink720 = json.optString("movieLink720", movieLink)
        val movieLink1080 = json.optString("movieLink1080", movieLink)
        val movieLink4k = json.optString("movieLink4k", movieLink1080)

        val createdAt = json.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = json.optLong("updatedAt", System.currentTimeMillis())

        val genres = parseGenres(json)
        val cast = parseCast(json)
        val parts = parseMovieParts(json)

        return AnimeItem(
            id = id,
            title = title,
            poster = poster,
            backdrop = backdrop,
            logo = logo,
            year = year,
            rating = rating,
            language = language,
            category = category,
            type = AnimeType.MOVIE,
            storyline = storyline,
            movieLink = movieLink,
            movieLink480 = movieLink480,
            movieLink720 = movieLink720,
            movieLink1080 = movieLink1080,
            movieLink4k = movieLink4k,
            downloadLink = downloadLink,
            parts = parts,
            trailer = trailer,
            genres = genres,
            cast = cast,
            dubType = dubType,
            tmdbId = tmdbId,
            isTrending = rating.toDoubleOrNull()?.let { it >= 8.0 } ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun parseGenres(json: JSONObject): List<String> {
        val result = mutableListOf<String>()
        val genresObj = json.opt("genres")
        if (genresObj is JSONArray) {
            for (i in 0 until genresObj.length()) {
                val g = genresObj.optString(i)
                if (g.isNotBlank()) result.add(g)
            }
        } else if (genresObj is String) {
            result.addAll(genresObj.split(",").map { it.trim() }.filter { it.isNotBlank() })
        }
        return result
    }

    private fun parseCast(json: JSONObject): List<CastMember> {
        val result = mutableListOf<CastMember>()
        val castArray = json.optJSONArray("cast")
        if (castArray != null) {
            for (i in 0 until castArray.length()) {
                val member = castArray.optJSONObject(i) ?: continue
                result.add(
                    CastMember(
                        name = member.optString("name", "Actor"),
                        character = member.optString("character", ""),
                        photo = member.optString("photo", "")
                    )
                )
            }
        }
        return result
    }

    private fun parseMovieParts(json: JSONObject): List<MoviePart> {
        val result = mutableListOf<MoviePart>()
        val partsArray = json.optJSONArray("parts")
        if (partsArray != null) {
            for (i in 0 until partsArray.length()) {
                val partObj = partsArray.optJSONObject(i) ?: continue
                result.add(
                    MoviePart(
                        partNumber = partObj.optInt("partNumber", i + 1),
                        title = partObj.optString("title", "Part ${i + 1}"),
                        link = partObj.optString("link", ""),
                        link480 = partObj.optString("link480", null),
                        link720 = partObj.optString("link720", null),
                        link1080 = partObj.optString("link1080", null),
                        link4k = partObj.optString("link4k", null)
                    )
                )
            }
        }
        return result
    }

    private fun parseSeasons(json: JSONObject): List<Season> {
        val result = mutableListOf<Season>()
        val seasonsObj = json.opt("seasons")

        if (seasonsObj is JSONArray) {
            for (i in 0 until seasonsObj.length()) {
                val seasonObj = seasonsObj.optJSONObject(i) ?: continue
                val name = seasonObj.optString("name", "Season ${i + 1}")
                val seasonNumber = seasonObj.optInt("seasonNumber", i + 1)
                val episodes = parseEpisodes(seasonObj.opt("episodes"))
                result.add(Season(name = name, seasonNumber = seasonNumber, episodes = episodes))
            }
        } else if (seasonsObj is JSONObject) {
            val keys = seasonsObj.keys()
            var index = 1
            while (keys.hasNext()) {
                val key = keys.next()
                val seasonObj = seasonsObj.optJSONObject(key) ?: continue
                val name = seasonObj.optString("name", "Season $index")
                val seasonNumber = seasonObj.optInt("seasonNumber", index)
                val episodes = parseEpisodes(seasonObj.opt("episodes"))
                result.add(Season(name = name, seasonNumber = seasonNumber, episodes = episodes))
                index++
            }
        }
        return result
    }

    private fun parseEpisodes(episodesObj: Any?): List<Episode> {
        val result = mutableListOf<Episode>()
        if (episodesObj is JSONArray) {
            for (i in 0 until episodesObj.length()) {
                val epObj = episodesObj.optJSONObject(i) ?: continue
                val epNum = epObj.optInt("episodeNumber", i + 1)
                val title = epObj.optString("title", "Episode $epNum")
                val link = epObj.optString("link", "")
                val link480 = epObj.optString("link480", link)
                val link720 = epObj.optString("link720", link)
                val link1080 = epObj.optString("link1080", link)
                val link4k = epObj.optString("link4k", link1080)
                val downloadLink = epObj.optString("downloadLink", null)
                val isPremium = epObj.optBoolean("isPremium", false)

                val audioTracks = parseAudioTracks(epObj.optJSONArray("audioTracks"))
                val subtitleTracks = parseSubtitleTracks(epObj.optJSONArray("subtitleTracks"))

                result.add(
                    Episode(
                        episodeNumber = epNum,
                        title = title,
                        link = link,
                        link480 = link480,
                        link720 = link720,
                        link1080 = link1080,
                        link4k = link4k,
                        downloadLink = downloadLink,
                        isPremium = isPremium,
                        audioTracks = audioTracks,
                        subtitleTracks = subtitleTracks
                    )
                )
            }
        } else if (episodesObj is JSONObject) {
            val keys = episodesObj.keys()
            var index = 1
            while (keys.hasNext()) {
                val key = keys.next()
                val epObj = episodesObj.optJSONObject(key) ?: continue
                val epNum = epObj.optInt("episodeNumber", index)
                val title = epObj.optString("title", "Episode $epNum")
                val link = epObj.optString("link", "")
                val link480 = epObj.optString("link480", link)
                val link720 = epObj.optString("link720", link)
                val link1080 = epObj.optString("link1080", link)
                val link4k = epObj.optString("link4k", link1080)
                val downloadLink = epObj.optString("downloadLink", null)
                val isPremium = epObj.optBoolean("isPremium", false)

                val audioTracks = parseAudioTracks(epObj.optJSONArray("audioTracks"))
                val subtitleTracks = parseSubtitleTracks(epObj.optJSONArray("subtitleTracks"))

                result.add(
                    Episode(
                        episodeNumber = epNum,
                        title = title,
                        link = link,
                        link480 = link480,
                        link720 = link720,
                        link1080 = link1080,
                        link4k = link4k,
                        downloadLink = downloadLink,
                        isPremium = isPremium,
                        audioTracks = audioTracks,
                        subtitleTracks = subtitleTracks
                    )
                )
                index++
            }
        }
        return result
    }

    private fun parseAudioTracks(jsonArray: JSONArray?): List<AudioTrack> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<AudioTrack>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            list.add(
                AudioTrack(
                    language = obj.optString("language", "ja"),
                    name = obj.optString("name", "Japanese"),
                    link = obj.optString("link", null)
                )
            )
        }
        return list
    }

    private fun parseSubtitleTracks(jsonArray: JSONArray?): List<SubtitleTrack> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<SubtitleTrack>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            list.add(
                SubtitleTrack(
                    language = obj.optString("language", "en"),
                    label = obj.optString("label", "English"),
                    src = obj.optString("src", null)
                )
            )
        }
        return list
    }

    private fun parseStringOrNumber(json: JSONObject, key: String, default: String): String {
        val opt = json.opt(key) ?: return default
        return opt.toString()
    }
}

