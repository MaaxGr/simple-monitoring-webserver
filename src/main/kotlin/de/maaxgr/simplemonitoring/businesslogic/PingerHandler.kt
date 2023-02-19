package de.maaxgr.simplemonitoring.businesslogic

import AppConfigPinger
import org.koin.core.component.KoinComponent
import kotlin.concurrent.thread

class PingerHandler(private val pingerConfigs: List<AppConfigPinger>): KoinComponent {

    private val pingers = mutableListOf<Pinger>()

    fun start() {
        for (pingerConfig in pingerConfigs) {
            val pinger = Pinger(pingerConfig)
            pingers.add(pinger)
            thread { pinger.run() }
        }
    }

    fun getPingerStates(): Map<String, Pinger.State> {
        return pingers.associate { it.pingerConfig.targetHost to it.storedReachableState }
    }

}