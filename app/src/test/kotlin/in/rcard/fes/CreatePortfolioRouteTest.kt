package `in`.rcard.fes

import `in`.rcard.fes.portfolio.CreatePortfolioDTO
import io.kotest.assertions.ktor.client.shouldHaveHeader
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

class CreatePortfolioRouteTest : ShouldSpec({
    context("The create portfolio route") {
        should("return a 201 status code if the portfolio has been created") {
            testApplication {
                application {
                    module()
                }
                val client = createClient {
                    install(ContentNegotiation) { json() }
                }
                val response = client.post {
                    url("/portfolios")
                    contentType(ContentType.Application.Json)
                    setBody(CreatePortfolioDTO("rcardin", 100.0))
                }
                response.shouldHaveStatus(201)
                response.shouldHaveHeader("Location", "/portfolios/1")
            }
        }
        should("return a 400 status code if the request has an empty userId") {
            testApplication {
                application {
                    module()
                }
                val client = createClient {
                    install(ContentNegotiation) { json() }
                }
                val response = client.post {
                    url("/portfolios")
                    contentType(ContentType.Application.Json)
                    setBody(CreatePortfolioDTO("", 100.0))
                }
                response.shouldHaveStatus(400)
                response.bodyAsText().shouldBe("InvalidFieldError(field=userId, error=The userId cannot be empty)")
            }
        }
        should("return a 400 status code if the request has an amount less than or equal to zero") {
            testApplication {
                application {
                    module()
                }
                val client = createClient {
                    install(ContentNegotiation) { json() }
                }
                val response = client.post {
                    url("/portfolios")
                    contentType(ContentType.Application.Json)
                    setBody(CreatePortfolioDTO("rcardin", -1.0))
                }
                response.shouldHaveStatus(400)
                response.bodyAsText().shouldBe("InvalidFieldError(field=amount, error=The amount cannot be negative or zero)")
            }
        }
    }
})