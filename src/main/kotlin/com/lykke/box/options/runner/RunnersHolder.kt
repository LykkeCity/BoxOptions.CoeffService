package com.lykke.box.options.runner

import com.lykke.box.options.config.Config
import com.lykke.box.options.daos.HistoryHolder
import kotlin.concurrent.fixedRateTimer

class RunnersHolder(val config: Config, val historyHolder: HistoryHolder) {
    init {
        config.instruments.forEach {
            fixedRateTimer(name = it.name, period = it.period) {

            }
        }
    }
}