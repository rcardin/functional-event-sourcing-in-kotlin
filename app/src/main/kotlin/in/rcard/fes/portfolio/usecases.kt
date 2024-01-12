package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.raise.either
import `in`.rcard.fes.env.Clock
import `in`.rcard.fes.env.UUIDGenerator
import `in`.rcard.fes.stock.FindStockPriceBySymbol

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
fun changePortfolioUseCase(
    portfolioService: PortfolioService,
    findStockBySymbol: FindStockPriceBySymbol,
) = object : ChangePortfolioUseCase {
    override suspend fun changePortfolio(model: ChangePortfolio): Either<DomainError, PortfolioId> =
        either {
            val stockPrice =
                findStockBySymbol.findPriceBySymbol(model.stock).mapLeft {
                    PortfolioError.PriceNotAvailable(model.portfolioId, model.stock)
                }.bind()
            if (model.quantity > 0) {
                buyStocksCommand(model, stockPrice)
            } else {
                sellStocksCommand(model, stockPrice)
            }.let { portfolioService.handle(it).bind() }
        }

    private suspend fun buyStocksCommand(
        model: ChangePortfolio,
        stockPrice: Money,
    ) = PortfolioCommand.BuyStocks(
        model.portfolioId,
        currentTimeMillis(),
        model.stock,
        model.quantity,
        stockPrice,
    )

    private suspend fun sellStocksCommand(
        model: ChangePortfolio,
        stockPrice: Money,
    ): PortfolioCommand.SellStocks =
        PortfolioCommand.SellStocks(
            model.portfolioId,
            currentTimeMillis(),
            model.stock,
            -model.quantity,
            stockPrice,
        )
}

data class ChangePortfolio(val portfolioId: PortfolioId, val stock: Stock, val quantity: Quantity)
