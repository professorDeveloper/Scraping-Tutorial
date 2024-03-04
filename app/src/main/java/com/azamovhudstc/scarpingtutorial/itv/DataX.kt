package com.azamovhudstc.scarpingtutorial.itv

data class DataX(
    val channelDescription: String,
    val channelId: Int,
    val channelTitle: String,
    val files: FilesX,
    val moduleId: Int,
    val params: ParamsX,
    val paymentParams: PaymentParamsX,
    val recommendedSubscriptions: List<Any>,
    val shareUrl: String
)