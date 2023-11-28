package `in`.rcard.fes.env

import arrow.fx.coroutines.ResourceScope
import com.eventstore.dbclient.EventStoreDBClient

class Dependencies

suspend fun ResourceScope.dependencies(env: Env): Dependencies {
    val eventStoreClient = eventStoreClient(env.eventStoreDataSource)
    return Dependencies()
}

suspend fun ResourceScope.eventStoreClient(eventStoreDataSource: Env.EventStoreDataSource): EventStoreDBClient {
    TODO("Not yet implemented")
}
