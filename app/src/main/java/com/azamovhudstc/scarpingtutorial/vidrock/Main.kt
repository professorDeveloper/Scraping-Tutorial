package com.azamovhudstc.scarpingtutorial.vidrock

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val source = VidRockSource()
    runBlocking {
        source.invokevidrock(550)

    }
}