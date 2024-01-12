package `in`.rcard.fes.portfolio

import arrow.core.nonEmptyListOf
import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.EventDataBuilder
import com.eventstore.dbclient.ExpectedRevision
import com.eventstore.dbclient.ReadResult
import com.eventstore.dbclient.ReadStreamOptions
import `in`.rcard.eventstore.DockerContainerDatabase
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

private val NOW_MILLIS = Instant.now().toEpochMilli()

class EventStoreTest : ShouldSpec({

    val logger = LoggerFactory.getLogger(PortfolioEventStore::class.java)

    context("The event store module") {
        should("write event stream to the event store") {
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
                    val portfolioEventStore = portfolioEventStore(eventStoreDBClient)
                    portfolioEventStore.saveState(
                        PortfolioId("1"),
                        -1L,
                        notCreatedPortfolio,
                        nonEmptyListOf(
                            PortfolioEvent.PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            ),
                        ),
                    ).shouldBeRight(PortfolioId("1"))

                    val result: ReadResult = eventStoreDBClient.readStream("portfolio-1", ReadStreamOptions.get()).get()
                    result.events.map {
                        decodeFromString<PortfolioEvent>(it.originalEvent.eventData.decodeToString())
                    }
                        .shouldContainExactly(
                            PortfolioEvent.PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            ),
                        )
                }
            }
        }

        should("return a ConcurrentModificationError in case of concurrent modification of the stream") {
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
                    val portfolioEventStore = portfolioEventStore(eventStoreDBClient)
                    portfolioEventStore.saveState(
                        PortfolioId("1"),
                        -1L,
                        notCreatedPortfolio,
                        nonEmptyListOf(
                            PortfolioEvent.PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            ),
                        ),
                    ).shouldBeRight(PortfolioId("1"))

                    portfolioEventStore.saveState(
                        PortfolioId("1"),
                        -1L,
                        notCreatedPortfolio,
                        nonEmptyListOf(
                            PortfolioEvent.PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            ),
                        ),
                    ).shouldBeLeft(
                        PortfolioEventStore.EventStoreError.SavingError.ConcurrentModificationError(
                            PortfolioId("1"),
                        ),
                    )
                }
            }
        }

        should("read events from an event stream") {
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
                    val portfolioEventStore = portfolioEventStore(eventStoreDBClient)
                    val events =
                        listOf(
                            PortfolioEvent.PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            ),
                            PortfolioEvent.StocksPurchased(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                Stock("AAPL"),
                                Quantity(10),
                                Money(1.5),
                            ),
                        )
                    val eventDataList =
                        events.map { event ->
                            EventDataBuilder.json(
                                UUID.randomUUID(),
                                event.eventType(),
                                encodeToString(event).encodeToByteArray(),
                            ).build()
                        }
                    val appendToStreamOptions: AppendToStreamOptions =
                        AppendToStreamOptions.get().expectedRevision(ExpectedRevision.noStream())
                    eventStoreDBClient.appendToStream(
                        "portfolio-1",
                        appendToStreamOptions,
                        eventDataList.iterator(),
                    ).await()

                    portfolioEventStore.loadState(PortfolioId("1")).shouldBeRight(1L to events)
                }
            }
        }

        should("return an UnknownStreamError in case of unknown stream") {
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
                    val portfolioEventStore = portfolioEventStore(eventStoreDBClient)
                    portfolioEventStore.loadState(PortfolioId("1")).shouldBeLeft(
                        PortfolioEventStore.EventStoreError.LoadingError.UnknownStreamError(PortfolioId("1")),
                    )
                }
            }
        }
    }
})
