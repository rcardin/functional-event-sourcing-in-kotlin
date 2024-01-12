package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import `in`.rcard.fes.env.Dependencies
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
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
        portfolioRoutes(deps.createPortfolioUseCase, deps.changePortfolioUseCase)
    }
}

fun Route.portfolioRoutes(
    createPortfolioUseCase: CreatePortfolioUseCase,
    changePortfolioUseCase: ChangePortfolioUseCase,
) {
    post("/portfolios") {
        with(createPortfolioDTOValidator) {
            either {
                val dto = call.validate<CreatePortfolioDTO>().bind()
                val model = dto.toModel()
                val portfolioId = createPortfolioUseCase.createPortfolio(model).bind()
                call.response.header("Location", "/portfolios/${portfolioId.id}")
            }.respond(HttpStatusCode.Created)
        }
    }
    put("/portfolios/{portfolioId}") {
        with(changePortfolioDTOValidator) {
            either {
                val portfolioId =
                    call.parameters["portfolioId"] ?: raise(
                        ValidationError(
                            nonEmptyListOf(
                                InvalidFieldError.MissingFieldError(
                                    "portfolioId",
                                ),
                            ),
                        ),
                    )
                val dto = call.validate<ChangePortfolioDTO>().bind()
                val model = dto.toModel(portfolioId)
                changePortfolioUseCase.changePortfolio(model).bind()
            }.respond(HttpStatusCode.NoContent)
        }
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
        Money(amount),
    )

@Serializable
data class ChangePortfolioDTO(val stock: String, val quantity: Int)

private fun ChangePortfolioDTO.toModel(portfolioId: String): ChangePortfolio =
    ChangePortfolio(
        PortfolioId(portfolioId),
        Stock(stock),
        Quantity(quantity),
    )

context(PipelineContext<Unit, ApplicationCall>)
suspend inline fun <reified A : Any> Either<DomainError, A>.respond(status: HttpStatusCode): Unit =
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }

suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: DomainError): Unit =
    when (error) {
        is ValidationError -> call.respond(BadRequest, error.toGenericError())
        is PortfolioError.PriceNotAvailable ->
            call.respond(
                BadRequest,
                GenericErrorDTO(listOf("Stock '${error.stock}' is not available")),
            )

        is InfrastructureError.PersistenceError -> call.respond(HttpStatusCode.InternalServerError)
        is PortfolioError.InsufficientFunds ->
            call.respond(BadRequest, GenericErrorDTO(listOf("The portfolio has insufficient funds")))

        is PortfolioError.NotEnoughStocks -> TODO()
        is PortfolioError.PortfolioAlreadyExists -> TODO()
        is PortfolioError.PortfolioIsClosed -> TODO()
        is PortfolioError.PortfolioNotAvailable -> TODO()
    }
