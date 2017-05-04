package com.lykke.box.options.http

import com.google.gson.GsonBuilder
import com.lykke.box.options.algo.GridsHolder
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import org.apache.log4j.Logger
import java.net.URLDecoder
import java.util.HashMap

class RequestHandler (val gridsHolder: GridsHolder) : HttpHandler {
    companion object {
        val LOGGER = Logger.getLogger(RequestHandler::class.java.name)
    }

    override fun handle(exchange: HttpExchange) {
        try {
            val parameters = HashMap<String, String>()
            val query = exchange.requestURI.rawQuery
            parseQuery(query, parameters)

            val instrument = parameters["pair"]!!

            LOGGER.info(query)

            val grid = gridsHolder.getGrid(instrument)!!

            val response = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create().toJson(grid.optionsGrid)
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