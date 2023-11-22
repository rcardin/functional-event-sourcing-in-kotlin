package `in`.rcard.fes.portfolio

sealed interface PortfolioCommand {
    val portfolioId: PortfolioId

    data class CreatePortfolio(override val portfolioId: PortfolioId, val userId: UserId, val amount: Money) :
        PortfolioCommand

    data class BuyStocks(
        override val portfolioId: PortfolioId,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioCommand

    data class SellStocks(
        override val portfolioId: PortfolioId,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioCommand

    data class ClosePortfolio(override val portfolioId: PortfolioId, val prices: Prices) :
        PortfolioCommand
}
