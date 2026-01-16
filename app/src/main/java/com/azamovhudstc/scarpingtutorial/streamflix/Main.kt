package com.azamovhudstc.scarpingtutorial.streamflix

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        // Movie uchun
//        val movie = Video.Type.Movie("tt1234567")
//        val server = VidsrcToExtractor().server(movie)
//
//        val video = VidsrcToExtractor().extract("https://playimdb.com/embed/movie/385687")
//        video.apply {
//            println(this.source)
//        }
        val primeSrcExtractor = PrimeSrcExtractor()
        val movie =Video.Type.Movie("385687")
        val server = primeSrcExtractor.server(movie).src
        println(server)
        val video = primeSrcExtractor.extract("https://primesrc.me/api/v1/l?key=nnyTE")

        println(video.source)
        println(video.headers)

    }
}