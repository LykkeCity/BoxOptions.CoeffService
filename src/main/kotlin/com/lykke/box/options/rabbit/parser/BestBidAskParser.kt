package com.lykke.box.options.rabbit.parser

import com.google.gson.Gson
import com.lykke.box.options.rabbit.IncomingPrice
import java.util.LinkedList

class BestBidAskParser: IncomingParser<IncomingPrice> {
    private val gson = Gson()

    override fun parse(input: String): List<IncomingPrice> {
        val result = LinkedList<IncomingPrice>()
        val bestBidAsk = gson.fromJson(input, BestBidAsk::class.java)

        if (bestBidAsk.bestBid != null) {
            result.add(IncomingPrice(bestBidAsk.asset, bestBidAsk.timestamp.time, true, bestBidAsk.bestBid))
        }

        if (bestBidAsk.bestAsk != null) {
            result.add(IncomingPrice(bestBidAsk.asset, bestBidAsk.timestamp.time, false, bestBidAsk.bestAsk))
        }
        return result
    }
}