package com.lykke.box.options.daos

import java.util.HashMap
import java.util.LinkedList

class HistoryHolder {
    private val historiesMap: MutableMap<String, MutableList<Price>> = HashMap()

    fun addPrice(instrument: String, price: Price) {
        val prices = historiesMap.getOrPut(instrument) { LinkedList<Price>() }
        if (prices.size > 0) {
            val lastPrice = prices.last()
            if (price.time > lastPrice.time) {
                prices.add(price)
            }
        } else {
            prices.add(price)
        }
    }

    fun getPrices(instrument: String): List<Price>? {
        return historiesMap[instrument]
    }

    fun addAllPrices(instrument: String, ticks: List<Price>) {
        val prices = historiesMap.getOrPut(instrument) { LinkedList<Price>() }
        ticks.forEach { tick ->
            prices.add(Price(tick.time, tick.bid, tick.ask))
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        historiesMap.entries.forEach {
            builder.append("${it.key}: ${it.value.size}\n")
        }
        return builder.toString()
    }
}