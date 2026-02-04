package com.azamovhudstc.scarpingtutorial.utils

import com.azamovhudstc.scarpingtutorial.utils.Utils
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object GitHubUnfollower {

    // Cookies'i tutmak için
    private val cookies = mapOf(
        "_octo" to "GH1.1.615507747.1744702571",
        "_device_id" to "cc71b7bc3fa30a373680e8499481e991",
        "logged_in" to "yes",
        "user_session" to "5zMMAIKQHO-Bj7uA5lj_6Iu6KeBOnOg_BlXE-bjwFpKqH2q7",
        "__Host-user_session_same_site" to "5zMMAIKQHO-Bj7uA5lj_6Iu6KeBOnOg_BlXE-bjwFpKqH2q7",
        "dotcom_user" to "foregroundUser"
    )

    // Headers'ları ayarlamak için
    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "accept" to "*/*",
            "accept-language" to "en-US,en;q=0.9,uz-UZ;q=0.8,uz;q=0.7",
            "content-type" to "multipart/form-data; boundary=----WebKitFormBoundaryyws7s8Dcjm4EU91H",
            "dnt" to "1",
            "origin" to "https://github.com",
            "referer" to "https://github.com/foregroundUser?tab=following",
            "sec-ch-ua" to "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Windows\"",
            "sec-fetch-dest" to "empty",
            "sec-fetch-mode" to "cors",
            "sec-fetch-site" to "same-origin",
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36",
            "x-requested-with" to "XMLHttpRequest"
        ) + cookies.map { (key, value) -> "cookie" to "$key=$value" }.toMap()
    }

    // Sayfadan kullanıcıları ve token'ları çıkarmak
    private fun extractUsersFromPage(doc: Document): List<Pair<String, String>> {
        val users = mutableListOf<Pair<String, String>>()

        // Her kullanıcı bloğunu bul
        val userBlocks =
            doc.select("div.d-table.table-fixed.col-12.width-full.py-4.border-bottom.color-border-muted")

        for (block in userBlocks) {
            // Unfollow formunu bul
            val unfollowForm = block.select("form[action*='/users/unfollow?target=']").first()
            if (unfollowForm != null) {
                // Username'i çıkar
                val action = unfollowForm.attr("action")
                val username = action.substringAfter("target=")

                // Authenticity token'ı çıkar
                val tokenInput = unfollowForm.select("input[name='authenticity_token']").first()
                val token = tokenInput?.attr("value") ?: continue

                users.add(Pair(username, token))
            }
        }

        return users
    }

    private fun unfollowUser(username: String, token: String): Boolean {
        try {
            val url = "https://github.com/users/unfollow?target=$username"

            // Multipart form data oluştur
            val boundary = "----WebKitFormBoundaryyws7s8Dcjm4EU91H"
            val body = """
                --$boundary
                Content-Disposition: form-data; name="commit"
                
                Unfollow
                --$boundary
                Content-Disposition: form-data; name="authenticity_token"
                
                $token
                --$boundary--
            """.trimIndent()

            val headers = getHeaders().toMutableMap()
            headers["content-type"] = "multipart/form-data; boundary=$boundary"

            val response = Utils.httpClient.newCall(
                okhttp3.Request.Builder()
                    .url(url)
                    .headers(headers.toHeaders())
                    .post(
                        okhttp3.RequestBody.create(
                            "multipart/form-data; boundary=$boundary".toMediaTypeOrNull(),
                            body
                        )
                    )
                    .build()
            ).execute()

            val isSuccess = response.isSuccessful
            response.close()

            return isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Tüm sayfalarda unfollow işlemi
    fun unfollowAllUsers(): Boolean {
        var page = 1
        var hasMorePages = true

        while (hasMorePages) {
            println("Processing page $page...")

            val url = "https://github.com/foregroundUser?page=$page&tab=following"

            try {
                // Sayfayı al
                val doc = Utils.getJsoup(url, getHeaders())

                // Kullanıcıları çıkar
                val users = extractUsersFromPage(doc)
                println(doc)
                if (users.isEmpty()) {
                    println("No users found on page $page")
                    hasMorePages = false
                    break
                }

                println("Found ${users.size} users on page $page")

                // Her kullanıcıyı unfollow et
                for ((index, user) in users.withIndex()) {
                    val (username, token) = user
                    println("Unfollowing $username... (${index + 1}/${users.size})")

                    val success = unfollowUser(username, token)

                    if (success) {
                        println("Successfully unfollowed $username")
                    } else {
                        println("Failed to unfollow $username")
                    }

                    // GitHub rate limit'ini aşmamak için biraz bekle
                    Thread.sleep(1000)
                }

                // Sonraki sayfa var mı kontrol et
                val nextPageLink = doc.select("a[rel='next']")
                hasMorePages = nextPageLink.isNotEmpty()
                page++

                // Sayfalar arasında bekle
                Thread.sleep(2000)

            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        println("Unfollow process completed!")
        return true
    }
}