package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.Either.Companion.zipOrAccumulate
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import `in`.rcard.fes.portfolio.InvalidFieldError.MissingFieldError
import `in`.rcard.fes.portfolio.InvalidFieldError.NegativeFieldError
import `in`.rcard.fes.portfolio.InvalidFieldError.ZeroFieldError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.serialization.Serializable

@Serializable
data class GenericErrorDTO(val errors: List<String>)

sealed interface InvalidFieldError {
    val field: String

    data class MissingFieldError(override val field: String) : InvalidFieldError {
        override fun toString(): String = "Field '$field' is required"
    }

    data class NegativeFieldError(override val field: String) : InvalidFieldError {
        override fun toString(): String = "Field '$field' must be positive"
    }

    data class ZeroFieldError(override val field: String) : InvalidFieldError {
        override fun toString(): String = "Field '$field' must be non zero"
    }
}

fun ValidationError.toGenericError(): GenericErrorDTO = GenericErrorDTO(fieldErrors.all.map { it.toString() })

interface ValidatorScope<T> {
    fun T.validate(): Either<ValidationError, T>
}

context (ValidatorScope<T>)
suspend inline fun <reified T : Any> ApplicationCall.validate(): Either<ValidationError, T> = receive<T>().validate()

class ValidationScope {
    context(Required<T>)
    fun <T> T.required(fieldName: String): EitherNel<MissingFieldError, T> =
        either {
            ensure(required()) { nonEmptyListOf(MissingFieldError(fieldName)) }
            this@required
        }

    context(Positive<T>)
    fun <T : Number> T.positive(fieldName: String): EitherNel<NegativeFieldError, T> =
        either {
            ensure(positive()) { nonEmptyListOf(NegativeFieldError(fieldName)) }
            this@positive
        }

    context(NonZero<T>)
    fun <T : Number> T.nonZero(fieldName: String): EitherNel<ZeroFieldError, T> =
        either {
            ensure(nonZero()) { nonEmptyListOf(ZeroFieldError(fieldName)) }
            this@nonZero
        }
}

fun <T> validation(validationBlock: ValidationScope.() -> Either<ValidationError, T>): Either<ValidationError, T> =
    with(ValidationScope(), validationBlock)

interface Required<T> {
    context(ValidationScope)
    fun T.required(): Boolean
}

interface Positive<T : Number> {
    context(ValidationScope)
    fun T.positive(): Boolean
}

interface NonZero<T : Number> {
    context(ValidationScope)
    fun T.nonZero(): Boolean
}

interface InRange<T : Number> {
    context(ValidationScope)
    fun T.inRange(
        min: T,
        max: T,
    ): Boolean
}

val requiredString =
    object : Required<String> {
        context(ValidationScope)
        override fun String.required(): Boolean = this.isNotBlank()
    }

val positiveDouble =
    object : Positive<Double> {
        context(ValidationScope)
        override fun Double.positive(): Boolean = this > 0.0
    }

val nonZeroInteger =
    object : NonZero<Int> {
        context(ValidationScope)
        override fun Int.nonZero(): Boolean = this != 0
    }

val rangeInteger =
    object : InRange<Int> {
        context(ValidationScope)
        override fun Int.inRange(
            min: Int,
            max: Int,
        ): Boolean = this in min..max
    }

val createPortfolioDTOValidator =
    object : ValidatorScope<CreatePortfolioDTO> {
        override fun CreatePortfolioDTO.validate(): Either<ValidationError, CreatePortfolioDTO> =
            validation {
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
    }

val changePortfolioDTOValidator =
    object : ValidatorScope<ChangePortfolioDTO> {
        override fun ChangePortfolioDTO.validate(): Either<ValidationError, ChangePortfolioDTO> =
            validation {
                with(requiredString) {
                    with(nonZeroInteger) {
                        zipOrAccumulate(
                            stock.required("stock"),
                            quantity.nonZero("quantity"),
                            ::ChangePortfolioDTO,
                        ).mapLeft(::ValidationError)
                    }
                }
            }
    }
