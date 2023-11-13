package `in`.rcard.fes

data class Portfolio(private val id: PortfolioId, private val userId: UserId, private val money: Money)

@JvmInline
value class PortfolioId(private val id: String)

@JvmInline
value class UserId(private val id: String)

@JvmInline
value class Money(private val amount: Double)

sealed interface PortfolioCommand {
    data class CreatePortfolio(val userId: UserId, val amount: Money) : PortfolioCommand
}
