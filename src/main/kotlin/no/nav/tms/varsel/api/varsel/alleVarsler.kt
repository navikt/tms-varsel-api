package no.nav.tms.varsel.api.varsel

import java.time.ZonedDateTime

data class Varsel(
    @Deprecated("Use varselId") val eventId: String,
    @Deprecated("Use tidspunkt") val forstBehandlet: ZonedDateTime,
    val isMasked: Boolean,
    val spraakkode: String?,
    val tekst: String?,
    val link: String?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>,
    val type: VarselType,
    val isInaktiverbar: Boolean,

    ) {
    companion object {
        fun fromVarsel(varsel: VarselAuthority.Varsel): Varsel {
            val isMasked = varsel.innhold == null
            val isInaktivertOppgave = varsel.type == VarselType.oppgave && !varsel.aktiv

            return Varsel(
                eventId = varsel.varselId,
                forstBehandlet = varsel.opprettet,
                isMasked = isMasked,
                spraakkode = varsel.innhold?.spraakkode,
                tekst = varsel.innhold?.tekst,
                link =  varsel.innhold?.link,
                eksternVarslingSendt = varsel.eksternVarslingSendt,
                eksternVarslingKanaler = varsel.eksternVarslingKanaler,
                type = if (varsel.type == VarselType.oppgave) VarselType.oppgave else VarselType.beskjed,
                isInaktiverbar = varsel.type == VarselType.beskjed && varsel.aktiv && !isMasked,
            )
        }
    }
}

data class AlleVarsler(
    val hasMaskedVarsel: Boolean,
    val aktive: AktivtVarselV2,
    val inaktive: List<Varsel>,
) {
    companion object {
        fun fromVarsler(varsler: List<VarselAuthority.Varsel>): AlleVarsler {
            val aktivBeskjeder = mutableListOf<Varsel>()
            val aktivOppgaver = mutableListOf<Varsel>()
            val inaktivtVarseler = mutableListOf<Varsel>()
            var hasMaskedVarsel = false

            varsler.map {
                if(it.innhold == null) {
                    hasMaskedVarsel = true
                }

                if (it.aktiv) {
                    if (it.type == VarselType.oppgave) {
                        aktivOppgaver.add(Varsel.fromVarsel(it))
                    } else {
                        aktivBeskjeder.add(Varsel.fromVarsel(it))
                    }
                } else {
                    inaktivtVarseler.add(Varsel.fromVarsel(it))
                }
            }
            return AlleVarsler(
                hasMaskedVarsel = hasMaskedVarsel,
                aktive = AktivtVarselV2(aktivBeskjeder, aktivOppgaver),
                inaktive = inaktivtVarseler
            )
        }
    }
}

data class AktivtVarselV2(
    val beskjeder: List<Varsel>,
    val oppgaver: List<Varsel>,
)