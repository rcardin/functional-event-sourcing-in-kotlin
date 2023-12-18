package `in`.rcard.fes.portfolio

import arrow.core.Either
import java.util.*

interface CreatePortfolioUseCase {
    suspend fun createPortfolio(model: CreatePortfolio): Either<DomainError, PortfolioId>
}

fun createPortfolioUseCase(portfolioService: PortfolioService) = object : CreatePortfolioUseCase {
    override suspend fun createPortfolio(model: CreatePortfolio): Either<DomainError, PortfolioId> =
        PortfolioCommand.CreatePortfolio(
            // TODO Move this away from here
            PortfolioId(UUID.randomUUID().toString()),
            System.currentTimeMillis(),
            model.userId,
            model.amount,
        ).let { portfolioService.handle(it) }
}

data class CreatePortfolio(val userId: UserId, val amount: Money)