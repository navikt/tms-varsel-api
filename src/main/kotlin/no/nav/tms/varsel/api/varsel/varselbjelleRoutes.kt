package no.nav.tms.varsel.api.varsel

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import no.nav.tms.varsel.api.user

fun Route.varselbjelle(varselConsumer: VarselConsumer) {
    route("/varselbjelle") {
        get("/varsler") {
            varselConsumer.getVarselbjelleVarsler(
                userToken = call.user.accessToken,
                preferertSpraak = call.request.preferertSpraak
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

fun Route.bjellevarsler(varselConsumer: VarselConsumer) {
    get("/bjellevarsler") {
        varselConsumer.getVarselbjelleVarsler(
            userToken = call.user.accessToken,
            preferertSpraak = call.request.preferertSpraak
        ).let {
            call.respond(HttpStatusCode.OK, it)
        }
    }
}

private val ApplicationRequest.preferertSpraak get() = queryParameters["preferert_spraak"]?.lowercase()
