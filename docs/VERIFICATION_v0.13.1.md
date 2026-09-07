# Verifisering 0.13.1

- 203 JVM-testar bestod, utan feil, inkludert ein ny test som hentar endra avspelingsposisjon to gonger og verifiserer at andre brukarar sine økter blir filtrerte bort.
- Testen kontrollerer at den lette avspelingshentinga ikkje hentar bibliotek, førespurnader eller Oppdag.
- Livssyklusen avgrensar oppdateringar til RESUMED, på Heim eller medan avspelingsdetaljar er opne. Kvar henting blir ferdig før neste ventetid startar.
- Full synkronisering får prioritet. Svar frå ei endra tenestetilkopling blir forkasta.
- Ingen emulator var tilkopla under denne kontrollen. Visuell kontroll med ekte avspeling og Android-instrumentering er ikkje utført for denne endringa.
