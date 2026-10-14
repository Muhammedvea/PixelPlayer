package com.theveloper.pixelplay.data.network.deezer

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fetches album cover art for a track from Deezer's public search API (no API key needed).
 * Returns JPEG bytes, or null if nothing was found / the request failed.
 */
object DeezerCoverFetcher {

    suspend fun fetchCover(artist: String, title: String, album: String?): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val cleanArtist = artist
                    .split(",", "&", ";", "/", " feat", " ft.", " x ")
                    .first()
                    .trim()
                val cleanTitle = title
                    .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
                    .trim()
                if (cleanTitle.isEmpty()) return@withContext null

                val queries = listOf(
                    "artist:\"$cleanArtist\" track:\"$cleanTitle\"",
                    "$cleanArtist $cleanTitle"
                )
                for (query in queries) {
                    val url = "https://api.deezer.com/search?limit=10&q=" +
                        URLEncoder.encode(query, "UTF-8")
                    val body = httpGet(url)?.let { String(it, Charsets.UTF_8) } ?: continue
                    val data = JSONObject(body).optJSONArray("data") ?: continue

                    var bestCover: String? = null
                    for (i in 0 until data.length()) {
                        val albumObj = data.getJSONObject(i).optJSONObject("album") ?: continue
                        val cover = albumObj.optString("cover_xl")
                        val md5 = albumObj.optString("md5_image")
                        if (cover.isBlank() || md5.isBlank()) continue
                        if (bestCover == null) bestCover = cover
                        if (!album.isNullOrBlank() &&
                            albumObj.optString("title").equals(album, ignoreCase = true)
                        ) {
                            bestCover = cover
                            break
                        }
                    }
                    if (bestCover != null) {
                        val bytes = httpGet(bestCover)
                        if (bytes != null && bytes.isNotEmpty()) return@withContext bytes
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

    private fun httpGet(url: String): ByteArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "PixelPlayer")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { it.readBytes() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }
}
