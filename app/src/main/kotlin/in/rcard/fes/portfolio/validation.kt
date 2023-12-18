package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.Either.Companion.zipOrAccumulate
import arrow.core.EitherNel
import arrow.core.left
import arrow.core.right
import arrow.core.toEitherNel
import `in`.rcard.fes.portfolio.InvalidFieldError.NegativeFieldError
import `in`.rcard.fes.portfolio.InvalidFieldError.RequiredFieldError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.serialization.Serializable

@Serializable
data class GenericErrorDTO(val errors: List<String>)

sealed interface InvalidFieldError {

    val field: String

    data class RequiredFieldError(override val field: String) : InvalidFieldError {
        override fun toString(): String = "Field '$field' is required"
    }

    data class NegativeFieldError(override val field: String) : InvalidFieldError {
        override fun toString(): String = "Field '$field' must be positive"
    }
}

fun ValidationError.toGenericError(): GenericErrorDTO =
    GenericErrorDTO(fieldErrors.all.map { it.toString() })

interface ValidationScope<T> {
    fun T.validate(): Either<ValidationError, T>
}

interface Required<T> {
    fun T.required(fieldName: String): EitherNel<RequiredFieldError, T>
}

interface Positive<T : Number> {
    fun T.positive(fieldName: String): EitherNel<NegativeFieldError, T>
}

val requiredString = object : Required<String> {
    override fun String.required(fieldName: String): EitherNel<RequiredFieldError, String> =
        if (this.isBlank()) {
            RequiredFieldError(fieldName).left().toEitherNel()
        } else {
            this.right()
        }
}

val positiveDouble = object : Positive<Double> {
    override fun Double.positive(fieldName: String): EitherNel<NegativeFieldError, Double> =
        if (this <= 0.0) {
            NegativeFieldError(fieldName).left().toEitherNel()
        } else {
            this.right()
        }
}

context (ValidationScope<T>)
suspend inline fun <reified T : Any> ApplicationCall.validate(): Either<ValidationError, T> =
    receive<T>().validate()

val createPortfolioDTOValidator = object : ValidationScope<CreatePortfolioDTO> {
    override fun CreatePortfolioDTO.validate(): Either<ValidationError, CreatePortfolioDTO> =
        with(requiredString) {
            with(positiveDouble) {
                zipOrAccumulate(
                    userId.required("userId"),
                    amount.positive("amount"),
                    ::CreatePortfolioDTO,
                ).mapLeft(::ValidationError)
            }
        }
}
