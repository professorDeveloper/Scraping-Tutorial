package com.azamovhudstc.scarpingtutorial.theflixer

data class TVShow(
    val dataId: String,
    val title: String,
    val year: String,
    val type: String,
    val bannerUrl: String,
    val ratingInfo: RatingInfo,
    val posterUrl: String,
    val overview: String,
    val released: String,
    val genres: List<String>,
    val casts: List<String>,
    val duration: String,
    val country: String,
    val production: String
) {
    override fun toString(): String {
        return """
            |TV Show:
            |  Data ID: $dataId
            |  Title: $title
            |  Year: $year
            |  Type: $type
            |  Banner URL: $bannerUrl
            |  Rating Info: $ratingInfo
            |  Poster URL: $posterUrl
            |  Overview: $overview
            |  Released: $released
            |  Genres: $genres
            |  Casts: $casts
            |  Duration: $duration
            |  Country: $country
            |  Production: $production
        """.trimMargin()
    }
}