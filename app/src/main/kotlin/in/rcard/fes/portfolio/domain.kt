package `in`.rcard.fes.portfolio

import kotlinx.serialization.Serializable

// Easiest implementation of the domain
typealias Portfolio = List<PortfolioEvent>

typealias Prices = Map<Stock, Money>

@Serializable
@JvmInline
value class PortfolioId(val id: String)

@Serializable
@JvmInline
value class UserId(private val id: String) {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class Money(val amount: Double) {
    operator fun plus(money: Money): Money = Money(this.amount + money.amount)

    operator fun minus(money: Money): Money = Money(this.amount - money.amount)

    operator fun times(quantity: Quantity): Money = Money(this.amount * quantity.amount)

    operator fun compareTo(money: Money): Int =
        when {
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
                        event.stock to
                            OwnedStock(
                                event.stock,
                                it.quantity + event.quantity,
                            )
                    )
                } ?: (
                    acc + (
                        event.stock to
                            OwnedStock(
                                event.stock,
                                event.quantity,
                            )
                    )
                )
            }

            is PortfolioEvent.StocksSold -> {
                acc[event.stock]?.let {
                    acc + (
                        event.stock to
                            OwnedStock(
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

@Serializable
@JvmInline
value class Stock(val symbol: String) {
    override fun toString(): String = symbol
}

@Serializable
@JvmInline
value class Quantity(val amount: Int) {
    operator fun plus(qty: Quantity): Quantity = Quantity(this.amount + qty.amount)

    operator fun minus(qty: Quantity): Quantity = Quantity(this.amount - qty.amount)

    operator fun unaryMinus(): Quantity = Quantity(-this.amount)

    operator fun compareTo(quantity: Quantity): Int {
        return when {
            this.amount > quantity.amount -> 1
            this.amount < quantity.amount -> -1
            else -> 0
        }
    }

    operator fun compareTo(quantity: Int): Int {
        return when {
            this.amount > quantity -> 1
            this.amount < quantity -> -1
            else -> 0
        }
    }
}
