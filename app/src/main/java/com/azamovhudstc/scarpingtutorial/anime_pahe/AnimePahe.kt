package com.azamovhudstc.scarpingtutorial.anime_pahe

import com.azamovhudstc.scarpingtutorial.model.AnimePaheData
import com.azamovhudstc.scarpingtutorial.model.EpisodeData
import com.azamovhudstc.scarpingtutorial.theflixer.Episode
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.regex.Pattern


class AnimePahe   {
     val name = "AnimePahe"
     val saveName = "anime_pahe_hu"
     private val hostUrl = "https://animepahe.ru/"
     val malSyncBackupName = "animepahe"
     val isDubAvailableSeparately = false

//     suspend fun loadEpisodes(
//        animeLink: String,
//        extra: Map<String, String>?
//    ): List<Episode> = coroutineScope {
//        val list = mutableListOf<Episode>()
//         val requests = Requests(Utils.httpClient, responseParser = parser)
//
//        // Birinchi sahifani olib, jami sahifalar sonini aniqlash
//        println("https://animepahe.ru/api?m=release&id=$animeLink&sort=episode_asc&page=1")
//        val totalPages = withContext(Dispatchers.IO) {
//            requests.get("https://animepahe.ru/api?m=release&id=$animeLink&sort=episode_asc&page=1")
//                .parsed<EpisodeData>().last_page
//        }
//
//        // Barcha sahifalarni parallel yuklash va natijalarni yig'ish
//        val allPagesData = (1..totalPages!!).map { page ->
//            async(Dispatchers.IO) {
//                requests.get(
//                    "https://animepahe.ru/api?m=release&id=$animeLink&sort=episode_asc&page=$page",
//                    mapOf(
//                        "dnt" to "1",
//                        "Cookie" to "\n" +
//                                "__ddg1_=Yhqnq62nxbM5uT9LNXBU; SERVERID=janna; latest=5633; ann-fakesite=0; res=720; aud=jpn; av1=0; dom3ic8zudi28v8lr6fgphwffqoz0j6c=33161aa3-e5ac-4f93-b315-e3165fddb0bf%3A3%3A1; sb_page_8966b6c0380845137e2f0bc664baf7be=3; sb_count_8966b6c0380845137e2f0bc664baf7be=3; sb_onpage_8966b6c0380845137e2f0bc664baf7be=1; XSRF-TOKEN=eyJpdiI6InV2RGVHeUhMNkxFelAzOG16TnRXa2c9PSIsInZhbHVlIjoiWkQyWTJaODErMnNVREhRdnZ5L0pycG1Sd2hWZkRhcjB6alN6MDZwb3ppOEpTNFpscWljYmRkVHI0RDNDN0ZxYkZIZE5jSTF2SWpjckZSaHhYWkVRZmdHMGgreE1LMlNLZXpPUnREQ3hjQ0NiZ1RZNUEwQ1hXNkxjaEdKdVc3YnAiLCJtYWMiOiJhMDRkOWU3ZjkzZWNjZmMxYTUxNTI0YWIwOTE2NTcxYTUyYWI3NTM4YTgyMzJhYmYyZDc3YjA2NWVlMjBmMDNhIiwidGFnIjoiIn0%3D; laravel_session=eyJpdiI6IlVtQnJPL3habzNObUJmWVpkTEZTTEE9PSIsInZhbHVlIjoiR2ZMditvM0ZvYnVLajArWnVYZllFcEpOUGVXYk95bWRkdXdGcUVMZE9mT0ZvYmpPSEpoMDdNeC9MWjlxMnluVHd4djZ1TGcyOHJxbEdxd013K09wemJiZlcrZHhUZUN5YkJma3pkZXN4ZVZyU0RQY0pvSnc1WHpHTHlDUWpvTE0iLCJtYWMiOiIzZGVjYTM3N2ZiYzc3ODAyOWMyNjAwODU4NWU4YTY0NTgwNjVhNTVjZGM0NjZjM2QxOTM5MzJlZTcwNTEyYzM3IiwidGFnIjoiIn0%3D; __ddgid_=QTDZaHo3uDoGqGuR; __ddgmark_=a9WzMcAyP2KIzfHF; __ddg2_=nslKhTTMfCM10kKQ",
//                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
//
//                    )
//                )
//                    .parsed<EpisodeData>().data!!
//            }
//        }.awaitAll()
//
//        // Epizodlarni yig'ish
//        allPagesData.flatten().forEach { episodeData ->
//            val num = episodeData.episode ?: 0
//            val thumb = episodeData.snapshot!!
//            val link = "$animeLink/${episodeData.session}"
//            list.add(EpisodeData(num.toString(), link, episodeData.title.toString(), thumb!!))
//        }
//
//        list
//    }
//

     suspend fun search(query: String) : List<ShowResponse>{
//        https://animepahe.ru/api?m=search&q=one%20piece
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
                "Referer" to "https://animepahe.ru//api?m=search&q=$formattedQuery",
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


    fun extractValue(scriptContent: String, key: String): String {
        val regex = "$key = \"(.*?)\""
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(scriptContent)
        return if (matcher.find()) {
            matcher.group(1)
        } else ""
    }
}

data class ShowResponse(
    val name: String,
    val link: String,
    val coverUrl: String,

    val otherNames: List<String> = listOf(),

    val total: Int? = null,

    val extra : Map<String,String>?=null,
) : Serializable {

}

fun main(args: Array<String>) {
    runBlocking {
        val animePahe = AnimePahe()
        val list = animePahe.search("One Piece")
        println(list[0].link)
    }
}