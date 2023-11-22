package `in`.rcard.fes

import arrow.core.nonEmptyListOf
import arrow.core.right
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioCommand.CreatePortfolio
import `in`.rcard.fes.portfolio.PortfolioError.PortfolioAlreadyExists
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.portfolio.notCreatedPortfolio
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

private val NOW: Instant = Instant.now()
private val FIXED_CLOCK: Clock = Clock.fixed(NOW, ZoneId.of("UTC"))

class PortfolioHandleCommandTest : ShouldSpec({
    context("a portfolio") {
        should("be created").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns ("0" to notCreatedPortfolio).right()
            coEvery {
                eventStore.saveState(
                    PortfolioId("1"),
                    "0",
                    nonEmptyListOf(
                        PortfolioCreated(
                            PortfolioId("1"),
                            NOW.toEpochMilli(),
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    ),
                )
            } returns Unit.right()
            with(FIXED_CLOCK) {
                with(eventStore) {
                    val actualResult = handle(CreatePortfolio(PortfolioId("1"), UserId("rcardin"), Money(100.0)))
                    actualResult.shouldBeRight(PortfolioId("1"))
                }
            }
        }
        should("not be created if already exists").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns (
                "0" to listOf(
                    PortfolioCreated(
                        PortfolioId("1"),
                        NOW.toEpochMilli(),
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                )
                ).right()
            with(FIXED_CLOCK) {
                with(eventStore) {
                    val actualResult = handle(CreatePortfolio(PortfolioId("1"), UserId("rcardin"), Money(100.0)))
                    actualResult.shouldBeLeft(Union.Second(PortfolioAlreadyExists(PortfolioId("1"))))
                }
            }
        }
        should("be closed") {
            // TODO
        }
        should("buy stocks") {
            // TODO
        }
        should("sell stocks") {
            // TODO
        }
    }
})
