package com.azamovhudstc.scarpingtutorial.anime_pahe

import android.net.Uri
import com.azamovhudstc.scarpingtutorial.model.AnimePaheData
import com.azamovhudstc.scarpingtutorial.model.EpisodeData
import com.azamovhudstc.scarpingtutorial.theflixer.Episode
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.azamovhudstc.scarpingtutorial.uzmovi.JsUnpacker
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.select.Elements
import java.io.Serializable
import java.util.regex.Pattern


class AnimePahe {
    val name = "AnimePahe"
    val saveName = "anime_pahe_hu"
    private val hostUrl = "https://animepahe.ru/"
    val malSyncBackupName = "animepahe"
    val isDubAvailableSeparately = false


    suspend fun search(query: String): List<ShowResponse> {
        val firstSpaceIndex = query.indexOf(" ")
        val formattedQuery = if (firstSpaceIndex != -1) {
            query.substring(0, firstSpaceIndex).replace(" ", "%")
        } else {
            query
        }
        val requests = Requests(httpClient, responseParser = parser)

        val list = mutableListOf<ShowResponse>()
        println("${hostUrl}api?m=search&q=$formattedQuery")
        requests.get(
            "${hostUrl}api?m=search&q=$query", mapOf(
                "dnt" to "1",
                "Cookie" to "\n" +
                        "__ddg1_=Yhqnq62nxbM5uT9LNXBU; SERVERID=janna; latest=5633; ann-fakesite=0; res=720; aud=jpn; av1=0; dom3ic8zudi28v8lr6fgphwffqoz0j6c=33161aa3-e5ac-4f93-b315-e3165fddb0bf%3A3%3A1; sb_page_8966b6c0380845137e2f0bc664baf7be=3; sb_count_8966b6c0380845137e2f0bc664baf7be=3; sb_onpage_8966b6c0380845137e2f0bc664baf7be=1; XSRF-TOKEN=eyJpdiI6InV2RGVHeUhMNkxFelAzOG16TnRXa2c9PSIsInZhbHVlIjoiWkQyWTJaODErMnNVREhRdnZ5L0pycG1Sd2hWZkRhcjB6alN6MDZwb3ppOEpTNFpscWljYmRkVHI0RDNDN0ZxYkZIZE5jSTF2SWpjckZSaHhYWkVRZmdHMGgreE1LMlNLZXpPUnREQ3hjQ0NiZ1RZNUEwQ1hXNkxjaEdKdVc3YnAiLCJtYWMiOiJhMDRkOWU3ZjkzZWNjZmMxYTUxNTI0YWIwOTE2NTcxYTUyYWI3NTM4YTgyMzJhYmYyZDc3YjA2NWVlMjBmMDNhIiwidGFnIjoiIn0%3D; laravel_session=eyJpdiI6IlVtQnJPL3habzNObUJmWVpkTEZTTEE9PSIsInZhbHVlIjoiR2ZMditvM0ZvYnVLajArWnVYZllFcEpOUGVXYk95bWRkdXdGcUVMZE9mT0ZvYmpPSEpoMDdNeC9MWjlxMnluVHd4djZ1TGcyOHJxbEdxd013K09wemJiZlcrZHhUZUN5YkJma3pkZXN4ZVZyU0RQY0pvSnc1WHpHTHlDUWpvTE0iLCJtYWMiOiIzZGVjYTM3N2ZiYzc3ODAyOWMyNjAwODU4NWU4YTY0NTgwNjVhNTVjZGM0NjZjM2QxOTM5MzJlZTcwNTEyYzM3IiwidGFnIjoiIn0%3D; __ddgid_=QTDZaHo3uDoGqGuR; __ddgmark_=a9WzMcAyP2KIzfHF; __ddg2_=nslKhTTMfCM10kKQ",
                "Referer" to "https://animepahe.si",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
            )
        )
            .parsed<AnimePaheData>()
            .apply {
                data.onEach {
                    val link = it.session
                    val title = it.title.toString()
                    val cover = it.poster

                    list.add(ShowResponse(title, link ?: "", cover ?: ""))
                }
            }
        return list
    }

    fun getEpisodeVideo(epId: String, id: String): String {
        //https://animepahe.ru/play/${id}/${epId}
        val doc = getJsoup("https://animepahe.ru/play/${id}/${epId}")

        // Script tegini olish
        val scriptContent = doc.select("script")
            .map { it.html() }
            .firstOrNull { it.contains("session") && it.contains("provider") && it.contains("url") }
            ?: ""

        // Regex yordamida qiymatlarni olish
        val sessionRegex = Pattern.compile("""let\s+session\s*=\s*"([^"]+)"""")
        val providerRegex = Pattern.compile("""let\s+provider\s*=\s*"([^"]+)"""")
        val urlRegex = Pattern.compile("""let\s+url\s*=\s*"([^"]+)"""")

        val session =
            sessionRegex.matcher(scriptContent).let { if (it.find()) it.group(1) else null }
        val provider =
            providerRegex.matcher(scriptContent).let { if (it.find()) it.group(1) else null }
        val url = urlRegex.matcher(scriptContent).let { if (it.find()) it.group(1) else null }

        println("Session: $session")
        println("Provider: $provider")
        println("URL: $url")
        return url ?: ""
    }

    suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> = coroutineScope {
        val list = mutableListOf<Episode>()
        val requests = Requests(httpClient, responseParser = parser)

        // Birinchi sahifani olib, jami sahifalar sonini aniqlash
        println("https://animepahe.ru/api?m=release&id=$animeLink&sort=episode_asc&page=1")
        val totalPages = withContext(Dispatchers.IO) {
            requests.get("https://animepahe.ru/api?m=release&id=$animeLink&sort=episode_asc&page=1")
                .parsed<EpisodeData>().last_page
        }

        // Barcha sahifalarni parallel yuklash va natijalarni yig'ish
        val allPagesData = (1..totalPages!!).map { page ->
            async(Dispatchers.IO) {
                requests.get(
                    "https://animepahe.ru/api?m=release&id=$animeLink&sort=episode_asc&page=$page",
                    mapOf(
                        "dnt" to "1",
                        "Cookie" to "\n" +
                                "__ddg1_=Yhqnq62nxbM5uT9LNXBU; SERVERID=janna; latest=5633; ann-fakesite=0; res=720; aud=jpn; av1=0; dom3ic8zudi28v8lr6fgphwffqoz0j6c=33161aa3-e5ac-4f93-b315-e3165fddb0bf%3A3%3A1; sb_page_8966b6c0380845137e2f0bc664baf7be=3; sb_count_8966b6c0380845137e2f0bc664baf7be=3; sb_onpage_8966b6c0380845137e2f0bc664baf7be=1; XSRF-TOKEN=eyJpdiI6InV2RGVHeUhMNkxFelAzOG16TnRXa2c9PSIsInZhbHVlIjoiWkQyWTJaODErMnNVREhRdnZ5L0pycG1Sd2hWZkRhcjB6alN6MDZwb3ppOEpTNFpscWljYmRkVHI0RDNDN0ZxYkZIZE5jSTF2SWpjckZSaHhYWkVRZmdHMGgreE1LMlNLZXpPUnREQ3hjQ0NiZ1RZNUEwQ1hXNkxjaEdKdVc3YnAiLCJtYWMiOiJhMDRkOWU3ZjkzZWNjZmMxYTUxNTI0YWIwOTE2NTcxYTUyYWI3NTM4YTgyMzJhYmYyZDc3YjA2NWVlMjBmMDNhIiwidGFnIjoiIn0%3D; laravel_session=eyJpdiI6IlVtQnJPL3habzNObUJmWVpkTEZTTEE9PSIsInZhbHVlIjoiR2ZMditvM0ZvYnVLajArWnVYZllFcEpOUGVXYk95bWRkdXdGcUVMZE9mT0ZvYmpPSEpoMDdNeC9MWjlxMnluVHd4djZ1TGcyOHJxbEdxd013K09wemJiZlcrZHhUZUN5YkJma3pkZXN4ZVZyU0RQY0pvSnc1WHpHTHlDUWpvTE0iLCJtYWMiOiIzZGVjYTM3N2ZiYzc3ODAyOWMyNjAwODU4NWU4YTY0NTgwNjVhNTVjZGM0NjZjM2QxOTM5MzJlZTcwNTEyYzM3IiwidGFnIjoiIn0%3D; __ddgid_=QTDZaHo3uDoGqGuR; __ddgmark_=a9WzMcAyP2KIzfHF; __ddg2_=nslKhTTMfCM10kKQ",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"

                    )
                )
                    .parsed<EpisodeData>().data!!
            }
        }.awaitAll()

        // Epizodlarni yig'ish
        allPagesData.flatten().forEach { episodeData ->
            val num = episodeData.episode ?: 0
            val thumb = episodeData.snapshot
            val link = "$animeLink/${episodeData.session}"
            list.add(Episode(num.toString(), link, episodeData.title.toString(), thumb!!))
        }

        list
    }

    suspend fun loadEpisodes(id: String, curPage: Int): EpisodeData? {
        val request = Requests(httpClient, responseParser = parser)
        try {
            return request.get(
                "${hostUrl}api?m=release&id=$id&sort=episode_asc&page=$curPage",
            ).parsed<EpisodeData>()

        } catch (e: Exception) {
//            Bugsnag.notify(e)
            return null
        }
    }

    suspend fun extractVideo(url: String) {
        val doc = getJsoup(
            url, mapOf(
                "Referer" to "https://animepahe.ru/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                "Alt-Used" to "kwik.si",
                "Host" to "kwik.si",
                "Sec-Fetch-User" to "?1"
            )
        )
        val scripts: Elements = doc.getElementsByTag("script")
        var evalContent: String? = null
        for (script in scripts) {
            val scriptContent = script.html()

            if (scriptContent.contains("eval(function(p,a,c,k,e,d){")) {
                println("Found eval function: \n$scriptContent")
                evalContent = scriptContent
                break
            }
        }

        val urlM3u8 = extractFileUrl(getAndUnpack(evalContent.toString())) ?: ""
        println(urlM3u8)
    }


    private val packedRegex = Regex("""eval\(function\(p,a,c,k,e,.*\)\)""")
    fun getPacked(string: String): String? {
        return packedRegex.find(string)?.value
    }

    fun getAndUnpack(string: String): String {
        val packedText = getPacked(string)
        return JsUnpacker(packedText).unpack() ?: string
    }

    fun extractFileUrl(input: String): String? {
        val regex = Regex("https?://\\S+\\.m3u8")
        val matchResult = regex.find(input)
        return m3u8ToMp4(matchResult?.value ?: "", "file") // Agar topilgan bo'lsa, URL manzilini qaytaradi
    }

    // https://anime.apex-cloud.workers.dev/?method=episode&session=778deaa9-5f4a-fe91-af6c-13f81bfd45a0&ep=bddc1dbe1d38ef8fe4596bc37627fd801ea63632ba65ea117f3e76f89238d9b4
    suspend fun getFileName(session: String, ep: String) {
        val requests = Requests(baseClient = httpClient, responseParser = parser)
        val getKiwik = requests.get("")
    }
    fun m3u8ToMp4(m3u8Url: String, fileName: String): String {
        val uri = java.net.URI(m3u8Url)

        // 1) pathni tozalaymiz va oxirgi fayl nomini olib tashlaymiz
        val cleanPath = uri.path
            .replaceFirst("/stream", "/mp4")  // stream -> mp4
            .substringBeforeLast("/")         // oxirgi segment (uwu.m3u8) ni olib tashlaymiz

        // 2) qayta URI yasaymiz va query qo'shamiz
        return java.net.URI(
            uri.scheme,
            uri.authority,
            cleanPath,
            "file=$fileName.mp4",
            null
        ).toString()
    }



}

data class ShowResponse(
    val name: String,
    val link: String,
    val coverUrl: String,

    val otherNames: List<String> = listOf(),

    val total: Int? = null,

    val extra: Map<String, String>? = null,
) : Serializable {

}

fun main(args: Array<String>) {
    runBlocking {
        val animePahe = AnimePahe()
        val list = animePahe.search("One Piece")
        println(list[0].link)
        animePahe.loadEpisodes(list[0].link, 1)?.let {
            animePahe.getEpisodeVideo(
                it.data?.get(11)?.session ?: "", list[0].link
            ).let {
                animePahe.extractVideo(url = it)
            }
        }
    }
}