# Verifisering: Spole 0.17.0-beta20

18. september 2026. Oppfølging av brukarens seks avspelingsobservasjonar frå beta19.

## Konkrete kodefunn

- `onPause` kalla `background`, som sette `playWhenReady=false` permanent. Pause skjer
  no ved `onStop` (appens minste API er 26), ikkje ved kort tap av fokus medan video er synleg.
- TV-kontrollane brukte `isPlaying` ved Play/Pause. Under bufring er denne false sjølv
  når avspeling er sett i gang; Play kunne difor slå `playWhenReady` av. Intensjon blir brukt no.
- Tvungne `preferredDisplayModeId`-endringar og tilbakeføring ved lukking er fjerna.
  Dei kunne krevje svart HDMI-overgang og ny lydruting; dette er ikkje fysisk målt her.
  Media3 sitt `ONLY_IF_SEAMLESS`-val og rammetiming blir bevarte.
- Transportreserve brukte full `prepare`, med Stopped og ny serverforhandling. Ho prøver no
  eksisterande Media3-kjelde på nytt. Avgrensa til to reserveforsøk per tittel; konto og
  generasjon blir kontrollerte. Lukking/ny kjelde avbryt ventande retry.
- Feila tekstuthenting vart repetert gjennom videoklienten etter den første 8-sekundsgrensa.
  Tekstfeil gir no tom WebVTT og synleg varsel, ikkje ny 20/45-sekunds tekstnedlasting eller
  codec-nedgradering av filmen. HTTP-kall for tekst blir kansellerte ved lukking/kjeldebyte.
- Stats gjorde to komplette kapabilitetssøk på hovudtråden ved lydformatbyte.
  Diagnostikk blir no henta på IO-tråden ved førebuing; fast kodekliste blir gjenbrukt,
  medan lydutgangskapasitet framleis blir lesen ved forhandling.

## Testar og avgrensingar

Nye regresjonstestar dekkjer kort fokusavbrot, same serverøkt/posisjon ved nettverksreserve,
uendra vindaugsmodus, feilande undertekst og lukking under hengande tekstuthenting,
samt TV-Play under buffering og lesbart tekstvarsel ved 2× skrift.
Fire einingstestar for den fjerna tvungne skjermmodusveljaren er fjerna saman med funksjonen;
test av gyldig kjeldefrekvens er bevart.

Ingen fysisk Pixel/Shield eller faktisk fil frå brukarens server er verifisert.
At 4K «300» fungerer er brukarens observasjon, ikkje ein lokal test.
Tidlegare review-profilar har ADB-/oppstartsproblem; ingen kontoar eller brukardata blir nullstilte.
Testar bruker isolert 5562, ikkje 5560/5564 med ekte kontoar.

### Utførte testar

- JVM: **628/628**, ingen feil. Fire testar for fjerna tvungen skjermmodus er erstatta
  av fjerning av funksjonen; ein ny test dekkjer Play under bufring.
- Avspelingsregresjon på isolert Android 5562: **37/37**, 191,589 sekund.
  Dette omfattar 21 spelar-/integrasjonstestar, fire kapabilitetstestar,
  elleve spelar-UI-testar og den nye TV-Play-testen. Lydprøvene omfattar
  AC3/EAC3/DTS/TrueHD for begge syntetiske servervariantane og lokal FFmpeg-reserve.
- Første ekstrarunde med elleve TV-kontrolltestar: ti bestod, éin feila berre ved
  skjermbiletopptak etter vellukka synlegheitskontroll. `onRoot()` fann både spelerot
  og eit lite ekstra vindauge. Opptaket er presisert til `jellyfin-player`; ingen
  produksjonskode er endra som følgje av denne testfeilen.
- Nettverksreservetesten injiserer transportfeil etter stopp, og stadfestar at posisjon
  og serverøkt blir bevarte. Dette er ikkje ein reproduksjon av brukarens nettverk.
- Lukking vart testa medan teksttenaren venta 30 sekund: aktiviteten vart lukka innan
  tresekundsgrensa. HDMI-overgangstid på Shield er ikkje målt.
- **Endeleg Android-runde: 48/48, ingen feil**, 216,559 sekund, etter reinstallasjon av
  siste debug-APK og test-APK. Dei same 21 spelar-, fire kapabilitets- og elleve UI-testane
  vart køyrde på nytt, no saman med alle tolv TV-kontrolltestane. TV-opptaksfeilen ovanfor
  er borte. TV-flata er testa på isolert 5562, ikkje på fysisk TV.
- Endeleg JVM-resultat er framleis **628/628**, null failures/errors.

## Artefakt og bygg

- Produksjonspakke `app.reelstack`, versjon `0.17.0-beta20`, kode **99**, minSdk 26.
- Universal-APK: arm64-v8a, armeabi-v7a, x86 og x86_64. Ikkje debuggable.
- Same signeringssertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Native-koden er uendra frå beta18/19. Kjelde-/relenkingsarkivets SHA-256:
  `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.
- Apache-2.0- og LGPL-2.1-tekstane er med i `assets/licenses/ffmpeg/` i APK-en.
- Første komplette produksjonsbygg: `BUILD SUCCESSFUL`, 9 minutt 40 sekund, exit 0.
- Endeleg byggpass etter siste kjelde-/testretting: **BUILD SUCCESSFUL**, 9 minutt
  14 sekund, exit 0. Testane ovanfor vart køyrde med APK-ane frå dette siste bygget.
- Lint: **0 feil, 93 åtvaringar**. Dette er ikkje ein påstand om null teknisk gjeld.
- Endeleg APK: **11 064 394 byte**, SHA-256:
  `458a571e8afc10226a62b6391d74b7cc1687983c47a8d9bc6ef8d9abec907e1a`.
- `apksigner verify --print-certs` og `zipalign -c -P 16 4`: godkjende for endeleg APK.
- Endeleg APK og R8-mapping er arkiverte i `app/build/release-v0.17.0-beta20/`.

## Publiseringskontroll

Ingen signert føroppdatering vart installert på review-profilane med ekte kontoar, grunna
ADB-/oppstartsavgrensingane ovanfor. Produksjons-beta19 (98) er bevart på isolert 5562
for den ekte GitHub-oppdateringsflyten etter publisering, med nynorsk og demodata.
### Utført publisering og oppdatering

- Kjeldecommit `1464cb575fc2657749eb9d86e79cbf5209c0f1df`, annotert tag
  `v0.17.0-beta20`, pusha atomisk med `main`.
- GitHub-kladden hadde nøyaktig éin APK og fire støttefiler, alle `uploaded`.
  APK-storleik/-digest og native-arkivets digest var identiske med lokale filer.
- Publisert som `draft=false`, `prerelease=true`, ikkje stabil latest.
- Den offentlege release-lista utan Authorization inneheld beta20. Ei separat offentleg
  nedlasting har SHA-256 `458a571e8afc10226a62b6391d74b7cc1687983c47a8d9bc6ef8d9abec907e1a`.
- **Oppdatering beta19 → beta20 gjennom appen er fullført:** Sjekk no → beta20
  funnen → Last ned → appens kontroll → Installer → Android-godkjenning → beta20.
  Ingen `adb install` vart brukt til denne produksjonsoppdateringa.
- Første installasjonsforsøk henta fram eit gammalt «App installed»-vindauge frå beta19.
  Kontroll av versjonskoden viste framleis 98. Den gamle Android Package Installer-prosessen
  vart stoppa på isolert 5562 (utan sletting av data); nytt trykk på Installer viste rett
  oppdateringsdialog. Dette miljøinngrepet er ei avgrensing ved ende-til-ende-testen.
- Play Protect bad om skanning. Ho fullførte med «This app looks safe», og installasjonen
  vart godkjend i systemdialogen. Dette er ikkje ei full tryggleiksrevisjon.
- Android stadfestar **versionCode=99, versionName=0.17.0-beta20**. Appen opna att
  med nynorsk og dei same demodataa (Maya/Severance) bevarte.
  Ekte kontoar og fysisk Pixel-/Shield-avspeling er framleis ikkje verifiserte.

## Primærkjelder

- [Android: ExoPlayer-livssyklus frå API 24](https://developer.android.com/media/implement/playback-app)
- [Media3 1.11.1: sømlaus Surface-frekvens og rammetiming](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/video/VideoFrameReleaseHelper.java)
