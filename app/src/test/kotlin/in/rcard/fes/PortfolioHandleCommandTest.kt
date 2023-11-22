package `in`.rcard.fes

import arrow.core.nonEmptyListOf
import arrow.core.right
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioCommand.CreatePortfolio
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.portfolio.notCreatedPortfolio
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
            val command = CreatePortfolio(UserId("rcardin"), Money(100.0))
            val decider = mockk<PortfolioDecider>()
            every { decider.initialState } returns notCreatedPortfolio
            every {
                decider.decide(
                    command,
                    notCreatedPortfolio,
                )
            } returns nonEmptyListOf(
                PortfolioCreated(
                    PortfolioId("rcardin-1"),
                    NOW.toEpochMilli(),
                    UserId("rcardin"),
                    Money(100.0),
                ),
            ).right()
            with(FIXED_CLOCK) {
                with(decider) {
                    with(eventStore) {
                        val actualResult = handle(command)
                        actualResult.shouldBeRight(PortfolioId("rcardin-1"))
                    }
                }
            }
        }
        should("not be created if the decide function fails") {
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
