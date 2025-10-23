package com.azamovhudstc.scarpingtutorial.playimdb

import com.azamovhudstc.scarpingtutorial.helper.M3u8Helper
import com.lagradost.cloudstream3.USER_AGENT
import okhttp3.Interceptor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink

class MoviehabNet : Moviehab() {
    override var mainUrl = "https://play.moviehab.asia"
}

open class Moviehab : ExtractorApi() {
    override var name = "Moviehab"
    override var mainUrl = "https://play.moviehab.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url)
        res.document.select("video#player").let {
            //should redirect first for making it works
            val link = app.get("$mainUrl/${it.select("source").attr("src")}", referer = url).url
            println(link)
        }
    }
}