package `in`.rcard.fes.env

import arrow.fx.coroutines.ResourceScope
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import `in`.rcard.fes.portfolio.PortfolioEventStore
import `in`.rcard.fes.portfolio.portfolioEventStore

class Dependencies(val portfolioEventStore: PortfolioEventStore)

suspend fun ResourceScope.dependencies(env: Env): Dependencies {
    val eventStoreClient = eventStoreClient(env.eventStoreDataSource)
    val portfolioEventStore = portfolioEventStore(eventStoreClient)
    return Dependencies(portfolioEventStore)
}

suspend fun ResourceScope.eventStoreClient(eventStoreDataSource: Env.EventStoreDataSource): EventStoreDBClient =
    install({
        val settings = EventStoreDBConnectionString.parseOrThrow(eventStoreDataSource.url)
        EventStoreDBClient.create(settings)
    }) { client, _ ->
        client.shutdown()
    }
