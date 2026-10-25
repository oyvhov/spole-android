# Spole beta21: lokal signert oppdatering på Shield

25. oktober 2026. Brukaren bad om å oppdatere Spole på Shield slik at Stova kan opne rett tittel.
Dette er ei lokal installering, ikkje ein publisert GitHub Release.

## Kjelde og bygg

- Grein: `feat/spole-launcher-link`, med eksisterande implementasjon av `spole://open`.
- `origin/main` beta20 (`1a8003a`) vart fletta inn i `dd46cbe`; berre CHANGELOG trong manuell
  samanslåing, og begge seksjonane vart bevarte.
- Ny lokal versjon: `1.0.0-beta21`, `versionCode=158`, produksjonspakke `app.reelstack`.
- Bygd med den eksisterande eigarnøkkelen. Sertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

```powershell
./gradlew.bat testDebugUnitTest lintDebug assembleRelease --console=plain --max-workers=2
```

JDK 21 og ASCII Gradle-/tempstiar etter prosjektreglane. Resultat: BUILD SUCCESSFUL på 4 min 53 s.
1288 JVM-testar totalt: **1287 bestått, 0 feila, 1 hoppa over**. Den eksisterande, eksplisitt
aktiverte LAN-nettverkstesten er ikkje køyrd. Dei fire launcher-testklassane har 42/42 bestått.
Lint: **0 feil**, 212 åtvaringar og 2 hint. Android-instrumentering er ikkje køyrd i denne økta.

## Artefakt

- APK: 28 060 332 byte, signert release med minSdk 26, ikkje debuggbar.
- SHA-256: `3fe9870fa82c6f37fcfa747e44c029ac60014dfe4dc63cd69368873e7ae5c4ee`.
- Alle 20 native bibliotekfiler er byteidentiske med den publiserte beta20-APK-en.
- FFmpeg-lisensane finst i APK-en. Dei eksisterande native kjelde-/relenkingsarkiva frå beta20
  er kopierte saman med APK og den nøyaktige R8-mappinga til det git-ignorerte lokale arkivet
  `app/build/release-v1.0.0-beta21-local/`.

## Shield-prøve og avgrensing

NVIDIA Shield `192.168.10.182:5555` hadde berre produksjonspakken `app.reelstack` beta20/157,
ingen `.debug`-pakke. Stova-favoritten peikar på produksjonspakken.

Før oppdateringa gav ei gyldig `spole://open`-førespurnad «No activity found» ved resolverprøve.
Etter `adb install -r` peikar den same ruta på `app.reelstack/.launcher.LauncherOpenActivity`.
Android stadfestar beta21/158; SHA-256 av den installerte APK-en er identisk med fila over.
`firstInstallTime` er framleis 10.09.2026, medan `lastUpdateTime` er 25.10.2026.

Ein detaljlenkje for ein faktisk film frå Stova sitt eksisterande Jellyfin-snapshot vart sendt med
`action=details`. Appen viste **Velkomen til Spole / Logg inn**. Ein separat vanleg kaldstart,
utan lenkje eller launcher-ekstra, viste også innlogging. Dette er difor ikkje berre eit avvist
titteloppslag. Eg kan ikkje stadfeste at eksisterande Spole-innlogging er tilgjengeleg i appen no,
og gjer ingen påstand om ei vellukka ekte detaljopning eller bevarte Spole-kontoar.

Ingen `pm clear`, avinstallasjon, utlogging, kontoendring, tokenmigrering eller avspeling er utført.
Produksjonspakken er ikkje debuggbar, så private kontofiler kunne ikkje samanliknast direkte.
Innloggingslagringa og tokenlagerkoden er uendra frå beta20. Det er ikkje fastslått kvifor eininga
viser innlogging; ikkje legg til kontoar eller flytt løyndomar utan ei ny, konkret avklaring.

Attståande prøve etter at innlogging er tilgjengeleg: kald og varm opning frå Stova til ein kjend
tittel, metadata og Back-retur. Avspeling, profilbyte og barnemodus er ikkje prøvde på fysisk Shield.
Shield-UI vart frigjeven til hovudagenten etter installasjon og dei avgrensa prøvene.

Ingen tag, push eller offentleg release er oppretta. Versjonskode 158 er no lokalt distribuert;
ei seinare annleis utgåve må ha høgare kode.
