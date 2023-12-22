package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.raise.either
import `in`.rcard.fes.env.Dependencies
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable

fun Application.configureRouting(deps: Dependencies) {
    routing {
        with(createPortfolioDTOValidator) {
            portfolioRoutes(deps.createPortfolioUseCase)
        }
    }
}

context (ValidationScope<CreatePortfolioDTO>)
fun Route.portfolioRoutes(createPortfolioUseCase: CreatePortfolioUseCase) {
    post("/portfolios") {
        either {
            val dto = call.validate<CreatePortfolioDTO>().bind()
            val model = dto.toModel()
            val portfolioId = createPortfolioUseCase.createPortfolio(model).bind()
            call.response.header("Location", "/portfolios/${portfolioId.id}")
        }.respond(HttpStatusCode.Created)
    }
    put("/portfolio/{portfolioId}") {
        call.respond(HttpStatusCode.NotImplemented)
    }
    delete("/portfolio/{portfolioId}") {
        call.respond(HttpStatusCode.NotImplemented)
    }
}

@Serializable
data class CreatePortfolioDTO(val userId: String, val amount: Double)

private fun CreatePortfolioDTO.toModel(): CreatePortfolio =
    CreatePortfolio(
        UserId(userId),
        Money(amount)
    )

@Serializable
data class ChangePortfolioDTO(val stock: String, val quantity: Int)

context(PipelineContext<Unit, ApplicationCall>)
suspend inline fun <reified A : Any> Either<DomainError, A>.respond(status: HttpStatusCode): Unit =
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }

suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: DomainError): Unit = when (error) {
    is ValidationError -> call.respond(HttpStatusCode.BadRequest, error.toGenericError())
    else -> call.respond(HttpStatusCode.InternalServerError)
}
