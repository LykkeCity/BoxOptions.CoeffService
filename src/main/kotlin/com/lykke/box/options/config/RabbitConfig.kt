package com.lykke.box.options.config

data class RabbitConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val exchange: String
)