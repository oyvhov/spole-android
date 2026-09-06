# Verifisering · Spole 0.12.0

Dato: 6. september 2026. Produksjonsutgåve, pakke `app.reelstack`, versionCode 30.

## Siste oppfølging · utgjevingar og personleg tilgang

- Endeleg lokal køyring: **168/168 einingstestar og 69/69 Android-testar bestod**, med lint og signert produksjonsbygg. Desse tala erstattar den tidlegare mellomkøyringa under.
- Oppdatert `Spole_Review` på port 5560 med `install -r`; ekte Seerr-, Jellyfin- og Emby-kontoar vart bevarte. «Nyleg tilgjengeleg» gjekk frå tom til ekte nye episodar, mellom anna Lioness og Forræder. Ingen ekte førespurnader eller avspelingskommandoar vart sende.
- Filmvilkåra er verifiserte med kontrollerte tenestesvar: eigne bibliotek-ID-ar + TMDB-ID + ny type-4 digitaldato. Kino, berre fysisk utgjeving, gamle filmar, manglande dato og virtuelle/manglande bibliotekpostar blir ikkje brukte som ny digital tilgjenge. Ein konkret digital film frå den ekte kontoen er ikkje individuelt etterprøvd i denne runden.
- Ordinære brukarar med Seerr + anten Jellyfin eller Emby er testa utan Radarr-/Sonarr-credentials. Ingen administratorkø, andres aktivitet eller barneserie-bibliotek blir henta i desse testane.
- «Kjem snart» er framleis berre Radarr/Sonarr. Etter personleg innlogging vart Radarr kontrollert som aktiv på den ekte emulatoren. Heim og kalenderen viser to komande filmar: Supergirl (8. september, fysisk utgjeving) og The End of Oak Street (15. september). Filmfilter, datoval og detaljopning vart prøvde utan å sende endringskommandoar. Sonarr står framleis som «Kople til Sonarr»; ekte Sonarr-data er difor ikkje verifiserte. Episodekalender og datovindauge er dekte av automatiske testar.
- Brukaren valde å **planleggje** trygg delt kalender. Sjå [planen](SHARED_CALENDAR_PLAN.md). Ingen ny tenarteneste, deling av nøklar eller distribusjon er gjennomført.
- Bibliotekoppslag er avgrensa til 60 postar per relevant bibliotek/type. Inntil 60 unike nye filmkandidatar får digitaldato kontrollert med høgst fire samtidige Seerr-kall og ti minutt kontobunden minnecache. Dette er ei Home-rad, ikkje eit fullstendig bibliotekarkiv. Nyaste episode per serie hindrar at ein sesong skjuler filmane.
- Endeleg release-APK SHA-256: `3271735407faf264e4786a278807002d4b36584b89d75639c272b38e956095f8`. Storleik: 7 140 068 byte. Produksjonssertifikatet er uendra. Dette erstattar hash frå det tidlegare lokale bygget; den nye APK-en inneheld oppdatert versjonskontrollinformasjon.

## Bygg og automatiske testar

- `testDebugUnitTest`: 168 testar, ingen feil. 13 av desse testar adressevalidering og normalisering. Endeleg byggkontroll gjenbrukte uendra, beståtte testresultat.
- `lintDebug`: bestått, 0 feil og 20 åtvaringar.
- `assembleRelease`: bestått, pakke `app.reelstack`, versionCode 30.
- APK-signatur kontrollert: SHA-256 `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` (same produksjonsnøkkel).
- Android API 36, isolert `Spole_Instrumentation`, port 5562: ny full køyring før publisering bestod **69/69 testar**, `OK (69 tests)`, på 118,051 sekund. Tidlegare testveljar- og kalendergrensefeil er retta og inngår i dette resultatet.
- Den endelege release-APK-en vart installert med `install -r` på review-emulatoren og starta utan avinstallering eller sletting av appdata. Kalenderen bevarte vald dag og filmfilter ved retur frå detaljar; detaljark og kalender hadde same ytre ramme.
- Dei andre testane omfatta stabile detaljark ved asynkrone oppdateringar og skriftstorleik 2.0, kalender, tilgjengefilter, eigne førespurnader og kontogrenser.

## Adressefiksen

- Blanda/store bokstavar i protokoll og domenenamn blir normaliserte før innlogging, inkludert felles Jellyfin/Seerr-innlogging.
- Protokoll blir lagt til når han manglar. Undermapper beheld store/små bokstavar.
- URL-felt slår av automatisk retting og stor forbokstav.
- Mellomrom, blank adresse, ugyldig protokoll og port blir avviste med konkrete nynorske feil. Eksisterande vern mot ukrypterte offentlege endepunkt og innloggingsdata i URL blir bevarte.
- Emulator testar overgangen frå adresse til innlogging med store bokstavar, og retting av ei ugyldig adresse utan å lukke vindauget.

## Avgrensingar og neste kontroll

- Synleg `Spole_Review`, port 5560, er opna for brukaren. Ikkje køyr instrumentering eller tøm appdata der.
- Personleg innlogging er gjennomført. Seerr, Jellyfin, Emby og Radarr vart viste som aktive i den ekte produksjonsappen. Sonarr er ikkje tilkopla. Ingen ekte førespurnader eller avspelingskommandoar er sende.
- Profilbilete som blinkar ved rulling er framleis eit eige oppfølgingspunkt. Utgjevingsradene er følgde opp ovanfor; dato lagt til er ikkje brukt som utgjevingsdato.
