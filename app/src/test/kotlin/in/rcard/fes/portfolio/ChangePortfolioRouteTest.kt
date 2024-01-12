package `in`.rcard.fes.portfolio

import arrow.core.left
import arrow.core.right
import `in`.rcard.fes.withServer
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
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
                            Quantity(100),
                        ),
                    )
                } returns PortfolioId("1").right()
                val response =
                    put {
                        url("/portfolios/1")
                        contentType(ContentType.Application.Json)
                        setBody(ChangePortfolioDTO("AAPL", 100))
                    }
                response.shouldHaveStatus(204)
            }
        }

        should("return a 400 status if the quantity is zero") {
            withServer {
                val response =
                    put {
                        url("/portfolios/1")
                        contentType(ContentType.Application.Json)
                        setBody(ChangePortfolioDTO("AAPL", 0))
                    }
                response.shouldHaveStatus(400)
                response.bodyAsText().shouldBe("{\"errors\":[\"Field 'quantity' must be non zero\"]}")
            }
        }

        should("return a 400 status if the stock name is empty") {
            withServer {
                val response =
                    put {
                        url("/portfolios/1")
                        contentType(ContentType.Application.Json)
                        setBody(ChangePortfolioDTO("", 100))
                    }
                response.shouldHaveStatus(400)
                response.bodyAsText().shouldBe("{\"errors\":[\"Field 'stock' is required\"]}")
            }
        }

        should("return a 400 if the stock to purchase or sell is not available") {
            withServer { deps ->
                coEvery {
                    deps.changePortfolioUseCase.changePortfolio(
                        ChangePortfolio(
                            PortfolioId("1"),
                            Stock("AAPL"),
                            Quantity(100),
                        ),
                    )
                } returns PortfolioError.PriceNotAvailable(PortfolioId("1"), Stock("AAPL")).left()
                val response =
                    put {
                        url("/portfolios/1")
                        contentType(ContentType.Application.Json)
                        setBody(ChangePortfolioDTO("AAPL", 100))
                    }
                response.shouldHaveStatus(400)
                response.bodyAsText().shouldBe("{\"errors\":[\"Stock 'AAPL' is not available\"]}")
            }
        }

        should("return a 400 is the portfolio has insufficient funds to purchase the stock") {
            withServer { deps ->
                coEvery {
                    deps.changePortfolioUseCase.changePortfolio(
                        ChangePortfolio(
                            PortfolioId("1"),
                            Stock("AAPL"),
                            Quantity(100),
                        ),
                    )
                } returns PortfolioError.InsufficientFunds(PortfolioId("1"), Money(1000.0), Money(100.0)).left()
                val response =
                    put {
                        url("/portfolios/1")
                        contentType(ContentType.Application.Json)
                        setBody(ChangePortfolioDTO("AAPL", 100))
                    }
                response.shouldHaveStatus(400)
                response.bodyAsText().shouldBe("{\"errors\":[\"The portfolio has insufficient funds\"]}")
            }
        }
    }
})
