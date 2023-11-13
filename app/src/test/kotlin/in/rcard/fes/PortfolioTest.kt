package `in`.rcard.fes

import arrow.core.nonEmptyListOf
import `in`.rcard.fes.PortfolioError.PortfolioAlreadyExists
import `in`.rcard.fes.PortfolioEvent.PortfolioCreated
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec

class PortfolioTest : ShouldSpec({
    context("A portfolio") {
        should("be created for a new user") {
            val cmd = PortfolioCommand.CreatePortfolio(UserId("rcardin"), Money(100.0))
            decide(cmd, notCreatedPortfolio).shouldBeRight(
                nonEmptyListOf(PortfolioCreated(PortfolioId("1"), UserId("rcardin"), Money(100.0))),
            )
        }

        should("not be created if the user already owns a portfolio") {
            val cmd = PortfolioCommand.CreatePortfolio(UserId("rcardin"), Money(100.0))
            val state = nonEmptyListOf(PortfolioCreated(PortfolioId("1"), UserId("rcardin"), Money(100.0)))
            decide(cmd, state).shouldBeLeft(
                PortfolioAlreadyExists(PortfolioId("1")),
            )
        }
    }
})
