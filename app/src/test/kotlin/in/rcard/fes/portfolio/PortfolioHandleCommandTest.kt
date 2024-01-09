package `in`.rcard.fes.portfolio

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant

private val NOW_MILLIS = Instant.now().toEpochMilli()

class PortfolioHandleCommandTest : ShouldSpec({
    context("The handle function") {
        should("return the portfolio id in case of no error").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns (0L to notCreatedPortfolio).right()
            coEvery {
                eventStore.saveState(
                    PortfolioId("1"),
                    0L,
                    notCreatedPortfolio,
                    nonEmptyListOf(
                        PortfolioEvent.PortfolioCreated(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    ),
                )
            } returns PortfolioId("1").right()
            with(eventStore) {
                val actualResult =
                    handle(
                        PortfolioCommand.CreatePortfolio(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    )
                actualResult.shouldBeRight(PortfolioId("1"))
            }
        }
        should("return a portfolio error").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns
                (
                    0L to
                        listOf(
                            PortfolioEvent.PortfolioCreated(
                                PortfolioId("1"),
                                NOW_MILLIS,
                                UserId("rcardin"),
                                Money(100.0),
                            ),
                        )
                ).right()
            with(eventStore) {
                val actualResult =
                    handle(
                        PortfolioCommand.CreatePortfolio(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    )
                actualResult.shouldBeLeft(PortfolioError.PortfolioAlreadyExists(PortfolioId("1")))
            }
        }
        should("return an event store error in case of error during the loading of the event").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns
                PortfolioEventStore.EventStoreError.LoadingError.StateLoadingError(
                    PortfolioId("1"),
                ).left()
            with(eventStore) {
                val actualResult =
                    handle(
                        PortfolioCommand.CreatePortfolio(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    )
                actualResult.shouldBeLeft(InfrastructureError.PersistenceError)
            }
        }
        should("return an event store error in case of error during the saving of the event").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns (0L to notCreatedPortfolio).right()
            coEvery {
                eventStore.saveState(
                    PortfolioId("1"),
                    0L,
                    notCreatedPortfolio,
                    nonEmptyListOf(
                        PortfolioEvent.PortfolioCreated(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    ),
                )
            } returns PortfolioEventStore.EventStoreError.SavingError.StateSavingError(PortfolioId("1")).left()
            with(eventStore) {
                val actualResult =
                    handle(
                        PortfolioCommand.CreatePortfolio(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    )
                actualResult.shouldBeLeft(InfrastructureError.PersistenceError)
            }
        }
        should("retry the save operation in case of concurrent modification error").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns (0L to notCreatedPortfolio).right()
            coEvery {
                eventStore.saveState(
                    PortfolioId("1"),
                    0L,
                    notCreatedPortfolio,
                    nonEmptyListOf(
                        PortfolioEvent.PortfolioCreated(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    ),
                )
            } returns
                PortfolioEventStore.EventStoreError.SavingError.ConcurrentModificationError(PortfolioId("1"))
                    .left() andThen PortfolioId("1").right()
            with(eventStore) {
                val actualResult =
                    handle(
                        PortfolioCommand.CreatePortfolio(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    )
                actualResult.shouldBeRight(PortfolioId("1"))
            }
        }
    }
})
