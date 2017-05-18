package com.lykke.box.options.rabbit

import com.google.gson.Gson
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.apache.log4j.Logger
import java.util.concurrent.BlockingQueue


class RabbitMqSubscriber(val host: String, val port: Int, val username: String, val password: String, val exchangeName: String, val queue: BlockingQueue<IncomingPrice>, val instruments: Set<String>) : Thread() {

    companion object {
        val LOGGER = Logger.getLogger(RabbitMqSubscriber::class.java.name)
        val EXCHANGE_TYPE = "fanout"
    }

    var connection: Connection? = null
    var channel: Channel? = null
    var queueName: String? = null

    fun connect(): Boolean {
        LOGGER.info("Connecting to RabbitMQ: $host:$port, exchange: $exchangeName")

        try {
            val factory = ConnectionFactory()
            factory.host = host
            factory.port = port
            factory.username = username
            factory.password = password

            this.connection = factory.newConnection()
            this.channel = connection!!.createChannel()
            channel!!.exchangeDeclare(exchangeName, EXCHANGE_TYPE, true)

            queueName = channel!!.queueDeclare().queue
            channel!!.queueBind(queueName, exchangeName, "")

            LOGGER.info("Connected to RabbitMQ: $host:$port, exchange: $exchangeName")

            return true
        } catch (e: Exception) {
            LOGGER.error("Unable to connect to RabbitMQ: $host:$port, exchange: $exchangeName: ${e.message}", e)
            return false
        }
    }

    override fun run() {
        while (!connect()) {
            Thread.sleep(1000)
        }
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(consumerTag: String, envelope: Envelope,
                                        properties: AMQP.BasicProperties, body: ByteArray) {
                val message = String(body)
                val orderBook = Gson().fromJson(message, OrderBook::class.java)
                if (instruments.contains(orderBook.assetPair) && orderBook.prices.isNotEmpty()) {
                    queue.put(IncomingPrice(orderBook.assetPair, orderBook.timestamp.time, orderBook.isBuy, orderBook.prices.first().price))
                }
            }
        }
        channel!!.basicConsume(queueName, true, consumer)
    }
}