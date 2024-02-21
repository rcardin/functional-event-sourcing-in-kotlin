package `in`.rcard.fes.projection

import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventDataBuilder
import com.eventstore.dbclient.ExpectedRevision
import `in`.rcard.eventstore.DockerContainerDatabase
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.portfolio.eventType
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.matchers.shouldBe
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
            val eventStoreDb =
                install(
                    ContainerExtension(
                        DockerContainerDatabase(
                            DockerContainerDatabase.Builder().version("latest")
                                .secure(false),
                        ),
                    ),
                )
            val eventStoreDBClient = eventStoreDb.defaultClient()
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
                    portfolioCreatedEventFlow.collect { actualEvent ->
                        actualEvent shouldBe portfolioCreatedEvent
                    }
                }
            }
        }
    }
})
