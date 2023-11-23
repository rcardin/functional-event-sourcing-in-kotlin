package `in`.rcard.fes.portfolio

sealed interface PortfolioCommand {
    val portfolioId: PortfolioId
    val occurredOn: Long

    data class CreatePortfolio(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val userId: UserId,
        val amount: Money,
    ) :
        PortfolioCommand

    data class BuyStocks(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioCommand

    data class SellStocks(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioCommand

    data class ClosePortfolio(
        override val portfolioId: PortfolioId,
        override val occurredOn: Long,
        val prices: Prices,
    ) :
        PortfolioCommand
}
