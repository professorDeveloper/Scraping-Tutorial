package com.azamovhudstc.scarpingtutorial.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.LoadResponse.Companion.getImdbId
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        val allMovieLandProvider = AllMovieLandProvider()
//        allMovieLandProvider.getMainPage(1, MainPageRequest("Info","https://allmovieland.ac/",true)).let {
//            println(it)
//        }
        allMovieLandProvider.load("https://allmovieland.ac/6806-hitpig.html").let {
          allMovieLandProvider.newMovieSearchResponse(it?.name?:"",it?.url?:"",it?.type?: TvType.Movie){

          }
        }
    }

}