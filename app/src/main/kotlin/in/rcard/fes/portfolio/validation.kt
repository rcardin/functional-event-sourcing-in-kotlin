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

fun String.validUserId(): EitherNel<ValidationError, String> = if (this.isBlank()) {
    nonEmptyListOf(ValidationError("userId", "The userId cannot be empty")).left()
} else {
    this.right()
}

fun Double.validAmount(): EitherNel<ValidationError, Double> = if (this < 0.0) {
    nonEmptyListOf(ValidationError("amount", "The amount cannot be negative")).left()
} else {
    this.right()
}

data class ValidationError(val field: String, val error: String)
