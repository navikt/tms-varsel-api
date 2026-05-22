package no.nav.tms.varsel.api.varsel

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import no.nav.tms.varsel.api.user

fun Route.varsel(
    varselConsumer: VarselConsumer
) {
    get("inaktive") {
        varselConsumer.getInaktiveVarsler(
            userToken = call.user.accessToken,
            preferertSpraak = call.request.preferertSpraak
        ).let { inaktiveVarsler ->
            call.respond(HttpStatusCode.OK, inaktiveVarsler)
        }
    }

    get("aktive") {
        varselConsumer.getAktiveVarsler(
            userToken = call.user.accessToken,
            preferertSpraak = call.request.preferertSpraak
        ).let { aktiveVarsler ->
            call.respond(HttpStatusCode.OK, aktiveVarsler)
        }
    }

    get("antall/aktive") {
        varselConsumer.getAktiveVarsler(
            userToken = call.user.accessToken,
            preferertSpraak = null
        ).let {
            AntallVarsler(
                beskjeder = it.beskjeder.size,
                oppgaver = it.oppgaver.size,
                innbokser = it.innbokser.size
            )
        }.let { antallAktive ->
            call.respond(HttpStatusCode.OK, antallAktive)
        }
    }


    post("beskjed/inaktiver") {
        varselConsumer.postInaktiver(varselId = call.varselId(), userToken = call.user.accessToken)
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.alleVarsler(
    varselConsumer: VarselConsumer
) {
    get("/alle"){
        varselConsumer.getAlleVarsler(
            userToken = call.user.accessToken,
            preferertSpraak = call.request.preferertSpraak
        ).let { alleVarsler ->
            call.respond(HttpStatusCode.OK, alleVarsler)
        }
    }
}

fun Route.antallAktiveVarsler(
    varselConsumer: VarselConsumer
) {
    get("/ssr/antall/aktive"){
        varselConsumer.getAktiveVarsler(
            userToken = call.user.accessToken,
            preferertSpraak = null
        ).let {
            AntallVarsler(
                beskjeder = it.beskjeder.size,
                oppgaver = it.oppgaver.size,
                innbokser = it.innbokser.size
            )
        }.let { antallAktive ->
            call.respond(HttpStatusCode.OK, antallAktive)
        }
    }
}

@Serializable
data class AntallVarsler(val beskjeder: Int, val oppgaver: Int, val innbokser: Int)

@Serializable
data class InaktiverVarselBody(
    val eventId: String? = null,
    val varselId: String? = null
)

private suspend fun ApplicationCall.varselId(): String = receive<InaktiverVarselBody>().let {
    it.varselId ?: it.eventId ?: throw IllegalArgumentException("Mangler varselId eller eventId i body")
}

private val ApplicationRequest.preferertSpraak get() = queryParameters["preferert_spraak"]?.lowercase()
