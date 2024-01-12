package `in`.rcard.fes.portfolio

import arrow.core.nonEmptyListOf
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

private val NOW_MILLIS: Long = Instant.now().toEpochMilli()

class PortfolioTest : ShouldSpec({
    context("The decider function") {
        should("create a portfolio for a new user") {
            val cmd = PortfolioCommand.CreatePortfolio(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0))
            decide(cmd, notCreatedPortfolio).shouldBeRight(
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(
                        PortfolioId("1"),
                        NOW_MILLIS,
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                ),
            )
        }

        should("not create a portfolio if the user already owns one") {
            val cmd = PortfolioCommand.CreatePortfolio(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0))
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(
                        PortfolioId("1"),
                        NOW_MILLIS,
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                )
            decide(cmd, state).shouldBeLeft(
                PortfolioError.PortfolioAlreadyExists(PortfolioId("1")),
            )
        }

        should("buy stocks if the portfolio has sufficient funds") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                )
            val cmd =
                PortfolioCommand.BuyStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(9),
                    Money(10.0),
                )

            decide(cmd, state).shouldBeRight(
                nonEmptyListOf(
                    PortfolioEvent.StocksPurchased(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(10.0),
                    ),
                ),
            )
        }

        should("not buy stocks if the portfolio has insufficient funds") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                )
            val cmd =
                PortfolioCommand.BuyStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(11),
                    Money(10.0),
                )

            decide(cmd, state).shouldBeLeft(
                PortfolioError.InsufficientFunds(
                    PortfolioId("rcardin-1"),
                    Money(110.0),
                    Money(100.0),
                ),
            )
        }

        should("not buy stocks for a non-existing portfolio") {
            val cmd =
                PortfolioCommand.BuyStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(11),
                    Money(10.0),
                )
            decide(cmd, notCreatedPortfolio).shouldBeLeft(
                PortfolioError.PortfolioNotAvailable(PortfolioId("rcardin-1")),
            )
        }

        should("not buy stocks if the portfolio is closed") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.PortfolioClosed(PortfolioId("rcardin-1"), NOW_MILLIS),
                )
            val cmd =
                PortfolioCommand.BuyStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(11),
                    Money(10.0),
                )

            decide(cmd, state).shouldBeLeft(
                PortfolioError.PortfolioIsClosed(PortfolioId("rcardin-1")),
            )
        }

        should("sell stocks from the portfolio") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.StocksPurchased(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(10.0),
                    ),
                )
            val cmd =
                PortfolioCommand.SellStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(8),
                    Money(12.0),
                )

            decide(cmd, state).shouldBeRight(
                nonEmptyListOf(
                    PortfolioEvent.StocksSold(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(8),
                        Money(12.0),
                    ),
                ),
            )
        }

        should("not sell stocks if the requested quantity is greater than the own quantity") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.StocksPurchased(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(10.0),
                    ),
                )
            val cmd =
                PortfolioCommand.SellStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(10),
                    Money(12.0),
                )

            decide(cmd, state).shouldBeLeft(
                PortfolioError.InsufficientStocks(
                    PortfolioId("rcardin-1"),
                    Stock("AAPL"),
                    Quantity(10),
                    Quantity(9),
                ),
            )
        }

        should("not sell stocks if the requested stocks are not owned by the portfolio") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.StocksPurchased(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(10.0),
                    ),
                )
            val cmd =
                PortfolioCommand.SellStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("GOOG"),
                    Quantity(10),
                    Money(12.0),
                )

            decide(cmd, state).shouldBeLeft(
                PortfolioError.InsufficientStocks(
                    PortfolioId("rcardin-1"),
                    Stock("GOOG"),
                    Quantity(10),
                    Quantity(0),
                ),
            )
        }

        should("not sell stocks for a non-existing portfolio") {
            val cmd =
                PortfolioCommand.SellStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(11),
                    Money(10.0),
                )
            decide(cmd, notCreatedPortfolio).shouldBeLeft(
                PortfolioError.PortfolioNotAvailable(PortfolioId("rcardin-1")),
            )
        }

        should("not sell stocks if the portfolio is closed") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.StocksPurchased(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(10.0),
                    ),
                    PortfolioEvent.PortfolioClosed(PortfolioId("rcardin-1"), NOW_MILLIS),
                )
            val cmd =
                PortfolioCommand.SellStocks(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    Stock("AAPL"),
                    Quantity(8),
                    Money(12.0),
                )

            decide(cmd, state).shouldBeLeft(
                PortfolioError.PortfolioIsClosed(PortfolioId("rcardin-1")),
            )
        }

        should("close a portfolio, selling all the stocks") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.StocksPurchased(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(10.0),
                    ),
                )
            val cmd =
                PortfolioCommand.ClosePortfolio(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    mapOf(Stock("AAPL") to Money(5.0)),
                )

            decide(
                cmd,
                state,
            ).shouldBeRight(
                nonEmptyListOf(
                    PortfolioEvent.StocksSold(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(5.0),
                    ),
                    PortfolioEvent.PortfolioClosed(PortfolioId("rcardin-1"), NOW_MILLIS),
                ),
            )
        }

        should("close an empty portfolio") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                )
            val cmd =
                PortfolioCommand.ClosePortfolio(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    mapOf(Stock("AAPL") to Money(5.0)),
                )

            decide(
                cmd,
                state,
            ).shouldBeRight(
                nonEmptyListOf(
                    PortfolioEvent.PortfolioClosed(PortfolioId("rcardin-1"), NOW_MILLIS),
                ),
            )
        }

        should("not close a portfolio already closed") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.PortfolioClosed(PortfolioId("rcardin-1"), NOW_MILLIS),
                )
            val cmd =
                PortfolioCommand.ClosePortfolio(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
                    mapOf(Stock("AAPL") to Money(5.0)),
                )

            decide(
                cmd,
                state,
            ).shouldBeLeft(
                PortfolioError.PortfolioIsClosed(PortfolioId("rcardin-1")),
            )
        }

        should("not close a portfolio is the price of a stock is not available") {
            val state =
                nonEmptyListOf(
                    PortfolioEvent.PortfolioCreated(PortfolioId("rcardin-1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)),
                    PortfolioEvent.StocksPurchased(
                        PortfolioId("rcardin-1"),
                        NOW_MILLIS,
                        Stock("AAPL"),
                        Quantity(9),
                        Money(10.0),
                    ),
                )
            val cmd =
                PortfolioCommand.ClosePortfolio(
                    PortfolioId("rcardin-1"),
                    NOW_MILLIS,
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
            val event = PortfolioEvent.PortfolioCreated(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0))
            evolve(notCreatedPortfolio, event) shouldBe nonEmptyListOf(event)
        }

        should("calculate the new state from the previous state") {
            // FIXME We can't have two portfolios for the same user
            val initialEvent =
                PortfolioEvent.PortfolioCreated(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0))
            val newEvent =
                PortfolioEvent.StocksPurchased(PortfolioId("1"), NOW_MILLIS, Stock("AAPL"), Quantity(9), Money(10.0))
            val state = nonEmptyListOf(initialEvent)
            evolve(state, newEvent) shouldBe nonEmptyListOf(initialEvent, newEvent)
        }
    }
})
