package de.maaxgr.simplemonitoring.userinterface

import AppConfigWebserver
import de.maaxgr.simplemonitoring.businesslogic.Pinger
import de.maaxgr.simplemonitoring.businesslogic.PingerHandler
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Suppress("ExtractKtorModule")
class Webserver: KoinComponent {

    private val logger: Logger = LoggerFactory.getLogger(Webserver::class.java)
    private val pingerHandler: PingerHandler by inject()
    private val configWebserver: AppConfigWebserver by inject()

    init {
        pingerHandler.start()

        embeddedServer(Netty, port = configWebserver.port) {
            configureSecurity()
            configureCORS()
            configureSerialization()

            routing {
                get("/") {
                    call.respondText("Simple Monitoring!")
                }

                authenticate {
                    get("/state") {
                        val anyDown = pingerHandler.getPingerStates().any { it.value == Pinger.State.DOWN }

                        if (!anyDown) {
                            call.respond(HttpStatusCode.OK, pingerHandler.getPingerStates())
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, pingerHandler.getPingerStates())
                        }
                    }
                }
            }

        }.start(wait = true)
    }

    private fun Application.configureSerialization() {
        install(ContentNegotiation) {
            json()
        }
    }

    private fun Application.configureCORS() {
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)

            for (cor in configWebserver.cors) {
                if (cor == "*") {
                    anyHost()
                } else {
                    try {
                        val (schema, host) = cor.split("://")
                        allowHost(host, listOf(schema))
                    } catch (e: Exception) {
                        logger.error("Invalid CORS entry: $cor (Message: ${e.message})")
                    }
                }
            }
        }
    }

    fun Application.configureSecurity() {
        authentication {
            basic {
                realm = "Ktor Server"
                validate { credentials ->
                    if (credentials.name == configWebserver.adminUser
                        && credentials.password == configWebserver.adminPassword) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }
    }

}