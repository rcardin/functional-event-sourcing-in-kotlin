package `in`.rcard.fes.stock

import arrow.core.Either
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.Stock

sealed interface StockError {
    // TODO Add possible errors
}

interface FindStockBySymbol {
    suspend fun findPriceBySymbol(symbol: Stock): Either<StockError, Money?>
}
