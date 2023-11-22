package `in`.rcard.fes

import arrow.core.nonEmptyListOf
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioCommand.CreatePortfolio
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

private val NOW: Instant = Instant.now()
private val FIXED_CLOCK: Clock = Clock.fixed(NOW, ZoneId.of("UTC"))

class PortfolioHandleCommandTest : ShouldSpec({
    context("a portfolio") {
        should("be created") {
            val eventStore = mockk<PortfolioEventStore>()
            every {
                eventStore.saveState(
                    PortfolioId("rcardin-1"),
                    nonEmptyListOf(
                        PortfolioCreated(
                            PortfolioId("rcardin-1"),
                            NOW.toEpochMilli(),
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    ),
                )
            } returns true
            with(FIXED_CLOCK) {
                with(eventStore) {
                    val actualResult = handle(CreatePortfolio(PortfolioId("1"), UserId("rcardin"), Money(100.0)))
                    actualResult.shouldBeRight(PortfolioId("1"))
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
