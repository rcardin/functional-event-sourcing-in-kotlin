package `in`.rcard.fes.projection

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.projection.PortfoliosError.PortfolioAlreadyExistsError
import `in`.rcard.fes.sqldelight.Portfolios
import `in`.rcard.fes.sqldelight.PortfoliosQueries
import org.slf4j.Logger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

sealed interface PortfoliosError {
    data class PortfolioAlreadyExistsError(val portfolioId: PortfolioId, val userId: UserId) : PortfoliosError
}

interface InsertPortfolio {
    suspend fun insertPortfolio(portfolioCreated: PortfolioCreated): Either<PortfoliosError, Unit>
}

context(Logger)
fun portfolioRepository(portfoliosQueries: PortfoliosQueries) =
    object : InsertPortfolio {
        override suspend fun insertPortfolio(portfolioCreated: PortfolioCreated): Either<PortfoliosError, Unit> =
            either {
                val creationDateTime =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(portfolioCreated.occurredOn), ZoneId.systemDefault())
                val newPortfolio =
                    Portfolios(
                        portfolio_id = portfolioCreated.portfolioId,
                        user_id = portfolioCreated.userId,
                        money = portfolioCreated.money,
                        created_at = creationDateTime,
                        updated_at = creationDateTime,
                    )

                catch({ portfoliosQueries.insertPortfolio(newPortfolio) }) { exception ->
                    this@Logger.error(
                        "Error, portfolio with id '{}' for user '{}' already exists",
                        newPortfolio.portfolio_id,
                        newPortfolio.user_id,
                        exception,
                    )
                    raise(PortfolioAlreadyExistsError(portfolioCreated.portfolioId, portfolioCreated.userId))
                }
            }
    }
