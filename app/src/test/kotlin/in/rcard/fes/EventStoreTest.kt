package `in`.rcard.fes

import `in`.rcard.eventstore.DockerContainerDatabase
import `in`.rcard.fes.portfolio.portfolioEventStore
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.testcontainers.ContainerExtension

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
            portfolioEventStore(eventStoreDBClient)
        }
    }
})
