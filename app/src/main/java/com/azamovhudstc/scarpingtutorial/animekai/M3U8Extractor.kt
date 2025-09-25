package com.azamovhudstc.scarpingtutorial.animekai

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class M3U8Extractor {
    
    data class ServerData(
        val sid: String,
        val eid: String,
        val lid: String,
        val serverName: String
    )
    
    data class VideoSource(
        val url: String,
        val quality: String,
        val type: String
    )
    
    /**
     * Decodes the obfuscated result data
     */
    @SuppressLint("NewApi")
    fun decodeObfuscatedData(encryptedData: String): String {
        return try {
            val decoded = Base64.getDecoder().decode(encryptedData)
            val decodedString = String(decoded, StandardCharsets.UTF_8)
            
            decompressLZString(decodedString)
        } catch (e: Exception) {
            decompressLZString(encryptedData)
        }
    }
    
    /**
     * LZ-String decompression algorithm (simplified version)
     */
    private fun decompressLZString(compressed: String): String {
        if (compressed.isEmpty()) return ""
        
        val dictionary = mutableMapOf<Int, String>()
        var dictSize = 4
        var numBits = 3
        var entry: String
        val result = StringBuilder()
        var bits = 0
        var maxPower = 2
        var power = 1
        val c: Char
        val data = "$compressed "
        var dataIndex = 0
        var dataVal = data[0].code
        var dataPosition = 32768
        
        for (i in 0..2) {
            dictionary[i] = i.toChar().toString()
        }
        
        while (power != maxPower) {
            val resb = dataVal and dataPosition
            dataPosition = dataPosition shr 1
            if (dataPosition == 0) {
                dataPosition = 32768
                dataVal = data[++dataIndex].code
            }
            bits = bits or (if (resb > 0) 1 else 0) * power
            power = power shl 1
        }
        
        when (bits) {
            0 -> {
                bits = 0
                maxPower = Math.pow(2.0, 8.0).toInt()
                power = 1
                while (power != maxPower) {
                    val resb = dataVal and dataPosition
                    dataPosition = dataPosition shr 1
                    if (dataPosition == 0) {
                        dataPosition = 32768
                        dataVal = data[++dataIndex].code
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = bits.toChar()
            }
            1 -> {
                bits = 0
                maxPower = Math.pow(2.0, 16.0).toInt()
                power = 1
                while (power != maxPower) {
                    val resb = dataVal and dataPosition
                    dataPosition = dataPosition shr 1
                    if (dataPosition == 0) {
                        dataPosition = 32768
                        dataVal = data[++dataIndex].code
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = bits.toChar()
            }
            else -> return ""
        }
        
        dictionary[3] = c.toString()
        var w = c.toString()
        result.append(w)
        
        while (true) {
            if (dataIndex > compressed.length) break
            
            bits = 0
            maxPower = Math.pow(2.0, numBits.toDouble()).toInt()
            power = 1
            
            while (power != maxPower) {
                val resb = dataVal and dataPosition
                dataPosition = dataPosition shr 1
                if (dataPosition == 0) {
                    dataPosition = 32768
                    if (++dataIndex >= data.length) break
                    dataVal = data[dataIndex].code
                }
                bits = bits or (if (resb > 0) 1 else 0) * power
                power = power shl 1
            }
            
            when (bits) {
                0 -> {
                    bits = 0
                    maxPower = Math.pow(2.0, 8.0).toInt()
                    power = 1
                    while (power != maxPower) {
                        val resb = dataVal and dataPosition
                        dataPosition = dataPosition shr 1
                        if (dataPosition == 0) {
                            dataPosition = 32768
                            dataVal = data[++dataIndex].code
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }
                    dictionary[dictSize++] = bits.toChar().toString()
                    bits = dictSize - 1
                    if (dictSize == Math.pow(2.0, numBits.toDouble()).toInt()) numBits++
                }
                1 -> {
                    bits = 0
                    maxPower = Math.pow(2.0, 16.0).toInt()
                    power = 1
                    while (power != maxPower) {
                        val resb = dataVal and dataPosition
                        dataPosition = dataPosition shr 1
                        if (dataPosition == 0) {
                            dataPosition = 32768
                            dataVal = data[++dataIndex].code
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }
                    dictionary[dictSize++] = bits.toChar().toString()
                    bits = dictSize - 1
                    if (dictSize == Math.pow(2.0, numBits.toDouble()).toInt()) numBits++
                }
                2 -> return result.toString()
            }
            
            if (dictionary.containsKey(bits)) {
                entry = dictionary[bits]!!
            } else {
                if (bits == dictSize) {
                    entry = w + w[0]
                } else {
                    return ""
                }
            }
            
            result.append(entry)
            dictionary[dictSize++] = w + entry[0]
            if (dictSize == Math.pow(2.0, numBits.toDouble()).toInt()) numBits++
            w = entry
        }
        
        return result.toString()
    }
    
    /**
     * Parses server data from HTML
     */
    fun parseServerData(html: String): List<ServerData> {
        val servers = mutableListOf<ServerData>()
        val serverRegex = """<span class="server" data-sid="([^"]+)" data-eid="([^"]+)" data-lid="([^"]+)"\s*>([^<]+)</span>""".toRegex()
        
        serverRegex.findAll(html).forEach { match ->
            servers.add(
                ServerData(
                    sid = match.groupValues[1],
                    eid = match.groupValues[2],
                    lid = match.groupValues[3],
                    serverName = match.groupValues[4].trim()
                )
            )
        }
        
        return servers
    }
    
    /**
     * Generates m3u8 URL based on server data and decoded information
     */
    fun generateM3U8Url(serverData: ServerData, decodedData: String): String? {
        return try {
            // Parse the decoded data to extract URL components
            val urlComponents = extractUrlComponents(decodedData, serverData)
            
            // Construct the m3u8 URL
            val baseUrl = "https://9dd.infinity-loop-21.biz"
            val path = "/p694/c4"
            val encodedPath = urlComponents["encodedPath"] ?: return null
            val playlistName = urlComponents["playlistName"] ?: "list"
            val token = urlComponents["token"] ?: return null
            
            "$baseUrl$path/$encodedPath/$playlistName,$token.m3u8"
        } catch (e: Exception) {
            println("Error generating m3u8 URL: ${e.message}")
            null
        }
    }
    
    /**
     * Extracts URL components from decoded data
     */
    @SuppressLint("NewApi")
    private fun extractUrlComponents(decodedData: String, serverData: ServerData): Map<String, String> {
        val components = mutableMapOf<String, String>()
        
        try {
            // Try to parse as JSON first
            val json = JSONObject(decodedData)
            components["encodedPath"] = json.optString("path", "")
            components["token"] = json.optString("token", "")
            components["playlistName"] = json.optString("playlist", "list")
        } catch (e: Exception) {
            // If not JSON, extract using pattern matching
            val pathPattern = """([a-zA-Z0-9_-]{50,})""".toRegex()
            val tokenPattern = """([a-zA-Z0-9_-]{20,40})""".toRegex()
            
            val pathMatch = pathPattern.find(decodedData)
            val tokenMatch = tokenPattern.findAll(decodedData).lastOrNull()
            
            components["encodedPath"] = pathMatch?.value ?: generateEncodedPath(serverData)
            components["token"] = tokenMatch?.value ?: generateToken(serverData)
            components["playlistName"] = "list"
        }
        
        return components
    }
    
    /**
     * Generates encoded path based on server data
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateEncodedPath(serverData: ServerData): String {
        val combined = "${serverData.eid}${serverData.lid}${serverData.sid}"
        return Base64.getEncoder().encodeToString(combined.toByteArray())
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }
    
    /**
     * Generates token based on server data
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateToken(serverData: ServerData): String {
        val timestamp = System.currentTimeMillis() / 1000
        val combined = "${serverData.lid}${timestamp}${serverData.eid}"
        val hash = MessageDigest.getInstance("MD5").digest(combined.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
            .take(20)
    }
    
    /**
     * Main extraction function
     */
    suspend fun extractM3U8Urls(encryptedResult: String, htmlData: String): List<VideoSource> {
        return withContext(Dispatchers.IO) {
            val sources = mutableListOf<VideoSource>()
            
            try {
                // Decode the encrypted data
                val decodedData = decodeObfuscatedData(encryptedResult)
                println("Decoded data: $decodedData")
                
                // Parse server data from HTML
                val servers = parseServerData(htmlData)
                println("Found ${servers.size} servers")
                
                // Generate m3u8 URLs for each server
                servers.forEach { server ->
                    val m3u8Url = generateM3U8Url(server, decodedData)
                    if (m3u8Url != null) {
                        sources.add(
                            VideoSource(
                                url = m3u8Url,
                                quality = "1080p", // Default quality
                                type = server.serverName
                            )
                        )
                        println("Generated URL for ${server.serverName}: $m3u8Url")
                    }
                }
                
            } catch (e: Exception) {
                println("Error extracting m3u8 URLs: ${e.message}")
                e.printStackTrace()
            }
            
            sources
        }
    }
}
