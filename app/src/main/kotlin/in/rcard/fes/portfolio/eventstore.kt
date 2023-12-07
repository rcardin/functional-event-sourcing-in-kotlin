package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventDataBuilder
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ExpectedRevision
import com.eventstore.dbclient.ExpectedRevision.noStream
import com.eventstore.dbclient.ReadResult
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.WrongExpectedVersionException
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioClosed
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksPurchased
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksSold
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.ConcurrentModificationError
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.CancellationException

typealias ETag = Long
typealias LoadedPortfolio = Pair<ETag, Portfolio>

interface PortfolioEventStore {

    context (Json)
    suspend fun loadState(portfolioId: PortfolioId): Either<EventStoreError, LoadedPortfolio>

    context (Json)
    suspend fun saveState(
        portfolioId: PortfolioId,
        eTag: ETag,
        oldPortfolio: Portfolio,
        newPortfolio: Portfolio,
    ): Either<EventStoreError, PortfolioId>

    sealed interface EventStoreError {
        data class StateLoadingError(val portfolioId: PortfolioId) : EventStoreError
        data class ConcurrentModificationError(val portfolioId: PortfolioId) : EventStoreError
        data class StateSavingError(val portfolioId: PortfolioId) : EventStoreError
    }
}

context (Json)
fun portfolioEventStore(eventStoreClient: EventStoreDBClient): PortfolioEventStore = object : PortfolioEventStore {

    override suspend fun loadState(portfolioId: PortfolioId): Either<EventStoreError, LoadedPortfolio> {
        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

        // TODO Check if the stream exists
        val result: ReadResult = eventStoreClient.readStream("portfolio-${portfolioId.id}", options).await()

        val eTag: Long = maxPosition(result.events)
        val loadedEvents =
            result.events.map { decodeFromString<PortfolioEvent>(it.originalEvent.eventData.decodeToString()) }
        return (eTag to loadedEvents).right()
    }

    private fun maxPosition(events: List<ResolvedEvent>) =
        events.maxByOrNull { it.originalEvent.revision }!!.originalEvent.revision

    override suspend fun saveState(
        portfolioId: PortfolioId,
        eTag: ETag,
        oldPortfolio: Portfolio,
        newPortfolio: Portfolio,
    ): Either<EventStoreError, PortfolioId> = either {
        val eventsToPersist = newPortfolio - oldPortfolio

        val appendToStreamOptions: AppendToStreamOptions = AppendToStreamOptions.get().let { options ->
            when (eTag) {
                -1L -> options.expectedRevision(noStream())
                else -> options.expectedRevision(ExpectedRevision.expectedRevision(eTag))
            }
        }

        val eventDataList = eventsToPersist.map { event ->
            EventDataBuilder.json(
                UUID.randomUUID(),
                event.eventType(),
                encodeToString(event).encodeToByteArray(),
            ).build()
        }

        catch(
            {
                eventStoreClient.appendToStream(
                    "portfolio-${portfolioId.id}",
                    appendToStreamOptions,
                    eventDataList.iterator(),
                )
                    .await()
            },
        ) { error: Throwable ->
            when (error) {
                is WrongExpectedVersionException -> raise(ConcurrentModificationError(portfolioId))
                // TODO Is it correct?
                is CancellationException -> throw error
                else -> {
                    println("The error is: $error")
                    raise(EventStoreError.StateSavingError(portfolioId))
                }
            }
        }
        portfolioId
    }
}

private fun PortfolioEvent.eventType(): String = when (this) {
    is PortfolioCreated -> "PortfolioCreated"
    is StocksPurchased -> "StocksPurchased"
    is StocksSold -> "StocksSold"
    is PortfolioClosed -> "PortfolioClosed"
}
