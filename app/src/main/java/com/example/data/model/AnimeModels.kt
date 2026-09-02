package com.example.data.model

data class AudioTrack(
    val language: String = "ja",
    val name: String = "Japanese (Original)",
    val link: String? = null
)

data class SubtitleTrack(
    val language: String = "en",
    val label: String = "English",
    val src: String? = null
)

data class MoviePart(
    val partNumber: Int = 1,
    val title: String? = null,
    val link: String = "",
    val link480: String? = null,
    val link720: String? = null,
    val link1080: String? = null,
    val link4k: String? = null
)

data class Episode(
    val episodeNumber: Int = 1,
    val title: String = "",
    val link: String = "",
    val link480: String? = null,
    val link720: String? = null,
    val link1080: String? = null,
    val link4k: String? = null,
    val downloadLink: String? = null,
    val isPremium: Boolean = false,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList()
)

data class Season(
    val name: String = "Season 1",
    val seasonNumber: Int = 1,
    val episodes: List<Episode> = emptyList()
)

data class CastMember(
    val name: String = "",
    val character: String? = null,
    val photo: String? = null
)

enum class AnimeType {
    SERIES,
    MOVIE
}

data class AnimeItem(
    val id: String,
    val title: String,
    val poster: String = "",
    val backdrop: String = "",
    val logo: String? = null,
    val year: String = "2024",
    val rating: String = "8.5",
    val language: String = "Japanese (Sub/Dub)",
    val category: String = "Action",
    val type: AnimeType = AnimeType.SERIES,
    val storyline: String = "",
    val seasons: List<Season> = emptyList(),
    val movieLink: String? = null,
    val movieLink480: String? = null,
    val movieLink720: String? = null,
    val movieLink1080: String? = null,
    val movieLink4k: String? = null,
    val downloadLink: String? = null,
    val parts: List<MoviePart> = emptyList(),
    val trailer: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val dubType: String? = null,
    val tmdbId: String? = null,
    val isPremium: Boolean = false,
    val isTrending: Boolean = false,
    val views: String = "120K",
    val likesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class HeroSlide(
    val id: String,
    val animeId: String,
    val title: String,
    val subtitle: String,
    val backdrop: String,
    val rating: String,
    val year: String,
    val category: String
)

data class LiveChannel(
    val id: String,
    val name: String,
    val category: String,
    val streamUrl: String,
    val currentShow: String = "Live Streaming 24/7",
    val viewersCount: Int = 1200,
    val isLive: Boolean = true,
    val posterUrl: String = "",
    val logo: String = "",
    val banner: String = "",
    val order: Int = 1
)

data class WeeklyScheduleItem(
    val id: String,
    val seriesId: String,
    val title: String,
    val day: String,
    val poster: String,
    val updatedAt: Long = 0L
)

data class DailyTask(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val isCompleted: Boolean = false,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val iconName: String = "star"
)

data class WatchHistoryItem(
    val animeId: String,
    val episodeNumber: Int = 1,
    val seasonName: String = "Season 1",
    val title: String,
    val poster: String,
    val lastPositionMs: Long = 0,
    val totalDurationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class ServerItem(
    val id: String = "1",
    val name: String = "RS FR 01",
    val domain: String = "http://51.75.118.79:20044",
    val proxy: String = "",
    val prefix: String = "",
    val isDefault: Boolean = false,
    val locked: Boolean = false
)

data class DownloadedEpisode(
    val animeId: String,
    val animeTitle: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val poster: String,
    val quality: String = "1080p FHD",
    val sizeMb: Double = 180.0,
    val downloadedAt: Long = System.currentTimeMillis(),
    val filePath: String = "",
    val isMovie: Boolean = false,
    val seasonName: String = "Season 1"
)

data class VipTier(
    val id: String,
    val name: String,
    val durationText: String,
    val coinPrice: Int,
    val bdtPrice: Int,
    val perks: List<String>,
    val isPopular: Boolean = false
)

data class CommentItem(
    val id: String,
    val text: String,
    val timestamp: Long,
    val userId: String,
    val userName: String
)

data class RedeemCodeItem(
    val id: String,
    val code: String,
    val days: Int,
    val note: String = "",
    val used: Boolean = false,
    val usedAt: Long? = null,
    val usedBy: String? = null,
    val createdAt: Long = 0L
)

