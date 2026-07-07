package com.example.data.network

import com.example.data.model.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class XtreamClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun login(baseUrl: String, username: String, password: String): Boolean {
        val url = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val requestUrl = "$url/player_api.php?username=$username&password=$password"
        val request = Request.Builder().url(requestUrl).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body?.string() ?: return false
                val json = JSONObject(body)
                json.has("user_info")
            }
        } catch (e: Exception) {
            false
        }
    }

    fun fetchChannels(
        playlistId: Int,
        baseUrl: String,
        username: String,
        password: String
    ): List<Channel> {
        val channels = mutableListOf<Channel>()
        val url = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl

        // 1. Fetch categories
        val liveCategories = fetchCategories(url, username, password, "get_live_categories")
        val vodCategories = fetchCategories(url, username, password, "get_vod_categories")
        val seriesCategories = fetchCategories(url, username, password, "get_series_categories")

        // 2. Fetch Live Streams
        try {
            val requestUrl = "$url/player_api.php?username=$username&password=$password&action=get_live_streams"
            val request = Request.Builder().url(requestUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val name = item.optString("name")
                        val streamId = item.optString("stream_id")
                        val categoryId = item.optString("category_id")
                        val icon = item.optString("stream_icon")
                        val categoryName = liveCategories[categoryId] ?: "Live"

                        val streamUrl = "$url/live/$username/$password/$streamId.ts"

                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                name = name,
                                streamUrl = streamUrl,
                                streamType = "LIVE",
                                categoryName = categoryName,
                                iconUrl = icon,
                                streamId = streamId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fetch Movie (VOD) Streams
        try {
            val requestUrl = "$url/player_api.php?username=$username&password=$password&action=get_vod_streams"
            val request = Request.Builder().url(requestUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val name = item.optString("name")
                        val streamId = item.optString("stream_id")
                        val categoryId = item.optString("category_id")
                        val icon = item.optString("stream_icon")
                        val ext = item.optString("container_extension", "mp4")
                        val categoryName = vodCategories[categoryId] ?: "Movies"

                        val streamUrl = "$url/movie/$username/$password/$streamId.$ext"

                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                name = name,
                                streamUrl = streamUrl,
                                streamType = "MOVIE",
                                categoryName = categoryName,
                                iconUrl = icon,
                                streamId = streamId,
                                containerExtension = ext
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Fetch Series Streams
        try {
            val requestUrl = "$url/player_api.php?username=$username&password=$password&action=get_series"
            val request = Request.Builder().url(requestUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val name = item.optString("name")
                        val seriesId = item.optString("series_id")
                        val categoryId = item.optString("category_id")
                        val icon = item.optString("cover")
                        val categoryName = seriesCategories[categoryId] ?: "Series"

                        val streamUrl = "$url/series/$username/$password/$seriesId.mp4"

                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                name = name,
                                streamUrl = streamUrl,
                                streamType = "SERIES",
                                categoryName = categoryName,
                                iconUrl = icon,
                                streamId = seriesId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return channels
    }

    private fun fetchCategories(
        baseUrl: String,
        username: String,
        password: String,
        action: String
    ): Map<String, String> {
        val categories = mutableMapOf<String, String>()
        val requestUrl = "$baseUrl/player_api.php?username=$username&password=$password&action=$action"
        val request = Request.Builder().url(requestUrl).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return emptyMap()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val id = item.optString("category_id")
                        val name = item.optString("category_name")
                        if (id.isNotEmpty() && name.isNotEmpty()) {
                            categories[id] = name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return categories
    }
}
