package `in`.rcard.fes

import com.eventstore.dbclient.EventStoreDBClient
import `in`.rcard.eventstore.DockerContainerDatabase
import io.kotest.core.extensions.install
import io.kotest.core.spec.Spec
import io.kotest.extensions.testcontainers.ContainerExtension

context(Spec)
suspend fun withEventStoreDb(testSuite: suspend (EventStoreDBClient) -> Unit) {
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
    testSuite(eventStoreDBClient)
}
