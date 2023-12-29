package `in`.rcard.fes

import arrow.core.right
import `in`.rcard.fes.portfolio.ChangePortfolio
import `in`.rcard.fes.portfolio.ChangePortfolioDTO
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.Quantity
import `in`.rcard.fes.portfolio.Stock
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.ShouldSpec
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.mockk.coEvery

class ChangePortfolioRouteTest : ShouldSpec({
    context("The change portfolio route") {
        should("return a 204 status if the portfolio has been successfully changed") {
            withServer { deps ->
                coEvery {
                    deps.changePortfolioUseCase.changePortfolio(
                        ChangePortfolio(
                            PortfolioId("1"),
                            Stock("AAPL"),
                            Quantity(100)
                        )
                    )
                } returns PortfolioId("1").right()
                val response = put {
                    url("/portfolios/1")
                    contentType(ContentType.Application.Json)
                    setBody(ChangePortfolioDTO("AAPL", 100))
                }
                response.shouldHaveStatus(204)
            }
        }
    }
})