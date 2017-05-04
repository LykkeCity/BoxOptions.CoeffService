package com.lykke.box.options.algo

import com.lykke.box.options.daos.HistoryHolder
import com.lykke.box.options.daos.Price
import com.lykke.box.options.daos.PricesHolder
import com.lykke.box.options.rabbit.IncomingPrice
import java.util.HashMap
import java.util.concurrent.BlockingQueue

class PriceProcessor(val queue: BlockingQueue<IncomingPrice>, val historyHolder: HistoryHolder, val pricesHolder: PricesHolder): Thread() {

    private val lastPrices = HashMap<String, Price>()

    override fun run() {
        while (true) {
            val item = queue.take()

            val price = lastPrices.getOrPut(item.instrument) { Price(0, 0.0, 0.0) }
            price.time = item.timestamp
            if (item.isBuy) {
                price.bid = item.price
            } else {
                price.ask = item.price
            }

            if (price.ask > 0 && price.bid > 0) {
                historyHolder.addPrice(item.instrument, price.clonePrice())
                pricesHolder.addPrice(item.instrument, price.clonePrice())
            }
        }
    }
}