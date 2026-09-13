# Kom i gang med Spole

Denne guiden gjeld innlogginga i 0.16.0-alpha24. Funksjonar for neste APK er merkte nedanfor.

## For deg som skal sjå

1. Installer APK-en frå [Spole sine utgåver](https://github.com/oyvhov/spole-android/releases).
   Android kan be deg tillate installasjon frå nettlesaren eller filappen du brukar.
2. Opne Spole og vel **Kom i gang**.
3. Har du fått ei oppsettslenkje, opne henne eller vel **Eg har ei oppsettslenkje** og lim henne inn.
   Kontroller tenesteadressene. Utan lenkje kan du skrive adressene sjølv.
4. Vel **Godkjenn på mobilen**. Godkjenn den viste Quick Connect-koden i ein Jellyfin-klient
   der du allereie er innlogga med kontoen du vil bruke. Du kan også velje brukarnamn og passord.
5. Spole opnar Heim når innlogginga er stadfesta. Dersom Seerr er vald i oppsettet,
   blir den personlege Seerr-innlogginga ordna i same flyt når tenaren støttar det.

På TV kan fjernkontrollappen på mobilen vere lettare å bruke til tekstinntasting.
Oppsettslenkja er ikkje ei innlogging i seg sjølv; du må godkjenne din eigen konto.

### Om det stoppar

- **Lenkja opnar ikkje appen:** installer Spole først, og prøv å lime lenkja inn i oppsettet.
- **Tenaren svarar ikkje:** kontroller adressa og at telefonen eller TV-en har tilgang til tenaren sitt nettverk.
- **Quick Connect er ikkje tilgjengeleg:** prøv brukarnamn og passord, eller kontakt den som driftar tenaren.
- **Jellyfin fungerer, men Seerr feilar:** Seerr må vere kopla til same Jellyfin-tenar og støtte innloggingsmåten.
  Du kan slå av Seerr i oppsettet og logge inn berre på Jellyfin, eller bruke separate innloggingar.

## For deg som deler appen

1. Opprett ein eigen Jellyfin-konto til kvar brukar. Gi berre tilgang til biblioteka dei skal bruke.
2. Dersom dei skal kunne be om innhald, set opp Seerr mot same Jellyfin-tenar og kontroller brukarrettane der.
3. Prøv tenesteadressene frå nettverket mottakaren skal bruke. Ei lokal adresse heimanfrå fungerer ikkje automatisk utanfor heimen.
4. I Spole, opne Jellyfin under **Innstillingar → Tenester**. Vel **Del oppsett med ein brukar** eller **Kopier oppsettslenkje**.
5. Send mottakaren [APK-lenkja](https://github.com/oyvhov/spole-android/releases) og oppsettslenkja.
   Mottakaren loggar inn med sin eigen konto.

Oppsettslenkja inneheld Jellyfin-adressa og eventuell Seerr-adresse. Ho inneheld ikkje passord,
tilgangsteikn eller brukarøkt. Adressene kan likevel vere private: del lenkja med mottakarane,
ikkje som eit offentleg skjermbilete. Ikkje del administratorteikn frå Radarr, Sonarr eller Seerr med vanlege brukarar.

## TV, nettbrett og telefon

- **TV:** bruk retningsknappane og OK. Tilbake går ut av panelet eller skjermen. TV har eigne innstillingskategoriar.
- **Nettbrett:** breie vindauge får sidanavigasjon og meir plass til innhald.
- **Telefon:** botnnavigasjon og innstillingar i eigne grupper.

Tema, aksentfarge, sesongtema og synlege Heim-rader kan tilpassast i innstillingane.
På TV finst også val for å skjule sidepanelet til du navigerer ut mot venstre kant.

## Nytt i kjeldekoden, kjem i neste APK

- **Rekkjefølgje på Heim:** Innstillingar → Heim → Rekkjefølgje. Flytt radene med pilene; valet blir lagra.
- **Filmvurdering:** ei diskret stjerne og poengsum på filmkortet når tenaren har ei vurdering.
  Valet for å vise vurderingar blir respektert.
- **Tenestestatus:** ikon og OK-/avviksstatus under Tenester i innstillingane.
- **Logg ut av alle tenester:** eitt trykk under Tenester loggar ut på denne eininga og viser oppstarten att.
  Utsjånad og Heim-oppsett blir bevarte. Det loggar ikkje ut andre einingar og slettar ikkje serverkontoane.

## Oppdateringar og avgrensingar

Bruk appen sin oppdateringssjekk eller installer ein nyare APK frå Releases over den gamle.
Du treng ikkje avinstallere først. Alpha-utgåver kan innehalde feil.

Den innebygde spelaren er førebels for Jellyfin. Emby-bibliotek og Emby-innlogging finst,
men innebygd Emby-avspeling er ikkje levert. Barnemodus, QR-vising av oppsettslenkjer
og korte oppsettkodar er heller ikkje implementerte.

[Tilbake til Spole](../README.md)
