package com.theveloper.pixelplay.utils

import java.io.File
import java.util.Locale

/**
 * Average bitrate (bits per second) of lossless files, computed from file size and duration.
 *
 * ExoPlayer/MediaMetadataRetriever can report a bogus bitrate for FLAC (e.g. 256 kbps),
 * so for FLAC and WAV the real value is derived from the file itself.
 * Returns null for other formats or when it can't be computed, so callers fall back.
 */
object LosslessBitrate {

    fun estimate(path: String?, durationMs: Long): Int? {
        if (path.isNullOrBlank() || durationMs <= 0L) return null
        val lower = path.lowercase(Locale.ROOT)
        if (!lower.endsWith(".flac") && !lower.endsWith(".wav")) return null
        return try {
            val size = File(path).length()
            if (size <= 0L) null else (size * 8000L / durationMs).toInt()
        } catch (e: Exception) {
            null
        }
    }
}
