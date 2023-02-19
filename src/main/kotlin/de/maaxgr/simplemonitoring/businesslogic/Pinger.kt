package de.maaxgr.simplemonitoring.businesslogic

import AppConfigPinger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class Pinger(val pingerConfig: AppConfigPinger): KoinComponent {

    private val notificationHandler: NotificationHandler by inject()
    private val logger = LoggerFactory.getLogger(Pinger::class.java)

    var storedReachableState = State.INITIAL
        private set

    fun run() {
        while (true) {
            logger.info("Checking Ping-State for ${pingerConfig.targetHost}...")
            val localReachableState = pingWithRetry(
                pingerConfig.targetHost,
                retries = pingerConfig.retries,
                retryAfterSeconds = pingerConfig.retryAfterSeconds,
                timeoutSeconds = pingerConfig.pingTimeoutSeconds
            )
            logger.info("Local state is: $localReachableState")

            if (localReachableState != storedReachableState) {
                if (storedReachableState != State.INITIAL && localReachableState == State.UP) {
                    notificationHandler.sendUpNotification(pingerConfig.upMessage)
                }
                if (localReachableState == State.DOWN) {
                    notificationHandler.sendDownNotification(pingerConfig.downMessage)
                }

                storedReachableState = localReachableState
                logger.info("Pinger state changed to: $storedReachableState")

            }
            Thread.sleep(60000)
        }
    }

    enum class State {
        INITIAL, UP, DOWN
    }

    private fun pingWithRetry(hostname: String, retries: Int, retryAfterSeconds: Int, timeoutSeconds: Int): State {
        var tryCounter = 1

        while (true) {
            val reachable = ping(hostname, timeoutSeconds)

            if (reachable) {
                return State.UP
            } else {
                if (tryCounter > retries) {
                    return State.DOWN
                }

                logger.debug("Host $hostname is not reachable. Try again in $retryAfterSeconds seconds ($tryCounter/$retries)")
                tryCounter++
                Thread.sleep(retryAfterSeconds * 1000L)
            }
        }
    }

    private fun ping(hostname: String, timeoutSeconds: Int): Boolean {
        val process = Runtime.getRuntime().exec("ping -c 1 $hostname")
        var processCompleted = false
        try {
            processCompleted = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return if (processCompleted) {
            val exitCode = process.exitValue()
            exitCode == 0
        } else {
            process.destroyForcibly()
            false
        }
    }

}