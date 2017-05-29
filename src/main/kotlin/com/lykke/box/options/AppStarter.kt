package com.lykke.box.options

import com.google.gson.Gson
import com.lykke.box.options.algo.GridsHolder
import com.lykke.box.options.algo.PriceProcessor
import com.lykke.box.options.config.Config
import com.lykke.box.options.daos.HistoryHolder
import com.lykke.box.options.daos.PricesHolder
import com.lykke.box.options.history.HistoryLoader
import com.lykke.box.options.http.PostRequestHandler
import com.lykke.box.options.http.RequestHandler
import com.lykke.box.options.rabbit.IncomingPrice
import com.lykke.box.options.rabbit.RabbitMqSubscriber
import com.sun.net.httpserver.HttpServer
import org.apache.log4j.Logger
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.LinkedBlockingQueue


val LOGGER = Logger.getLogger("AppStarter")

fun main(args: Array<String>) {
    val startTime = LocalDateTime.now()

    teeLog("Application launched at " + startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))

    Runtime.getRuntime().addShutdownHook(ShutdownHook())

    val config = loadConfig(args[0])
    val instruments = config.instruments.map { it.name }
    val historyHolder = HistoryHolder()
    val pricesHolder = PricesHolder()
    val incomingQueue = LinkedBlockingQueue<IncomingPrice>()

    HistoryLoader().load(instruments, historyHolder)

    val priceProcessor = PriceProcessor(incomingQueue, historyHolder, pricesHolder)
    priceProcessor.start()

    val gridsHolder = GridsHolder(config, historyHolder, pricesHolder)
    RabbitMqSubscriber(config.rabbitMq.host, config.rabbitMq.port, config.rabbitMq.username, config.rabbitMq.password, config.rabbitMq.exchange, incomingQueue, config.instruments.map { it.name }.toSet()).start()
    LOGGER.info(historyHolder.toString())

    val server = HttpServer.create(InetSocketAddress(config.httpPort), 0)
    server.createContext("/request", RequestHandler(gridsHolder))
    server.createContext("/change", PostRequestHandler(gridsHolder))
    server.executor = null // creates a default executor
    server.start()
}

private fun loadConfig(path: String): Config {
    val file = File(path)
    return Gson().fromJson(FileReader(file), Config::class.java)
}

private fun teeLog(message: String) {
    println(message)
    LOGGER.info(message)
}

internal class ShutdownHook : Thread() {
    init {
        this.name = "ShutdownHook"
    }

    override fun run() {
        LOGGER.info("Stopping application")
    }
}