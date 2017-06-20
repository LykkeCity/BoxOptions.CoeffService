package com.lykke.box.options.rabbit

import com.lykke.box.options.rabbit.parser.IncomingParser
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.apache.log4j.Logger
import java.util.UUID
import java.util.concurrent.BlockingQueue


class RabbitMqSubscriber(
        private val host: String,
        private val port: Int,
        private val username: String,
        private val password: String,
        private val exchangeName: String,
        private val queueName: String,
        private val parser: IncomingParser<IncomingPrice>,
        private val queue: BlockingQueue<IncomingPrice>,
        private val instruments: Set<String>) : Thread() {

    companion object {
        val LOGGER = Logger.getLogger(RabbitMqSubscriber::class.java.name)
    }

    var connection: Connection? = null
    var channel: Channel? = null
    val customQueueName = "${queueName}_${UUID.randomUUID()}"

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
            channel!!.exchangeDeclarePassive(exchangeName)
            channel!!.queueDeclare(customQueueName, false, false, false, null)
            channel!!.queueBind(customQueueName, exchangeName, "")

            LOGGER.info("Connected to RabbitMQ: $host:$port, exchange: $exchangeName, queue: $customQueueName. Instruments: $instruments")

            return true
        } catch (e: Exception) {
            LOGGER.error("Unable to connect to RabbitMQ: $host:$port, exchange: $exchangeName, queue: $customQueueName: ${e.message}", e)
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
                val prices = parser.parse(String(body))
                prices.forEach { price ->
                    if (instruments.contains(price.instrument)) {
                        queue.put(price)
                    }
                }
            }
        }
        channel!!.basicConsume(customQueueName, true, consumer)
    }
}