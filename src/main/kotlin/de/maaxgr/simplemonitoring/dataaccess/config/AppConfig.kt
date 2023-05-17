
data class AppConfig(
    val webserver: AppConfigWebserver,
    val pingers: List<AppConfigPinger>,
    val notifications: List<AppConfigNotifications>
)

data class AppConfigWebserver(
    val port: Int,
    val cors: List<String>,
    val adminUser: String,
    val adminPassword: String
)

data class AppConfigPinger(
    val targetHost: String,
    val retries: Int,
    val retryAfterSeconds: Int,
    val pingTimeoutSeconds: Int,
    val sleepSeconds: Int,
    val upMessage: String,
    val downMessage: String
)

sealed interface AppConfigNotifications

data class AppConfigPushover(
    val pushoverAppToken: String,
    val pushoverReceiverTokens: List<String>
): AppConfigNotifications