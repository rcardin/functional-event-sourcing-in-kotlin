package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.right
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ReadResult
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.RecordedEvent
import com.eventstore.dbclient.ResolvedEvent
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioClosed
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksPurchased
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksSold
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json

typealias ETag = String
typealias LoadedPortfolio = Pair<ETag, Portfolio>

interface PortfolioEventStore {

    suspend fun loadState(portfolioId: PortfolioId): Either<EventStoreError, LoadedPortfolio>

    suspend fun saveState(portfolioId: PortfolioId, eTag: ETag, portfolio: Portfolio): Either<EventStoreError, Unit>

    sealed interface EventStoreError {
        data class StateLoadingError(val portfolioId: PortfolioId) : EventStoreError
        data class ConcurrentModificationError(val portfolioId: PortfolioId) : EventStoreError
        data class StateSavingError(val portfolioId: PortfolioId) : EventStoreError
    }
}

fun portfolioEventStore(eventStoreClient: EventStoreDBClient): PortfolioEventStore = object : PortfolioEventStore {
    override suspend fun loadState(portfolioId: PortfolioId): Either<EventStoreError, LoadedPortfolio> {
        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

        // TODO Check if the stream exists
        val result: ReadResult = eventStoreClient.readStream("portfolio-${portfolioId.id}", options).await()

        val eTag: String = maxPosition(result.events).toString()
        val loadedEvents = result.events.map { decode(it.originalEvent) }
        return (eTag to loadedEvents).right()
    }

    private fun decode(recordedEvent: RecordedEvent): PortfolioEvent =
        when (recordedEvent.eventType) {
            "PortfolioCreated" -> Json.decodeFromString<PortfolioCreated>(recordedEvent.eventData.decodeToString())
            "StocksPurchased" -> Json.decodeFromString<StocksPurchased>(recordedEvent.eventData.decodeToString())
            "StocksSold" -> Json.decodeFromString<StocksSold>(recordedEvent.eventData.decodeToString())
            "PortfolioClosed" -> Json.decodeFromString<PortfolioClosed>(recordedEvent.eventData.decodeToString())
            else -> throw IllegalArgumentException("Unknown event type ${recordedEvent.eventType}")
            // TODO Use a sealed class to represent the error
        }

    private fun maxPosition(events: List<ResolvedEvent>) =
        events.maxByOrNull { it.originalEvent.revision }!!.originalEvent.revision

    override suspend fun saveState(
        portfolioId: PortfolioId,
        eTag: ETag,
        portfolio: Portfolio,
    ): Either<EventStoreError, Unit> {
        TODO("Not yet implemented")
    }
}
