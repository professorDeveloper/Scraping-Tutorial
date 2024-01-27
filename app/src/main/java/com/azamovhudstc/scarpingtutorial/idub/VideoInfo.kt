package com.azamovhudstc.scarpingtutorial.idub

data class VideoInfo(val title: String, val playerUrl: String, val trailerUrl: String) {
    override fun toString(): String {
        return """
            Video Title: $title
            Player URL: ${convertMoverEmbedToMp4(playerUrl)}
            Trailer URL: $trailerUrl
        """.trimIndent()
    }
}


fun convertMoverEmbedToMp4(embedUrl: String): String {
    val regex = Regex("https://mover\\.uz/video/embed/(\\w+)")
    val matchResult = regex.find(embedUrl)

    return if (matchResult != null) {
        val videoEndpoint = matchResult.groupValues[1]
        "https://mover.uz/${videoEndpoint}_m.mp4".replace(
            "https://mover.uz", "https://v.mover.uz"
        )
    } else {
        // Return the original URL if it doesn't match the expected pattern
        embedUrl
    }
}
