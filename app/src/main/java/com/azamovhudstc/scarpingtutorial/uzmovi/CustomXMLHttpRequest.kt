package com.azamovhudstc.scarpingtutorial.uzmovi

import android.annotation.SuppressLint
import java.net.URL
import java.util.Base64

class CustomXMLHttpRequest {
    var origOpen: ((String, String, Boolean, String?, String?, String?) -> Unit)? = null

    fun open(method: String, url: String, async: Boolean, user: String?, password: String?, vararg args: String?) {
        val _0x500e36 = URL(url)
        if (_0x500e36.host.contains("uzdown.space")) {
            val origin = URL(url).protocol + "://" + URL(url).host
            val urlObj = URL(url)
//            args[1] = "${urlObj.protocol}://${urlObj.host}/" +
//                    "${generateRandomString(30)}/" +
//                    "${generateRandomString(10)}.mpd"

//            origOpen?.invoke(method, args.joinToString("/"), async, user, password)

            // Replace these placeholder functions with your actual logic
            val xAttDeviceId = generateRandomString(16)
            val xMatch = btoa(generateRandomString(16))
            val xPath = generateRandomString(40)

            println("Modified URL: ${args[1]}")
            println("X-ATT-DeviceId: $xAttDeviceId")
            println("X-Match: $xMatch")
            println("X-Path: $xPath")
        } else {
//            origOpen?.invoke(method, url, async, user, password, *args.toString())
        }
    }

    // Placeholder functions, replace with your actual logic
    private fun generateRandomString(length: Int): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { characters.random() }
            .joinToString("")
    }

    @SuppressLint("NewApi")
    private fun btoa(data: String): String {
        return Base64.getEncoder().encodeToString(data.toByteArray())
    }
}








fun main() {
    // Asl URL
    val originalURL = "https://srv416.uzdown.space/ghtj21e75mbfa72sh94gocpq0ids2q/65da7d6th2.mpd"

    // Modified URL generatsiyasi
    val modifiedURL = generateSimilarURL(originalURL)
    println("Original URL: $originalURL")
    println("Generated URL: $modifiedURL")

    // X-ATT-DeviceId, X-Match, va X-Path generatsiyasi
    val xAttDeviceId = generateRandomString(16)
    val xMatch = btoa(generateRandomString(16))
    val xPath = generateRandomString(40)

    // Natijalarni chiqarish
    println("X-ATT-DeviceId: $xAttDeviceId")
    println("X-Match: $xMatch")
    println("X-Path: $xPath")
}

fun generateSimilarURL(baseURL: String): String {
    val url = URL(baseURL)
    val generatedPath = "/${generateRandomString(30)}/${generateRandomString(10)}.mpd"
    return "${url.protocol}://${url.host}$generatedPath"
}

fun generateRandomString(length: Int): String {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { characters.random() }
        .joinToString("")
}

fun btoa(data: String): String {
    return Base64.getEncoder().encodeToString(data.toByteArray())
}