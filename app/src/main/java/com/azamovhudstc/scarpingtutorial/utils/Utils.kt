package com.azamovhudstc.scarpingtutorial.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.lagradost.nicehttp.addGenericDns
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

object Utils {

    var httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addCloudFlareDns()
        .callTimeout(2, TimeUnit.MINUTES)
        .build()
    fun OkHttpClient.Builder.addCloudFlareDns() = (
            addGenericDns(
                "https://cloudflare-dns.com/dns-query",
                // https://www.cloudflare.com/ips/
                listOf(
                    "1.1.1.1",
                    "1.0.0.1",
                    "2606:4700:4700::1111",
                    "2606:4700:4700::1001"
                )
            ))


    fun getAsilMedia(
        host: String? = null,
        pathSegment: ArrayList<String>? = null,
        mapOfHeaders: Map<String, String>? = null,
        params: Map<String, String>? = null,
    ): String {
        val urlBuilder = HttpUrl.Builder()
            .scheme("http") // Replace with your scheme (http or https)
            .host(host!!) // Replace with your actual host
        pathSegment?.forEach {
            urlBuilder.addPathSegment(it)
        }


        if (!params.isNullOrEmpty()) {
            params.forEach {
                urlBuilder.addQueryParameter(it.key, it.value)
            }
        }

        val requestBuilder = Request.Builder().url(urlBuilder.build())
        if (!mapOfHeaders.isNullOrEmpty()) {
            mapOfHeaders.forEach {
                requestBuilder.addHeader(it.key, it.value)
            }
        }
        var data = httpClient.newCall(requestBuilder.build())
            .execute()

        println(data.body?.string())
        return data.body.string()
    }

    fun get(
        url: String,
        mapOfHeaders: Map<String, String>? = null
    ): String {
        val requestBuilder = Request.Builder().url(url)
        if (!mapOfHeaders.isNullOrEmpty()) {
            mapOfHeaders.forEach {
                requestBuilder.addHeader(it.key, it.value)
            }
        }
        return httpClient.newCall(requestBuilder.build())
            .execute().body!!.string()
    }

    fun post(
        url: String,
        mapOfHeaders: Map<String, String>? = null,
        payload: Map<String, String>? = null
    ): String {
        val requestBuilder = Request.Builder().url(url)

        if (!mapOfHeaders.isNullOrEmpty()) {
            mapOfHeaders.forEach {
                requestBuilder.addHeader(it.key, it.value)
            }
        }

        val requestBody = payload?.let {
            FormBody.Builder().apply {
                it.forEach { (key, value) ->
                    add(key, value)
                }
            }.build()
        }

        if (requestBody != null) {
            requestBuilder.post(requestBody)
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        return response.body?.string() ?: ""
    }

    fun getJsoup(
        url: String,
        mapOfHeaders: Map<String, String>? = null
    ): Document {
        return Jsoup.parse(get(url, mapOfHeaders))
    }

    fun getJsoupAsilMedia(
        host: String,
        pathSegment: ArrayList<String>? /* = java.util.ArrayList<kotlin.String>? */ = null,
        params: Map<String, String>? = null,
        mapOfHeaders: Map<String, String>? = null
    ): Document {
        return Jsoup.parse(
            getAsilMedia(
                host = host,
                pathSegment = pathSegment,
                params = params,
                mapOfHeaders = mapOfHeaders
            )
        )
    }

    fun getJson(
        url: String,
        mapOfHeaders: Map<String, String>? = null
    ): JsonElement? {
        return JsonParser.parseString(get(url, mapOfHeaders))
    }

    fun postJson(
        url: String,
        mapOfHeaders: Map<String, String>? = null,
        payload: Map<String, String>? = null
    ): JsonElement? {
        val res = post(url, mapOfHeaders, payload)
        return JsonParser.parseString(res)
    }
}