package `in`.rcard.fes

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue

class ChangePortfolioRouteTest : ShouldSpec({
    context("The change portfolio route") {
        should("return a 200 status if the portfolio has been successfully changed") {
            true.shouldBeTrue() // FIXME
        }
    }
})