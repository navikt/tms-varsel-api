package no.nav.tms.varsel.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.*
import no.nav.tms.common.logging.TeamLogs
import no.nav.tms.common.metrics.installTmsMicrometerMetrics
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import no.nav.tms.token.support.user.token.verification.userToken
import no.nav.tms.varsel.api.varsel.*

fun Application.varselApi(
    corsAllowedOrigins: String,
    httpClient: HttpClient,
    varselConsumer: VarselConsumer,
    authInstaller: Application.() -> Unit = {
        authentication {
            userToken {
                levelOfAssurance = LevelOfAssurance.Substantial
            }
        }
    }
) {
    val log = KotlinLogging.logger { }
    val teamLog = TeamLogs.logger { }

    install(DefaultHeaders)

    authInstaller()

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.warn { "Kall til ${call.request.uri} feilet]." }
            teamLog.warn(cause) { "Kall til ${call.request.uri} feilet: ${cause.message}" }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(CORS) {
        allowCredentials = true
        allowHost(corsAllowedOrigins, schemes = listOf("https"))
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        jackson { jsonConfig() }
    }

    installTmsMicrometerMetrics {
        setupMetricsRoute = true
        installMicrometerPlugin = true
    }

    routing {

        metaRoutes()
        authenticate {
            varsel(varselConsumer)
            varselbjelle(varselConsumer)
            bjellevarsler(varselConsumer)
            alleVarsler(varselConsumer)
            antallAktiveVarsler(varselConsumer)
        }

    }

    configureShutdownHook(httpClient)
}

private fun Application.configureShutdownHook(httpClient: HttpClient) {
    monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }
}

val ApplicationCall.user get() = principal<UserPrincipal>()
    ?: throw IllegalStateException("Fant ikke UserPrincipal i context")


fun ObjectMapper.jsonConfig(): ObjectMapper {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    return this
}
