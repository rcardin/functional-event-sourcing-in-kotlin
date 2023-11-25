package `in`.rcard.fes.portfolio

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    routing {
        post("/portfolios") {
            val dto = call.receive<CreatePortfolioDTO>()
            call.response.header("Location", "/portfolios/1")
            call.respond(HttpStatusCode.Created)
        }
    }
}

@Serializable
data class CreatePortfolioDTO(val userId: String, val amount: Double)
