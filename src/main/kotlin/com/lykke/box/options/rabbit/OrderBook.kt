package com.lykke.box.options.rabbit

import java.util.ArrayList
import java.util.Date

class OrderBook(
        val assetPair: String,
        val isBuy: Boolean,
        val timestamp: Date) {

    val prices: MutableList<VolumePrice> = ArrayList()
}

class VolumePrice(val volume: Double, val price: Double)
