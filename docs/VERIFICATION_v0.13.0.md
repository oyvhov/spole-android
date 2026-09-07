# Verifisering – Spole 0.13.0

## Endring

Åtte nye funksjonar og elleve rettingar. Sjå `docs/release-v0.13.0.md` for heile lista.

Dei to endringane med størst risiko:

- **Mellomlageret vart flytta frå JSON i SharedPreferences til Room.** Alt som blir lagra er framleis
  berre kunst- og tittelrader; avspeling, køar og aktivitetsfeeden blir framleis aldri skrivne til
  disk. Skjemaversjonen er bumpa, så ei kopi frå ein eldre build blir sletta i staden for tolka.
- **`identityUrl` er innført.** `RequestTrackingRepository.scope()` hasha `baseUrl`. Utan ein fast
  identitet ville failover mellom heime- og borteadressa gitt ein ny hash og fått alle følgde
  førespurnader til å forsvinne. Identiteten blir skriven éin gong ved første lagring og aldri
  endra av failover. Eksisterande installasjonar har ingen lagra identitet og fell tilbake til
  adressa si, så hashane deira er uendra.

## Automatiske kontrollar

- **202/202 JVM-testar** bestod (var 169 før denne utgåva).
- **82/82 Android-testar** bestod på isolert emulator 5562.
- Lint: **0 feil, 26 åtvaringar** (alle frå før: nyare bibliotekversjonar og targetSdk).
- Rein `clean testDebugUnitTest lintDebug assembleRelease` med Windows-Gradle og JDK 21.

Nye testar dekkjer: resume-lista per bibliotek og at barnebiblioteket aldri blir spurt,
bibliotek-søk og escaping av søkeordet, at ein tom søkestreng aldri når tenaren, at ein
førespurnad som tilhøyrer ein annan konto aldri blir sletta, at ein administratornøkkel ikkje kan
trekkje tilbake på vegner av andre, at serveridentiteten er stabil når adressa byter, paginering
med og utan `totalPages`, og at eit sidetal under 1 blir avvist før det blir sendt.

Tre reelle feil i mine eigne endringar vart funne av Android-testpakken og retta før denne
utgåva: Room-lageret mista nynorsk-omsetjinga som JSON-lageret gjorde ved lesing, skiljeteiknet
var eit rått kontrollteikn i kjelda, og forventa sett av Heim-seksjonar mangla den nye rada.

## Emulator

Oppgraderingstesten som `docs/AI_INSTRUCTIONS.md` krev vart utført på `Spole_Review` (5560):

1. Produksjons-APK 0.12.4 vart lasta ned frå GitHub-releasen og installert.
2. Førstegongsoppsettet vart fullført, og «Kjem snart» vart slått **av** i Innstillingar.
3. Produksjons-APK 0.13.0 vart installert med `adb install -r` over den.

Resultat etter oppgraderinga:

- Appen opna direkte i Heim; førstegongsoppsettet kom ikkje tilbake.
- «Kjem snart» stod framleis **av**. Det eksplisitte valet overlevde.
- «Hald fram å sjå» var lagt til og stod **på**, éin gong, utan å røre dei andre vala.

Det siste punktet er den nye `HOME_SECTIONS_VERSION`-migreringa, stadfesta ende-til-ende på ei
ekte produksjon-til-produksjon-oppgradering.

Ingen ekte tenestekontoar var innlogga under kontrollen, så innlogging mot Jellyfin, Emby eller
Seerr er verifisert gjennom kodeflyt og einingstestar, ikkje mot ein levande tenar.

## Ikkje verifisert mot ekte tenester

- **Failover mellom to adresser** er einingstesta, men ikkje køyrd mot to reelle adresser til
  same tenar.
- **«Opne i appen»** finn ein installert klient som har registrert seg for tenaradressa. Dette er
  den korrekte Android-mekanismen, og `<queries>`-elementet som mangla er grunnen til at det ikkje
  kunne fungere før. Det er ikkje testa med Jellyfin-appen faktisk installert. Utan ein slik klient
  oppfører knappen seg som før, med rett etikett.
- **Widgeten** er bygd og registrert, men ikkje plassert på ein heimeskjerm med ein levande tenar.

## Produksjonsfil

- Versjon 0.13.0, versionCode 35, pakke `app.reelstack`, minSdk 26, targetSdk 36.
- Storleik: 7 122 917 byte.
- SHA-256: `5d77935bb1873ecf4ed8762091957029888e25b66a5e1d092f47b7c2aaca68a2`.
- Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Same produksjonsnøkkel som 0.12.4, stadfesta med `apksigner verify --print-certs`. APK-en kan
  installerast som ei vanleg oppdatering, og det er faktisk gjort i oppgraderingstesten over.

## Nye avhengigheiter

KSP 2.3.10 og Room 2.8.4. Første Gradle-synk etter denne utgåva lastar dei ned.
