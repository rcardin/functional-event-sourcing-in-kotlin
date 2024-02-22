package `in`.rcard.fes.projection

import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventDataBuilder
import com.eventstore.dbclient.ExpectedRevision
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.portfolio.eventType
import `in`.rcard.fes.withEventStoreDb
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
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
                        val portfolioCreatedEvent =
                            PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            )
                        val portfolioCreatedEventData =
                            portfolioCreatedEvent.let {
                                EventDataBuilder.json(
                                    UUID.randomUUID(),
                                    it.eventType(),
                                    encodeToString(it).encodeToByteArray(),
                                ).build()
                            }
                        val appendToStreamOptions: AppendToStreamOptions =
                            AppendToStreamOptions.get().expectedRevision(ExpectedRevision.noStream())
                        eventStoreDBClient.appendToStream(
                            "portfolio-1",
                            appendToStreamOptions,
                            portfolioCreatedEventData,
                        ).await()
                        val portfolioCreatedEventFlow = portfolioCreatedEventFlow(eventStoreDBClient)
                        val actualEvent = portfolioCreatedEventFlow.take(1).single()
                        actualEvent shouldBe portfolioCreatedEvent
                    }
                }
            }
        }
    }
})
