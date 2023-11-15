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
    operator fun plus(money: Money): Money = Money(this.amount + money.amount)
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
            is PortfolioEvent.StocksSold -> acc + (event.price * event.quantity)
        }
    }

fun Portfolio.ownedStocks(stock: Stock): Quantity =
    this.fold(Quantity(0)) { acc, event ->
        when (event) {
            is PortfolioEvent.PortfolioCreated -> Quantity(0)
            is PortfolioEvent.StocksPurchased -> if (event.stock == stock) acc + event.quantity else acc
            is PortfolioEvent.StocksSold -> if (event.stock == stock) acc - event.quantity else acc
        }
    }

val notCreatedPortfolio: List<PortfolioEvent> = emptyList()

@JvmInline
value class Stock(private val symbol: String)

@JvmInline
value class Quantity(val amount: Int) {
    operator fun plus(qty: Quantity): Quantity = Quantity(this.amount + qty.amount)

    operator fun minus(qty: Quantity): Quantity = Quantity(this.amount - qty.amount)
    operator fun compareTo(quantity: Quantity): Int {
        return when {
            this.amount > quantity.amount -> 1
            this.amount < quantity.amount -> -1
            else -> 0
        }
    }
}

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

    data class StocksSold(
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

    data class NotEnoughStocks(
        override val portfolioId: PortfolioId,
        val stock: Stock,
        val requested: Quantity,
        val owned: Quantity,
    ) :
        PortfolioError
}
