# Verifisering – Oppdag og Aktivitet 0.12.3

## Endring

- Heim slepp no tom andrelinje under korte serienamn, sjølv når eit nabokort har lang tittel.
- Oppdag og Aktivitet brukar same personlege Seerr-portrett som Heim, med stabil 48 dp kontohandling.
- Oppdag har éi filterlinje. Bibliotekstatus ligg i ein meny i staden for ei ny rad.
- Aktivitet brukar eitt kjeldeval for administrator og kompakte ferdigkort. Berre aktive førespurnader
  viser trestegsframdrift og varselval.
- Profilbilete har eit avgrensa minnelager og blir ikkje lasta på nytt berre fordi toppen har vore
  utanfor den late lista.

## Automatiske kontrollar

- **168/168 JVM-testar** bestod.
- **81/81 Android-testar** bestod på isolert emulator 5562 (`OK (81 tests)`, 196,38 sekund).
- Nye testar måler den faktiske avstanden mellom serienamn og episode, kontrollerer den personlege
  profilen i Oppdag og skil kompakte ferdigkort frå aktiv framdrift.
- Lint: **0 feil, 20 åtvaringar**.
- Produksjonsbygget og release-signeringa bestod.

## Ekte kontoar

Produksjons-APK-en vart installert med oppgradering på emulator 5560 utan sletting av data. Seerr-,
Jellyfin-, Emby- og Radarr-tilkoplingane var bevarte. Med ekte data vart dette kontrollert:

- korte og lange episodetitlar legg episodeteksten rett under sin eigen tittel;
- Oppdag viser éi filterlinje og det ekte Seerr-profilbiletet øvst til høgre;
- profilbiletet står att etter rulling ned og opp;
- Aktivitet viser ekte førespurnader som kompakte ferdigkort og aktive framdriftskort;
- administratoren har framleis tilgang til alle kjelder gjennom `Vising`-menyen.

Kontrollen var berre lesande. Ingen førespurnad, avspelingskommando, innlogging eller utlogging vart sendt.
Lokale skjermbilete med ekte kontodata er ikkje lagde i Git eller utgåva.

## Produksjonsfil

- Versjon 0.12.3, versionCode 33, pakke `app.reelstack`, minSdk 26, targetSdk 36.
- Storleik: 7 085 484 byte.
- SHA-256: `0f72cdc48a3814c7e02e01c787b9840f80ffab48a24ea3b9f4b9e5669e317cd0`.
- Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Same produksjonsnøkkel som 0.12.2; oppgradering vart stadfesta utan avinstallering.
