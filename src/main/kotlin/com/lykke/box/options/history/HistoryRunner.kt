package com.lykke.box.options.history

import com.dukascopy.api.Instrument
import com.dukascopy.api.system.ClientFactory
import com.dukascopy.api.system.ISystemListener
import com.lykke.box.options.daos.HistoryHolder
import org.apache.log4j.Logger
import java.util.HashSet

class HistoryRunner {
    companion object {
        val LOGGER = Logger.getLogger("HistoryRunner")
    }

    //url of the DEMO jnlp
    private val jnlpUrl = "http://platform.dukascopy.com/demo/jforex.jnlp"
    //user name
    private val userName = "DEMO2RTxea"
    //password
    private val password = "RTxea"


    fun loadHistory(instruments: List<String>, historyHolder: HistoryHolder) {
        //get the instance of the IClient interface
        val client = ClientFactory.getDefaultInstance()
        //set the listener that will receive system events
        client.setSystemListener(object : ISystemListener {
            private var lightReconnects = 3

            override fun onStart(processId: Long) {
                LOGGER.info("Strategy started: " + processId)
            }

            override fun onStop(processId: Long) {
                LOGGER.info("Strategy stopped: " + processId)
            }

            override fun onConnect() {
                LOGGER.info("Connected")
                lightReconnects = 3
            }

            override fun onDisconnect() {
                val runnable = Runnable {
                    if (lightReconnects > 0) {
                        client.reconnect()
                        --lightReconnects
                    } else {
                        do {
                            try {
                                Thread.sleep((60 * 1000).toLong())
                            } catch (e: InterruptedException) {
                            }

                            try {
                                if (client.isConnected) {
                                    break
                                }
                                client.connect(jnlpUrl, userName, password)

                            } catch (e: Exception) {
                                LOGGER.error(e.message, e)
                            }

                        } while (!client.isConnected)
                    }
                }
                Thread(runnable).start()
            }
        })

        LOGGER.info("Connecting...")
        //connect to the server using jnlp, user name and password
        client.connect(jnlpUrl, userName, password)

        //wait for it to connect
        var i = 10 //wait max ten seconds
        while (i > 0 && !client.isConnected) {
            Thread.sleep(1000)
            i--
        }
        if (!client.isConnected) {
            LOGGER.error("Failed to connect Dukascopy servers")
            System.exit(1)
        }

        //subscribe to the instruments
        Instrument.getPairsSeparator()
        val instrumentsSet = HashSet<Instrument>()
        instrumentsSet.addAll(instruments.map { Instrument.fromString(it) })
        LOGGER.info("Subscribing instruments: $instrumentsSet")
        client.subscribedInstruments = instrumentsSet

        //start the strategy
        LOGGER.info("Starting strategy")
        var running = true
        val id = client.startStrategy(HistoryTicksSync(instrumentsSet, historyHolder, running))
        client.stopStrategy(id)
//        while (running) {
//            Thread.sleep(300000)
//        }
//        client.disconnect()
        //now it's running
    }
}