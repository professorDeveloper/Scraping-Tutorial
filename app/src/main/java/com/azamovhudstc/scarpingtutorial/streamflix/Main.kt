package com.azamovhudstc.scarpingtutorial.streamflix

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val seriesEp1 = Video.Type.Episode(Video.Type.TvShow("106379"), Video.Type.Season(1), 1)
    runBlocking {

        val vidKing = VidKingExtractor()
        vidKing.server(seriesEp1).let {
            vidKing.extract(it.src)
        }
    }
}