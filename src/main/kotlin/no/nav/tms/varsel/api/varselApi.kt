package no.nav.tms.varsel.api

import io.ktor.client.HttpClient
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import no.nav.tms.token.support.tokenx.validation.installTokenXAuth
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import no.nav.tms.varsel.api.varsel.VarselConsumer
import no.nav.tms.varsel.api.varsel.varsel

fun Application.varselApi(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    httpClient: HttpClient,
    varselConsumer: VarselConsumer,
    authInstaller: Application.() -> Unit = {
        installTokenXAuth {
            setAsDefault = true

        }
    }
) {
    val collectorRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val securelog = KotlinLogging.logger("secureLog")

    install(DefaultHeaders)

    authInstaller()

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            securelog.warn { "Kall til ${call.request.uri} feilet: ${cause.message}" }
            securelog.warn { cause.stackTrace }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(CORS) {
        allowCredentials = true
        allowHost(corsAllowedOrigins, schemes = listOf(corsAllowedSchemes))
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json(jsonConfig())
    }

    install(MicrometerMetrics) {
        registry = collectorRegistry
    }

    routing {
        meta(collectorRegistry)

        authenticate {
            varsel(varselConsumer)
        }
    }

    configureShutdownHook(httpClient)
}

private fun Application.configureShutdownHook(httpClient: HttpClient) {
    environment.monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }
}

val PipelineContext<Unit, ApplicationCall>.userToken: String
    get() = TokenXUserFactory.createTokenXUser(call).tokenString

fun jsonConfig(): Json {
    return Json {
        this.ignoreUnknownKeys = true
        this.encodeDefaults = true
    }
}