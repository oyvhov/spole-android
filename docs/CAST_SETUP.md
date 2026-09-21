# Chromecast i Spole

Cast-mottakaren ligg i `cast-receiver/` og vert bygd som ei offentleg, statisk GitHub Pages-side på
`https://oyvhov.github.io/spole-android/cast/`. Ho inneheld aldri tenaradresse, brukar-ID eller
tilgangsteikn. Ein Android-bygg er medvite utan Cast-knapp til ein team-eigd Receiver-ID er sett i
`app/src/main/res/values/cast.xml`.

## Før produksjon

1. Opprett ein Custom Web Receiver i Google Cast SDK Developer Console på ein varig team-eigd
   Google-konto. Set mottakar-URL til Pages-adressa over.
2. Registrer den fysiske utviklings-Chromecasten i same konsoll. Gjer produksjonsmottakaren
   publisert først når ende-til-ende-sjekken nedanfor er godkjend.
3. Set den offentlege Receiver Application ID-en i alle tre `cast.xml`-filene. ID-en er ikkje ei
   løynd; ikkje set tenarteikn, URL eller brukaropplysningar der.
4. Slå på GitHub Pages med GitHub Actions som kjelde. Arbeidsflyten `Publiser Cast-mottakar`
   byggjer, testar og publiserer berre `cast-receiver/dist`.

## Jellyfin, Emby og CORS

Chromecasten må kunne nå same primær- eller alternativadresse som mottakaren får frå telefonen.
Telefonen er ein fjernkontroll, aldri ein straumproxy. Konfigurer Jellyfin, Emby eller revers-proxy
med eit smalt `Access-Control-Allow-Origin` for `https://oyvhov.github.io`, og tillat minst
`Authorization`, `X-Emby-Token`, `X-Emby-Authorization`, `Range`, `Content-Type` og
`Accept-Encoding` på manifest, segment, eksterne undertekstar og kunst. Ikkje bruk `*` saman med
innloggingshovud.

Mottakaren hentar `PlaybackInfo` sjølv med ein konservativ Chromecast-profil. Ho vel
direkteavspeling når tenaren uttrykkeleg stadfestar det; elles brukar ho tenaren si HLS/H.264/AAC-
transkoding. Alle media-URL-ar vert kontrollerte mot den valde primær-/alternativruta, og
omdirigeringar vert avviste.

`CastLoadSpec` er offentleg medietittel/køinformasjon. `CastCredentialEnvelope` er berre i minnet
og går i Cast LOAD credentials. Ho skal aldri hamne i media-URL, kødata, logging,
SharedPreferences eller GitHub Pages-bygg. Mottakaren slettar henne ved idle, feil og stopp.

«Cast resten av sesongen» er den einaste køhandlinga. Ho tek den valde episoden og seinare
episodar frå same serie og sesong, i episoderekkjefølgje. Mottakaren byggjer ei CAF-kø frå denne
offentlege lista og forhandlar kvar episode med tenaren medan den same minneberte innlogginga er
gyldig. Ho kan ikkje blandast med annan serie, sesong, teneste eller konto.

## Test før publisering

Køyr frå `cast-receiver`:

```powershell
npm install --ignore-scripts
npm run check:public
npm test
npm run build
```

Den statiske kontrollen stoppar bygget dersom kjeldefilene ser ut til å innehalde token eller
interne nettadresser. Test manuelt på registrert Chromecast med Jellyfin og Emby: direkte fil,
HLS-transkoding, lydspor, teksting, telefonlås, app-avslutting, Wi-Fi/mobilnett-byte og retur til
telefon. Prøv særskilt utgått teikn, CORS-avslag, uoppnåeleg rute og uspeleleg format; telefonen
skal halde lokal avspeling i gang når handoveren ikkje vert akseptert.

## Avgrensing i denne arbeidsmappa

Google Cast-konsollregistrering, fysisk mottakartest og GitHub Pages-aktivering treng eigarskap til
eksterne kontoar og kan ikkje gjerast frå Android-kjeldekoden. Fram til Receiver-ID-en er sett,
held appen Cast UI skjult i staden for å tilby ei halvferdig handling.
