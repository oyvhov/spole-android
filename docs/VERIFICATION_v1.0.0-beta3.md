# Verifisering av Spole 1.0.0-beta3

5. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 140.

## Testgrunnlag

- Personleg kalender bruker Seerr, med datoar frå seriedetaljar/sesongruter og digital utgjevingstype 4 for film. Det finst ingen direkte Sonarr-/Radarr-klient eller API-rute i appen.
- Regeltestane dekkjer fleire episodar i same sesong, sesongbyte, episodebatch, ukjende/flytta datoar, date-only-grenser, digital dato og region, delvis feil, 429, 401/403 og konto-/profil-/cookiescope. Ein kalender med 86 episodar og eigne førespurnader over fleire sider blir ikkje avkorta.
- Migreringstestane bruker det gamle lagringsformatet, hovud- og barneprofil, krypterte nøklar, gjentaking og avbrot. Jellyfin/Seerr blir bevarte. Ukjende cached kjelder blir droppa utan å bli omdøypte til Jellyfin.
- Kalendercache-testen bevarer 85 hendingar med TMDB-ID, sesong, episode, dato, region og ekte bibliotekreferanse.
- Reelle lesetestar på lagra TV-profil 5564 gav 57 daterte hendingar utan kalenderfeilmelding. Ein konkret episode frå «71° nord» viste S18 E13 og 6. oktober frå Seerr, med planlagd utgjeving og utan oppdikta avspelingsknapp.
- Følgje/skjuling vart prøvd på den same tittelen og gjenoppretta. Ingen førespurnad vart sendt, og ingen tenarteneste vart endra.
- Automatiske Android-testar bruker isolert mobil 5562 og TV 5566. Ekte review-profilar 5560/5564 blir ikkje instrumenterte, sletta eller nullstilte.

## Avgrensingar

Den tilgjengelege ekte Seerr-sessionen er brukt til lesetest. Ordinære kontoar og eigarskap er dekte av syntetiske session-/transporttestar; ingen ekstra ekte ordinær konto eller fysisk telefon/TV vart brukt. Spesialsesong 0 er utelaten, manuelle følgjeval er lokale på kvar eining, og kalenderen lovar premieredato — ikkje lokal eller norsk strøymetilgjenge. Seerr sine tenartenester for automatisk henting er framleis nødvendige for den funksjonen.

## Endeleg kandidat

- 857 einingstestar bestod, ingen feil. Kalenderklienten har 16 regeltestar, inkludert metadata utan `mediaInfo` og over 100 eigne førespurnader.
- Isolert TV: 39 testtilfelle, 35 beståtte og 4 hoppa over fordi dei gjeld touch-/mobiloppsettet; ingen feil.
- Isolert mobil: 39 testtilfelle med 4 TV-tilfelle hoppa over. 34 bestod i samla køyring; den attståande kalender-returtesten trefte Espresso sitt vindaugsfokusproblem. Testen bruker no arket si dismiss-handling, som går gjennom same returkode. Alle 7 kalender-/detaljtestane bestod i ny køyring, inkludert retur, filter, dobbel skrift og stabil detaljvising. Dermed er alle 35 relevante mobiltestar beståtte.
- TV-returtesten brukte faktisk Back-tast. Mobiltesten kontrollerer dismiss/toolbar og tilstand; han skal ikkje omtalast som eit prov på fysisk tilbakegest.
- Lint: 0 feil, 188 åtvaringar og 1 hint. Ingen lint-regel vart slått av. Ein Kotlin/lint-feil i analysen av enum-konstruktørfelt vart unngått med ein vanleg enum-getter; sluttanalysen lykkast.
- Språkressursar: 1440 standardnøklar, 1 tillaten standardnøkkel utan omsetjing; ingen manglande nynorsk-/bokmålsnøklar. Versjonskonsistens og diffkontroll bestod.
- 639 kjelde-/byggfiler er frosne før det endelege releasebygget. Loggar og skjermbilete er lokale og blir ikkje publiserte med konto- eller emulator-data.

## Signert produksjonsartefakt

- Endeleg bygg avslutta med `BUILD SUCCESSFUL`, kode 0, etter 8 minutt 10 sekund. Ingen frosne kjelder vart endra.
- Universal-APK: 11 490 338 byte; `app.reelstack`, kode 140, `1.0.0-beta3`, minSdk 26 og targetSdk 36; ikkje debuggable.
- SHA-256: `4fa83253a2318118fcb39f0909f5f63245355bc5099eba9cf9b0d49cc2e10ee0`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Nøyaktig R8-mapping: 89 117 114 byte. APK, mapping og FFmpeg-arkiv er frosne i `app/build/release-v1.0.0-beta3`.
- Alle åtte native bibliotek og begge FFmpeg-lisensfilene er byteidentiske med beta2. Tilhøyrande kjelde-/relenkingsarkiv er 30 790 595 byte, SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Endeleg signert APK er installert med `install -r` på lagra TV-profil 5564. Kontoar og ekte Jellyfin-/Emby-innhald er bevarte. Første installasjonstid er framleis 9. september 2026 kl. 20:50:58.
- Endeleg kandidat viste 66 hendingar frå den eksisterande Seerr-kontoen. Kalenderen er kontrollert visuelt ved normal skrift og 2.0. Fjernkontrollen rullar til heile kortet med synleg fokus; premieredetaljar og Back går tilbake til same agenda/kort. Systemskrifta er sett tilbake til den opphavlege verdien 1.0.

Offentleg release og oppdatering gjennom appen blir dokumenterte etter publisering. Mobilprofil 5560 er framleis på beta2 for den ekte nedlastings-/installasjonstesten.
