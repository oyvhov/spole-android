# Spole 0.14.0

Ein publiseringsrunde. Appen er 69 % mindre, feilmeldingar som før var eit statusnummer forklarer
no kva du skal gjere, og ein krasj etterlèt for første gong eit spor det går an å lese.

## Mindre app

**2,2 MB, ned frå 7,1 MB.** 5,1 MB av det førre bygget var tre demobilete som ein tilkopla brukar
aldri ser. Dei er no WebP i staden for PNG, visuelt uskiljelege frå originalane.

## Feil som seier kva du skal gjere

- **Ei omdirigering forklarer seg sjølv.** Ein omvend proxy som sender `http://` vidare til
  `https://` gav før «Jellyfin svara med status 301». No står adressa tenaren faktisk svarar på
  rett i tilkoplingstesten, der du står i det feltet du må rette.
- **Ei avbroten handling er ikkje ein feil.** Lukka du eit popupark midt i eit kall, fekk du ei
  feilmelding for noko du sjølv valde å avbryte. Det skjer ikkje lenger.
- **Ei ulesbar innlogging seier frå.** Blir tilgangsteiknet ulesbart etter ei gjenoppretting til
  ei ny eining, sa Innstillingar før «Konfigurert» medan Heim stille fall tilbake til demoinnhald.
  No blir du beden om å logge inn på nytt.
- **`Retry-After` blir gjenteken.** Er tenesta oppteken og seier kor lenge, står talet i meldinga.
- Berre tekst som er skriven for deg, blir vist til deg. Ein intern feil kan ikkje lenger hamne i
  same felt som «Sjekk tenaradressa og nettet».

## Søk

**Same tittel blir vist éin gong.** Eit søk etter ein serie du har på både Jellyfin og Emby gav to
kort med same plakat og ingenting som skilde dei. Treffa blir no slegne saman på TMDB-ID, og
episodar forsvinn frå treffa når serien deira alt er der.

## Krasjrapport

Stoppar Spole uventa, blir det lagra ei fil på eininga med versjon, einingsmodell og kva som gjekk
gale. Tenaradresser og alt som liknar eit tilgangsteikn blir fjerna før fila blir skriven.
**Ingenting blir sendt nokon stad.** Innstillingar får ein «Del feilrapport»-knapp du sjølv
trykkjer på, og ein «Slett» ved sida av.

## Personvern

- Ny personvernerklæring i `docs/PRIVACY.md`: kva som blir lagra kvar, kva som er kryptert, og dei
  to adressene utanom dine eigne tenarar som appen kontaktar.
- **Einings-ID er ikkje lenger `ANDROID_ID`.** Det var ein varig maskinvarebunden identifikator som
  overlever avinstallering. No er det ein tilfeldig verdi per installasjon. Du blir ikkje logga ut:
  eksisterande installasjonar tek med seg ID-en dei alt hadde.
- **Tilrådingslista blir ikkje henta når rada er av.** Valet i Innstillingar stoppar no sjølve
  førespurnaden, ikkje berre visinga.
- Attribusjon for TMDB, og ei tydeleg fråskriving om at Spole ikkje er tilknytt Jellyfin, Emby,
  Overseerr/Jellyseerr, Radarr eller Sonarr.

## Elles

- Bakgrunnsoppdateringa gir opp etter fire forsøk i staden for å prøve ein daud tenar i det
  uendelege.
- Widgeten rekk å svare: tidsavbrotet er 8 sekund i staden for 12, med eigne kortare tidsavbrot
  mot tenaren.
- Eitt nytt forsøk på GET når det første går tapt. POST blir aldri prøvd på nytt.
- Oppstarten les ikkje lenger innstillingar frå disk på hovudtråden.

## Kontrollar

- 225/225 einingstestar, 82/82 Android-testar i ei rein køyring, 0 lint-feil.
- Oppgradert frå 0.13.3 over ekte innlogga kontoar: alle tre tenestene heldt innlogginga.
- 16 KB-sidestorleik verifisert, både APK-justering og ELF-segment.
- Same signeringsnøkkel som før. APK-en kan installerast rett over 0.13.3.

Sjå `docs/VERIFICATION_v0.14.0.md` for fullstendige tal og for kva som *ikkje* er verifisert.
