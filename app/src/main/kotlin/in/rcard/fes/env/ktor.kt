package `in`.rcard.fes.env

import `in`.rcard.fes.portfolio.configure
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configure() {
    configureSerialization()
    configureRequestValidation()
    configureStatusPages()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        configure()
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
        }
    }
}
