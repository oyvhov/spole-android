# Verifisering: oppstart og avspelingskontrollar

Dato: 8. september 2026. Byggjer vidare på `5a8b903` (Spole 0.15.0).
Dette er eit lokalt, upublisert førehandbygg, ikkje ei erstatning for GitHub-releasen 0.15.0.

## Endringar

- Den eksisterande Spole-vektoren blir forma over namnet med fast layout. Sluttforma er
  pikseltesta mot originalen. Tyngre appinnhald blir først komponert etter logoforminga;
  datahentinga byrjar uavhengig av dette. Venting på nettverk er avgrensa.
- Tilbake har eit fast 48 dp treffområde utanfor rulling og automatisk skjuling.
  Android si navigering blir ikkje automatisk gøymd. Førstegongsmeldinga
  `ImmersiveModeConfirmation` vart observert med vindaugsfokus i den opphavlege tilbakefeilen.
- Tilbakehandtering er registrert før innlasting. PlayerView tek ikkje tastaturfokus.
- Videoberøring viser kontrollane ved nedtrykk. Berøringsobservatøren lar den faste
  tilbakeknappen og andre underliggjande kontrollar få sitt opphavlege trykk.
- Kontrollane tonar inn på 90 ms og ut på 140 ms. Tidslinjedraging, menyar, pause,
  bufring og feil hindrar automatisk skjuling.

## Endeleg kontroll

| Kontroll | Resultat |
|---|---|
| `testDebugUnitTest` | 253 bestått, 0 feil |
| `lintDebug` | 0 feil, 28 åtvaringar |
| Debug, test-APK og signert R8-release | Bygde |
| `JellyfinPlayerTest` | 13/13 bestått |
| `JellyfinPlayerUiTest` | 8/8 bestått |
| `StartupRevealTest` | 6/6 bestått |
| `HomeSearchNavigationTest` | 3/3 bestått |
| Endeleg målretta Android-runde | **OK (30 tests)**, 129,737 sekund |
| Signering | Eksisterande Spole-sertifikat verifisert |

Videotestane bruker syntetiske kontoar og faktisk AVC/AAC-, HLS- og VTT-avspeling på
`emulator-5562`. Dei dekkjer mellom anna fysiske skjermtrykk for vising/pause,
tilbakeknappen etter automatisk skjuling, Android Tilbake, spoling, rotasjon,
spor-/kvalitetsbyte, bakgrunn og feil/retry. UI-testane dekkjer òg skriftstorleik 2.0.

Testane ventar på at oppstartsdekket slepper innhaldet. Videoberøringstesten oppdaterer
UiAutomation sin cache før han les noder som AnimatedVisibility har oppretta på nytt;
eit lokalt skjermbilete stadfesta at ein eldre cache kunne melde skjulte kontrollar
sjølv når dei var synlege og videoen framleis spelte.

### Avgrensing av fulltest

Ein tidlegare fullrunde køyrde 127 testar: 126 bestod, og testen av tilbake med skjulte
kontrollar avdekte at berøringsobservatøren tok nedtrykket. Dette er retta og testen
bestod i den endelege 30-testarsrunden. Forsøk på ny fullrunde vart avbrotne av
omstart/minnemangel i emulatormiljøet. **127/127 er difor ikkje attestert på endeleg kode.**

WSL hadde brukt opp både RAM og swap; Linux-loggen viste minnemangel. Den isolerte
testemulatoren vart stoppa og starta utan snapshot med mindre minne (emulatoren valde
minimum 2560 MB). Ingen AVD, konto eller appdata vart sletta. Etter 30-testarsrunden
vart testemulatoren avslutta for å frigjere minne til den synlege review-emulatoren.

## Manuell review med ekte konto

Den signerte APK-en vart installert med `-r` på `Spole_Review` / `emulator-5560`.
Innloggingane og profilen var bevarte. Logoforminga vart teken opp og kontrollert i
enkeltbilete; ho endar i den eksisterande logoen over Spole-namnet. Miljøet hadde
minneproblem under opptaket, så dette er ikkje ein attest på bildefrekvens eller total kaldstartstid.

Etter at miljøet kom seg, vart ein ekte Jellyfin-film opna frå framsida:

1. Trykk på videoen viste avspelingskontrollane.
2. Neste skjermtrykk på pause skifta til «Spel av».
3. Tidslinja spola tilbake til 0:00 medan filmen var på pause.
4. Eitt trykk på tilbakepila returnerte til detaljane med «Spel av i Spole».

Testframdrifta vart nullstilt til den opphavlege starten før spelaren vart lukka.
Skjermbilete, UI-uttrekk og video ligg berre i ignorert `app/build/startup-review/`;
dei skal ikkje publiserast med persondata.

## Lokalt APK-artefakt

- Fil: `app/build/startup-review/Spole-startup-player-preview.apk`
- Pakke: `app.reelstack`; førebels versjon 0.15.0 / 42, berre lokal testing.
- Storleik: 3 477 301 byte.
- SHA-256: `81b1dd4c8f1690224c5ccd682cd3c08342963606c6c5614f6dc6324804e16df9`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Tilhøyrande R8-mapping: `app/build/startup-review/mapping-startup-player.txt`.

Ein seinare GitHub-release må få nytt versjonsnummer og eige release-artefakt.
Den publiserte 0.15.0-APK-en er ikkje overskriven.
