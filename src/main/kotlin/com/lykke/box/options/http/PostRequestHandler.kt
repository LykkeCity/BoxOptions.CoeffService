package com.lykke.box.options.http

import com.lykke.box.options.algo.GridsHolder
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import org.apache.log4j.Logger
import java.net.URLDecoder
import java.util.HashMap

class PostRequestHandler (val gridsHolder: GridsHolder) : HttpHandler {
    companion object {
        val LOGGER = Logger.getLogger(RequestHandler::class.java.name)
    }

    override fun handle(exchange: HttpExchange) {
        try {
            val parameters = HashMap<String, String>()
            val query = exchange.requestURI.rawQuery
            parseQuery(query, parameters)

            val instrument = parameters["pair"]!!
            val timeToFirstOption = parameters["timeToFirstOption"]!!.toLong()
            val optionLen = parameters["optionLen"]!!.toLong()
            val priceSize = parameters["priceSize"]!!.toDouble()
            val nPriceIndex = parameters["nPriceIndex"]!!.toInt()
            val nTimeIndex = parameters["nTimeIndex"]!!.toInt()

            LOGGER.info(query)

            gridsHolder.reinitGrid(instrument, timeToFirstOption, optionLen, priceSize, nPriceIndex, nTimeIndex)

            val response = "OK"
            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
            LOGGER.info("Order book snapshot sent to ${exchange.remoteAddress}")
        } catch (e: Exception) {
            LOGGER.error("Unable to write order book snapshot request to ${exchange.remoteAddress}", e)
        }
    }

    fun parseQuery(query: String?, parameters: MutableMap<String, String>) {
        if (query != null) {
            val pairs = query.split("&")
            for (pair in pairs) {
                val param = pair.split("=")
                if (param.size == 2) {
                    val key: String = URLDecoder.decode(param[0], "utf-8")
                    val value: String = URLDecoder.decode(param[1], "utf-8")
                    parameters.put(key, value)
                }
            }
        }
    }
}