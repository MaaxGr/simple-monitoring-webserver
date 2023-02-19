package de.maaxgr.simplemonitoring

import AppConfig
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import de.maaxgr.simplemonitoring.businesslogic.NotificationHandler
import de.maaxgr.simplemonitoring.businesslogic.PingerHandler
import de.maaxgr.simplemonitoring.userinterface.Webserver
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module


fun main() {
    val config = ConfigLoaderBuilder.default()
        .addFileSource("config-env.yaml")
        .addFileSource("config-base.yaml")
        .build()
        .loadConfigOrThrow<AppConfig>()

    startKoin {
        val mainModule = module {
            single { config.webserver }
            single { NotificationHandler(config.notifications) }
            single { PingerHandler(config.pingers) }
        }
        modules(mainModule)
    }

    Webserver()
}