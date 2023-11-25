/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package `in`.rcard.fes

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.withError
import arrow.core.toNonEmptyListOrNull
import `in`.rcard.fes.portfolio.Portfolio
import `in`.rcard.fes.portfolio.PortfolioCommand
import `in`.rcard.fes.portfolio.PortfolioCommand.BuyStocks
import `in`.rcard.fes.portfolio.PortfolioCommand.ClosePortfolio
import `in`.rcard.fes.portfolio.PortfolioCommand.CreatePortfolio
import `in`.rcard.fes.portfolio.PortfolioCommand.SellStocks
import `in`.rcard.fes.portfolio.PortfolioError
import `in`.rcard.fes.portfolio.PortfolioError.PortfolioIsClosed
import `in`.rcard.fes.portfolio.PortfolioError.PortfolioNotAvailable
import `in`.rcard.fes.portfolio.PortfolioError.PriceNotAvailable
import `in`.rcard.fes.portfolio.PortfolioEvent
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioClosed
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksSold
import `in`.rcard.fes.portfolio.PortfolioEventStore
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.ConcurrentModificationError
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.availableFunds
import `in`.rcard.fes.portfolio.configureRouting
import `in`.rcard.fes.portfolio.id
import `in`.rcard.fes.portfolio.isAvailable
import `in`.rcard.fes.portfolio.isClosed
import `in`.rcard.fes.portfolio.ownedStocks
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

// TODO Put a limit on the recursion depth
context (PortfolioEventStore)
suspend fun handle(command: PortfolioCommand): Either<Union<EventStoreError, PortfolioError>, PortfolioId> = either {
    val (eTag, portfolio) = withError({ Union.First(it) }) { loadState(command.portfolioId).bind() }
    val events = withError({ Union.Second(it) }) { decide(command, portfolio).bind() }
    val newPortfolio = events.fold(portfolio) { currentPortfolio, event -> evolve(currentPortfolio, event) }
    saveState(command.portfolioId, eTag, newPortfolio).fold(
        {
            when (it) {
                is ConcurrentModificationError -> handle(command).bind()
                else -> raise(Union.First(it))
            }
        },
        { command.portfolioId },
    )
}

// https://kotlinlang.slack.com/archives/C5UPMM0A0/p1690285846689249?thread_ts=1690281738.955939&cid=C5UPMM0A0
// Thanks to Simon Vergauwen for the Union type
sealed interface Union<out A, out B> {
    data class First<A>(val value: A) : Union<A, Nothing>
    data class Second<B>(val value: B) : Union<Nothing, B>
}

fun decide(command: PortfolioCommand, portfolio: Portfolio): Either<PortfolioError, NonEmptyList<PortfolioEvent>> =
    when (command) {
        is CreatePortfolio -> createPortfolio(portfolio, command)
        is BuyStocks -> buyStocks(portfolio, command)
        is SellStocks -> sellStocks(portfolio, command)
        is ClosePortfolio -> closePortfolio(portfolio, command)
    }

private fun createPortfolio(
    portfolio: Portfolio,
    command: CreatePortfolio,
) = either {
    if (portfolio.isAvailable()) {
        raise(PortfolioError.PortfolioAlreadyExists(portfolio.id))
    }
    nonEmptyListOf(
        PortfolioEvent.PortfolioCreated(
            command.portfolioId,
            command.occurredOn,
            command.userId,
            command.amount,
        ),
    )
}

private fun buyStocks(
    portfolio: Portfolio,
    command: BuyStocks,
): Either<PortfolioError, NonEmptyList<PortfolioEvent>> = either {
    if (!portfolio.isAvailable()) {
        raise(PortfolioNotAvailable(command.portfolioId))
    }
    if (portfolio.isClosed()) {
        raise(PortfolioIsClosed(command.portfolioId))
    }
    val requestedFundsForStocks = command.price * command.quantity
    val availableFunds = portfolio.availableFunds()
    if (availableFunds < requestedFundsForStocks) {
        raise(PortfolioError.InsufficientFunds(portfolio.id, requestedFundsForStocks, availableFunds))
    }
    nonEmptyListOf(
        PortfolioEvent.StocksPurchased(
            command.portfolioId,
            command.occurredOn,
            command.stock,
            command.quantity,
            command.price,
        ),
    )
}

private fun sellStocks(
    portfolio: Portfolio,
    command: SellStocks,
): Either<PortfolioError, NonEmptyList<PortfolioEvent>> = either {
    if (portfolio.isClosed()) {
        raise(PortfolioIsClosed(command.portfolioId))
    }
    val ownedStocks = portfolio.ownedStocks(command.stock)
    if (ownedStocks < command.quantity) {
        raise(
            PortfolioError.NotEnoughStocks(
                portfolio.id,
                command.stock,
                command.quantity,
                ownedStocks,
            ),
        )
    }

    nonEmptyListOf(
        StocksSold(
            command.portfolioId,
            command.occurredOn,
            command.stock,
            command.quantity,
            command.price,
        ),
    )
}

fun closePortfolio(
    portfolio: Portfolio,
    command: ClosePortfolio,
): Either<PortfolioError, NonEmptyList<PortfolioEvent>> = either {
    if (portfolio.isClosed()) {
        raise(PortfolioIsClosed(command.portfolioId))
    }
    val stocksSoldEvents: List<StocksSold> = portfolio.ownedStocks().map {
        StocksSold(
            command.portfolioId,
            command.occurredOn,
            it.stock,
            it.quantity,
            command.prices[it.stock] ?: raise(
                PriceNotAvailable(command.portfolioId, it.stock),
            ),
        )
    }
    (stocksSoldEvents + PortfolioClosed(command.portfolioId, command.occurredOn)).toNonEmptyListOrNull()
        ?: nonEmptyListOf(
            PortfolioClosed(
                command.portfolioId,
                command.occurredOn,
            ),
        )
}

fun evolve(portfolio: Portfolio, event: PortfolioEvent): Portfolio = portfolio + event

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureRouting()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
