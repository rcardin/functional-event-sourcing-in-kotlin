package `in`.rcard.fes.projection

import `in`.rcard.fes.portfolio.PortfolioEvent

interface CreatePortfolioUseCase {
    // TODO We need to transform this in an suspend function
    // suspend
    fun createPortfolio(event: PortfolioEvent.PortfolioCreated)
}
