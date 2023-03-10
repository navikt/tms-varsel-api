package no.nav.tms.varsel.api.varsel

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import no.nav.tms.varsel.api.userToken

fun Route.varsel(
    varselConsumer: VarselConsumer
) {
    get("inaktive") {
        val inaktiveVarsler = varselConsumer.getInaktiveVarsler(userToken)

        call.respond(HttpStatusCode.OK, inaktiveVarsler)
    }

    get("aktive") {
        val aktiveVarsler = varselConsumer.getAktiveVarsler(userToken)

        call.respond(HttpStatusCode.OK, aktiveVarsler)
    }

    get("antall/aktive") {
        val antallAktive = varselConsumer.getAktiveVarsler(userToken).let {
            AntallVarsler(
                beskjeder = it.beskjeder.size,
                oppgaver = it.oppgaver.size,
                innbokser = it.innbokser.size
            )
        }

        call.respond(HttpStatusCode.OK, antallAktive)
    }
}

@Serializable
data class AntallVarsler(val beskjeder: Int, val oppgaver: Int, val innbokser: Int)