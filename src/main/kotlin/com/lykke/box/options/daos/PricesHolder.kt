package com.lykke.box.options.daos

import java.util.HashMap
import java.util.LinkedList

class PricesHolder {
    private val pricesMap: MutableMap<String, MutableList<Price>> = HashMap()

    fun addPrice(instrument: String, price: Price) {
        synchronized(pricesMap) {
            val prices = pricesMap.getOrPut(instrument) { LinkedList<Price>() }
            prices.add(price)
        }
    }

    fun getPrices(instrument: String): List<Price> {
        synchronized(pricesMap) {
            var prices = pricesMap.remove(instrument)
            if (prices == null) {
                prices = LinkedList<Price>()
            }
            return prices
        }
    }
}