package `in`.rcard.fes.stock

import arrow.fx.coroutines.continuations.resource
import `in`.rcard.fes.env.sqlDelight
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.Stock
import `in`.rcard.fes.sqldelight.StockPrices
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.fx.coroutines.extension
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer

class FindStockPriceBySymbolTest : ShouldSpec({

    val postgres = PostgreSQLContainer<Nothing>("postgres:latest")

    val dataSource = install(JdbcDatabaseContainerExtension(postgres))
    val database =
        install(
            resource {
                sqlDelight(dataSource)
            }.extension(),
        )

    val logger = LoggerFactory.getLogger(FindStockPriceBySymbolTest::class.java)

    beforeSpec {
        database.get().stockPricesQueries.insertStockPrice(StockPrices(Stock("AAPL"), Money(100.0)))
    }

    context("The findPriceBySymbol function") {

        should("return the price of the stock") {
            val stockPricesRepository = with(logger) { stockPricesRepository(database.get().stockPricesQueries) }
            stockPricesRepository.findPriceBySymbol(Stock("AAPL")).shouldBeRight(Money(100.0))
        }

        should("return a StockPricesNotFoundError if the symbol is not present") {
            val stockPricesRepository = with(logger) { stockPricesRepository(database.get().stockPricesQueries) }
            stockPricesRepository.findPriceBySymbol(Stock("GOOG"))
                .shouldBeLeft(StockPricesError.StockPricesNotFoundError(Stock("GOOG")))
        }
    }
})
