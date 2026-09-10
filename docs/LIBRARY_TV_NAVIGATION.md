# Bibliotek og TV-navigasjon

10. september 2026. Lokal endring, ikkje publisert.

- TV-sidemenyen startar minimert og utvidar seg når fjernkontrollfokus kjem inn i menyen. Høgre til innhaldet minimerer igjen. Ingen eigen minimerknapp på TV. Nettbrett bevarer den manuelle knappen og det lagra valet.
- Aktivitetsikonet er eit enkelt listeikon i den eksisterande strekfamilien.
- Eiga Bibliotek-fane viser dei valde Jellyfin-biblioteka. «Vel bibliotek» hentar alle biblioteka profilen får tilgang til, også dei som tidlegare var utelatne frå heimesida. Ingen lokal grense på talet på bibliotek.
- Mappevis navigering inkluderer samlingar, seriar, sesongar og andre mapper. Innhald blir henta med profil-ID og ParentId i sider på 60, med stabil server-sortering. Neste side blir lagt til med ID-deduplisering. Feil bevarer allereie lasta innhald og kan prøvast igjen. Kontobytte avbryt og tømmer bibliotekdata.
- Titlar opnar eksisterande detaljvising og Jellyfin-avspeling med originale element-ID-ar. Bibliotekval styrer også nye filmar, nye seriar, Hald fram å sjå, bibliotekbaserte nye utgjevingar og biblioteksøket. Filteret blir brukt før førespurnader om bibliotekinnhald, både i appen og bakgrunnsoppdateringa.

## Verifisering

- 287/287 einingstestar, inkludert fire nye nettverks-/parsertestar for bibliotek, profilavgrensing, paginering og avvist tilgang.
- 8/8 målretta emulator-testar: TV venstre/høgre utan knapp, nettbrettminimering, fokus/navigasjon, bibliotekval, neste side og retry utan tap av synlege element.
- Debug-APK og test-APK bygde. Lint: 0 feil, 36 åtvaringar.
- Loggar: app/build/library-pass-verified.log og app/build/library-ui-verified.log.
- Fysisk TV og live Jellyfin-bibliotek er ikkje verifiserte i denne runda. Biblioteklesaren tek med alle returnerte medietypar; dette dokumenterer ikkje full avspelingsstøtte for musikk, foto, bøker eller Live TV. Ingen ny slik avspelar er implementert.

API-grunnlag: https://typescript-sdk.jellyfin.org/interfaces/generated-client.LibraryApiGetItemsRequest.html

## Bibliotekval

«Vel bibliotek» på biblioteksida har avkryssing, Alle, Ingen, Lagre og Avbryt. Ingen er eit eksplisitt tomt val og gir inga innlasting av bibliotekinnhald. Tilgjengelege biblioteksnamn blir framleis henta slik at brukaren kan slå dei på igjen. Utan eit lagra val blir det tidlegare standardfilteret brukt; eit eksplisitt val overstyrer det, også for barnebibliotek.

Val blir lagra per tenaridentitet og Jellyfin-profil. Lagre avbryt aktive bibliotek-/søk-/innhaldsoppdateringar, fjernar gamle bibliotekrader frå skjermen og hentar nye data. Mellomlageret er knytt til bibliotekvalet, så ei tidlegare eller samtidig bakgrunnsoppdatering ikkje kan gjeninnføre innhald frå eit eldre val ved omstart. Offentlege tilrådingar, Seerr-oppdaging og aktive avspelingsøkter har eigne datakjelder og blir ikkje filtrerte som bibliotekrader.

Verifisering av bibliotekval: 289/289 einingstestar. Nye testar kontrollerer at berre valde bibliotek blir spurde etter nye filmar/seriar, resume og søk; Ingen sender ingen slike innhaldsførespurnader. Emulator-testar kontrollerer lagring av tomt val, profil-/tenarisolering og endra mellomlageridentitet. Loggar: `app/build/library-selection-final.log` og `app/build/library-selection-ui.log`.
