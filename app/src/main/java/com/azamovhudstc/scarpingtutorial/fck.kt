package com.azamovhudstc.scarpingtutorial

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {

        val requests = Requests(baseClient = Utils.httpClient)
        for (i in 1..10000) {
            val url = "https://count.getloli.com/@professorDeveloper?name=Sozo-tv&theme=rule34&padding=8&offset=0&align=top&scale=1&pixelated=1&darkmode=auto"

            val headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "Accept-Language" to "en-US,en;q=0.9,uz-UZ;q=0.8,uz;q=0.7",
                "Cache-Control" to "max-age=0",
                "DNT" to "1",
                "Priority" to "u=0, i",
                "Sec-Ch-Ua" to "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"",
                "Sec-Ch-Ua-Mobile" to "?0",
                "Sec-Ch-Ua-Platform" to "\"Windows\"",
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "none",
                "Sec-Fetch-User" to "?1",
                "Upgrade-Insecure-Requests" to "1",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
            )

            val response = requests.get(
                url = url,
                headers = headers
            )
            println("$i ${response.code}")
        }
    }
}
