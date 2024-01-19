package com.azamovhudstc.scarpingtutorial.aniwave

import android.os.Parcelable
import androidx.annotation.Keep

@Keep
data class AnimePlayingDetails(
    val animeName: String,
    val animeUrl: String,
    var animeEpisodeIndex: String,
    val animeEpisodeMap: HashMap<String, String>,
    val animeTotalEpisode: String,
    val epType: String
)