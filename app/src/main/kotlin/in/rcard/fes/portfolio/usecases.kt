package `in`.rcard.fes.portfolio

import arrow.core.Either
import `in`.rcard.fes.env.Clock
import `in`.rcard.fes.env.UUIDGenerator

interface CreatePortfolioUseCase {
    suspend fun createPortfolio(model: CreatePortfolio): Either<DomainError, PortfolioId>
}

context(UUIDGenerator, Clock)
fun createPortfolioUseCase(portfolioService: PortfolioService) =
    object : CreatePortfolioUseCase {
        override suspend fun createPortfolio(model: CreatePortfolio): Either<DomainError, PortfolioId> =
            PortfolioCommand.CreatePortfolio(
                PortfolioId(uuid()),
                currentTimeMillis(),
                model.userId,
                model.amount,
            ).let { portfolioService.handle(it) }
    }

data class CreatePortfolio(val userId: UserId, val amount: Money)

interface ChangePortfolioUseCase {
    suspend fun changePortfolio(model: ChangePortfolio): Either<DomainError, PortfolioId>
}

context(Clock)
fun changePortfolioUseCase(portfolioService: PortfolioService) =
    object : ChangePortfolioUseCase {
        override suspend fun changePortfolio(model: ChangePortfolio): Either<DomainError, PortfolioId> =
            if (model.quantity > 0) {
                buyStocksCommand(model)
            } else {
                sellStocksCommand(model)
            }.let { portfolioService.handle(it) }

        private suspend fun buyStocksCommand(model: ChangePortfolio) =
            PortfolioCommand.BuyStocks(
                model.portfolioId,
                currentTimeMillis(),
                model.stock,
                model.quantity,
                Money(0.0), // FIXME
            )

        private suspend fun sellStocksCommand(model: ChangePortfolio): PortfolioCommand.SellStocks =
            PortfolioCommand.SellStocks(
                model.portfolioId,
                currentTimeMillis(),
                model.stock,
                model.quantity,
                Money(0.0), // FIXME
            )
    }

data class ChangePortfolio(val portfolioId: PortfolioId, val stock: Stock, val quantity: Quantity)
