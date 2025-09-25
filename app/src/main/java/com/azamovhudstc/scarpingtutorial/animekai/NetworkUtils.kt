package com.azamovhudstc.scarpingtutorial.animekai

import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

object NetworkUtils {
    
    /**
     * Makes HTTP request to fetch additional data if needed
     */
    fun makeRequest(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            
            // Set headers
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            // Common headers for video streaming sites
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate")
            connection.setRequestProperty("Connection", "keep-alive")
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
            
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                response
            } else {
                println("HTTP Error: $responseCode")
                null
            }
        } catch (e: Exception) {
            println("Network error: ${e.message}")
            null
        }
    }
    
    /**
     * Validates if m3u8 URL is accessible
     */
    fun validateM3U8Url(url: String): Boolean {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }
}
