package `in`.rcard.fes.portfolio

import arrow.core.Either
import `in`.rcard.fes.env.Clock
import `in`.rcard.fes.env.UUIDGenerator

interface CreatePortfolioUseCase {
    suspend fun createPortfolio(model: CreatePortfolio): Either<DomainError, PortfolioId>
}

context(UUIDGenerator, Clock)
fun createPortfolioUseCase(portfolioService: PortfolioService) = object : CreatePortfolioUseCase {
    override suspend fun createPortfolio(model: CreatePortfolio): Either<DomainError, PortfolioId> =
        PortfolioCommand.CreatePortfolio(
            PortfolioId(uuid()),
            currentTimeMillis(),
            model.userId,
            model.amount,
        ).let { portfolioService.handle(it) }
}

data class CreatePortfolio(val userId: UserId, val amount: Money)