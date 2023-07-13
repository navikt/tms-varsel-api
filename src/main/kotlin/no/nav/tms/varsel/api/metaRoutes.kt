package no.nav.tms.varsel.api

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

fun Route.meta() {

    get("/internal/isAlive") {
        call.respondText(text = "ALIVE", contentType = ContentType.Text.Plain)
    }

    get("/internal/isReady") {
        call.respondText(text = "READY", contentType = ContentType.Text.Plain)
    }

    get("/metrics") {
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004), HttpStatusCode.OK) {
            TextFormat.write004(this, CollectorRegistry.defaultRegistry.metricFamilySamples())
        }
    }
}

