# Verifisering · 0.12.0 (lokalt bygg)

Dato: 6. september 2026. Ikkje publisert enno.

## Siste oppfølging · utgjevingar og personleg tilgang

- Endeleg lokal køyring: **168/168 einingstestar og 69/69 Android-testar bestod**, med lint og signert produksjonsbygg. Desse tala erstattar den tidlegare mellomkøyringa under.
- Oppdatert `Spole_Review` på port 5560 med `install -r`; ekte Seerr-, Jellyfin- og Emby-kontoar vart bevarte. «Nyleg tilgjengeleg» gjekk frå tom til ekte nye episodar, mellom anna Lioness og Forræder. Ingen ekte førespurnader eller avspelingskommandoar vart sende.
- Filmvilkåra er verifiserte med kontrollerte tenestesvar: eigne bibliotek-ID-ar + TMDB-ID + ny type-4 digitaldato. Kino, berre fysisk utgjeving, gamle filmar, manglande dato og virtuelle/manglande bibliotekpostar blir ikkje brukte som ny digital tilgjenge. Ein konkret digital film frå den ekte kontoen er ikkje individuelt etterprøvd i denne runden.
- Ordinære brukarar med Seerr + anten Jellyfin eller Emby er testa utan Radarr-/Sonarr-credentials. Ingen administratorkø, andres aktivitet eller barneserie-bibliotek blir henta i desse testane.
- «Kjem snart» er framleis berre Radarr/Sonarr. Den ekte emulatoren har inga kalenderkjelde tilkopla; appen seier no dette i staden for å melde ein vellukka tom kalender. Kalenderens datovindauge og vising er testa med fixtures, ikkje mot ein ekte Radarr-/Sonarr-kalender i denne runden.
- Brukaren valde å **planleggje** trygg delt kalender. Sjå [planen](SHARED_CALENDAR_PLAN.md). Ingen ny tenarteneste, deling av nøklar eller distribusjon er gjennomført.
- Bibliotekoppslag er avgrensa til 60 postar per relevant bibliotek/type. Inntil 60 unike nye filmkandidatar får digitaldato kontrollert med høgst fire samtidige Seerr-kall og ti minutt kontobunden minnecache. Dette er ei Home-rad, ikkje eit fullstendig bibliotekarkiv. Nyaste episode per serie hindrar at ein sesong skjuler filmane.
- Endeleg lokal APK SHA-256: `4566a11b41d5c185efe815b7b8e5ea41d61e4713366f0348e4689b8a51fb3f37`. Produksjonssertifikatet er uendra.

## Bygg og automatiske testar

- `testDebugUnitTest`: 158 testar, ingen feil. 13 av desse testar adressevalidering og normalisering.
- `lintDebug`: bestått.
- `assembleRelease`: bestått, pakke `app.reelstack`, versionCode 30.
- APK-signatur kontrollert: SHA-256 `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` (same produksjonsnøkkel).
- Android API 36, isolert `Spole_Instrumentation`, port 5562: samla køyring 67 testar, 66 bestod. Éin ny test fann både «Brukarnamn»-metodeknappen og tekstfeltet; testveljaren vart presisert til det redigerbare feltet. Heile `LoginExperienceTest` vart deretter køyrd på nytt: 7/7 bestod.
- Dei andre testane omfatta stabile detaljark ved asynkrone oppdateringar og skriftstorleik 2.0, kalender, tilgjengefilter, eigne førespurnader og kontogrenser.

## Adressefiksen

- Blanda/store bokstavar i protokoll og domenenamn blir normaliserte før innlogging, inkludert felles Jellyfin/Seerr-innlogging.
- Protokoll blir lagt til når han manglar. Undermapper beheld store/små bokstavar.
- URL-felt slår av automatisk retting og stor forbokstav.
- Mellomrom, blank adresse, ugyldig protokoll og port blir avviste med konkrete nynorske feil. Eksisterande vern mot ukrypterte offentlege endepunkt og innloggingsdata i URL blir bevarte.
- Emulator testar overgangen frå adresse til innlogging med store bokstavar, og retting av ei ugyldig adresse utan å lukke vindauget.

## Avgrensingar og neste kontroll

- Synleg `Spole_Review`, port 5560, er opna for brukaren. Ikkje køyr instrumentering eller tøm appdata der.
- Brukaren er beden om å seie frå når innlogging er ferdig før APK-en blir oppdatert der. Ingen ekte førespurnader eller avspelingskommandoar er sende.
- Profilbilete som blinkar ved rulling er framleis eit eige oppfølgingspunkt. Utgjevingsradene er følgde opp ovanfor; dato lagt til er ikkje brukt som utgjevingsdato.
