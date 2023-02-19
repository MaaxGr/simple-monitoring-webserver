package de.maaxgr.simplemonitoring.businesslogic

import AppConfigNotifications
import AppConfigPushover
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory

class NotificationHandler(private val notificationConfigs: List<AppConfigNotifications>) : KoinComponent {

    private val logger = LoggerFactory.getLogger(NotificationHandler::class.java)

    fun sendDownNotification(message: String) {
        for (configNotification in notificationConfigs) {
            when (configNotification) {
                is AppConfigPushover -> {
                    runBlocking { sendPushoverNotification(configNotification, message, 1) }
                }

                else -> {
                    logger.warn("Unknown notification type: ${configNotification::class.simpleName}")
                }
            }
        }
    }

    fun sendUpNotification(message: String) {
        for (configNotification in notificationConfigs) {
            when (configNotification) {
                is AppConfigPushover -> {
                    runBlocking { sendPushoverNotification(configNotification, message, 0) }
                }

                else -> {
                    logger.warn("Unknown notification type: ${configNotification::class.simpleName}")
                }
            }
        }
    }

    private val pusherHttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private suspend fun sendPushoverNotification(
        pushoverConfig: AppConfigPushover,
        message: String,
        priority: Int
    ) {

        for (pushoverReceiverToken in pushoverConfig.pushoverReceiverTokens) {
            val response = pusherHttpClient.post("https://api.pushover.net/1/messages.json") {
                header("Content-Type", "application/json")
                setBody(buildJsonObject {
                    put("token", pushoverConfig.pushoverAppToken)
                    put("user", pushoverReceiverToken)
                    put("message", message)
                    put("priority", priority)

                    if (priority == 1) {
                        put("sound", "siren")
                    }

                })
            }
            if (response.status == HttpStatusCode.OK) {
                logger.info("Sending Pushover notification '$message' to $pushoverReceiverToken successful")
            } else {
                logger.error("Sending Pushover notification '$message' to " +
                        "$pushoverReceiverToken failed (Code: ${response.status})")
            }
        }


    }


}