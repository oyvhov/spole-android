## Spole 0.11.0 · Nytt namn og enklare innlogging

- Nytt namn: **Spole**, med eit reint filmrute-S som appikon og støtte for temafarga Android-ikon.
- Kalenderikonet er fjerna frå toppen av framsida. Kalenderen er framleis tilgjengeleg under «Kjem snart».
- Samla kontovisning med Seerr-profilbiletet først og kompakte Jellyfin-/Emby-rader.
- Logg inn på Emby med vanleg brukarnamn og passord til Emby-tenaren.
- Valfri innlogging på Jellyfin og Seerr frå same skjema. Passordet blir ikkje lagra, og appen kontrollerer at kontoane høyrer saman før tilkoplingane blir lagra.
- Kopier Quick Connect-koden med eitt trykk. Quick Connect må framleis godkjennast for kvar teneste.
- Tydeleg «Logg ut» med stadfesting. Spole hugsar tenaradressa til neste gong; andre tenester blir verande innlogga.
- Enklare oppstart: tekniske tenarverktøy og API-nøklar er sekundære val.

Installer over førre Reelune/HomeReel-versjon. Same app-ID og signering er bevarte, slik at innloggingane dine blir med vidare. Dette er framleis ein debug-signert APK for direkte installasjon, ikkje ein Play Butikk-versjon.

Emby-innlogginga gjeld kontoen på Emby-tenaren, ikkje Emby Connect i nettskya. Felles Jellyfin/Seerr-innlogging er testa med lokale testtenester; ekte Emby-innlogging med brukarnamn/passord må fullførast med din eigen konto.

APK SHA-256: `ec04412dda7f11c05f5ff318d1098e71b6eddae44c243af5f13f461b7756b4ad`

Kontrollert publiseringsbygg: 145 einingstestar og alle 61 Android-testar bestått. Bygg og lint fullførte med 0 feil og 20 åtvaringar. Oppdatering i emulatoren bevarte dei eksisterande innloggingane.
