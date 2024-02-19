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
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.LoadingError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.LoadingError.StateLoadingError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.LoadingError.UnknownStreamError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.SavingError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.SavingError.ConcurrentModificationError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.SavingError.StateSavingError
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.util.UUID

typealias ETag = Long
typealias LoadedPortfolio = Pair<ETag, Portfolio>

interface PortfolioEventStore {
    suspend fun loadState(portfolioId: PortfolioId): Either<LoadingError, LoadedPortfolio>

    suspend fun saveState(
        portfolioId: PortfolioId,
        eTag: ETag,
        oldPortfolio: Portfolio,
        newPortfolio: Portfolio,
    ): Either<SavingError, PortfolioId>

    sealed interface EventStoreError {
        sealed interface LoadingError : EventStoreError {
            data class UnknownStreamError(val portfolioId: PortfolioId) : LoadingError

            data class StateLoadingError(val portfolioId: PortfolioId) : LoadingError
        }

        sealed interface SavingError : EventStoreError {
            data class ConcurrentModificationError(val portfolioId: PortfolioId) : SavingError

            data class StateSavingError(val portfolioId: PortfolioId) : SavingError
        }
    }
}

context (Json, Logger)
fun portfolioEventStore(eventStoreClient: EventStoreDBClient): PortfolioEventStore =
    object : PortfolioEventStore {
        override suspend fun loadState(portfolioId: PortfolioId): Either<LoadingError, LoadedPortfolio> =
            either {
                val options =
                    ReadStreamOptions.get()
                        .forwards()
                        .fromStart()
                catch({
                    val result = eventStoreClient.readStream("portfolio-${portfolioId.id}", options).await()
                    val eTag: Long = maxPosition(result.events)
                    val loadedEvents =
                        result.events.map { decodeFromString<PortfolioEvent>(it.originalEvent.eventData.decodeToString()) }
                    (eTag to loadedEvents)
                }) { exception: Throwable ->
                    when (exception) {
                        is StreamNotFoundException -> raise(UnknownStreamError(portfolioId))
                        else -> {
                            this@Logger.error(
                                "Generic error while loading the state of portfolio {}",
                                portfolioId,
                                exception,
                            )
                            raise(StateLoadingError(portfolioId))
                        }
                    }
                }
            }

        private fun maxPosition(events: List<ResolvedEvent>) = events.maxByOrNull { it.originalEvent.revision }!!.originalEvent.revision

        override suspend fun saveState(
            portfolioId: PortfolioId,
            eTag: ETag,
            oldPortfolio: Portfolio,
            newPortfolio: Portfolio,
        ): Either<SavingError, PortfolioId> =
            either {
                val eventsToPersist = newPortfolio - oldPortfolio

                val appendToStreamOptions: AppendToStreamOptions =
                    AppendToStreamOptions.get().let { options ->
                        when (eTag) {
                            -1L -> options.expectedRevision(noStream())
                            else -> options.expectedRevision(ExpectedRevision.expectedRevision(eTag))
                        }
                    }

                val eventDataList =
                    eventsToPersist.map { event ->
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
                ) { exception: Throwable ->
                    when (exception) {
                        is WrongExpectedVersionException -> raise(ConcurrentModificationError(portfolioId))
                        else -> {
                            this@Logger.error(
                                "Generic error while saving the state of portfolio {}",
                                portfolioId,
                                exception,
                            )
                            println("The error is: $exception")
                            raise(StateSavingError(portfolioId))
                        }
                    }
                }
                portfolioId
            }
    }

internal fun PortfolioEvent.eventType(): String =
    when (this) {
        is PortfolioCreated -> "portfolio-created"
        is StocksPurchased -> "stocks-purchased"
        is StocksSold -> "stocks-sold"
        is PortfolioClosed -> "portfolio-closed"
    }
