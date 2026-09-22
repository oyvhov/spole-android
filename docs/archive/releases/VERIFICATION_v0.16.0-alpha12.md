# Verifikasjon: Spole 0.16.0-alpha12

Dato: 10. september 2026. Pakke `app.reelstack`, versjonskode 55. Sjå [endringsrapporten](UPDATES_LIBRARY_ALPHA12.md) for funksjonar og oppdateringsflyt.

## Automatiske kontrollar

- Siste bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` bestod (`alpha12-build6.log`, 2 min 29 sek). Lint: 0 feil og 54 åtvaringar. Det nye lagringsrådet foreslår `StorageManager` for plass som kan frigjerast; nedlastaren brukar konservativt allereie ledig plass og viser feil dersom plassen er for liten.
- 340 JVM-testar bestått, ingen feil. Nye kontrollar dekkjer versjonssamanlikning, release-utval, APK-identitet/signaturkrav, tillatne nedlastingsvertar og serverbaserte bibliotekfilter med paginering.
- Android TV: 29 testar bestått i den breie runden. Etter at bibliotekvalet fekk to kolonnar og oppdateringsdialogen fekk tydeleg første fokus, bestod 17 målretta testar.
- Telefon: den breie runden køyrde 40 testar med bestått resultat; éin ekstra TV-test vart korrekt hoppa over. Den målretta runden køyrde åtte testar med bestått resultat og hoppa over den same TV-testen.
- TV-kontrollane omfattar synleg/fokusert førespurnadsknapp før lang omtale, Lagre ved skriftstorleik 2.0, fjernkontrollnavigasjon, oppdateringsdialog og bilettekstar utanfor fokusramma ved alle avrundingsval.
- Oppstartskontrollane dekkjer innlasting medan animasjonen køyrer, sperring av skjulte kontrollar og avgrensa ventetid. Logoen ved avslutta animasjon samsvarar med originalen.

Loggar ligg lokalt under `app/build/`: `alpha12-tv-final.txt`, `alpha12-phone-final.txt`, `alpha12-tv-refinement-final.txt` og `alpha12-phone-refinement-final.txt`. Instrumenteringa brukar isolerte testprofilar, ikkje dei innlogga brukarprofilane. Etter desse testane vart standardikonet ved lagring av musikkbibliotek justert slik at det samsvarar med førehandsvisinga.

## Oppdatering og offentleg repo

Repoet `oyvhov/spole-android` er stadfesta PUBLIC. GitHub sitt release-API er tilgjengeleg utan innlogging. Ingen GitHub-token eller medietenestelegitimasjon blir sende av oppdateringsklienten.

På kontrolltidspunktet var siste publiserte testutgåve alpha06, eldre enn denne APK-en. Ein komplett installasjon av ei nyare utgåve gjennom GitHub-flyten kan derfor ikkje stadfestast i denne leveransen. Release-utvalet og tryggleikskontrollane er testa automatisk; inga kunstig ny utgåve eller nedgradering vart publisert for å omgå dette. Alpha12 er levert som lokal signert APK, ikkje publisert som GitHub Release.

Android krev at brukaren godkjenner installasjon. Avbroten nedlasting kan startast på nytt, men held ikkje fram frå siste byte. Ei framtidig utgåve må ha éin universal-APK, GitHub SHA-256-digest, høgare versjonskode og same signeringsnøkkel.

## Avgrensingar

Emulatorane dokumenterer layout, navigasjon og oppdatering av appdata. Dei erstattar ikkje prøving av alle fysiske TV-ar, fjernkontrollar og maskinvarekodekar. Denne runden endrar ikkje avspelaren sine kodekreglar. Oppløysingsfilteret følgjer videobreidda som Jellyfin har registrert; mapper og ikkje-videoelement har ikkje nødvendigvis slike metadata.

## Signert APK

- Fil: `app/build/test-alpha12/Spole-0.16.0-alpha12.apk`.
- Storleik: 3 902 565 byte.
- SHA-256: `5ce71b367a19d419c82d9fcd39732b3639d555d529d3f08e13a0c94e79fff8dd`.
- Signatur kontrollert med Android `apksigner`; eksisterande sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Produksjonsbygg, ikkje debug. Android 8.0/API 26 eller nyare, target API 36. R8-mapping og `SHA256SUMS.txt` ligg ved APK-en.

## Produksjonsbygg med ekte data

Den endelege APK-en vart installert med `adb install -r` på både TV-profilen (`emulator-5564`) og telefonprofilen (`emulator-5560`). Begge rapporterer alpha12/55. Ingen profilar vart sletta. Dei innlogga Jellyfin-, Emby- og Seerr-kontoane på telefonen vart bevarte; heiminnhaldet frå Jellyfin vart lasta. TV-en beheldt Forest-bakgrunn, Ocean-aksent, kompakte plakatar, mjuke hjørne og kombinert Hald fram/Neste episode. Telefonen beheldt nynorsk, lime-aksent og separate framhaldsrader.

Manuelt kontrollert i produksjonsbygget:

- Bibliotekval på TV i to kolonnar og telefon i éin kolonne, med Lagre og Avbryt synlege. Dei ekte bibliotekvala vart bevarte.
- Ikonveljaren kunne opnast frå ein mellombels meny-snarveg. Gjennomgangen vart avbroten med Tilbake utan å lagre endringar i brukarval.
- TV-fokus vart flytta med retningsknapp frå filter til plakat. Fokusramma omkransar biletet; tittel og år ligg under ramma.
- Jellyfin leverte ekte sjangrar. Drama-filteret endra lista frå det ufiltrerte biblioteket til mellom anna «12 edsvorne menn», «1917» og «About Time». Nullstill gav den opphavlege lista tilbake.
- Manuell GitHub-sjekk i den signerte TV-appen fullførte og viste at inga nyare kompatibel utgåve var tilgjengeleg. Ingen nedgradering vart tilbydd.
- Begge emulatorane er opne i eigne synlege vindauge med ekte data.

Lokale skjermbilete ligg i `app/build/alpha12-library-final.png`, `alpha12-library-phone.png`, `alpha12-focus-final.png` og `alpha12-updates-final.png`. Dei er ikkje lagde i det offentlege repoet.

Telefonemulatoren viste først tomme rader etter kald oppstart. Etter ny opning av appen då nettverket var klart, kom det ekte innhaldet tilbake utan ny innlogging eller sletting av data. Automatisk gjenoppretting ved akkurat denne nettverksovergangen er ikkje stadfesta som løyst i alpha12.
