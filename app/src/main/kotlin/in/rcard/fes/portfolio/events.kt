package `in`.rcard.fes.portfolio

sealed interface PortfolioEvent {
    val portfolioId: PortfolioId
    val occurredOn: Long

    data class PortfolioCreated(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val userId: UserId,
        val money: Money,
    ) :
        PortfolioEvent

    data class StocksPurchased(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioEvent

    data class StocksSold(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioEvent

    data class PortfolioClosed(override val portfolioId: PortfolioId, override val occurredOn: Long) : PortfolioEvent
}
