package com.lykke.box.options.algo

import com.lykke.box.options.LOGGER
import com.lykke.box.options.config.Config
import com.lykke.box.options.daos.HistoryHolder
import com.lykke.box.options.daos.Price
import com.lykke.box.options.daos.PricesHolder
import java.io.BufferedReader
import java.io.FileReader
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import kotlin.concurrent.fixedRateTimer

class GridsHolder (val config: Config, val historyHolder: HistoryHolder, val pricesHolder: PricesHolder) {
    private val grids = HashMap<String, OptionsGrid>()
    private val activities = HashMap<String, List<Double>>()

    fun getGrid(instrument: String): OptionsGrid? {
        return grids[instrument]
    }

    init {
        config.instruments.forEach {

            val activityDistrib = readActivityFile(it.activityFileName)
            activities[it.name] = activityDistrib

            val grid = initGrid(it.timeToFirstOption, it.optionLen, it.priceSize, it.nPriceIndex, it.nTimeIndex,
                    it.marginHit, it.marginMiss, it.maxPayoutCoeff, it.bookingFee)

            val history = historyHolder.getPrices(it.name)
            if (history != null) {
                val currentPrice = history[history.size - 1]
                grid.initiateGrid(activityDistrib, history, it.delta, it.movingWindow, currentPrice)

                grids[it.name] = grid

                fixedRateTimer(name = it.name, period = it.period) {
                    val now = Date().time
                    val newPrices = pricesHolder.getPrices(it.name)

                    val newPrice: Price
                    if (newPrices.isNotEmpty()) {
                        val lastPrice = newPrices.last()
                        val bid = lastPrice.bid
                        val ask = lastPrice.ask
                        newPrice = Price(now, bid, ask)
                    } else {
                        val price = historyHolder.getPrices(it.name)!!.last()
                        newPrice = Price(now, price.bid, price.ask)
                    }
                    grids[it.name]!!.updateCoefficients(newPrices, newPrice)
                    if (newPrices.isNotEmpty()) {
                        LOGGER.info("[${it.name}] Updated. New prices size: ${newPrices.size}. Current price: $newPrice")
                    }
                }
            }
        }
    }

    fun initGrid(timeToFirstOption: Long, optionLen: Long, priceSize: Double, nPriceIndex: Int, nTimeIndex: Int,
                 marginHit: Double, marginMiss: Double, maxPayoutCoeff: Double, bookingFee: Double): OptionsGrid {
        return OptionsGrid(timeToFirstOption, optionLen, priceSize, nPriceIndex, nTimeIndex, marginHit, marginMiss, maxPayoutCoeff, bookingFee )
    }

    fun reinitGrid(instrument: String, timeToFirstOption: Long, optionLen: Long, priceSize: Double, nPriceIndex: Int, nTimeIndex: Int) {
        val cfg = config.instruments.find { it.name == instrument }
        if (cfg == null) {
            LOGGER.error("Unknown instrument $instrument")
        } else {
            val grid = initGrid(timeToFirstOption, optionLen, priceSize, nPriceIndex, nTimeIndex, cfg.marginHit, cfg.marginMiss, cfg.maxPayoutCoeff, cfg.bookingFee)
            val activities = activities[instrument]!!
            val history = historyHolder.getPrices(instrument)!!
            val currentPrice = history[history.size - 1]
            grid.initiateGrid(activities, history, cfg.delta, cfg.movingWindow, currentPrice)

            grids[instrument] = grid
            LOGGER.info("[$instrument] Updated grid")
        }
    }

    fun readActivityFile(fileName: String): ArrayList<Double> {
        val activity = ArrayList<Double>()
        try {
            val bufferedReader = BufferedReader(FileReader(fileName))
            var line: String? = bufferedReader.readLine()

            while (line != null) {
                activity.add(java.lang.Double.parseDouble(line))
                line = bufferedReader.readLine()
            }
            bufferedReader.close()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return activity
    }
}