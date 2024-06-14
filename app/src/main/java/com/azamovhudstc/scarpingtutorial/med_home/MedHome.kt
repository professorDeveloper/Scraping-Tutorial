package com.azamovhudstc.scarpingtutorial.med_home
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests

import kotlinx.coroutines.runBlocking

private val baseUrl ="https://test.jadidlar.uz/api"

fun main(args: Array<String>) {
    val medHome =MedHome()
    medHome.postRequest()
}
class MedHome {
    fun postRequest(){
        runBlocking {
            val niceHttp= Requests(baseClient = Utils.httpClient, responseParser = parser)
            val request =niceHttp.post("$baseUrl/accounts/token", data = mapOf("phone" to "+998992803809", "password" to "Azamov2456??") )

            println(request.body.string())
            println(request.code)
        }
    }
}