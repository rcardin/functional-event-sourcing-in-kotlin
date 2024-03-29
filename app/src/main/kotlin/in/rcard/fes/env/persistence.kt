package `in`.rcard.fes.env

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.closeable
import arrow.fx.coroutines.continuations.ResourceScope
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.Stock
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.sqldelight.Database
import `in`.rcard.fes.sqldelight.Portfolios
import `in`.rcard.fes.sqldelight.StockPrices
import java.math.BigDecimal
import javax.sql.DataSource

// Thanks to Simon Vergauwen for the idea
suspend fun ResourceScope.hikari(env: Env.DataSource): HikariDataSource =
    autoCloseable {
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = env.url
                username = env.username
                password = env.password
                driverClassName = env.driver
            },
        )
    }

suspend fun ResourceScope.sqlDelight(dataSource: DataSource): Database {
    val driver = closeable { dataSource.asJdbcDriver() }
    Database.Schema.create(driver)
    return Database(
        driver,
        Portfolios.Adapter(portfolioIdColumnAdapter, userIdColumnAdapter, moneyColumnAdapter),
        StockPrices.Adapter(stockIdColumnAdapter, moneyColumnAdapter),
    )
}

private val portfolioIdColumnAdapter: ColumnAdapter<PortfolioId, String> =
    object : ColumnAdapter<PortfolioId, String> {
        override fun decode(databaseValue: String): PortfolioId = PortfolioId(databaseValue)

        override fun encode(value: PortfolioId): String = value.id
    }

private val userIdColumnAdapter: ColumnAdapter<UserId, String> =
    object : ColumnAdapter<UserId, String> {
        override fun decode(databaseValue: String): UserId = UserId(databaseValue)

        override fun encode(value: UserId): String = value.id
    }

private val moneyColumnAdapter: ColumnAdapter<Money, BigDecimal> =
    object : ColumnAdapter<Money, BigDecimal> {
        override fun decode(databaseValue: BigDecimal): Money = Money(databaseValue.toDouble())

        override fun encode(value: Money): BigDecimal = value.amount.toBigDecimal()
    }

private val stockIdColumnAdapter: ColumnAdapter<Stock, String> =
    object : ColumnAdapter<Stock, String> {
        override fun decode(databaseValue: String): Stock = Stock(databaseValue)

        override fun encode(value: Stock): String = value.symbol
    }
