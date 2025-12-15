package com.azamovhudstc.scarpingtutorial.vidsrc

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.O)
fun main() {
    val vidsrc = VidsrcSource()
    runBlocking {

        vidsrc.invokeVidsrccc(
            id = 550        ) {
            println(it.name)
            println(it.source)
            println(it.url)
        }
//        vidsrc.invokeVidsrccc(
//            id = 1399,
//            season = 1,
//            episode = 1
//        ) {
//            println(it.name)
//            println(it.source)
//            println(it.url)
//        }
    }

}
