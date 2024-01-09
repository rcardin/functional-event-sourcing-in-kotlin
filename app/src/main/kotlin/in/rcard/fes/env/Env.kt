package `in`.rcard.fes.env

private const val DEFAULT_EVENT_STORE_URL = "esdb://localhost:2113"
private const val PORT: Int = 8080
private const val JDBC_URL: String = "jdbc:postgresql://localhost:5432/fes-database"
private const val JDBC_USER: String = "postgres"
private const val JDBC_PW: String = "postgres"
private const val JDBC_DRIVER: String = "org.postgresql.Driver"

data class Env(
    val http: Http = Http(),
    val eventStoreDataSource: EventStoreDataSource = EventStoreDataSource(),
    val dataSource: DataSource = DataSource(),
) {
    data class EventStoreDataSource(
        val url: String = System.getenv("EVENT_STORE_URL") ?: DEFAULT_EVENT_STORE_URL,
    )

    data class Http(
        val host: String = System.getenv("HOST") ?: "0.0.0.0",
        val port: Int = System.getenv("SERVER_PORT")?.toIntOrNull() ?: PORT,
    )

    data class DataSource(
        val url: String = System.getenv("POSTGRES_URL") ?: JDBC_URL,
        val username: String = System.getenv("POSTGRES_USERNAME") ?: JDBC_USER,
        val password: String = System.getenv("POSTGRES_PASSWORD") ?: JDBC_PW,
        val driver: String = JDBC_DRIVER,
    )
}
