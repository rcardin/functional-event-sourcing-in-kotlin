package `in`.rcard.fes.portfolio

sealed interface DomainError

data class InvalidFieldError(val field: String, val error: String) : DomainError

sealed interface PortfolioError : DomainError {
    val portfolioId: PortfolioId

    data class PortfolioAlreadyExists(override val portfolioId: PortfolioId) : PortfolioError

    data class PortfolioNotAvailable(override val portfolioId: PortfolioId) : PortfolioError

    data class InsufficientFunds(override val portfolioId: PortfolioId, val requested: Money, val owned: Money) :
        PortfolioError

    data class NotEnoughStocks(
        override val portfolioId: PortfolioId,
        val stock: Stock,
        val requested: Quantity,
        val owned: Quantity,
    ) :
        PortfolioError

    data class PortfolioIsClosed(override val portfolioId: PortfolioId) : PortfolioError
    data class PriceNotAvailable(override val portfolioId: PortfolioId, val stock: Stock) : PortfolioError
}
