/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package `in`.rcard.fes

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import `in`.rcard.fes.env.Dependencies
import `in`.rcard.fes.env.Env
import `in`.rcard.fes.env.configure
import `in`.rcard.fes.env.dependencies
import `in`.rcard.fes.portfolio.configureRouting
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import kotlinx.coroutines.awaitCancellation

suspend fun main() = SuspendApp {
    val env = Env()
    resourceScope {
        val deps = dependencies(env)
        server(Netty, port = 8080, host = "0.0.0.0") { module(deps) }
        awaitCancellation()
    }
}

fun Application.module(deps: Dependencies) {
    configure()
    configureRouting(deps)
}
