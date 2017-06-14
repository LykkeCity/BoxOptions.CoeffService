package com.lykke.box.options.rabbit.parser

import com.google.gson.Gson
import com.lykke.box.options.rabbit.IncomingPrice

class OrderBookParser: IncomingParser<IncomingPrice> {
    private val gson = Gson()

    override fun parse(input: String): List<IncomingPrice> {
        val orderBook = gson.fromJson(input, OrderBook::class.java)
        if (orderBook.prices.isNotEmpty()) {
            return listOf(IncomingPrice(orderBook.assetPair, orderBook.timestamp.time, orderBook.isBuy, orderBook.prices.first().price))
        }
        return emptyList()
    }
}