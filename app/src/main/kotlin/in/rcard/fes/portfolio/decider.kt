package `in`.rcard.fes.portfolio

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.withError
import arrow.core.toNonEmptyListOrNull
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError

interface PortfolioService {
    suspend fun handle(command: PortfolioCommand): Either<Union<EventStoreError, PortfolioError>, PortfolioId>
}

fun portfolioService(portfolioEventStore: PortfolioEventStore): PortfolioService =
    object : PortfolioService {
        override suspend fun handle(command: PortfolioCommand): Either<Union<EventStoreError, PortfolioError>, PortfolioId> =
            handle(command)
    }

// TODO Put a limit on the recursion depth
context (PortfolioEventStore)
suspend fun handle(command: PortfolioCommand): Either<Union<EventStoreError, PortfolioError>, PortfolioId> =
    either {
        val (eTag, portfolio) = withError({ Union.First(it) }) { loadState(command.portfolioId).bind() }
        val events = withError({ Union.Second(it) }) { decide(command, portfolio).bind() }
        val newPortfolio = events.fold(portfolio) { currentPortfolio, event -> evolve(currentPortfolio, event) }
        saveState(command.portfolioId, eTag, portfolio, newPortfolio).fold(
            {
                when (it) {
                    is EventStoreError.ConcurrentModificationError -> handle(command).bind()
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
        is PortfolioCommand.CreatePortfolio -> createPortfolio(portfolio, command)
        is PortfolioCommand.BuyStocks -> buyStocks(portfolio, command)
        is PortfolioCommand.SellStocks -> sellStocks(portfolio, command)
        is PortfolioCommand.ClosePortfolio -> closePortfolio(portfolio, command)
    }

private fun createPortfolio(
    portfolio: Portfolio,
    command: PortfolioCommand.CreatePortfolio,
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
    command: PortfolioCommand.BuyStocks,
): Either<PortfolioError, NonEmptyList<PortfolioEvent>> = either {
    if (!portfolio.isAvailable()) {
        raise(PortfolioError.PortfolioNotAvailable(command.portfolioId))
    }
    if (portfolio.isClosed()) {
        raise(PortfolioError.PortfolioIsClosed(command.portfolioId))
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
    command: PortfolioCommand.SellStocks,
): Either<PortfolioError, NonEmptyList<PortfolioEvent>> = either {
    if (portfolio.isClosed()) {
        raise(PortfolioError.PortfolioIsClosed(command.portfolioId))
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
        PortfolioEvent.StocksSold(
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
    command: PortfolioCommand.ClosePortfolio,
): Either<PortfolioError, NonEmptyList<PortfolioEvent>> = either {
    if (portfolio.isClosed()) {
        raise(PortfolioError.PortfolioIsClosed(command.portfolioId))
    }
    val stocksSoldEvents: List<PortfolioEvent.StocksSold> = portfolio.ownedStocks().map {
        PortfolioEvent.StocksSold(
            command.portfolioId,
            command.occurredOn,
            it.stock,
            it.quantity,
            command.prices[it.stock] ?: raise(
                PortfolioError.PriceNotAvailable(command.portfolioId, it.stock),
            ),
        )
    }
    (stocksSoldEvents + PortfolioEvent.PortfolioClosed(command.portfolioId, command.occurredOn)).toNonEmptyListOrNull()
        ?: nonEmptyListOf(
            PortfolioEvent.PortfolioClosed(
                command.portfolioId,
                command.occurredOn,
            ),
        )
}

fun evolve(portfolio: Portfolio, event: PortfolioEvent): Portfolio = portfolio + event
