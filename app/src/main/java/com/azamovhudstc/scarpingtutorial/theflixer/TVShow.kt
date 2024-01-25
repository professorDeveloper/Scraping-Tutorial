package com.azamovhudstc.scarpingtutorial.theflixer

data class TVShow(
    val dataId: String,
    val title: String,
    val year: String,
    val type: String,
    val bannerUrl:String,
    val ratingInfo: RatingInfo,
    val posterUrl: String,
    val overview: String,
    val released: String,
    val genres: List<String>,
    val casts: List<String>,
    val duration: String,
    val country: String,
    val production: String
)