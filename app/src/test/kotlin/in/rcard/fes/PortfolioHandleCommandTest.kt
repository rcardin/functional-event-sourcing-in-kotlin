package `in`.rcard.fes

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.mockk

class PortfolioHandleCommandTest : ShouldSpec({
    context("a portfolio") {
        should("be created") {
            val eventStore = mockk<PortfolioEventStore>()
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
