# Verifisering · Spole 0.14.1

## Leveranse

Integrert søkelinje på Heim, delt felt-overgang til Oppdag og fokus etter animasjonen.
Den første, meir framheva utforminga vart tona ned etter brukaren si tilbakemelding:
ingen limeknapp, kortramme eller hjelpetekst. Ingen førespurnads- eller innloggingslogikk er endra.

## Kontrollar

- Einingstestar: 225/225 bestod på det endelege bygget.
- Heile Android-testpakken: 87/87 bestod på emulator-5562 (239,171 sekund) før den siste
  visuelle nedtoninga. Dette inkluderer fem nye søketestar.
- Etter nedtoninga og versjonsendringa: alle fem søketestane bestod på nytt (35,586 sekund).
- Bygg: assembleRelease, assembleDebug, assembleDebugAndroidTest, testDebugUnitTest og lintDebug bestod.
- Lint: 0 feil, 22 åtvaringar. Fire nye stilåtvaringar gjeld namnet/plasseringa på den sekundære
  Modifier-parameteren som knyter søkefelta til overgangen; dei andre 18 var eksisterande.
- Stor tekst: søkefeltet er testa ved 300 dp breidd og fontScale 2.0.

## Visuell Android-kontroll

Produksjonsbygget vart installert med -r på den innlogga Spole_Review (5560). Profil og ekte
bibliotek vart bevarte. Heim → søk → Oppdag → Silo → Heim vart kontrollert utan å sende noko.
Tastaturet vart kontrollert med vising av skjermtastatur ved fysisk tastatur slått på; emulatorvalet
vart sett tilbake til den opphavlege verdien 0 etter testen.

Fem kontrollpunkt mot den eksisterande Spole-designen og den reviderte bestillinga:

1. Plassering: søkelina ligg tett under merke/profil, før avspeling og bibliotek.
2. Ordlyd: éi søkeoppmoding; ekstra forklaring og pilknapp er fjerna.
3. Typografi: nedtona 14 sp tekst, utan den første skissa si halvfeite overskrift.
4. Flater: sidebakgrunn og ein tynn skiljestrek, inga kortflate eller ny dekorativ farge.
5. Rørsle: delt felt-overgang kontrollert i eit skjermopptak; fokus og tastatur kjem etter
   overgangen. Feltet er også testa fleire gonger og via vanleg fanenavigering.

Skjermbilete/opptak med ekte innhald ligg berre lokalt under app/build. Dei blir ikkje publiserte.
Ingen ny bildegenerert konseptflate var nødvendig for denne avgrensa endringa. Ingen nettlesartest
kan erstatte testen av det innfødde Android-grensesnittet. Ingen kjende visuelle avvik frå den
reviderte søkelinja står att i dei kontrollerte visingane. Dette er ikkje ei måling av rammetid på
fysiske telefonar eller ein full test av nettverk/tilgangsrettar.

## APK

- Versjon 0.14.1, versionCode 40, pakke app.reelstack.
- 2 251 724 byte.
- SHA-256: `9cc9286d5d52ec4ce65461d5c605cf23f226d95596e6590d7d8a9f5839994f4a`.
- Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mappinga og SHA256SUMS.txt følgjer releasen.

## Separat undersøking

Brukaren spurde også om delvis tilgjengelege og pågåande seriar. Forslaget står i
SERIES_FOLLOW_PROPOSAL.md. Det er ikkje implementert i denne APK-en. Konkret serie/sesong og
Sonarr-overvaking på den installerte Seerr-versjonen må framleis stadfestast.
