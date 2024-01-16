package com.azamovhudstc.scarpingtutorial.socket

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*

private const val URL = "ws://127.0.0.1:8080/chat"
fun main(args: Array<String>) {
    runBlocking {
        launch {
            val serverUri = URI(URL)
            val client = ChatWebSocketClient(serverUri) {
                println(it)
            }
            client.connect()
            client.setSuccessListener {
                println("1 -> Send Message ")
                println("Choose :")
                val scanner = Scanner(System.`in`)
                val str = scanner.nextLine()
                when (str) {
                    "1" -> {
                        print("Enter Message :")
                        val text = scanner.next()
                        client.sendMessage(text.toString())
                    }
                    else -> {
                        print("Enter Message :")
                        val text = scanner.next()
                        client.sendMessage(text.toString())
                        println("Nothing")
                    }
                }


            }
        }


    }
}


class ChatWebSocketClient(serverURI: URI, private val messageListener: (String) -> Unit) :
    WebSocketClient(serverURI) {
    private lateinit var onSuccessListener: (String) -> Unit
    fun setSuccessListener(onSuccessListener: (String) -> Unit) {
        this.onSuccessListener = onSuccessListener
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        println("Chat Connected")
    }

    override fun onMessage(message: String?) {
        println(message)
        messageListener.invoke(message!!)
        onSuccessListener.invoke("Chat Connected !")
    }


    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println(code)
    }

    override fun onError(ex: Exception?) {
        ex!!.printStackTrace()
    }

    fun sendMessage(message: String) {
        send(message)
    }

}