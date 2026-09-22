# Spole 0.17.0-beta17

Dette er ei testutgåve. Ho er måleutstyr, ikkje ei åtferdsendring.

## Viktige endringar

- Stats for Nerds har fått ei «Fall ned»-linje: Media3 sin feilkode, formatet renderaren gav opp på, og kva trinn avspelinga hamna på. Til dømes `ERROR_CODE_AUDIO_TRACK_INIT_FAILED · audio/eac3 → AUDIO_ONLY`.
- Dette fanst berre i logcat før, altså berre for den som har eininga i ein USB-kabel. Ein TV har ingen logcat, og spørsmålet panelet finst for å svare på — «kvifor omkodar denne?» — kunne dermed ikkje svarast der det oftast blir stilt.
- Linja står tom for ein straum som starta og vart verande der han starta, og blir nullstilt når du vel ein ny tittel.
- Ingen endring i forhandling, profil eller avspeling.

## Distribusjon

- Pakke: `app.reelstack`
- Testutgåve: `0.17.0-beta17`
- APK-en er signert med den eksisterande Spole-produksjonsnøkkelen.
- Releasen er ei prerelease og kan installerast over tidlegare Spole-betaer.

Sjå verifikasjonsrapporten for bygg, testar, signatur og SHA-256.
