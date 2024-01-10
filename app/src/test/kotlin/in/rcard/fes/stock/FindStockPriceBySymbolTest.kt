package `in`.rcard.fes.stock

import arrow.fx.coroutines.continuations.resource
import `in`.rcard.fes.env.sqlDelight
import io.kotest.assertions.arrow.fx.coroutines.extension
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
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

    context("The findPriceBySymbol function") {
        should("return the price of the stock") {
            val stockPricesRepository = stockPricesRepository(database.get().stockPricesQueries)
        }
    }
})
