package `in`.rcard.fes.portfolio

import arrow.core.Either
import com.eventstore.dbclient.EventStoreDBClient
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError

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
        TODO("Not yet implemented")
    }

    override suspend fun saveState(
        portfolioId: PortfolioId,
        eTag: ETag,
        portfolio: Portfolio,
    ): Either<EventStoreError, Unit> {
        TODO("Not yet implemented")
    }
}
