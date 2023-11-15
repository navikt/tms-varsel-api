package no.nav.tms.varsel.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.*
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.tms.token.support.idporten.sidecar.mock.LevelOfAssurance
import no.nav.tms.varsel.api.varsel.AktiveVarsler
import no.nav.tms.varsel.api.varsel.AntallVarsler
import no.nav.tms.varsel.api.varsel.InaktivtVarsel
import no.nav.tms.varsel.api.varsel.VarselType
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.time.ZonedDateTime

class VarselRoutesTest {

    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    @Test
    fun `Henter inaktiverte varsler`() = varselRoutesTest { client ->
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        setupVarselAuthority(inaktiveVarslerFromEventHandler = varsler)
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val response = client.get("/inaktive")
        response.status shouldBe HttpStatusCode.OK
        response.status shouldBe HttpStatusCode.OK

        val inaktiveVarsler: List<InaktivtVarsel> = response.body()
        inaktiveVarsler.size shouldBe 6
        inaktiveVarsler.map { it.type } shouldContainExactlyInAnyOrder listOf(
            VarselType.beskjed,
            VarselType.oppgave,
            VarselType.oppgave,
            VarselType.innboks,
            VarselType.innboks,
            VarselType.innboks
        )

        val beskjed = varsler.first { it.type == VarselType.beskjed }
        inaktiveVarsler.first { it.type == VarselType.beskjed }.apply {
            varselId shouldBe beskjed.varselId
            eventId shouldBe beskjed.varselId
            tidspunkt shouldBe beskjed.opprettet
            forstBehandlet shouldBe beskjed.opprettet
            type shouldBe VarselType.beskjed
            isMasked shouldBe false
            tekst shouldBe beskjed.innhold?.tekst
        }
    }

    @Test
    fun `Henter alle aktive varsler`() = varselRoutesTest { client ->
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        setupVarselAuthority(aktiveVarslerFromEventHandler = varsler)
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val response = client.get("/aktive")
        response.status shouldBe HttpStatusCode.OK

        val aktiveVarsler = Json.decodeFromString<AktiveVarsler>(response.bodyAsText())
        aktiveVarsler.beskjeder.size shouldBe 1
        aktiveVarsler.oppgaver.size shouldBe 2
        aktiveVarsler.innbokser.size shouldBe 3

        val beskjed = varsler.first { it.type == VarselType.beskjed }
        aktiveVarsler.beskjeder.first().apply {
            varselId shouldBe beskjed.varselId
            eventId shouldBe beskjed.varselId
            tidspunkt shouldBe beskjed.opprettet
            forstBehandlet shouldBe beskjed.opprettet
            isMasked shouldBe (beskjed.innhold == null)
            tekst shouldBe beskjed.innhold?.tekst
            link shouldBe beskjed.innhold?.link
            eksternVarslingSendt shouldBe beskjed.eksternVarslingSendt
            eksternVarslingKanaler shouldBe beskjed.eksternVarslingKanaler
        }
    }

    @Test
    fun `Henter aktive varsler for nivå 3`() = varselRoutesTest { client ->
        setupVarselAuthority(VarselTestData.varsel(type = VarselType.beskjed, isMasked = true),
            VarselTestData.varsel(type = VarselType.oppgave, isMasked = true)
        )
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(LevelOfAssurance.LEVEL_3)
        )

        val response = client.get("/aktive")
        response.status shouldBe HttpStatusCode.OK

        val aktiveVarsler = Json.decodeFromString<AktiveVarsler>(response.bodyAsText())
        aktiveVarsler.beskjeder.size shouldBe 1
        aktiveVarsler.oppgaver.size shouldBe 1
        aktiveVarsler.innbokser.size shouldBe 0

        (aktiveVarsler.beskjeder+ aktiveVarsler.oppgaver).forEach {
            it.isMasked shouldBe true
            it.tekst shouldBe null
            it.link shouldBe null
        }

    }

    @Test
    fun `Henter antall aktive varsler`() = varselRoutesTest { client ->

        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        setupVarselAuthority(aktiveVarslerFromEventHandler = varsler)
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val response = client.get("/antall/aktive")

        response.status shouldBe HttpStatusCode.OK

        val antallVarsler: AntallVarsler = response.body()
        antallVarsler.beskjeder shouldBe 1
        antallVarsler.oppgaver shouldBe 2
        antallVarsler.innbokser shouldBe 3
    }

    @Test
    fun `markerer varsler som lest`() = testApplication {
        val expeectedEventId = "hhuu33-91sdf-shdkfh"
        var postCount = 0
        externalServices {
            hosts(varselAuthorityTestUrl) {
                install(ContentNegotiation) { jackson() }
                routing {
                    post("beskjed/inaktiver") {
                        Json.parseToJsonElement(call.receiveText()).apply {
                            this.jsonObject["varselId"]?.jsonPrimitive?.content shouldBe expeectedEventId
                        }
                        call.request.headers["Authorization"] shouldBe "Bearer authorityToken"
                        postCount++
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )
        client.post("/beskjed/inaktiver") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"varselId": "$expeectedEventId"}""")
        }.status shouldBe HttpStatusCode.OK

        postCount shouldBe 1
    }

    @Test
    fun `er bakoverkompatibel med fronted ved henting av varsler`() = varselRoutesTest { client ->

        val beskjed = VarselTestData.varsel(type = VarselType.beskjed)
        val oppgave = VarselTestData.varsel(type = VarselType.oppgave, aktiv = false, inaktivert = ZonedDateTime.now())

        setupVarselAuthority(aktiveVarslerFromEventHandler = listOf(beskjed), inaktiveVarslerFromEventHandler = listOf(oppgave))
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val aktiveVarsler: AktiveVarsler = client.get("/aktive").body()
        aktiveVarsler.beskjeder.size shouldBe 1
        aktiveVarsler.oppgaver.size shouldBe 0
        aktiveVarsler.innbokser.size shouldBe 0

        aktiveVarsler.beskjeder.first().apply {
            eventId shouldBe beskjed.varselId
            forstBehandlet shouldBe beskjed.opprettet
            isMasked shouldBe (beskjed.innhold == null)
            tekst shouldBe beskjed.innhold?.tekst
            link shouldBe beskjed.innhold?.link
            eksternVarslingSendt shouldBe beskjed.eksternVarslingSendt
            eksternVarslingKanaler shouldBe beskjed.eksternVarslingKanaler
        }

        val inaktiveVarsler: List<InaktivtVarsel> = client.get("/inaktive").body()
        inaktiveVarsler.size shouldBe 1

        inaktiveVarsler.first().apply {
            type shouldBe VarselType.oppgave
            eventId shouldBe oppgave.varselId
            forstBehandlet shouldBe oppgave.opprettet
            isMasked shouldBe (oppgave.innhold == null)
            tekst shouldBe oppgave.innhold?.tekst
            eksternVarslingSendt shouldBe oppgave.eksternVarslingSendt
            eksternVarslingKanaler shouldBe oppgave.eksternVarslingKanaler
        }
    }


    @Test
    fun `er bakoverkompatibel med frontend ved inaktivering av beskjed`() = testApplication {
        val expeectedEventId = "hhuu33-91sdf-shdkfh"
        var postCount = 0
        externalServices {
            hosts(varselAuthorityTestUrl) {
                install(ContentNegotiation) { jackson() }
                routing {
                    post("beskjed/inaktiver") {
                        Json.parseToJsonElement(call.receiveText()).apply {
                            this.jsonObject["varselId"]?.jsonPrimitive?.content shouldBe expeectedEventId
                        }
                        call.request.headers["Authorization"] shouldBe "Bearer authorityToken"
                        postCount++
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )
        client.post("/beskjed/inaktiver") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"eventId": "$expeectedEventId"}""")
        }.status shouldBe HttpStatusCode.OK

        postCount shouldBe 1
    }

    private fun varselRoutesTest(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                jackson { jsonConfig() }
            }
        }.let { block(it) }
    }
}
