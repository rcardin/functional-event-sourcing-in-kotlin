package `in`.rcard.fes.portfolio

import arrow.core.left
import arrow.core.right
import `in`.rcard.fes.env.Clock
import `in`.rcard.fes.portfolio.PortfolioCommand.BuyStocks
import `in`.rcard.fes.portfolio.PortfolioCommand.SellStocks
import `in`.rcard.fes.portfolio.PortfolioError.PriceNotAvailable
import `in`.rcard.fes.stock.FindStockPriceBySymbol
import `in`.rcard.fes.stock.StockPricesError.StockPricesGenericError
import `in`.rcard.fes.stock.StockPricesError.StockPricesNotFoundError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant

private val NOW_MILLIS = Instant.now().toEpochMilli()
private val FIXED_CLOCK =
    object : Clock {
        override suspend fun currentTimeMillis(): Long = NOW_MILLIS
    }

class ChangePortfolioUseCaseTest : ShouldSpec({

    val findStockPriceBySymbol = mockk<FindStockPriceBySymbol>()
    val portfolioService = mockk<PortfolioService>()

    context("The change of a portfolio") {
        should("retrieve the price of the stock to purchase") {
            coEvery {
                findStockPriceBySymbol.findPriceBySymbol(Stock("AAPL"))
            } returns Money(100.0).right()
            coEvery {
                portfolioService.handle(
                    BuyStocks(
                        PortfolioId("123"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(100),
                        Money(100.0),
                    ),
                )
            } returns PortfolioId("123").right()
            with(FIXED_CLOCK) {
                val changePortfolioUseCase = changePortfolioUseCase(portfolioService, findStockPriceBySymbol)
                val actualPortfolioId =
                    changePortfolioUseCase.changePortfolio(
                        ChangePortfolio(
                            PortfolioId("123"),
                            Stock("AAPL"),
                            Quantity(100),
                        ),
                    )
                actualPortfolioId.shouldBeRight(PortfolioId("123"))
            }
        }

        should("retrieve the price of the stock to sell") {
            coEvery {
                findStockPriceBySymbol.findPriceBySymbol(Stock("AAPL"))
            } returns Money(100.0).right()
            coEvery {
                portfolioService.handle(
                    SellStocks(
                        PortfolioId("123"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(100),
                        Money(100.0),
                    ),
                )
            } returns PortfolioId("123").right()
            with(FIXED_CLOCK) {
                val changePortfolioUseCase = changePortfolioUseCase(portfolioService, findStockPriceBySymbol)
                val actualPortfolioId =
                    changePortfolioUseCase.changePortfolio(
                        ChangePortfolio(
                            PortfolioId("123"),
                            Stock("AAPL"),
                            Quantity(-100),
                        ),
                    )
                actualPortfolioId.shouldBeRight(PortfolioId("123"))
            }
        }

        should("return a PriceNotAvailable if the stock symbol is not available") {
            coEvery {
                findStockPriceBySymbol.findPriceBySymbol(Stock("AAPL"))
            } returns StockPricesNotFoundError(Stock("AAPL")).left()
            with(FIXED_CLOCK) {
                val changePortfolioUseCase = changePortfolioUseCase(portfolioService, findStockPriceBySymbol)
                val actualPortfolioId =
                    changePortfolioUseCase.changePortfolio(
                        ChangePortfolio(
                            PortfolioId("123"),
                            Stock("AAPL"),
                            Quantity(100),
                        ),
                    )
                actualPortfolioId.shouldBeLeft(PriceNotAvailable(PortfolioId("123"), Stock("AAPL")))
            }
        }

        should("return a PersistenceError if there is an unexpected error during the connection with the db") {
            coEvery {
                findStockPriceBySymbol.findPriceBySymbol(Stock("AAPL"))
            } returns StockPricesGenericError.left()
            with(FIXED_CLOCK) {
                val changePortfolioUseCase = changePortfolioUseCase(portfolioService, findStockPriceBySymbol)
                val actualPortfolioId =
                    changePortfolioUseCase.changePortfolio(
                        ChangePortfolio(
                            PortfolioId("123"),
                            Stock("AAPL"),
                            Quantity(100),
                        ),
                    )
                actualPortfolioId.shouldBeLeft(InfrastructureError.PersistenceError)
            }
        }
    }
})
