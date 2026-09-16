# Avspeling og sesongtema etter beta04

Lokal vidareutvikling. Ikkje publisert som ny automatisk oppdatering.

## Endringar

- Avspeling frå kortmenyen sender med Jellyfin/Emby-kjelda. OSD og ekstern avspeling brukar
  namnet frå den aktive kjelda på nynorsk, bokmål og engelsk.
- Avspelingslesing og formatforhandling toler opptil to nye forsøk ved mellombelse
  nettverksfeil og HTTP 408/429/500/502/503/504. Lange Retry-After-verdiar blir ikkje ignorerte.
  Framdriftsrapportering blir ikkje repetert av denne mekanismen.
- Media3 kan starte ei ny forhandling opptil to gonger ved nettverksbrot. Ein 404 kan
  prøvast på nytt for ein omkoda straum, ikkje for ei manglande direktefil. Løyvefeil
  blir aldri skjulte av ein endelaus retry-løkke. Posisjon, spor og kvalitetsval blir bevarte.
- Ein mellombels Seerr-feil stoppar ikkje ein ferskt stadfesta personleg mediekonto.
  Eksisterande identitetskontroll gjeld framleis når begge profilar er tilgjengelege.
- Diagnostikk loggar berre kjeldetype, steg og feilkode/type, aldri adresser eller tokens.
- Vanlege bibliotekvideoar (`Video`) går gjennom same avspelingsforhandling som filmar.
  Dei vart tidlegare avviste av medietypekontrollen sjølv om detaljsida tilbydde avspeling.
- Avspeling-innstillingane har føretrekt tekstspråk og reservespråk, med Norsk og Engelsk
  som standard. Norsk samlar bokmål, nynorsk, lokaliserte namn og kodane no/nor/nb/nob/nn/nno.
  Manglar begge språk, blir teksten av. «Følg tenaren» og «Av» er eigne alternativ.
  Full tekst blir prioritert over forced-spor innanfor same språk. Manuelt val i detaljar/OSD
  vinn over standarden. Jellyfin og Emby forhandlar det valde sporet med tenaren også ved omkoding.
- OSD har kompakte ikon med 48 dp trefflate. Spor, kvalitet, kortmenyar og temaval får
  ein felles kompakt meny med fast tittel/lukkeknapp og rullbare val.
- Langt OK/Enter-trykk på TV opnar kortmenyen ved slepp. Det same trykket kan ikkje
  aktivere eit val i menyen. Kort trykk og langt trykk med fingeren blir bevarte.
- Tilpassingsikonet på biblioteksida ligg nedst, etter alle radene. Ein tom topprad
  under den leiande heroen blir ikkje lenger laga når bibliotekoverskrifta er skjult.
  Kjelda er framleis tilgjengeleg i heroen, og innstillingsmenyen er uendra.
- Clearlogoar blir trimma for gjennomsiktig marg i Coil-cache og venstrejusterte på
  hero, detaljside og OSD. Originalfilene på tenaren blir ikkje endra.
- Hero prioriterer framleis clearlogo når tilgjengeleg og brukar toppen av bakgrunnsbiletet
  ved utsnitt. Tomatikon og prosent erstattar synleg «Rotten Tomatoes»-tekst;
  skjermlesaren får framleis kjeldenamnet. Ingen eksterne vurderingar blir dikta opp.
- «Vel uttrykk», temagalleriet, «Mine uttrykk» og dei tre snarvegane er fjerna saman
  med lagringsfunksjonane deira. Aktive fargar, sesongtema og andre brukarval blir ikkje nullstilte.
- Sesongbakgrunnen og hero tonar til same bakgrunnsfarge. Jul får meir gran, stjerner,
  juletre, gåve og snøkrystallar; halloween får graskar i tillegg til spindelvev og figurar.
  Pynt ligg ved biletets høgre kant, og brytaren og redusert rørsle blir respekterte.

## Kontrollgrunnlag

Regresjonstestar dekkjer kjeldenamn på alle tre språk, retry-grenser og løyvefeil,
gjennomsiktig clearlogo-marg, langt trykk med TV-tast, store OSD-kontrollar/menyar ved
skriftstorleik 2.0, fjerna uttrykksval og pikselfarge ved nedre kant av jule-/halloweenhero.

## Verifisering 16. september 2026

- Einingstestar: **536/536**, ingen feil eller hoppa over.
- Isolert TV 5566: **47 ulike testar bestått**. `TvRefinementUiTest` gav 26/26.
  `DesignRefreshUiTest` gav 21/21 etter at ein eldre test med fast radnummer vart
  endra til den stabile nøkkelen `libraries`. Flyttinga av ikonet endra radnummeret,
  ikkje regelen om at biblioteknamn skal stå under bileta.
- Isolert mobil 5562: **11/11** for spelar, kompakt meny og tekstspråk ved skriftstorleik
  2.0. Denne runden vart køyrd etter språkendringa, før den siste flyttinga av ikonet
  og utvidinga til `Video`. Desse siste endringane er testa med TV og einingstestar.
- Lint: **0 feil, 80 åtvaringar**. Debug, instrumenterings-APK og signert release bygde.
- Ein tidleg byggprosess stoppa under minnepress, og mobilens første kalde teststart
  fekk oppstartstimeout. Endelege bygg og testkøyringar fullførte etter omkøyring.
- Visuell TV-kontroll ved skriftstorleik 2.0: ikon nedst utan overlapp, og menyen opnar.
  Lokalt testbilete: `app/build/library-customize-footer-tv.png`.
- Ekte TV 5564: signert oppdatering med `install -r`, kontoar og brukarval bevarte.
  Emby-film spela med omkoding i 31 sekund; engelsk tekst var automatisk valt og
  avkryssa i OSD når norsk ikkje fanst. Ein bibliotekvideo som tidlegare vart avvist,
  spela direkte i 11 sekund etter at `Video` vart støtta. Avspelingane er avslutta.
- Dette stadfestar dei prøvde filene og dei avgrensa retry-reglane, ikkje at alle
  kodekar eller mellombelse tenarfeil er uttømmande testa.

Lokal APK: `app/build/outputs/apk/release/app-release.apk`, pakkenamn `app.reelstack`,
versjon `0.17.0-beta04` / 83, 11 747 058 byte. Dette er ei lokal førehandsvising;
den publiserte beta04-releasen og Auto-oppdateringa er ikkje endra.

SHA-256: `07532002958b1da5e0dfe7d90d6ed84704193cf03ab72c3dda2b369bce0c0f1c`.
Signatur: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

## Emby-integrasjon 16. september 2026

- Emby Connect-klienten støttar kontopålogging, serverliste og den dokumenterte lokale
  `/Connect/Exchange`-utvekslinga. Server-ID, AccessKey og lokal token blir haldne per server;
  dei blir ikkje blanda med Jellyfin eller ein annan Emby-server.
- Tilkoplingskontrollen kan oppdage Emby/Jellyfin frå `System/Info`, og PlaybackInfo retryar
  mellombelse 503/502/429-feil med avgrensa backoff. Ny test simulerer første Emby-feil og
  stadfestar at neste kall startar avspelinga.
- Spor har no stabil nøkkel frå språk, codec, namn og forced-status. Dette gjer at minnet kan
  følgje sporet når Emby endrar stream-indeks frå episode til episode. ASS/SSA blir annonsert som
  tekstspor; PGS og andre biletespor går gjennom serveromkoding/burn-in.
- Ratinglaget skil mellom tenarens CriticRating (tomat), TMDB/CommunityRating og MDBList når
  desse felta finst i payloaden. Spole viser ikkje manglande tredjepartsrating som om den var ekte.
- Samanlikna med JellyWatch/Moonfin manglar framleis Live TV/EPG/DVR, musikk/bøker, SyncPlay,
  OpenSubtitles og full lokal libass/PGS-rendering. Dette er medvitne neste steg, ikkje skjulte
  Emby-funksjonar.

Implementasjonen brukar eksisterande Media3 og Coil, med same opphavs-/autentiseringsvern:
[Emby videoavspeling](https://dev.emby.media/doc/restapi/Video-Streaming.html) og
[Coil-transformasjonar](https://coil-kt.github.io/coil/api/coil-core/coil3.transform/-transformation/index.html).
