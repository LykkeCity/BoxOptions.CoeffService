package com.lykke.box.options.rabbit.parser

import java.util.Date

data class BestBidAsk(
        val source: String,
        val asset: String,
        val timestamp: Date,
        val bestAsk: Double?,
        val bestBid: Double?
) {
    override fun toString(): String {
        return "BestBidAsk(source='$source', asset='$asset', timestamp=$timestamp, bestAsk=$bestAsk, bestBid=$bestBid)"
    }
}