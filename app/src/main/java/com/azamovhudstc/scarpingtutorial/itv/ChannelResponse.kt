package com.azamovhudstc.scarpingtutorial.itv

data class ChannelResponse(
    val code: Int,
    val `data`: DataX,
    val groupIds: List<Int>,
    val language: String,
    val message: String?=null,
    val meta: Any
)