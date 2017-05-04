package com.lykke.box.options.config

data class Config(
    var httpPort: Int,
    var instruments: Array<InstrumentConfig>,
    var rabbitMq: RabbitConfig
)