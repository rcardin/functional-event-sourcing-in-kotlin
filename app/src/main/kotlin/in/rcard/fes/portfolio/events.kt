package `in`.rcard.fes.portfolio

import kotlinx.serialization.Serializable

@Serializable
sealed interface PortfolioEvent {
    val portfolioId: PortfolioId
    val occurredOn: Long

    @Serializable
    data class PortfolioCreated(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val userId: UserId,
        val money: Money,
    ) :
        PortfolioEvent

    @Serializable
    data class StocksPurchased(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioEvent

    @Serializable
    data class StocksSold(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioEvent

    @Serializable
    data class PortfolioClosed(override val portfolioId: PortfolioId, override val occurredOn: Long) : PortfolioEvent
}
