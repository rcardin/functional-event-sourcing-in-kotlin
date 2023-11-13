package `in`.rcard.fes

// Easiest implementation of the domain
typealias Portfolio = List<PortfolioEvent>

@JvmInline
value class PortfolioId(private val id: String)

@JvmInline
value class UserId(private val id: String) {
    override fun toString(): String = id
}

@JvmInline
value class Money(private val amount: Double)

val Portfolio.initial: Boolean
    get() = this.isEmpty()

val Portfolio.id: PortfolioId
    get() = this.first().portfolioId

val notCreatedPortfolio: List<PortfolioEvent> = emptyList()

@JvmInline
value class Stock(private val symbol: String)

@JvmInline
value class Quantity(private val amount: Int)

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
    }
}

sealed interface PortfolioEvent {
    val portfolioId: PortfolioId

    data class PortfolioCreated(override val portfolioId: PortfolioId, val userId: UserId, val money: Money) :
        PortfolioEvent

    data class StocksPurchased(
        override val portfolioId: PortfolioId,
        val stock: Stock,
        val quantity: Quantity,
        val price: Money,
    ) : PortfolioEvent
}

sealed interface PortfolioError {
    val portfolioId: PortfolioId

    data class PortfolioAlreadyExists(override val portfolioId: PortfolioId) : PortfolioError
}
