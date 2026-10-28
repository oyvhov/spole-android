# Verifisering · Spole 1.0.0-beta25, lokal kandidat

28. oktober 2026 · `versionCode=162` · produksjonspakke `app.reelstack`.

Beta25 samlar direkte biblioteklenkjer frå Stova og rettar venstremargen i TV-rader.
Kjelda omfattar `b4775cf`, ei retting av ei konkurrerande statusoppdatering ved bibliotekopning
og TV-rullerettinga med eigne regresjonstestar.

## Bygg og kontrollar

| Kontroll | Resultat |
| --- | --- |
| Windows-bygg med JDK 21 | `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: `BUILD SUCCESSFUL` |
| JVM-einingstestar | 1322 bestått, 0 feil, 1 hoppa over (1323 totalt) |
| Lint | 0 feil, 212 åtvaringar |
| Native undertekstar | Fire ABI-ar, 16 KB-justering for 64-bit og åtte lisensmerknader stadfesta |
| Signeringssertifikat, SHA-256 | `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` |

Den signerte universal-APK-en er 28 060 404 byte, `versionCode=162`,
`versionName=1.0.0-beta25`, `minSdk=26`, utan debug-flagget, og har SHA-256
`bcf57ab884b584d8a8aec4c3f8dd1368be3de94d45b4bca3a98f45b6bd1c7f7b`.

FFmpeg-kjelde-/relenkingsarkivet og libass-kjeldearkivet er frå den førre kandidaten.
Alle 30 native bibliotek- og lisensoppføringar i denne APK-en er byteidentiske med
beta24-kandidaten som arkiva vart kontrollerte mot. Arkiva og den tilhøyrande
R8-mappinga er lagde i release-mappa.

## Android og ekte data

Den tidlegare beta24-kandidaten inneheldt biblioteklenkjene og statusrettinga. På den
vart bibliotekintegrasjonen prøvd med syntetiske data på TV, og heile mobilpakken
gav 455 beståtte, sju feila og 55 hoppa over. Dette er ikkje eit resultat for beta25.
Feila gjaldt `LanguagePickerLayoutTest`, to i `ReelstackSmokeTest`, `ReleaseRowsTest`,
`SmartShelvesTest`, `SteadyRemoteRowsTest` og `TvLibraryRegressionTest`. Den runden
avslutta med `FAILURES`, ikkje `OK`. Android-testpakka for beta25 er bygd, men ikkje
køyrd på dei innlogga profilane eller på ein isolert test-AVD.

Beta25 vart ikkje publisert. Den signerte beta25-APK-en vart installert med `adb install -r` over beta23 på den
lagra TV-profilen `Spole_GoogleTV_Test` / 5564. Pakken viser kode 162, og
`firstInstallTime` er framleis 9. september 2026. Appen viser den eksisterande
innlogginga og ekte bibliotekinnhald. Fem trykk til høgre og fem tilbake i
«Hald fram å sjå» viste første flisa på linje med overskrifta igjen.
Skjermbileta er berre lagra lokalt under den Git-ignorerte byggmappa.

Den lagra mobilprofilen `Spole_Review` / 5560 står på den lokale beta24-kandidaten
med kode 161 for ei eventuell prøving av den publiserte oppdateringsflyten. Ei
biblioteklenkje med ekte brukar- og bibliotek-ID er ikkje prøvd på desse profilane.

## Publisering

GitHub-asset, offentleg nedlasting og oppdateringsflyten gjennom appen er ikkje
kontrollerte før utgåva er publisert.
