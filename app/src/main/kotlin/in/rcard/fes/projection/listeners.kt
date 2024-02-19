package `in`.rcard.fes.projection

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionListener
import `in`.rcard.fes.portfolio.PortfolioEvent
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.slf4j.Logger

interface PortfolioEventListener {
    suspend fun start()
}

context(Logger)
fun portfolioEventListener(
    eventStoreClient: EventStoreDBClient,
    createPortfolioUseCase: CreatePortfolioUseCase,
): PortfolioEventListener =
    object : PortfolioEventListener {
        val logger = this@Logger

        override suspend fun start()  {
            val sl: SubscriptionListener =
                object : SubscriptionListener() {
                    override fun onEvent(
                        subscription: Subscription?,
                        event: ResolvedEvent?,
                    ) {
                        if (event != null) {
                            logger.info(
                                "Event received: ${event.event.eventType} for stream ${event.event.streamId} " +
                                    "by subscription: ${subscription?.subscriptionId}",
                            )
                            val portfolioEvent =
                                decodeFromString<PortfolioEvent>(event.originalEvent.eventData.decodeToString())
                            when (portfolioEvent) {
                                is PortfolioEvent.PortfolioCreated -> createPortfolioUseCase.createPortfolio(portfolioEvent)
                                is PortfolioEvent.StocksPurchased -> logger.info("Stocks purchased: $portfolioEvent")
                                is PortfolioEvent.StocksSold -> logger.info("Stocks sold: $portfolioEvent")
                                is PortfolioEvent.PortfolioClosed -> logger.info("Portfolio closed: $portfolioEvent")
                            }
                        }
                    }
                }
            eventStoreClient.subscribeToAll(sl)
        }
    }
