# Verifisering · 0.12.0 (lokalt bygg)

Dato: 6. september 2026. Ikkje publisert enno.

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
- Profilbilete som blinkar ved rulling, og tomme «Nyleg tilgjengeleg»/«Kjem snart»-rader, er framleis oppfølgingspunkt. Dei er ikkje stadfesta retta av denne adressefiksen. Kontroller kjelder og tenestesvar med dei ekte kontoane; ikkje erstatt utgjevingsdato med dato lagt til.
