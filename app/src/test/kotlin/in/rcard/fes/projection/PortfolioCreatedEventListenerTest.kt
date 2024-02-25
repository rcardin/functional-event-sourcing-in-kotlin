package `in`.rcard.fes.projection

import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventDataBuilder
import com.eventstore.dbclient.ExpectedRevision
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioEvent.StocksPurchased
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.Quantity
import `in`.rcard.fes.portfolio.Stock
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.portfolio.eventType
import `in`.rcard.fes.toListDuring
import `in`.rcard.fes.withEventStoreDb
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val NOW_MILLIS = Instant.now().toEpochMilli()

class PortfolioCreatedEventListenerTest : ShouldSpec({

    val logger = LoggerFactory.getLogger(PortfolioCreatedEventListenerTest::class.java)

    context("The portfolio created event listener") {
        should("receive a portfolio created event") {
            withEventStoreDb { eventStoreDBClient ->
                with(Json) {
                    with(logger) {
                        val rcardinPortfolioCreated =
                            PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            )
                        val danielPortfolioCreated =
                            PortfolioCreated(
                                PortfolioId("2"),
                                NOW_MILLIS,
                                UserId("daniel"),
                                Money(50.0),
                            )
                        val portfolioEvents =
                            listOf(
                                StocksPurchased(
                                    PortfolioId("3"),
                                    NOW_MILLIS,
                                    Stock("AAPL"),
                                    Quantity(1000),
                                    Money(120.0),
                                ),
                                rcardinPortfolioCreated,
                                danielPortfolioCreated,
                            )
                        val portfolioEventsData =
                            portfolioEvents.map {
                                EventDataBuilder.json(
                                    UUID.randomUUID(),
                                    it.eventType(),
                                    encodeToString(it).encodeToByteArray(),
                                ).build()
                            }
                        val appendToStreamOptions: AppendToStreamOptions =
                            AppendToStreamOptions.get().expectedRevision(ExpectedRevision.noStream())
                        eventStoreDBClient.appendToStream(
                            "portfolios",
                            appendToStreamOptions,
                            portfolioEventsData.iterator(),
                        ).await()
                        val portfolioCreatedEventFlow = portfolioCreatedEventFlow(eventStoreDBClient)
                        val actualEvent = portfolioCreatedEventFlow.toListDuring(Duration.ofSeconds(1L))
                        actualEvent.shouldContainExactly(rcardinPortfolioCreated, danielPortfolioCreated)
                    }
                }
            }
        }
    }
})
