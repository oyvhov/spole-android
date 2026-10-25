# Spole beta22: tittelopning når Android gjenbruker oppgåva

25. oktober 2026. Lokal oppdatering av produksjonspakken `app.reelstack` til
`1.0.0-beta22`, `versionCode=159`. Ingen push, tag eller offentleg release.

## Feilen og endringa

Beta21 opna «One Battle After Another» med rette Jellyfin-metadata ved direkte varm og kald
ADB-lenkje. Faktisk korttrykk frå Stova sende den same rette URI-en med `ACTION_VIEW` og
`FLAG_ACTIVITY_NEW_TASK`, men Spole viste Heim. Det vidare `START MainActivity`-kallet frå
mottakaren mangla i ActivityTaskManager-loggen.

Dette samsvarer med at Android gjenbruker ei oppgåve som opphavleg vart starta med den
kortliva mottakaren. [Android dokumenterer at NEW_TASK kan hente fram ei eksisterande
oppgåve](https://developer.android.com/guide/components/activities/tasks-and-back-stack).
Det er ei forklaring ut frå logg og kode; intern Android-dispatch vart ikkje instrumentert.

- `PendingLauncherOpen` godtek no også rå `ACTION_VIEW`-lenkjer som kjem til `MainActivity`,
  gjennom den eksisterande strenge parseren og dei same konto-/barnegrensene. Ei ugyldig rå
  lenkje fell ikkje tilbake til interne ekstra. Ei barneprofil blir løyst frå lagra kontoar.
- Mottakaren har tom `taskAffinity`, slik at nye mottakaroppgåver ikkje eig hovudskjermen sin
  oppgåvetilknyting. Han handterer også `onNewIntent` gjennom same dispatch som `onCreate`.
- Øktbinding, validering etter profilbyte og høgst eitt konsum per motteken handling står ved lag.
  Ingen endring av kontolagring, tokenlager, tenarkall eller detaljlasting.

## Regresjon og bygg

Seks nye regresjonar feila før rettinga: rå detaljopning ved gjenbruk (inkludert gjenteken
levering), rå barneprofilopning, stopp av vaksenlenkje i barnemodus, avvising av ugyldig URI
med gyldige ekstra, isolert mottakar-affinity og mottak av ein ny intensjon på eksisterande
mottakar. Etter rettinga er dei to målretta klassane grøne: **18/18**.

```powershell
./gradlew.bat testDebugUnitTest lintDebug assembleRelease --console=plain --max-workers=2
```

JDK 21 og ASCII Gradle-/tempstiar etter prosjektreglane. **BUILD SUCCESSFUL på 4 min 2 s**.
1294 JVM-testar totalt: **1293 bestått, 0 feil, 1 eksisterande eksplisitt LAN-test hoppa over**.
Lint: **0 feil, 212 åtvaringar, 2 hint**. Ingen Android-instrumentering i denne økta.

## Signert artefakt

- Produksjonspakke `app.reelstack`, versjon `1.0.0-beta22`, kode 159, minSdk 26.
- Release-APK: **28 060 364 byte**.
- SHA-256: `f309682bc23f04535877fe0a54624950037d234893f1b77b86957788447351e8`.
- Eksisterande signeringssertifikat kontrollert med `apksigner verify --print-certs`:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Alle 20 native bibliotek er byteidentiske med beta21 (og tidlegare beta20).
- Eksakt APK, R8-mapping, dei uendra native kjelde-/relenkingsarkiva og `SOURCE_COMMIT.txt`
  ligg i det git-ignorerte `app/build/release-v1.0.0-beta22-local/`, med `SHA256SUMS.txt`.

## Fysisk prøve

Hovudagenten installerte beta22 med `install -r` på Shield. Android stadfestar beta22/159;
SHA-256 av den installerte APK-en er identisk med det lokale artefaktet ovanfor.
`firstInstallTime` er framleis 10.09.2026. Prøvene vart samordna med brukaren gjennom ein
om lag eitt minutt lang pause i fjernkontrollbruken.

| Faktisk kortprøve på Shield | Resultat |
| --- | --- |
| Spole køyrer: fokuser «One Battle After Another» i Stova og send D-pad OK | Rett detaljside i Spole, med Jellyfin, Trailer og «Hald fram · 143 min att». Bestått. |
| Stopp Spole-prosessen med `am force-stop app.reelstack` i bakgrunnen; trykk same Stova-kort med D-pad OK | Same rette detaljside etter kald oppstart. Bestått. |
| Back frå detaljsida | Spole Heim. Bestått; Back går ikkje direkte til Stova. |
| Opne Stova uttrykkeleg etterpå | Stova Heim. Bestått. |

Begge kortprøvene venta til oppstartsskjermen var borte og både rett tittel og Trailer var
synlege. Det lokale skjermbiletet `C:/Launcher/.superpowers/spole-beta22-actual-card.png`
viser den kalde kortprøva og er kontrollert visuelt; det er ikkje lagt i Git. Dette er faktisk
Stova-kortopning, ikkje berre eit direkte ADB-kall av lenkja.

Ingen avspeling, utlogging, kontoimport, tokenmigrering eller datatømming vart utført av
agentane. Jellyfin-innlogginga frå beta21 var tilgjengeleg etter beta22-oppdateringa, og
det eksisterande halde-fram-punktet vart vist. Emby, avspeling, profilbyte og barnemodus er
ikkje fysisk prøvde i denne oppdateringa.

Berre dokumentasjon vart endra etter artefaktcommit `830cd4d`, som det arkiverte
`SOURCE_COMMIT.txt` peikar på. Denne dokumentasjonsoppdateringa gjorde ingen nye bygg eller
einingskall. Ei seinare annleis APK må ha høgare versjonskode enn 159.
