package com.azamovhudstc.scarpingtutorial.streamflix

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        // Movie uchun
        val movie = Video.Type.Episode(Video.Type.TvShow("119051"), Video.Type.Season(1), 1)
//        val server = VidsrcToExtractor().server(movie)
//
//        val video = VidsrcToExtractor().extract("https://playimdb.com/embed/movie/385687")
//        video.apply {
//            println(this.source)
//        }
//        val primeSrcExtractor = PrimeSrcExtractor()
//        val server = primeSrcExtractor.server(movie).src
//        println(server)
//        val video = primeSrcExtractor.extract("https://primesrc.me/api/v1/l?key=1QGxc")
//
//        println(video.source)
//        println(video.headers)

        val extractor = VixSrcExtractor()
        val server = extractor.server(movie)
        extractor.extract(server.src).let {
            println(server.src)
        }
    }
}