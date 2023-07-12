package no.nav.tms.varsel.api.varsel

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
import mu.KotlinLogging


fun Route.varsel(
    varselConsumer: VarselConsumer,
    tokenResolver: PipelineContext<Unit, ApplicationCall>.() -> String
) {
    val log = KotlinLogging.logger {  }

    get("inaktive") {
        log.debug { "mottok api-kall inaktive" }
        val inaktiveVarsler = varselConsumer.getInaktiveVarsler(tokenResolver())

        call.respond(HttpStatusCode.OK, inaktiveVarsler)
    }

    get("aktive") {
        log.debug { "mottok api-kall aktive" }
        val aktiveVarsler = varselConsumer.getAktiveVarsler(tokenResolver())

        call.respond(HttpStatusCode.OK, aktiveVarsler)
    }

    get("antall/aktive") {
        log.debug { "mottok api-kall antall" }
        val antallAktive = varselConsumer.getAktiveVarsler(tokenResolver()).let {
            AntallVarsler(
                beskjeder = it.beskjeder.size,
                oppgaver = it.oppgaver.size,
                innbokser = it.innbokser.size
            )
        }

        call.respond(HttpStatusCode.OK, antallAktive)
    }

    post("beskjed/inaktiver") {
        log.debug { "mottok api-kall inaktiver" }
        varselConsumer.postInaktiver(varselId = call.varselId(), userToken = tokenResolver())
        call.respond(HttpStatusCode.OK)
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
