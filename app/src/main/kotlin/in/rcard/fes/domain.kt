package `in`.rcard.fes

// Easiest implementation of the domain
typealias Portfolio = List<PortfolioEvent>

typealias Prices = Map<Stock, Money>

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
            is PortfolioEvent.PortfolioClosed -> Money(0.0)
        }
    }

fun Portfolio.ownedStocks(stock: Stock): Quantity =
    this.fold(Quantity(0)) { acc, event ->
        when (event) {
            is PortfolioEvent.PortfolioCreated -> Quantity(0)
            is PortfolioEvent.StocksPurchased -> if (event.stock == stock) acc + event.quantity else acc
            is PortfolioEvent.StocksSold -> if (event.stock == stock) acc - event.quantity else acc
            is PortfolioEvent.PortfolioClosed -> Quantity(0)
        }
    }

fun Portfolio.ownedStocks(): List<OwnedStock> =
    this.fold(mapOf<Stock, OwnedStock>()) { acc, event ->
        when (event) {
            is PortfolioEvent.PortfolioCreated -> acc
            is PortfolioEvent.StocksPurchased -> {
                acc[event.stock]?.let {
                    acc + (
                        event.stock to OwnedStock(
                            event.stock,
                            it.quantity + event.quantity,
                        )
                        )
                } ?: (
                    acc + (
                        event.stock to OwnedStock(
                            event.stock,
                            event.quantity,
                        )
                        )
                    )
            }

            is PortfolioEvent.StocksSold -> {
                acc[event.stock]?.let {
                    acc + (
                        event.stock to OwnedStock(
                            event.stock,
                            it.quantity - event.quantity,
                        )
                        )
                } ?: acc
            }

            is PortfolioEvent.PortfolioClosed -> acc
        }
    }.values.toList()

data class OwnedStock(val stock: Stock, val quantity: Quantity)

fun Portfolio.isClosed(): Boolean = this.last() is PortfolioEvent.PortfolioClosed

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

        data class ClosePortfolio(override val portfolioId: PortfolioId, val prices: Prices) :
            PortfolioCommandWithPortfolioId
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

    data class PortfolioClosed(override val portfolioId: PortfolioId) : PortfolioEvent
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

    data class PortfolioIsClosed(override val portfolioId: PortfolioId) : PortfolioError
    data class PriceNotAvailable(override val portfolioId: PortfolioId, val stock: Stock) : PortfolioError
}
