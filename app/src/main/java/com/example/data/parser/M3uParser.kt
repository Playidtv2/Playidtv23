package com.example.data.parser

import com.example.data.model.Channel
import java.io.BufferedReader
import java.io.StringReader

object M3uParser {
    fun parse(m3uContent: String, playlistId: Int): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(m3uContent))
        var line: String? = reader.readLine()

        var currentName = ""
        var currentLogo = ""
        var currentGroup = "Default"

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse EXTINF line
                currentName = parseTag(trimmed, "tvg-name")
                if (currentName.isEmpty()) {
                    // Try parsing after comma if tvg-name is empty
                    val commaIndex = trimmed.lastIndexOf(',')
                    if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        currentName = trimmed.substring(commaIndex + 1).trim()
                    }
                }
                currentLogo = parseTag(trimmed, "tvg-logo")
                currentGroup = parseTag(trimmed, "group-title")
                if (currentGroup.isEmpty()) {
                    currentGroup = "Other"
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This line is the stream URL
                if (currentName.isEmpty()) {
                    currentName = "Channel ${channels.size + 1}"
                }
                
                // Classify streamType
                val type = when {
                    trimmed.contains("/movie/") || trimmed.contains("/movies/") -> "MOVIE"
                    trimmed.contains("/series/") -> "SERIES"
                    else -> "LIVE"
                }

                channels.add(
                    Channel(
                        playlistId = playlistId,
                        name = currentName,
                        streamUrl = trimmed,
                        streamType = type,
                        categoryName = currentGroup,
                        iconUrl = currentLogo
                    )
                )

                // Reset for next
                currentName = ""
                currentLogo = ""
                currentGroup = "Other"
            }
            line = reader.readLine()
        }
        return channels
    }

    private fun parseTag(line: String, tagName: String): String {
        val key = "$tagName=\""
        val start = line.indexOf(key)
        if (start == -1) return ""
        val startValue = start + key.length
        val end = line.indexOf('"', startValue)
        if (end == -1) return ""
        return line.substring(startValue, end)
    }
}
