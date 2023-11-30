package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.right
import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ExpectedRevision
import com.eventstore.dbclient.ExpectedRevision.noStream
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
import java.util.*

typealias ETag = Long
typealias LoadedPortfolio = Pair<ETag, Portfolio>

interface PortfolioEventStore {

    suspend fun loadState(portfolioId: PortfolioId): Either<EventStoreError, LoadedPortfolio>

    suspend fun saveState(
        portfolioId: PortfolioId,
        eTag: ETag,
        oldPortfolio: Portfolio,
        newPortfolio: Portfolio,
    ): Either<EventStoreError, Unit>

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

        val eTag: Long = maxPosition(result.events)
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
        oldPortfolio: Portfolio,
        newPortfolio: Portfolio,
    ): Either<EventStoreError, Unit> {
        val eventsToPersist = newPortfolio - oldPortfolio

        val appendToStreamOptions: AppendToStreamOptions = AppendToStreamOptions.get().let { options ->
            when (eTag) {
                -1L -> options.expectedRevision(noStream())
                else -> options.expectedRevision(ExpectedRevision.expectedRevision(eTag))
            }
        }

        val eventDataList = eventsToPersist.map { event ->
            EventData.builderAsJson(
                UUID.randomUUID(), // TODO Should we move away from here?
                event.eventType(),
                event,
            ).build()
        }

        // WrongExpectedVersionException
        eventStoreClient.appendToStream("portfolio-${portfolioId.id}", appendToStreamOptions, eventDataList.iterator())

        return Unit.right()
    }
}

private fun PortfolioEvent.eventType(): String = when (this) {
    is PortfolioCreated -> "PortfolioCreated"
    is StocksPurchased -> "StocksPurchased"
    is StocksSold -> "StocksSold"
    is PortfolioClosed -> "PortfolioClosed"
}
