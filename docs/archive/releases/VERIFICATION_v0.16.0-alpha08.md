# Verifisering — Spole 0.16.0-alpha08

10. september 2026 · bygg 51 · `app.reelstack` · lokal test-APK, ingen GitHub-publisering.

## Bygg og einingstestar

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease` og `lintDebug`: BUILD SUCCESSFUL.
- 305/305 JVM-testar, 0 feil og 0 hoppa over.
- Lint: 0 feil, 36 åtvaringar. Ingen påstand om at alle eldre åtvaringar er retta.
- Bygglogg: `app/build/roadmap-alpha08-build.log`.

## Android og visuell kontroll

Instrumentering gjekk berre på isolert WSL-AVD 5562, Android API 36. Ekte kontoar på review-einingane 5560/5564 vart ikkje brukte av testane.

- Full pakke rapporterte 219 testar og fire feil. Fem av dei 219 er TV-spesifikke testar som blir avslutta med føresetnadssjekk på denne telefonprofilen; dei tel ikkje som verifisert TV-støtte.
- Utloggingstesten kravde rulling også når heile teksten fekk plass i telefonens dialogvindauge. Testen kontrollerer no rulling ved faktisk overflyt og synleg tekst/Avbryt elles. Ingen endring i utloggingskoden.
- Tre spelartestar fekk tidsavbrot fordi Android-vindauget `ImmersiveModeConfirmation` tok fokus framfor spelaren. Fullskjermrettleiinga vart markert gjennomført på test-AVD-en (`settings put secure immersive_mode_confirmations confirmed`). Ingen endring i spelarkoden.
- Etter desse avklaringane bestod rein omkøyring av heile `JellyfinPlayerTest` og `AccountOptionsTest`: `OK (20 tests)`; 19 utførte og éin TV-føresetnad som ikkje gjaldt. Ingen samtidige UI-avlesingar i denne køyringa.
- Samla dekning etter omkøyring: **214 utførte Android-testar bestod; fem TV-spesifikke testar er ikkje verifiserte på telefonprofilen.** Dette er full pakke pluss omkøyring av råka klassar, ikkje ein ny samanhengande grønn fullkøyring.
- Dei 15 nye Android-testane for historikk og kvotar bestod i fullpakken, inkludert fire ViewModel-/HTTP-integrasjonstestar. Tidlegare bibliotek-/lagrings-/språk-/breiddetestar vart òg køyrde.
- Nynorsk telefon og breitt vindauge med skrift 2.0, og engelsk kvotepanel med skrift 2.0, er kontrollerte visuelt med mørk appflate og syntetiske data. Last eldre, Tilbake og Send er testa som tilgjengelege gjennom rulling.

Loggar: `app/build/roadmap-alpha08-android-cold.log` og `app/build/roadmap-alpha08-regression-final.log`. Skjermbilete: `app/build/roadmap-review/history-nn-phone.png`, `history-nn-landscape-2x.png`, `quota-nn-phone.png` og `quota-en-landscape-2x.png`.

Første fullforsøk vart avbrote av SIGSEGV i WSL-emulatorens grafikkprosess; same AVD vart kaldstarta utan snapshot og utan nullstilling av data. Eit mellomforsøk under spelarfeilsøking vart ugyldig då ei samtidig `uiautomator`-avlesing kolliderte med instrumenteringa. Det forsøket blir ikkje brukt som testbevis. UI-avlesing skal skje etter avslutta instrumentering.

## Oppdatering

Produksjonspakken alpha07 vart installert på isolert 5562. Språkvalet vart endra i appen til «English · Preview». Alpha08 vart deretter installert med `install -r`, starta og opna i Innstillingar. «English · Preview» og engelsk grensesnitt var bevarte; installert versjonskode var 51. Skjermbilete: `app/build/roadmap-review/upgrade-alpha08.png`.

Dette stadfestar signeringskompatibel oppdatering og bevart språkval. Det var ingen ekte kontoar i denne produksjonsinstallasjonen på test-AVD-en; testen dokumenterer ikkje ny kontroll av bevarte ekte innloggingar.

## APK

- `app/build/test-alpha08/Spole-0.16.0-alpha08.apk`: 3 765 409 byte.
- SHA-256: `8624CAF873F6EDCD25672DF6F59E98B64AC90D6D4B15BCEDB9B9650A2624ACF5`.
- Pakke/versjon stadfesta med APK-verktøy: `app.reelstack`, versjonskode 51, namn `0.16.0-alpha08`, ikkje debuggable.
- Signering verifisert; eksisterande sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping bevart ved sida av APK-en: `mapping-0.16.0-alpha08.txt`.

## Avgrensingar

Ingen ekte førespurnad er sendt til Seerr. Reell kvote/godkjenning, import/varsel, lang avspeling, nettbyte og fysisk telefon/nettbrett/TV står att. Biblioteklesing inkluderer mapper og returnerte medietypar, men gir ikkje nye avspelarar for musikk, foto, bøker eller Live TV.

Sjå [historikk og kvotar](ROADMAP_HISTORY_RULES.md), [bibliotekval](LIBRARY_TV_NAVIGATION.md) og [oppdatert veikart](../ROADMAP.md).
