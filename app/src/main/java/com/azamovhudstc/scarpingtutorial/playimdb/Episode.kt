package com.saikou.sozo_tv.data.model

data class Episode(
    val air_date: String,
    val crew: List<Crew>,
    val episode_number: Int,
    val still_path: String?,
    val vote_average: Double,
    val vote_count: Int
)