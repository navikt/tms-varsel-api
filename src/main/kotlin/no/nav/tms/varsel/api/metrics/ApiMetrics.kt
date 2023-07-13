package no.nav.tms.varsel.api.metrics

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.prometheus.client.Counter
import mu.KotlinLogging


val ApiResponseMetrics = createApplicationPlugin(name = "ApiResponseMetrics") {
    on(ResponseSent) { call ->
        val status = call.response.status()
        val log = KotlinLogging.logger {  }
        log.debug { "Legger til metriks med status $status" }
        val route = call.request.uri
        //legge til sensitivitet?
        val tag = status.resolveTag()
        ApiMetricsCounter.countApiCall(status, route, tag)
    }
}

object ApiMetricsCounter {
    private val log = KotlinLogging.logger {  }
    const val COUNTER_NAME = "tms_api_call"
    private val counter = Counter.build()
        .name(COUNTER_NAME)
        .help("Kall til team minside sine api-er")
        .labelNames("route", "status", "statusgroup")
        .register()
    fun countApiCall(statusCode: HttpStatusCode?, route: String, tag: String) {
        log.debug { "Adding apimetrics count" }
        counter.labels("${statusCode?.value ?: "NAN"}", route, tag).inc()
    }
}

private fun HttpStatusCode?.resolveTag() =
    when {
        this == null -> "unresolved"
        value isInStatusRange 200 -> "OK"
        value isInStatusRange 300 -> "Redirection"
        value isInStatusRange (400 excluding 401 and 403) -> "client_error"
        value == 401 || value == 403 -> "auth_issues"
        value isInStatusRange 500 -> "server_error"
        else -> "unresolved"
    }

private infix fun Pair<Int, List<Int>>.and(i: Int) = this.copy(second = listOf(i) + this.second)
private infix fun Int.isInStatusRange(i: Int): Boolean = this >= i && this < (i + 100)
private infix fun Int.isInStatusRange(p: Pair<Int, List<Int>>): Boolean =
    p.second.any { it == this } || this isInStatusRange p.first

private infix fun Int.excluding(i: Int) = Pair(this, listOf(i))


