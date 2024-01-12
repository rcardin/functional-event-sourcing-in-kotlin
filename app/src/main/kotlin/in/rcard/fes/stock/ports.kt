package `in`.rcard.fes.stock

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.Stock
import `in`.rcard.fes.sqldelight.StockPricesQueries
import `in`.rcard.fes.stock.StockPricesError.StockPricesGenericError
import org.slf4j.Logger

sealed interface StockPricesError {
    data class StockPricesNotFoundError(val stock: Stock) : StockPricesError

    data object StockPricesGenericError : StockPricesError
}

interface FindStockPriceBySymbol {
    suspend fun findPriceBySymbol(symbol: Stock): Either<StockPricesError, Money>
}

context(Logger)
fun stockPricesRepository(stockPricesQueries: StockPricesQueries): FindStockPriceBySymbol =
    object : FindStockPriceBySymbol {
        override suspend fun findPriceBySymbol(symbol: Stock): Either<StockPricesError, Money> =
            either {
                catch({
                    stockPricesQueries.findPriceByStockId(symbol).executeAsOneOrNull()?.price ?: raise(
                        StockPricesError.StockPricesNotFoundError(symbol),
                    )
                }) { exception ->
                    this@Logger.error("Error while retrieving the price of the stock $symbol", exception)
                    raise(StockPricesGenericError)
                }
            }
    }
