package com.azamovhudstc.scarpingtutorial.idub

data class TVShow(
    val title: String,
    val date: String,
    val year: String,
    val country: String,
    val age: String,
    val description: String,
    val ratingKP: String,
    val ratingIMDb: String,
    val episodeCount: String,
    val seasonCount: String,
    val href: String,
    val imageLink: String

) {
    override fun toString(): String {
        return """
            Title: $title
            Date: $date
            Year: $year
            Country: $country
            Age: $age
            Description: $description
            KP Rating: $ratingKP
            IMDb Rating: $ratingIMDb
            Episode Count: $episodeCount
            Season Count: $seasonCount
            Href: $href
            Image Link: $imageLink
            ---------------------
        """.trimIndent()
    }
}