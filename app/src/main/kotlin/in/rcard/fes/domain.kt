package `in`.rcard.fes

// Easiest implementation of the domain
typealias Portfolio = List<PortfolioEvent>

@JvmInline
value class PortfolioId(private val id: String)

@JvmInline
value class UserId(private val id: String)

@JvmInline
value class Money(private val amount: Double)

val Portfolio.initial: Boolean
    get() = this.isEmpty()

val Portfolio.id: PortfolioId
    get() = this.first().portfolioId

val notCreatedPortfolio: List<PortfolioEvent> = emptyList()

sealed interface PortfolioCommand {
    data class CreatePortfolio(val userId: UserId, val amount: Money) : PortfolioCommand
}

sealed interface PortfolioEvent {
    val portfolioId: PortfolioId

    data class PortfolioCreated(override val portfolioId: PortfolioId, val userId: UserId, val money: Money) :
        PortfolioEvent
}

sealed interface PortfolioError {
    val portfolioId: PortfolioId

    data class PortfolioAlreadyExists(override val portfolioId: PortfolioId) : PortfolioError
}
