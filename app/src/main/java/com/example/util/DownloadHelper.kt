package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.data.model.AnimeItem
import com.example.data.model.AnimeType
import com.example.data.model.Episode
import com.example.data.model.MoviePart

object DownloadHelper {

    /**
     * Cleans a string to be safe for filenames on Android / Linux / FAT32 systems.
     */
    fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[\\\\/:*?\"<>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Generates a clean, well-formatted filename according to user specification:
     * e.g. "Solo Leveling - S01E02 - The Awakening - [1080p].mp4"
     * or for movies: "Demon Slayer Infinity Castle - Part 1 - [1080p].mp4"
     */
    fun generateFormattedFileName(
        animeTitle: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        episodeTitle: String? = null,
        quality: String = "1080p",
        partNumber: Int? = null,
        isMovie: Boolean = false
    ): String {
        val cleanTitle = sanitizeFileName(animeTitle)
        val cleanQuality = sanitizeFileName(quality.replace("FHD", "").replace("UHD", "").replace("HD", "").trim())
        val extension = ".mp4"

        return if (isMovie) {
            val partSuffix = if (partNumber != null && partNumber > 0) " - Part $partNumber" else ""
            "$cleanTitle$partSuffix - [$cleanQuality]$extension"
        } else {
            val sNum = String.format("%02d", seasonNumber ?: 1)
            val eNum = String.format("%02d", episodeNumber ?: 1)
            val epTitleSuffix = if (!episodeTitle.isNullOrBlank() && !episodeTitle.startsWith("Episode", ignoreCase = true)) {
                " - ${sanitizeFileName(episodeTitle)}"
            } else ""
            "$cleanTitle - S${sNum}E$eNum$epTitleSuffix - [$cleanQuality]$extension"
        }
    }

    /**
     * Initiates a download using Android's system DownloadManager with the formatted filename.
     */
    fun startDownload(
        context: Context,
        url: String,
        anime: AnimeItem,
        episode: Episode? = null,
        seasonNumber: Int = 1,
        part: MoviePart? = null,
        quality: String = "1080p",
        userPrefsRepo: com.example.data.repository.UserPreferencesRepository? = null
    ): Long {
        if (url.isBlank()) {
            Toast.makeText(context, "Download link is not available", Toast.LENGTH_SHORT).show()
            return -1L
        }

        try {
            val isMovie = anime.type == AnimeType.MOVIE
            val fileName = generateFormattedFileName(
                animeTitle = anime.title,
                seasonNumber = seasonNumber,
                episodeNumber = episode?.episodeNumber,
                episodeTitle = episode?.title,
                quality = quality,
                partNumber = part?.partNumber,
                isMovie = isMovie
            )

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                Toast.makeText(context, "Download service is not available", Toast.LENGTH_SHORT).show()
                return -1L
            }

            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setDescription("Downloading $fileName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("video/mp4")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Downloading: $fileName", Toast.LENGTH_LONG).show()

            // Register in local repository for offline library
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val expectedFile = java.io.File(downloadDir, fileName)

            userPrefsRepo?.addDownload(
                com.example.data.model.DownloadedEpisode(
                    animeId = anime.id,
                    animeTitle = anime.title,
                    episodeNumber = episode?.episodeNumber ?: (part?.partNumber ?: 1),
                    episodeTitle = episode?.title ?: (part?.title ?: "Full Movie"),
                    poster = anime.poster,
                    quality = quality,
                    sizeMb = when {
                        quality.contains("4k", true) -> 450.0
                        quality.contains("1080", true) -> 240.0
                        quality.contains("720", true) -> 120.0
                        else -> 75.0
                    },
                    downloadedAt = System.currentTimeMillis(),
                    filePath = expectedFile.absolutePath,
                    isMovie = isMovie,
                    seasonName = "Season $seasonNumber"
                )
            )

            return downloadId
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }

    /**
     * Finds the local file if it exists on disk.
     */
    fun getLocalFile(download: com.example.data.model.DownloadedEpisode): java.io.File? {
        if (download.filePath.isNotBlank()) {
            val f = java.io.File(download.filePath)
            if (f.exists()) return f
        }
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = downloadDir.listFiles() ?: return null
        val cleanTitle = sanitizeFileName(download.animeTitle)
        return files.find { it.name.contains(cleanTitle, ignoreCase = true) && it.name.endsWith(".mp4", ignoreCase = true) }
    }
}
