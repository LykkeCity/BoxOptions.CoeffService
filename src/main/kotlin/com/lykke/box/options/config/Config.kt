package com.lykke.box.options.config

data class Config(
    var httpPort: Int,
    var instruments: Set<InstrumentConfig>,
    var rabbitMq: Set<RabbitConfig>
)