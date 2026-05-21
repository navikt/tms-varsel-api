package no.nav.tms.varsel.api

import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.token.support.user.token.exchange.UserTokenExchangerBuilder
import no.nav.tms.varsel.api.varsel.VarselConsumer

fun main() {
    val environment = Environment()

    val httpClient = HttpClientBuilder.build()

    embeddedServer(
        factory = Netty,
        configure = {
            connector {
                port = 8080
            }
        },
        module = {
            varselApi(
                corsAllowedOrigins = environment.corsAllowedOrigins,
                httpClient = httpClient,
                varselConsumer = VarselConsumer(
                    client = httpClient,
                    varselAuthorityUrl = "http://tms-varsel-authority",
                    varselAuthorityClientId = environment.eventhandlerClientId,
                    tokenExchanger = UserTokenExchangerBuilder.build(),
                )
            )
        }
    ).start(wait = true)
}
