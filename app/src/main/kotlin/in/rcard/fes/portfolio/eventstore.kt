package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventDataBuilder
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ExpectedRevision
import com.eventstore.dbclient.ExpectedRevision.noStream
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.StreamNotFoundException
import com.eventstore.dbclient.WrongExpectedVersionException
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioClosed
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksPurchased
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksSold
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.ConcurrentModificationError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.StateSavingError
import java.util.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias ETag = Long
typealias LoadedPortfolio = Pair<ETag, Portfolio>

interface PortfolioEventStore {

    suspend fun loadState(portfolioId: PortfolioId): Either<EventStoreError, LoadedPortfolio>

    suspend fun saveState(
        portfolioId: PortfolioId,
        eTag: ETag,
        oldPortfolio: Portfolio,
        newPortfolio: Portfolio,
    ): Either<EventStoreError, PortfolioId>

    sealed interface EventStoreError {

        data class UnknownStreamError(val portfolioId: PortfolioId) : EventStoreError
        data class StateLoadingError(val portfolioId: PortfolioId) : EventStoreError
        data class ConcurrentModificationError(val portfolioId: PortfolioId) : EventStoreError
        data class StateSavingError(val portfolioId: PortfolioId) : EventStoreError
    }
}

context (Json)
fun portfolioEventStore(eventStoreClient: EventStoreDBClient): PortfolioEventStore =
    object : PortfolioEventStore {

        override suspend fun loadState(portfolioId: PortfolioId): Either<EventStoreError, LoadedPortfolio> = either {
            val options = ReadStreamOptions.get()
                .forwards()
                .fromStart()
            val result = catch({
                eventStoreClient.readStream("portfolio-${portfolioId.id}", options).await()
            }) { error: Throwable ->
                when (error) {
                    is StreamNotFoundException -> raise(EventStoreError.UnknownStreamError(portfolioId))
                    else -> raise(EventStoreError.StateLoadingError(portfolioId))
                }
            }
            val eTag: Long = maxPosition(result.events)
            // TODO Should we trust what we read from the event store?
            val loadedEvents =
                result.events.map { decodeFromString<PortfolioEvent>(it.originalEvent.eventData.decodeToString()) }
            (eTag to loadedEvents)
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
                    else -> {
                        println("The error is: $error")
                        raise(StateSavingError(portfolioId))
                    }
                }
            }
            portfolioId
        }
    }

internal fun PortfolioEvent.eventType(): String = when (this) {
    is PortfolioCreated -> "PortfolioCreated"
    is StocksPurchased -> "StocksPurchased"
    is StocksSold -> "StocksSold"
    is PortfolioClosed -> "PortfolioClosed"
}
