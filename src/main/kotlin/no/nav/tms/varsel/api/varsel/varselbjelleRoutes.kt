package no.nav.tms.varsel.api.varsel

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUser
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

private val log= KotlinLogging.logger {  }
fun Route.varselbjelle(varselConsumer: VarselConsumer) {
    route("/varselbjelle") {
        get("/varsler") {
            log.debug { "mottok api-kall varselbjelle" }
            call.respond(varselConsumer.getVarselbjelleVarsler(idportenUser.tokenString))
        }
    }
}


private val PipelineContext<Unit, ApplicationCall>.idportenUser: IdportenUser
    get() = IdportenUserFactory.createIdportenUser(this.call)
