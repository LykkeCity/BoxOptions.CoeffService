package com.lykke.box.options.daos

class Price(
        var time: Long,
        var bid: Double,
        var ask: Double) {

    fun midPrice(): Double {
        return (bid + ask) / 2.0
    }

    fun clonePrice(): Price {
        val price = Price(time, bid, ask)
        return price
    }

    override fun toString(): String {
        return "Price(time=$time, bid=$bid, ask=$ask)"
    }
}