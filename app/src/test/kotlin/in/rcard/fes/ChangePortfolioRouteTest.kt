package `in`.rcard.fes

import `in`.rcard.fes.portfolio.ChangePortfolioDTO
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.ShouldSpec
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ChangePortfolioRouteTest : ShouldSpec({
    context("The change portfolio route") {
        should("return a 204 status if the portfolio has been successfully changed") {
            withServer {
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