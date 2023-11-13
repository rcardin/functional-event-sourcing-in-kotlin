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
value class Money(private val amount: Double) {
    operator fun minus(money: Money): Money = Money(this.amount - money.amount)
    operator fun times(quantity: Quantity): Money = Money(this.amount * quantity.amount)
    operator fun compareTo(money: Money): Int = when {
        this.amount > money.amount -> 1
        this.amount < money.amount -> -1
        else -> 0
    }
}

fun Portfolio.isAvailable(): Boolean = this.isNotEmpty()

val Portfolio.id: PortfolioId
    get() = this.first().portfolioId

fun Portfolio.availableFunds(): Money =
    this.fold(Money(0.0)) { acc, event ->
        when (event) {
            is PortfolioEvent.PortfolioCreated -> event.money
            is PortfolioEvent.StocksPurchased -> acc - (event.price * event.quantity)
        }
    }

val notCreatedPortfolio: List<PortfolioEvent> = emptyList()

@JvmInline
value class Stock(private val symbol: String)

@JvmInline
value class Quantity(val amount: Int)

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

    data class PortfolioNotAvailable(override val portfolioId: PortfolioId) : PortfolioError

    data class InsufficientFunds(override val portfolioId: PortfolioId, val requested: Money, val owned: Money) :
        PortfolioError
}
