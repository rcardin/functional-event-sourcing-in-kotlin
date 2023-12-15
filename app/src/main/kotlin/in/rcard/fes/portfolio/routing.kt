package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.either
import `in`.rcard.fes.env.Dependencies
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable

fun Application.configureRouting(deps: Dependencies) {
    routing {
        with(createPortfolioDTOValidator) {
            portfolioRoutes(deps.portfolioService)
        }
    }
}

context (ValidationScope<CreatePortfolioDTO>)
fun Route.portfolioRoutes(portfolioService: PortfolioService) {
    post("/portfolios") {
        either {
            val dto = call.validate<CreatePortfolioDTO>().bind()
            call.response.header("Location", "/portfolios/1")
        }.respond(HttpStatusCode.Created)
    }
}

@Serializable
data class CreatePortfolioDTO(val userId: String, val amount: Double)

context(PipelineContext<Unit, ApplicationCall>)
suspend inline fun <reified A : Any> EitherNel<DomainError, A>.respond(status: HttpStatusCode): Unit =
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }

suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: NonEmptyList<DomainError>): Unit =
    call.respond(HttpStatusCode.BadRequest)

context (ValidationScope<T>)
suspend inline fun <reified T : Any> ApplicationCall.validate(): EitherNel<DomainError, T> =
    receive<T>().validate()
