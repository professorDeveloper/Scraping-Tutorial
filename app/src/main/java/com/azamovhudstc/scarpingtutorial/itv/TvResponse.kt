package com.azamovhudstc.scarpingtutorial.itv

data class TvResponse(
    val code: Int,
    val `data`: List<Data>,
    val groupIds: List<Int>,
    val language: String,
    val message: String,
    val meta: Meta
)