package `in`.rcard.fes.env

private const val DEFAULT_EVENT_STORE_URL = "esdb://localhost:2113"

data class Env(
    val eventStoreDataSource: EventStoreDataSource = EventStoreDataSource(),
) {
    data class EventStoreDataSource(
        val url: String = System.getenv("EVENT_STORE_URL") ?: DEFAULT_EVENT_STORE_URL,
    )
}
