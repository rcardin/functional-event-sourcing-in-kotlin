package `in`.rcard.fes.projection

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.SubscribeToAllOptions
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionFilter
import com.eventstore.dbclient.SubscriptionListener
import `in`.rcard.fes.portfolio.PortfolioEvent
import `in`.rcard.fes.portfolio.PortfolioEvent.PortfolioCreated
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import org.slf4j.Logger

context(Logger, Json)
fun portfolioCreatedEventFlow(eventStoreClient: EventStoreDBClient): Flow<PortfolioEvent> =
    channelFlow {
        val subscriptionListener =
            object : SubscriptionListener() {
                override fun onEvent(
                    subscription: Subscription?,
                    event: ResolvedEvent?,
                ) {
                    if (event != null) {
                        this@Logger.info(
                            "Event received: ${event.event.eventType} for stream ${event.event.streamId} " +
                                "by subscription: ${subscription?.subscriptionId}",
                        )
                        val portfolioEvent =
                            decodeFromString<PortfolioCreated>(event.originalEvent.eventData.decodeToString())
                        trySendBlocking(portfolioEvent)
                    }
                }

                override fun onCancelled(
                    subscription: Subscription?,
                    exception: Throwable?,
                ) {
                    cancel("Subscription to portfolio-created stream was cancelled", exception)
                }
            }
        val subscriptionFilter =
            SubscribeToAllOptions.get()
                .filter(
                    SubscriptionFilter.newBuilder()
                        .withEventTypeRegularExpression("portfolio-create")
                        .build(),
                )
        eventStoreClient.subscribeToAll(subscriptionListener, subscriptionFilter)
        awaitClose()
    }
