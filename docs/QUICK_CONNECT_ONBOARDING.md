# Enkel oppstart med Jellyfin og Seerr

Førstevalet er «Jellyfin + Seerr». «Andre innloggingsmåtar» opnar dei eksisterande
vala for Jellyfin, Emby, Seerr og administratortenester. Brukarar med lagra
tilkoplingar får ikkje oppsettet på nytt.

## Flyt

1. Skriv Jellyfin- og Seerr-adresse. Ingen passord eller API-nøkkel.
2. Godkjenn den viste koden i ein allereie innlogga Jellyfin-klient.
3. Spole stadfestar Jellyfin-identiteten, startar Seerr si eiga Quick Connect-økt,
   kontrollerer heile utfordringa på den valde Jellyfin-tenaren før godkjenning og stadfestar at Seerr sin
   `mediaUserId` samsvarer. Begge øktene blir lagra gjennom eksisterande kryptert
   lagring, og heimesida opnar.

Jellyfin-teiknet går berre til Jellyfin. Seerr får si eiga økt og sine eigne
rettar. Namn og e-post blir aldri brukte for å matche kontoar. Nettverksfeil eller
feil identitet før lagring gir ingen nye lagra tilkoplingar. Avbryt stoppar
polling; ein forseinka respons skal ikkje lagrast etter at brukaren har gått ut.

Dette krev Seerr med Quick Connect-støtte, kopla til same Jellyfin-tenar.
Ustøtta oppsett kan bruke dei separate innloggingane. Det er ikkje barnemodus
eller profilbyte. Eksisterande kontoar blir ikkje automatisk kopla saman.

## Vidare forenklingar, ikkje implementerte

- Ei personleg oppsettslenkje eller QR-kode med berre serveradressene, utan
  innloggingsteikn. Då slepp TV-brukaren å skrive lange adresser.
- Ei mobilside for å skrive adressene til TV-en. Ho må krevje ei kortvarig
  paring og ikkje eksponere innlogginga på heile lokalnettet.
- Kontroller tilgjenge og Quick Connect-støtte før kode-steget, med konkrete
  feil for kvar teneste. Unngå ekstra kontrollkall ved kvar tast.
- Hent første synlege innhaldsrad før resten; medverkandebilete og andre
  sekundære bilete får låg prioritet på treige TV-ar.
- Vis tema, bibliotekrekkjefølgje og andre personlege val etter første
  vellukka avspeling, slik at oppstarten held seg kort.

## Verifisering

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
