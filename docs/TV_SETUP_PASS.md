# TV-oppsett og innlogging — 9. september 2026

## Endra

- Eiga startside for TV: Spole og kort hjelp til venstre; Jellyfin, Emby og Seerr til høgre. Ingen stor dekorasjon framfor tenestevala.
- Jellyfin får fokus ved første opning. Piltastar flyttar fokus; OK opnar. Fokus er ei teikna ramme, ikkje skalering som flyttar nabokort.
- Når ei teneste er konfigurert, er «Opne oversikta mi» første fokuserte handling. Demo forsvinn; andre tenester kan framleis leggjast til.
- Administratorverktøy ligg under eit eksplisitt val. Lange tekstar og 2× skrift kan rullast; ingen fast teksthøgd.
- På TV vel ny Seerr-tilkopling Quick Connect etter adressesteget. Jellyfin har alt Quick Connect som standard. Konto/passord er framleis tilgjengeleg; Emby bruker lokal konto som før.
- TV har eige kodepanel med lesbar kode og nummererte godkjenningssteg på mobilen. Ingen animert storleik når koden kjem. Kopiering og ventestatus er bevarte.
- Adresseframhald, metodeval, kodekopiering, adresseeksempel og innsending har synleg fjernkontrollfokus. Brukarnamnets Neste-handling går vidare i skjemaet.
- Nye tekstar og adressehjelp finst på nynorsk og engelsk. Resten av engelsk er framleis førehandsvising.

## Tryggleik og avgrensing

Ingen nye innloggingstenarar, QR-overføring av passord, automatisk deling av tilgangsteikn eller lagring av passord. Same autentiseringscallbackar og serverkontrollar som før. Seerr Quick Connect krev støtte på Seerr-tenaren; kontoinnlogging er alternativet.

Kontrollert på isolert Google TV API 36-emulator (5564), ikkje ei omforma telefonflate. 13 målretta Android-testar: fire nye TV-oppsettstestar og ni eksisterande innloggingstestar. Dei sjekkar UI og syntetiske kontoar, ikkje ei ny innlogging med brukaren sitt verkelege passord. Ekte kontoar på review-eininga 5560 er ikkje endra.

Fysisk TV, full reell Quick Connect-godkjenning og TV-launcher/store-pakking står att. Dette er kjeldearbeid utan ny publisering eller versjonsauke.

Sluttkontroll: debug- og test-APK bygde; 283 JVM-testar og 13 Android-testar bestod på endeleg kjelde. Lint: 0 feil, 32 åtvaringar. Midlertidig tom `values-en`-mappe utløyste falske manglande-omsetjingar under arbeidet; ho er fjerna, og endeleg ressursstruktur følgjer prosjektet sitt engelske grunnspråk og `values-b+nn`.

Lokalt bilete frå faktisk TV-emulator: `app/build/tv-welcome.png` (engelsk førstegongsoppsett, ingen kontoar).
