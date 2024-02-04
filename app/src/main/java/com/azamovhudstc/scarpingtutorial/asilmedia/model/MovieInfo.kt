package com.azamovhudstc.scarpingtutorial.asilmedia.model
data class MovieInfo(
    val genre: String,
    val rating: String,
    val title: String,
    val image: String,
    val href: String,
    val quality: List<String>,
    val year: String
)