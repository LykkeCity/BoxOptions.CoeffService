package com.lykke.box.options.history

import com.dukascopy.api.IAccount
import com.dukascopy.api.IBar
import com.dukascopy.api.IContext
import com.dukascopy.api.IHistory
import com.dukascopy.api.IMessage
import com.dukascopy.api.IStrategy
import com.dukascopy.api.ITick
import com.dukascopy.api.Instrument
import com.dukascopy.api.Period
import com.dukascopy.api.util.DateUtils
import com.lykke.box.options.daos.HistoryHolder
import org.apache.log4j.Logger
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.HashSet


class HistoryTicksSync(val instrumentsSet: HashSet<Instrument>, val historyHolder: HistoryHolder, var running: Boolean) : IStrategy {
    companion object {
        val LOGGER = Logger.getLogger("HistoryTicksSync")
    }

    private var history: IHistory? = null

    override fun onStart(context: IContext) {
        history = context.history
        context.setSubscribedInstruments(instrumentsSet, true)
        instrumentsSet.forEach {
            historyHolder.addAllPrices(it, getTicksByTimeInterval(it))
        }
        running = false
    }

    private fun getTicksByTimeInterval(instrument: Instrument): MutableList<ITick> {
        val lastTick = history!!.getLastTick(instrument)
        val now = Date().time
        var prevDay = now - 24 * 60 * 60 * 1000
        var prevDayDate = Instant.ofEpochMilli(prevDay).atZone(ZoneId.systemDefault()).toLocalDate()
        while (prevDayDate.dayOfWeek == DayOfWeek.SATURDAY || prevDayDate.dayOfWeek == DayOfWeek.SUNDAY) {
            prevDay =- 24 * 60 * 60 * 1000
            prevDayDate = Instant.ofEpochMilli(prevDay).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        val ticks = history!!.getTicks(instrument, prevDay, lastTick.time)


        val last = ticks.size - 1
        LOGGER.info(String.format(
                "Tick count=%s; Latest bid price=%.5f, time=%s; Oldest bid price=%.5f, time=%s",
                ticks.size, ticks[last].bid, DateUtils.format(ticks[last].time), ticks[0].bid, DateUtils.format(ticks[0].time)))
        return ticks
    }


    override fun onTick(instrument: Instrument, tick: ITick) {
    }

    override fun onBar(instrument: Instrument, period: Period, askBar: IBar, bidBar: IBar) {
    }

    override fun onMessage(message: IMessage) {
    }

    override fun onAccount(account: IAccount) {
    }

    override fun onStop() {
        LOGGER.info("Stop strategy")
    }

}