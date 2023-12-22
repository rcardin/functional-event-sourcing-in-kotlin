package `in`.rcard.fes.env

private const val DEFAULT_EVENT_STORE_URL = "esdb://localhost:2113"
private const val PORT: Int = 8080

data class Env(
    val http: Http = Http(),
    val eventStoreDataSource: EventStoreDataSource = EventStoreDataSource(),
) {
    data class EventStoreDataSource(
        val url: String = System.getenv("EVENT_STORE_URL") ?: DEFAULT_EVENT_STORE_URL,
    )

    data class Http(
        val host: String = System.getenv("HOST") ?: "0.0.0.0",
        val port: Int = System.getenv("SERVER_PORT")?.toIntOrNull() ?: PORT,
    )
}
