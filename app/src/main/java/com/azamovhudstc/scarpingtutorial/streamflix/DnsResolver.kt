package com.azamovhudstc.scarpingtutorial.streamflix

import android.util.Log
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.net.InetAddress

object DnsResolver : Dns {
    private const val TAG = "DnsResolver"

    private val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
    )
    private val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAllCerts, SecureRandom()) }
    private val trustManager = trustAllCerts[0] as X509TrustManager

    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, trustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private var _url: String = "https://cloudflare-dns.com/dns-query"
    private var _internalDoh: Dns = buildDoh(_url)

    override fun lookup(hostname: String): List<InetAddress> {
        val providerName = if (_url.isEmpty()) "SYSTEM" else _url
        return try {
            val addresses = _internalDoh.lookup(hostname)
            addresses
        } catch (e: Exception) {
            throw e
        }
    }

    val doh: Dns get() = this

    @Synchronized
    fun setDnsUrl(newUrl: String) {
        if (newUrl != _url) {
            _url = newUrl
            _internalDoh = buildDoh(_url)
        } else {
        }
    }

    @Synchronized
    private fun buildDoh(url: String): Dns {
        return if (url.isNotEmpty()) {
            try {
                DnsOverHttps.Builder()
                    .client(client)
                    .url(url.toHttpUrl())
                    .build()
            } catch (e: Exception) {
                Dns.SYSTEM
            }
        } else {
            Dns.SYSTEM
        }
    }
}