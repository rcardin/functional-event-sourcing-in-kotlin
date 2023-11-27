package `in`.rcard.fes.portfolio

import arrow.core.Either.Companion.zipOrAccumulate
import arrow.core.EitherNel
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun RequestValidationConfig.configure() {
    validate<CreatePortfolioDTO> {
        it.validate()
    }
}

fun CreatePortfolioDTO.validate(): ValidationResult =
    zipOrAccumulate(
        userId.validUserId(),
        amount.validAmount(),
        ::CreatePortfolioDTO,
    ).fold(
        { ValidationResult.Invalid(it.joinToString(",")) },
        { ValidationResult.Valid },
    )

fun String.validUserId(): EitherNel<InvalidFieldError, String> = if (this.isBlank()) {
    nonEmptyListOf(InvalidFieldError("userId", "The userId cannot be empty")).left()
} else {
    this.right()
}

fun Double.validAmount(): EitherNel<InvalidFieldError, Double> = if (this < 0.0) {
    nonEmptyListOf(InvalidFieldError("amount", "The amount cannot be negative")).left()
} else {
    this.right()
}

data class InvalidFieldError(val field: String, val error: String)
