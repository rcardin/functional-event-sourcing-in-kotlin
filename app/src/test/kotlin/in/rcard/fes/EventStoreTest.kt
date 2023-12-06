package `in`.rcard.fes

import arrow.core.nonEmptyListOf
import com.eventstore.dbclient.ReadResult
import com.eventstore.dbclient.ReadStreamOptions
import `in`.rcard.eventstore.DockerContainerDatabase
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.portfolio.notCreatedPortfolio
import `in`.rcard.fes.portfolio.portfolioEventStore
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.json.Json
import java.time.Instant

private val NOW_MILLIS = Instant.now().toEpochMilli()

class EventStoreTest : ShouldSpec({
    context("The event store module") {
        should("be able to connect to the event store") {
            val eventStoreDb = install(
                ContainerExtension(
                    DockerContainerDatabase(
                        DockerContainerDatabase.Builder().version("latest")
                            .secure(false),
                    ),
                ),
            )
            val eventStoreDBClient = eventStoreDb.defaultClient()
            val portfolioEventStore = portfolioEventStore(eventStoreDBClient)
            portfolioEventStore.saveState(
                PortfolioId("1"),
                -1L,
                notCreatedPortfolio,
                nonEmptyListOf(
                    PortfolioCreated(
                        PortfolioId("1"),
                        NOW_MILLIS,
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                ),
            ).shouldBeRight(PortfolioId("1"))

            val result: ReadResult = eventStoreDBClient.readStream("portfolio-1", ReadStreamOptions.get()).get()
            result.events.map {
                Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<PortfolioCreated>(it.originalEvent.eventData.decodeToString())
            }
                .shouldContainExactly(
                    PortfolioCreated(
                        PortfolioId("1"),
                        NOW_MILLIS,
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                )
        }
    }
})
