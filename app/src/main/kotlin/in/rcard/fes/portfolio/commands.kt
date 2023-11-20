package `in`.rcard.fes.portfolio

sealed interface PortfolioCommand {
    data class CreatePortfolio(val userId: UserId, val amount: Money) : PortfolioCommand

    sealed interface PortfolioCommandWithPortfolioId : PortfolioCommand {
        val portfolioId: PortfolioId

        data class BuyStocks(
            override val portfolioId: PortfolioId,
            val stock: Stock,
            val quantity: Quantity,
            val price: Money,
        ) : PortfolioCommandWithPortfolioId

        data class SellStocks(
            override val portfolioId: PortfolioId,
            val stock: Stock,
            val quantity: Quantity,
            val price: Money,
        ) : PortfolioCommandWithPortfolioId

        data class ClosePortfolio(override val portfolioId: PortfolioId, val prices: Prices) :
            PortfolioCommandWithPortfolioId
    }
}
