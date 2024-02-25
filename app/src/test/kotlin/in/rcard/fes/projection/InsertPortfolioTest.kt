package `in`.rcard.fes.projection

import arrow.fx.coroutines.continuations.resource
import `in`.rcard.fes.env.sqlDelight
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.sqldelight.Portfolios
import `in`.rcard.fes.stock.FindStockPriceBySymbolTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.fx.coroutines.extension
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val NOW_MILLIS = Instant.now().toEpochMilli()
private val NOW_LOCAL_DATE_TIME =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(NOW_MILLIS), ZoneId.systemDefault())

class InsertPortfolioTest : ShouldSpec({

    val postgres = PostgreSQLContainer<Nothing>("postgres:latest")

    val dataSource = install(JdbcDatabaseContainerExtension(postgres))
    val database =
        install(
            resource {
                sqlDelight(dataSource)
            }.extension(),
        )

    val logger = LoggerFactory.getLogger(FindStockPriceBySymbolTest::class.java)

    context("The insertPortfolio function") {
        should("insert a new portfolio") {
            val portfolioRepository = with(logger) { portfolioRepository(database.get().portfoliosQueries) }
            val portfolioCreated =
                PortfolioCreated(
                    PortfolioId("1"),
                    NOW_MILLIS,
                    UserId("rcardin"),
                    Money(100.0),
                )
            portfolioRepository.insertPortfolio(portfolioCreated).shouldBeRight(Unit)

            val actualRetrievedPortfolio =
                database.get().portfoliosQueries.findPortfolioByPortfolioIdAndUserId(
                    PortfolioId("1"),
                    UserId("rcardin"),
                ).executeAsOne()

            actualRetrievedPortfolio shouldBe
                Portfolios(
                    portfolio_id = PortfolioId("1"),
                    user_id = UserId("rcardin"),
                    money = Money(100.0),
                    created_at = NOW_LOCAL_DATE_TIME,
                    updated_at = NOW_LOCAL_DATE_TIME,
                )
        }
    }
})
