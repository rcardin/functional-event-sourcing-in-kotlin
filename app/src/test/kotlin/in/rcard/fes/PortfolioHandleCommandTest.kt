package `in`.rcard.fes

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import `in`.rcard.fes.portfolio.InfrastructureError.PersistenceError
import `in`.rcard.fes.portfolio.Money
import `in`.rcard.fes.portfolio.PortfolioCommand.CreatePortfolio
import `in`.rcard.fes.portfolio.PortfolioError.PortfolioAlreadyExists
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import `in`.rcard.fes.portfolio.PortfolioEventStore
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.LoadingError.StateLoadingError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.SavingError.ConcurrentModificationError
import `in`.rcard.fes.portfolio.PortfolioEventStore.EventStoreError.SavingError.StateSavingError
import `in`.rcard.fes.portfolio.PortfolioId
import `in`.rcard.fes.portfolio.UserId
import `in`.rcard.fes.portfolio.handle
import `in`.rcard.fes.portfolio.notCreatedPortfolio
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
                        PortfolioCreated(
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
                    handle(CreatePortfolio(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)))
                actualResult.shouldBeRight(PortfolioId("1"))
            }
        }
        should("return a portfolio error").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns (
                0L to listOf(
                    PortfolioCreated(
                        PortfolioId("1"),
                        NOW_MILLIS,
                        UserId("rcardin"),
                        Money(100.0),
                    ),
                )
                ).right()
            with(eventStore) {
                val actualResult =
                    handle(CreatePortfolio(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)))
                actualResult.shouldBeLeft(PortfolioAlreadyExists(PortfolioId("1")))
            }
        }
        should("return an event store error in case of error during the loading of the event").config(coroutineTestScope = true) {
            val eventStore = mockk<PortfolioEventStore>()
            coEvery { eventStore.loadState(PortfolioId("1")) } returns StateLoadingError(
                PortfolioId("1"),
            ).left()
            with(eventStore) {
                val actualResult =
                    handle(CreatePortfolio(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)))
                actualResult.shouldBeLeft(PersistenceError)
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
                        PortfolioCreated(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    ),
                )
            } returns StateSavingError(PortfolioId("1")).left()
            with(eventStore) {
                val actualResult =
                    handle(CreatePortfolio(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)))
                actualResult.shouldBeLeft(PersistenceError)
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
                        PortfolioCreated(
                            PortfolioId("1"),
                            NOW_MILLIS,
                            UserId("rcardin"),
                            Money(100.0),
                        ),
                    ),
                )
            } returns ConcurrentModificationError(PortfolioId("1")).left() andThen PortfolioId("1").right()
            with(eventStore) {
                val actualResult =
                    handle(CreatePortfolio(PortfolioId("1"), NOW_MILLIS, UserId("rcardin"), Money(100.0)))
                actualResult.shouldBeRight(PortfolioId("1"))
            }
        }
    }
})
