package com.lykke.box.options.history

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lykke.box.options.daos.HistoryHolder
import com.lykke.box.options.daos.Price
import org.apache.log4j.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.naming.ConfigurationException

class HistoryLoader {
    companion object {
        val LOGGER = Logger.getLogger("HistoryLoader")
    }
    private val FROM_HOLDER = "{FROM_HOLDER}"
    private val TO_HOLDER = "{TO_HOLDER}"
    private val PAIR_HOLDER = "{PAIR_HOLDER}"


    private val historyUrl = "http://13.93.116.252:5050/api/history/BidHistory?dtFrom=$FROM_HOLDER&dtTo=$TO_HOLDER&assetPair=$PAIR_HOLDER"
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    //2017-05-20

    fun load(instruments: List<String>, historyHolder: HistoryHolder) {
        instruments.forEach {
            val now = LocalDate.now()
            var prevDay = now.minusDays(1)
            while (prevDay.dayOfWeek == DayOfWeek.SATURDAY || prevDay.dayOfWeek == DayOfWeek.SUNDAY) {
                prevDay = prevDay.minusDays(1)
            }

            val formattedUrl = historyUrl.replace(FROM_HOLDER, DATE_TIME_FORMATTER.format(prevDay)).replace(TO_HOLDER, DATE_TIME_FORMATTER.format(now)).replace(PAIR_HOLDER, it)
            LOGGER.info("[$it] Loading history: $formattedUrl")
            try {
                val cfgUrl = URL(formattedUrl)
                val connection = cfgUrl.openConnection()
                val inputStream = BufferedReader(InputStreamReader(connection.inputStream))

                val response = StringBuilder()
                var inputLine = inputStream.readLine()

                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = inputStream.readLine()
                }

                inputStream.close()

                val result = response.toString()
                if (result == "history is empty") {
                    LOGGER.error("[$it] Empty history")
                } else {
                    val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create()
                    val itemsListType = object : TypeToken<List<Price>>() {}.type
                    val ticks: List<Price> = gson.fromJson(response.toString(), itemsListType)
                    historyHolder.addAllPrices(it, ticks)
                }
            } catch (e: Exception) {
                LOGGER.error("History: ", e)
                throw ConfigurationException("Unable to read history prices from $formattedUrl: ${e.message}")
            }
        }
    }
}