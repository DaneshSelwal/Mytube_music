package com.mark1.mytubemusic.util

object YouTubeUrlParser {
    private val urlPattern = Regex("""https?://[^\s]+""")
    private val youtubeHostPattern = Regex("""https?://(?:www\.|m\.|music\.)?(?:youtube\.com|youtu\.be)(?:/.*|$)""", RegexOption.IGNORE_CASE)

    fun extractUrl(text: String): String? {
        return urlPattern.find(text)?.value
    }

    fun isYouTubeUrl(url: String): Boolean {
        return youtubeHostPattern.matches(url)
    }

    fun extractVideoId(url: String): String? {
        // 1. Short links (youtu.be/ID)
        val shortRegex = Regex("""youtu\.be/([a-zA-Z0-9_-]{11})""")
        val matchShort = shortRegex.find(url)
        if (matchShort != null) {
            return matchShort.groupValues[1]
        }
        
        // 2. Query param (v=ID)
        val queryRegex = Regex("[?&]v=([a-zA-Z0-9_-]{11})")
        val matchQuery = queryRegex.find(url)
        if (matchQuery != null) {
            return matchQuery.groupValues[1]
        }
        
        // 3. Path patterns (/embed/ID, /shorts/ID)
        val pathRegex = Regex("/(?:embed|shorts|v)/([a-zA-Z0-9_-]{11})")
        val matchPath = pathRegex.find(url)
        if (matchPath != null) {
            return matchPath.groupValues[1]
        }
        
        return null
    }
}
