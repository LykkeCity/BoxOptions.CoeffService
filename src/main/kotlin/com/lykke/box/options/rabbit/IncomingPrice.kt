package com.lykke.box.options.rabbit

data class IncomingPrice (
        val instrument: String,
        val timestamp: Long,
        val isBuy: Boolean,
        val price: Double
)