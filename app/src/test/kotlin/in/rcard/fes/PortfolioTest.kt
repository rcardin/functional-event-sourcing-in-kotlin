package `in`.rcard.fes

import arrow.core.nonEmptyListOf
import `in`.rcard.fes.PortfolioCommand.CreatePortfolio
import `in`.rcard.fes.PortfolioCommand.PortfolioCommandWithPortfolioId.BuyStocks
import `in`.rcard.fes.PortfolioCommand.PortfolioCommandWithPortfolioId.ClosePortfolio
import `in`.rcard.fes.PortfolioCommand.PortfolioCommandWithPortfolioId.SellStocks
import `in`.rcard.fes.PortfolioError.InsufficientFunds
import `in`.rcard.fes.PortfolioError.NotEnoughStocks
import `in`.rcard.fes.PortfolioError.PortfolioAlreadyExists
import `in`.rcard.fes.PortfolioError.PortfolioIsClosed
import `in`.rcard.fes.PortfolioError.PortfolioNotAvailable
import `in`.rcard.fes.PortfolioEvent.PortfolioClosed
import `in`.rcard.fes.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.PortfolioEvent.StocksPurchased
import `in`.rcard.fes.PortfolioEvent.StocksSold
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class PortfolioTest : ShouldSpec({
    context("The decider function") {
        should("create a portfolio for a new user") {
            val cmd = CreatePortfolio(UserId("rcardin"), Money(100.0))
            decide(cmd, notCreatedPortfolio).shouldBeRight(
                nonEmptyListOf(PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0))),
            )
        }

        should("not create a portfolio if the user already owns one") {
            val cmd = CreatePortfolio(UserId("rcardin"), Money(100.0))
            val state = nonEmptyListOf(PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)))
            decide(cmd, state).shouldBeLeft(
                PortfolioAlreadyExists(PortfolioId("rcardin-1")),
            )
        }

        should("buy stocks if the portfolio has sufficient funds") {
            val state = nonEmptyListOf(PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)))
            val cmd = BuyStocks(
                PortfolioId("rcardin-1"),
                Stock("AAPL"),
                Quantity(9),
                Money(10.0),
            )

            decide(cmd, state).shouldBeRight(
                nonEmptyListOf(
                    StocksPurchased(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(9), Money(10.0)),
                ),
            )
        }

        should("not buy stocks if the portfolio has insufficient funds") {
            val state = nonEmptyListOf(PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)))
            val cmd = BuyStocks(
                PortfolioId("rcardin-1"),
                Stock("AAPL"),
                Quantity(11),
                Money(10.0),
            )

            decide(cmd, state).shouldBeLeft(
                InsufficientFunds(
                    PortfolioId("rcardin-1"),
                    Money(110.0),
                    Money(100.0),
                ),
            )
        }

        should("not buy stocks for a non-existing portfolio") {
            val cmd = BuyStocks(
                PortfolioId("rcardin-1"),
                Stock("AAPL"),
                Quantity(11),
                Money(10.0),
            )

            decide(cmd, notCreatedPortfolio).shouldBeLeft(
                PortfolioNotAvailable(PortfolioId("rcardin-1")),
            )
        }

        should("not buy stocks if the portfolio is closed") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
                PortfolioClosed(PortfolioId("rcardin-1")),
            )
            val cmd = BuyStocks(
                PortfolioId("rcardin-1"),
                Stock("AAPL"),
                Quantity(11),
                Money(10.0),
            )

            decide(cmd, state).shouldBeLeft(
                PortfolioIsClosed(PortfolioId("rcardin-1")),
            )
        }

        should("sell stocks from the portfolio") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
                StocksPurchased(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(9), Money(10.0)),
            )
            val cmd = SellStocks(
                PortfolioId("rcardin-1"),
                Stock("AAPL"),
                Quantity(8),
                Money(12.0),
            )

            decide(cmd, state).shouldBeRight(
                nonEmptyListOf(
                    StocksSold(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(8), Money(12.0)),
                ),
            )
        }

        should("not sell stocks if the requested quantity is greater than the own quantity") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
                StocksPurchased(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(9), Money(10.0)),
            )
            val cmd = SellStocks(
                PortfolioId("rcardin-1"),
                Stock("AAPL"),
                Quantity(10),
                Money(12.0),
            )

            decide(cmd, state).shouldBeLeft(
                NotEnoughStocks(
                    PortfolioId("rcardin-1"),
                    Stock("AAPL"),
                    Quantity(10),
                    Quantity(9),
                ),
            )
        }

        should("not sell stocks if the requested stocks are not owned by the portfolio") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
                StocksPurchased(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(9), Money(10.0)),
            )
            val cmd = SellStocks(
                PortfolioId("rcardin-1"),
                Stock("GOOG"),
                Quantity(10),
                Money(12.0),
            )

            decide(cmd, state).shouldBeLeft(
                NotEnoughStocks(
                    PortfolioId("rcardin-1"),
                    Stock("GOOG"),
                    Quantity(10),
                    Quantity(0),
                ),
            )
        }

        should("close a portfolio, selling all the stocks") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
                StocksPurchased(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(9), Money(10.0)),
            )
            val cmd = ClosePortfolio(
                PortfolioId("rcardin-1"),
                mapOf(Stock("AAPL") to Money(5.0)),
            )

            decide(
                cmd,
                state,
            ).shouldBeRight(
                nonEmptyListOf(
                    StocksSold(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(9), Money(5.0)),
                    PortfolioClosed(PortfolioId("rcardin-1")),
                ),
            )
        }

        should("close an empty portfolio") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
            )
            val cmd = ClosePortfolio(
                PortfolioId("rcardin-1"),
                mapOf(Stock("AAPL") to Money(5.0)),
            )

            decide(
                cmd,
                state,
            ).shouldBeRight(
                nonEmptyListOf(
                    PortfolioClosed(PortfolioId("rcardin-1")),
                ),
            )
        }

        should("not close a portfolio already closed") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
                PortfolioClosed(PortfolioId("rcardin-1")),
            )
            val cmd = ClosePortfolio(
                PortfolioId("rcardin-1"),
                mapOf(Stock("AAPL") to Money(5.0)),
            )

            decide(
                cmd,
                state,
            ).shouldBeLeft(
                PortfolioIsClosed(PortfolioId("rcardin-1")),
            )
        }

        should("not close a portfolio is the price of a stock is not available") {
            val state = nonEmptyListOf(
                PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0)),
                StocksPurchased(PortfolioId("rcardin-1"), Stock("AAPL"), Quantity(9), Money(10.0)),
            )
            val cmd = ClosePortfolio(
                PortfolioId("rcardin-1"),
                mapOf(Stock("GOOG") to Money(10.0)),
            )

            decide(
                cmd,
                state,
            ).shouldBeLeft(
                PortfolioError.PriceNotAvailable(PortfolioId("rcardin-1"), Stock("AAPL")),
            )
        }
    }

    context("The evolve function") {
        should("calculate the new state from the initial state") {
            val event = PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0))
            evolve(notCreatedPortfolio, event) shouldBe nonEmptyListOf(event)
        }

        should("calculate the new state from the previous state") {
            // FIXME We can't have two portfolios for the same user
            val event = PortfolioCreated(PortfolioId("rcardin-1"), UserId("rcardin"), Money(100.0))
            val state = nonEmptyListOf(event)
            evolve(state, event) shouldBe nonEmptyListOf(event, event)
        }
    }
})
