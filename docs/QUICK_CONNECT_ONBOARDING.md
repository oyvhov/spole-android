# Enkel oppstart med Jellyfin og Seerr

Førstevalet er «Kom i gang». «Andre innloggingsmåtar» opnar dei eksisterande
vala for Jellyfin, Emby, Seerr og administratortenester. Brukarar med lagra
tilkoplingar får ikkje oppsettet på nytt.

## Flyt

1. Opne ei oppsettslenkje, lim henne inn i oppsettet, eller skriv adressene sjølv.
   Seerr er valfri og kan slåast av før innlogging.
2. Vel «Godkjenn på mobilen» i ein allereie innlogga Jellyfin-klient, eller
   «Brukarnamn og passord» med din eigen Jellyfin-konto.
3. Når Seerr er vald, stadfestar Spole Jellyfin-identiteten, startar Seerr si eiga Quick Connect-økt,
   kontrollerer heile utfordringa på den valde Jellyfin-tenaren før godkjenning og stadfestar at Seerr sin
   `mediaUserId` samsvarer. Begge øktene blir lagra gjennom eksisterande kryptert
   lagring, og heimesida opnar.

Ved passordinnlogging blir dei eksisterande personlege innloggingane brukte,
med same kontroll av Jellyfin-/Seerr-identitet før lagring. Vellukka oppsett
går rett til heimesida, også når brukaren vel berre Jellyfin.

Jellyfin-teiknet går berre til Jellyfin. Seerr får si eiga økt og sine eigne
rettar. Namn og e-post blir aldri brukte for å matche kontoar. Nettverksfeil eller
feil identitet før lagring gir ingen nye lagra tilkoplingar. Avbryt stoppar
polling; ein forseinka respons skal ikkje lagrast etter at brukaren har gått ut.

Dette krev Seerr med Quick Connect-støtte, kopla til same Jellyfin-tenar.
Ustøtta oppsett kan bruke dei separate innloggingane. Det er ikkje barnemodus
eller profilbyte. Eksisterande kontoar blir ikkje automatisk kopla saman.

## Del oppsett

Opne Jellyfin under tilkopla tenester og vel «Del oppsett med ein brukar» eller
«Kopier oppsettslenkje». Lenka inneheld berre lagra Jellyfin- og eventuell
Seerr-adresse. Ho inneheld ingen konto, passord, tilgangsteikn eller økt.
Mottakaren må ha Spole installert. Støttar ikkje meldingsappen `spole://`-lenkjer,
kan mottakaren lime lenkja inn under «Eg har ei oppsettslenkje» i oppsettet.
På TV kan tekst limast inn med fjernkontrollappen på mobilen der denne støttar det.

Adressene blir synlege før brukaren sjølv startar innlogging. Importen gjer ingen
nettverkskall og byter aldri ut eksisterande kontoar. Ukjende/gjentekne felt,
innloggingsdata i URL, feil skjema og offentleg HTTP blir avviste. Kvar teneste
brukar framleis si eiga verifiserte brukarøkt. Ved Seerr-feil kan brukaren slå av
Seerr og prøve Jellyfin-innlogging på nytt; det blir ikkje lagra ein halv felles
innlogging automatisk.

Importerte adresser blir viste som eit kort samandrag med «Endre». TV-fokus
startar på «Godkjenn på mobilen». Passord er eit eige alternativ rett under.
Lenkjer opnar same aktivitet når appen alt er open; pågåande innlogging blir
ikkje avbroten av ei ny lenkje. Varsellenkjer til Aktivitet er framleis handterte.

Korte oppsettkodar og mobil-til-TV-paring er ikkje innførte: dei krev ei eiga
teneste for oppslag eller ei kontrollert lokal paring. Denne løysinga krev inga
ny drift eller sentral lagring av serveradressene. QR-vising er ikkje bygd.

## Vidare forenklingar, ikkje implementerte

- QR-vising av oppsettslenkja for enkel deling mellom einingar.
- Ei mobilside for å skrive adressene til TV-en. Ho må krevje ei kortvarig
  paring og ikkje eksponere innlogginga på heile lokalnettet.
- Kontroller tilgjenge og Quick Connect-støtte før kode-steget, med konkrete
  feil for kvar teneste. Unngå ekstra kontrollkall ved kvar tast.
- Hent første synlege innhaldsrad før resten; medverkandebilete og andre
  sekundære bilete får låg prioritet på treige TV-ar.
- Vis tema, bibliotekrekkjefølgje og andre personlege val etter første
  vellukka avspeling, slik at oppstarten held seg kort.

## Verifisering av oppsettslenkjer og passordval · 13. september 2026

- 465/465 einingstestar, 0 feil. Fire nye testar dekkjer lenkjeflyt, valfri Seerr,
  avvising av innloggingsdata og ukjende/gjentekne felt.
- 10/10 TV-testar (29,913 s) og 18/18 mobiltestar (48,838 s), ingen hoppa over.
  Testane dekkjer import utan automatisk innlogging, redigering av importerte
  adresser, passordval, valfri Seerr, stor tekst 2.0 og eksisterande oppsett.
  Sju nettverks-/konto-integrasjonstestar bruker berre lokale syntetiske tenarar;
  to av dei kontrollerer den nye enkle passordflyten og vern mot kontooverskriving.
- Android sine eksporterte lenkjer er prøvde ved både kald og varm oppstart på
  TV og mobil. Det siste opnar same aktivitet og viser den nye syntetiske adressa.
- Endeleg bygg: `app/build/setup-link-final-build.log`, 0 lint-feil og 31
  eksisterande åtvaringar. Testloggar: `app/build/setup-tv-tests.log` og
  `app/build/setup-phone-tests.log`. Visuell kontroll av kompakt oppsett og stor
  tekst er utført med syntetiske skjermbilete i `app/build/setup-*-images/`.
- Første TV-test stoppa på Compose sin venting medan tekstmarkøren blinka.
  Testhjelparen styrer klokka under innskriving og flyttar fokus ut av feltet før
  vanleg venting. Innskriving og feltinnhald blir framleis prøvde og stadfesta.
- Berre isolerte instrumenteringsprofilar (5562/5566) er brukte. Ingen ekte
  kontoar er endra. Innlogging mot brukaren sine verkelege tenarar og deling via
  ei ekstern meldingsapp er ikkje prøvde. QR og korte oppsettkodar er ikkje bygde.
- Ingen ny APK-release er publisert. Alpha23 er uendra.

## Førre verifisering (før oppsettslenkjer og passordval)

- 461/461 einingstestar bestått, inkludert åtte nye testar for sams server,
  kontomatching, manglande støtte, avvist godkjenning, avbrot og tidsavgrensing.
- 8/8 TV-testar og 9/9 mobiltestar bestått på isolerte profilar. Dei dekkjer den
  nye flyten og dei eksisterande innloggingsvala. Endelege loggar ligg lokalt i
  `app/build/setup-tv-tests.log` og `app/build/setup-phone-tests.log`.
- Velkomst, adressefelt, kode og avbryt er gjennomgått visuelt. Skriftstorleik
  2.0 er testa på begge skjermtypane. Skjermbileta inneheld berre syntetiske data.
- Lint: 0 feil, 31 eksisterande åtvaringar. Verifisert hovudbygg:
  `app/build/combined-setup-verified-build.log`. Siste visuelle justering er
  avrunding av adressefelta; `app/build/setup-final-ui-build.log` byggjer denne.
- Nettverksflyten er testa med syntetiske API-responsar. Éi samla godkjenning
  mot brukaren sine verkelege tenarar er ikkje prøvd. Ingen kontoar er bytte
  eller sletta på dei innlogga review-profilane.
- Dette arbeidet er ikkje publisert som ny APK; alpha23 er uendra.

API-grunnlag:
- [Jellyfin Quick Connect-kontroller](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/QuickConnectController.cs)
- [Seerr Quick Connect](https://docs.seerr.dev/api/initiate-jellyfin-quick-connect/)
