package com.azamovhudstc.scarpingtutorial.newlms_pdp

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

object TokenFetcher {
    private val gson = Gson()
    private val httpClient = Utils.httpClient
    private val currentId = AtomicInteger(62751) // Starting from 62751 as you mentioned
    
    // Main authorization token (this seems to be static)
    private val authToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkMTZhZjY0ZC0yNTdiLTQ3OWItOTk5NzNkYTgtNGJlNy00ODhiLThkZDgtN2Y4YjRkN2FkNDcyLWEyZTUtNmJhMjRlOTVhYjE1IiwiaWF0IjoxNzY5MjYxOTE0LCJleHAiOjE3Njk4NjY3MTR9.c5LQz54Kbs-us8E_euu62tsCg0S_5Mj2eEbqhscAp0c"

    data class TokenResponse(
        val token: String? = null,
        val success: Boolean? = null,
        val empty: Boolean? = null,
        val viewOnly: Boolean? = null,
        val evaluationEnabled: Boolean? = null,
        val documentExternalId: String? = null,
        val exp: Long? = null,
        val iat: Long? = null,
        val expirationRedirection: String? = null
    )

    /**
     * Get token for a specific document ID
     */
    fun getTokenForDocument(documentId: Int): String? {
        val url = "https://api.pdp.university/api/university/v2/strike-plagiarism/get-token?documentId=$documentId"
        
        val headers = mapOf(
            "Authorization" to authToken,
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept" to "application/json, text/plain, */*"
        )
        
        try {
            val response = Utils.get(url, headers)
            val jsonResponse = JsonParser.parseString(response).asJsonObject
            
            // Check if response is incorrect style (empty response)
            if (jsonResponse.has("success") && jsonResponse.has("empty")) {
                val success = jsonResponse.get("success").asBoolean
                val empty = jsonResponse.get("empty").asBoolean
                
                if (success && empty) {
                    println("❌ Document ID $documentId: Incorrect style response (empty)")
                    return null
                }
            }
            
            // Parse the token response
            val tokenResponse = gson.fromJson(jsonResponse, TokenResponse::class.java)
            
            return if (tokenResponse.token != null && tokenResponse.token.isNotBlank()) {
                println("✅ Document ID $documentId: Token found!")
                tokenResponse.token
            } else {
                println("❌ Document ID $documentId: No token in response")
                null
            }
            
        } catch (e: Exception) {
            println("⚠️ Document ID $documentId: Error - ${e.message}")
            return null
        }
    }

    /**
     * Generate the final report URL using the token
     */
    fun generateReportUrl(token: String): String {
        return "https://lmsapi.plagiat.pl/report/?auth=$token"
    }

    /**
     * Check if the generated token URL is valid by making a request
     */
    fun checkTokenUrl(reportUrl: String): Boolean {
        return try {
            val response = Utils.get(reportUrl, mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ))
            // Check if we get a valid response (not error page)
            !response.contains("error", ignoreCase = true) && 
            response.isNotBlank() && 
            response.length > 100 // Basic check that we got some content
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Try multiple document IDs to find a working one
     */
    fun findWorkingToken(maxAttempts: Int = 100): Pair<String, String>? {
        var attempts = 0
        
        while (attempts < maxAttempts) {
            val documentId = currentId.getAndIncrement()
            println("🔄 Trying Document ID: $documentId (Attempt ${attempts + 1}/$maxAttempts)")
            
            val token = getTokenForDocument(documentId)
            
            if (token != null) {
                val reportUrl = generateReportUrl(token)
                
                // Check if the token URL is valid
                println("🔗 Generated URL: $reportUrl")
                println("🔍 Checking if URL is valid...")
                
                if (checkTokenUrl(reportUrl)) {
                    println("✅ Token URL is valid!")
                    return Pair(token, reportUrl)
                } else {
                    println("❌ Token URL is not valid, continuing search...")
                }
            }
            
            attempts++
            Thread.sleep(500) // Small delay to avoid rate limiting
        }
        
        println("❌ No working token found after $maxAttempts attempts")
        return null
    }

    /**
     * Try specific document IDs
     */
    fun trySpecificIds(ids: List<Int>): Pair<String, String>? {
        for (documentId in ids) {
            println("🔄 Trying Document ID: $documentId")
            
            val token = getTokenForDocument(documentId)
            
            if (token != null) {
                val reportUrl = generateReportUrl(token)
                
                println("🔗 Generated URL: $reportUrl")
                println("🔍 Checking if URL is valid...")
                
                if (checkTokenUrl(reportUrl)) {
                    println("✅ Token URL is valid!")
                    return Pair(token, reportUrl)
                } else {
                    println("❌ Token URL is not valid, trying next ID...")
                }
            }
            
            Thread.sleep(500) // Small delay
        }
        
        println("❌ No working token found in specified IDs")
        return null
    }
}

// Example usage
fun main() = runBlocking {
    println("🚀 Starting token search...")
    
    // Option 1: Try incremental IDs starting from current position
    val result1 = TokenFetcher.findWorkingToken(maxAttempts = 50)
    
    if (result1 != null) {
        println("\n🎉 SUCCESS!")
        println("Token: ${result1.first}")
        println("Report URL: ${result1.second}")
        
        // You can use this URL directly in browser or for further processing
    } else {
        println("\n⚠️ Trying specific document IDs...")
        
        // Option 2: Try specific known IDs
        val specificIds = listOf(
            64400, 64401, 64402, 64403, 64404, 64405,
            62987, 62988, 62989, 62990,
            62751, 62752, 62753, 62754, 62755
        )
        
        val result2 = TokenFetcher.trySpecificIds(specificIds)
        
        if (result2 != null) {
            println("\n🎉 SUCCESS with specific ID!")
            println("Token: ${result2.first}")
            println("Report URL: ${result2.second}")
        } else {
            println("\n❌ FAILED: Could not find any working token")
        }
    }
}