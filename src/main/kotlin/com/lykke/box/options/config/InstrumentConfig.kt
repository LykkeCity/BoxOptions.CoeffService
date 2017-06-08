package com.lykke.box.options.config

data class InstrumentConfig (
    val name: String,
    val period: Long,
    val timeToFirstOption: Long,
    val optionLen: Long,
    val priceSize: Double,
    val nPriceIndex: Int,
    val nTimeIndex: Int,
    val marginHit: Double,
    val marginMiss: Double,
    val maxPayoutCoeff: Double,
    val bookingFee: Double,
    val hasWeekend: Boolean,

    val delta: Double,
    val activityFileName: String,
    val movingWindow: Long
)