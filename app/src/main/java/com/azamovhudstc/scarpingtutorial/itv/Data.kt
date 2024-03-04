package com.azamovhudstc.scarpingtutorial.itv

data class Data(
    val channelId: Int,
    val channelTitle: String,
    val files: Files,
    val moduleId: Int,
    val params: Params,
    val paymentParams: PaymentParams
)