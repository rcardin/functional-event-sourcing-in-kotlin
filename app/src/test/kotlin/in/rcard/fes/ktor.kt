package `in`.rcard.fes

import `in`.rcard.fes.env.Dependencies
import `in`.rcard.fes.portfolio.CreatePortfolioUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.mockk.mockk

suspend fun withServer(testSuite: suspend HttpClient.(dep: Dependencies) -> Unit) {
    val deps = Dependencies(mockk<CreatePortfolioUseCase>())
    testApplication {
        application {
            module(deps)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        client.use { testSuite(it, deps) }
    }
}