package `in`.rcard.fes.env

import arrow.fx.coroutines.ResourceScope
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import `in`.rcard.fes.portfolio.CreatePortfolioUseCase
import `in`.rcard.fes.portfolio.createPortfolioUseCase
import `in`.rcard.fes.portfolio.portfolioEventStore
import `in`.rcard.fes.portfolio.portfolioService
import java.util.*
import kotlinx.serialization.json.Json

class Dependencies(val createPortfolioUseCase: CreatePortfolioUseCase)

suspend fun ResourceScope.dependencies(env: Env): Dependencies {
    val eventStoreClient = eventStoreClient(env.eventStoreDataSource)
    val portfolioEventStore = with(jsonModule()) {
        portfolioEventStore(eventStoreClient)
    }
    val portfolioService = portfolioService(portfolioEventStore)
    val createPortfolioUseCase = with(uuidGenerator()) {
        with(clock()) {
            createPortfolioUseCase(portfolioService)
        }
    }
    return Dependencies(createPortfolioUseCase)
}

suspend fun ResourceScope.eventStoreClient(eventStoreDataSource: Env.EventStoreDataSource): EventStoreDBClient =
    install({
        val settings = EventStoreDBConnectionString.parseOrThrow(eventStoreDataSource.url)
        EventStoreDBClient.create(settings)
    }) { client, _ ->
        client.shutdown()
    }

fun jsonModule() = Json

interface UUIDGenerator {
    suspend fun uuid(): String
}

fun uuidGenerator() = object : UUIDGenerator {
    override suspend fun uuid(): String = UUID.randomUUID().toString()
}

interface Clock {
    suspend fun currentTimeMillis(): Long
}

fun clock() = object : Clock {
    override suspend fun currentTimeMillis(): Long = System.currentTimeMillis()
}
