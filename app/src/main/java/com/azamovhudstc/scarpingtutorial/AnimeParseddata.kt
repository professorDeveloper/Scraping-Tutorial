package com.azamovhudstc.scarpingtutorial

data class AnimeParseData(
    val title: String,
    val href: String,
    val image: String
) {
    override fun toString(): String {
        return "Title=$title\nHref=$href,\nImage=$image"
    }
}