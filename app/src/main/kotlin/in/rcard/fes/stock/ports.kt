package `in`.rcard.fes.stock

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.Stock
import `in`.rcard.fes.sqldelight.StockPricesQueries
import `in`.rcard.fes.stock.StockPricesError.StockPricesGenericError

sealed interface StockPricesError {
    data object StockPricesGenericError : StockPricesError
}

interface FindStockPriceBySymbol {
    // TODO Is it correct to return a StockPricesError?
    suspend fun findPriceBySymbol(symbol: Stock): Either<StockPricesError, Money?>
}

fun stockPricesRepository(stockPricesQueries: StockPricesQueries): FindStockPriceBySymbol =
    object : FindStockPriceBySymbol {
        override suspend fun findPriceBySymbol(symbol: Stock): Either<StockPricesError, Money?> =
            either {
                catch({
                    stockPricesQueries.findPriceByStockId(symbol).executeAsOneOrNull()?.price
                }) {
                    raise(StockPricesGenericError)
                }
            }
    }
