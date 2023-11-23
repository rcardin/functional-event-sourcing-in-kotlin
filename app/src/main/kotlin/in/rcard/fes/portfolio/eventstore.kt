package `in`.rcard.fes.portfolio

import arrow.core.Either

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
