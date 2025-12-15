package com.azamovhudstc.scarpingtutorial.vidsrc

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.azamovhudstc.scarpingtutorial.hianime.ExtractorLink
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.Jsoup

class VidsrcSource {

    private val BASE_URL = "https://vidsrc.cc"
    val vidsrctoAPI = "https://vidsrc.cc"

    /**
     * MOVIE yoki TV uchun serverlar ro'yxati
     *
     * movie → id
     * tv → id + season + episode
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getServers(
        id: Int,
        season: Int? = null,
        episode: Int? = null
    ): List<VidsrcServer> {
        return try {
            val embedUrl = if (season == null) {
                "$BASE_URL/v2/embed/movie/$id?autoPlay=false"
            } else {
                "$BASE_URL/v2/embed/tv/$id/$season/$episode?autoPlay=false"
            }

            val doc = Utils.getJsoup(embedUrl)

            val scriptText = doc.select("script").joinToString("\n") { it.html() }

            val variables = extractJsVariables(scriptText)

            val v = variables["v"] ?: ""
            val userId = variables["userId"] ?: ""
            val imdbId = variables["imdbId"] ?: ""
            val movieId = variables["movieId"] ?: ""
            val movieType = variables["movieType"] ?: ""

            val vrf = generateVrfAES(movieId, userId)
            val apiUrl = if (season == null) {
                "$BASE_URL/api/$id/servers?id=$id&type=$movieType&v=$v&vrf=$vrf&imdbId=$imdbId"
            } else {
                "$BASE_URL/api/$id/servers?id=$id&type=$movieType&season=$season&episode=$episode&v=$v&vrf=$vrf&imdbId=$imdbId"
            }

            val json = Utils.get(apiUrl)
            println(apiUrl)
            Gson().fromJson(json, VidsrcServersResponse::class.java).data

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Server hash → m3u8 source
     */
    fun getVideoSource(serverHash: String): String? {
        return try {
            val json = Utils.get("$BASE_URL/api/source/$serverHash")
            Gson().fromJson(json, VidsrcSourceResponse::class.java)
                .data
                .source
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun invokeVidsrccc(
        id: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit,
    ) {
        val url = if (season == null) {
            "$vidsrctoAPI/v2/embed/movie/$id?autoPlay=false"
        } else {
            "$vidsrctoAPI/v2/embed/tv/$id/$season/$episode?autoPlay=false"
        }

        println("EMBED → $url")

        val doc = app.get(url).document.toString()

        val regex = Regex("""var\s+(\w+)\s*=\s*(?:"([^"]*)"|(\w+));""")
        val variables = mutableMapOf<String, String>()

        regex.findAll(doc).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].ifEmpty { match.groupValues[3] }
            variables[key] = value
        }

        val vvalue = variables["v"]
        val userId = variables["userId"]
        val imdbId = variables["imdbId"] ?: ""
        val movieId = variables["movieId"]
        val movieType = variables["movieType"] ?: ""
        println(userId)
        println(vvalue)
        println(imdbId)
        println(movieId)
        println(movieType)
        if (vvalue == null || userId == null || movieId == null) {
            println("JS VARS MISSING → $variables")
            return
        }

        val vrf = generateVrfAES(movieId, userId)
        println("VRF → $vrf")

        val apiurl = if (season == null) {
            "$vidsrctoAPI/api/$id/servers?id=$id&type=$movieType&v=$vvalue&vrf=$vrf&imdbId=$imdbId"
        } else {
            "$vidsrctoAPI/api/$id/servers?id=$id&type=$movieType&season=$season&episode=$episode&v=$vvalue&vrf=$vrf&imdbId=$imdbId"
        }

        println("SERVERS API → $apiurl")

        val servers = app.get(apiurl).parsedSafe<Vidsrcccservers>()

        if (servers == null || !servers.success || servers.data.isEmpty()) {
            println(" NO SERVERS → $servers")
            return
        }

        servers.data.forEach {
            val servername = it.name

            val iframe = app.get("$vidsrctoAPI/api/source/${it.hash}")
                .parsedSafe<Vidsrcccm3u8>()
                ?.data
                ?.source

            if (iframe.isNullOrBlank()) return@forEach
            if (iframe.contains("vidbox", true)) return@forEach

            callback.invoke(
                newExtractorLink(
                    "Vidsrc",
                    "⌜ Vidsrc ⌟ | [$servername]",
                    iframe,
                )
            )
        }
    }

    suspend fun newExtractorLink(
        source: String,
        name: String,
        url: String,
        type: ExtractorLinkType? = null,
        initializer: suspend com.lagradost.cloudstream3.utils.ExtractorLink.() -> Unit = { }
    ): com.lagradost.cloudstream3.utils.ExtractorLink {

        @Suppress("DEPRECATION_ERROR")
        val builder =
            com.lagradost.cloudstream3.utils.ExtractorLink(
                source = source,
                name = name,
                url = url,
                referer = "",
                quality = 1080,
                true,
                mapOf()
            )

        builder.initializer()
        return builder
    }

    private fun extractJsVariables(script: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""var\s+(\w+)\s*=\s*(?:"([^"]*)"|(\w+));""")

        regex.findAll(script).forEach {
            val key = it.groupValues[1]
            val value = it.groupValues[2].ifEmpty { it.groupValues[3] }
            map[key] = value
        }
        return map
    }
}
