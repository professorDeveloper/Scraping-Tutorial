package com.azamovhudstc.scarpingtutorial.streamflix

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val seriesEp1 = Video.Type.Episode(Video.Type.TvShow("119051"), Video.Type.Season(1), 1)
    runBlocking {

        val vidEasy = VideasyExtractor()
        vidEasy.server(seriesEp1, "de").let {
            vidEasy.extract(it?.src ?: "").let { video ->
                println("VidEasy: ${video.source}")
//                println("VidEasy: ${video.subtitles}")
                println("VidEasy: ${video.headers}")
            }
        }
    }
}