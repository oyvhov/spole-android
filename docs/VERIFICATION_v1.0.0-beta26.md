# Verifisering · Spole 1.0.0-beta26

28. oktober 2026 · `versionCode=163` · produksjonspakke `app.reelstack`.

Utgåva samlar biblioteklenkjene frå `b4775cf`, rettar ein konkurrerande
statusoppdatering ved bibliotekopning og rettar navigering i TV-rader og -dialogar.

## Bygg og kontrollar

| Kontroll | Resultat |
| --- | --- |
| Windows-bygg med JDK 21 | `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: `BUILD SUCCESSFUL` |
| JVM-einingstestar | 1322 bestått, 0 feil, 1 hoppa over (1323 totalt) |
| Android på isolert mobil, 5562 | `OK (520 tests)`: 464 bestått, 0 feil, 56 hoppa over |
| Android på isolert TV, 5566 | `OK (53 tests)`: 53 bestått, 0 feil |
| Lint | 0 feil, 212 åtvaringar |
| Native undertekstar | Fire ABI-ar, 16 KB-justering for 64-bit og åtte lisensmerknader stadfesta |
| Signeringssertifikat, SHA-256 | `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` |

Mobilpakken inkluderer heile Android-suiten; testtilfelle som krev TV-modus og
andre avgrensa miljø blir hoppa over der. TV-køyringa omfattar biblioteklenkja,
fokus i rutenett og bibliotekredigerar, navigasjon, TV-skjermar, oppsett og
oppdateringsgrensesnitt. Popup-testane held fram med å kontrollere lukking og
fokusretur, med ei tidsgrense som toler ein langsam emulator.

Den signerte universal-APK-en er 28 060 400 byte, `versionCode=163`,
`versionName=1.0.0-beta26`, `minSdk=26`, utan debug-flagget, og har SHA-256
`eb2fe8103a08a7d385670ca770d77430965dee76993161b448a4c58e32af3c52`.
Mappinga høyrer til nett dette bygget. FFmpeg-kjelde-/relenkingsarkivet og
libass-kjeldearkivet er dei kontrollerte arkiva frå beta25-kandidaten. Alle 30
native bibliotek- og lisensoppføringar i APK-en er byteidentiske med den kandidaten.
Hashar for APK, mapping og begge arkiva står i `SHA256SUMS.txt`.

## Kontroll med ekte data

Den signerte APK-en vart installert med `adb install -r` over beta25 på den lagra
TV-profilen `Spole_GoogleTV_Test` / 5564. Den opphavlege installasjonstida frå
9. september 2026 er bevart, og appen viser den eksisterande kontoen og ekte
bibliotekinnhald. Fem trykk til høgre og fem tilbake i «Hald fram å sjå» gav
same venstremarg på første flis som før navigeringa. Bileta frå kontrollen er
berre lagra lokalt i den Git-ignorerte byggmappa.

Den lagra mobilprofilen `Spole_Review` / 5560 står på beta24-kandidaten (kode
161) for kontroll av oppdateringsflyten etter publisering. Biblioteklenkja er
prøvd ende til ende med syntetisk tenar på både mobil og TV; ho er ikkje prøvd
med ekte brukar- og bibliotek-ID i denne kontrollen.

## Publisering

GitHub-asset, offentleg nedlasting og oppdateringsflyten gjennom appen blir
kontrollerte etter at utgåva er publisert. Resultatet blir lagt til her i ein
eigen dokumentasjonscommit utan å flytte release-taggen.
