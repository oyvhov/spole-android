# Kva Spole kan låne frå Swiftly Player

1. november 2026. Fire gjennomgangar av github.com/paulherter/swiftly-player (MPL-2.0: idéar og tal,
aldri kode). Kvart punkt vart sjekka mot Spole-koden før det kom med. Eigaren har bede om 1–7 og
det kosmetiske etter runden i `RORSLE_OG_TOPPMENY_2026-11-01.md`.

Status 2. november 2026, før beta31: punkt 1–7 og det kosmetiske er bygde og JVM-testa — søk på
TV med eige tastatur og historikk først ved inngang i treffa, hopp-over-reglane og lyd/undertekst
per serie (sjå «Hopp over» og «Lyd og undertekst per serie» i `JELLYFIN_PLAYER.md`), episoderada på
neste episode, éin framdriftsline, varselfargar per aksent, profilfargar, heildekkjande tekst over
bilete og plasshaldar i temaet. Kva som er køyrt på TV 5566, står i `VERIFICATION_v1.0.0-beta31.md`.

## Først

1. **Søk på TV med eige skjermtastatur** ved sida av resultata (`GlobalSearchScreen.kt` opnar IME over
   treffa). Bokstavrutenett til venstre, Tilbake går til siste tast. Middels–stort. **Bygd:**
   `TvSearchKeyboard.kt` (kort med spørjelinje og tastar), `SearchKeys.kt` (oppsett og skrivereglar).
2. **Rolegare søk** (`DiscoverSearchCoordinator.kt:69,87,89`): gamle treff står til nye kjem, eigen
   tenar før Seerr, historikk først når ein går inn i resultata. Lite. **Bygd:** historikken blir
   skriven av `commitSearchToHistory()` når eit treff får fokus eller blir opna (TV), når eit treff
   blir trykt eller søkehandlinga blir brukt (telefon), aldri for «Ga» og «Gam» på vegen til «Game».
3. **Lyd/undertekst hugsa per serie**, òg «av» (`JellyfinPlayerModel.kt:111,642`). Lite–middels.
4. **Hopp-over-reglar** (`JellyfinPlayback.kt:320`, `JellyfinPlayerActivity.kt:1087`): resymé og
   førehandsvising, knappen 6 s så berre i OSD, rulletekst til slutt → «Neste episode». **Feil
   stadfesta:** `SkipSegmentButton` melder ikkje `nextHasFocus`, så OK på TV gir pause, ikkje hopp.
5. **Episoderada opnar på neste episode**, sesongbyte tømmer ikkje lista (`DetailSeries.kt:207`,
   `DetailsCoordinator.kt:485`). Lite–middels.
6. **Éin framdriftsline** (fem variantar: `LibraryScreen.kt:266`, `SmartShelfPage.kt:476`,
   `HomeScreen.kt:1167,1468`, `MobileLibraryFeature.kt:347`). 4 dp, kvitt 30 % spor, aksentfyll.
7. **Varselfarge som følgjer aksenten**: Warning #FF7A7D ≈ HOLLY, Caution ≈ GOLD. Legg warning/caution
   på `AccentPalette`, minsteavstand i `AccentContrastTest`.
8. Release-sjekk på tvers av arbeidsmapper i `check-release-consistency.py`: fetch tags, høgaste
   tag < versionCode, greiner føre main, grep etter nøklar.
9. Einings-ID ut av overføring: `reelstack_device.xml` i `data_extraction_rules.xml`.
10. Logg i minnet (4 000 liner, token fjerna per line) + «meld feil»-lenkje.

## Søk på TV (punkt 1 og 2)

- **Tastaturet:** seks kolonnar, A–Å (Æ Ø Å etter Z), 0–9, bindestrek, mellomrom og slett, i tastar på minst
  48 dp utan fast høgd. Mikrofon på ei eiga rad berre når `SpeechRecognizer.isRecognitionAvailable` seier ja
  (`<queries>` for `RecognitionService` i manifestet). Mikrofonen opnar systemet sin eigen talerøyst, så
  ingen mikrofontilgang blir spurd om. Fokus er ein ring, som elles på TV.
- **Spørjelinja** er berre visning: ho tek aldri fokus, har ingen blinkande peikar og kuttar teksten framme,
  så slutten står att. Kvart ord får stor forbokstav (også tale og eit tilkopla tastatur).
- **Slett:** eitt trykk tek ein bokstav når tasten blir sleppt; OK halde inne i 500 ms tømmer heile søket.
- **Fokus:** Tilbake frå treffa går til siste tast, Tilbake frå tastane lukkar søk som før, Venstre frå treffa
  går til siste tast. Opp frå øvste rad kallar `onNavigateUp` (toppmenyen).
- **Tilkopla tastatur** (USB/Bluetooth): bokstavar, sifre, bindestrek, mellomrom og Backspace via
  `onPreviewKeyEvent` på søkesida. Teiknet kjem frå tastaturoppsettet, så Æ Ø Å verkar på nordisk oppsett.
- **Ikkje prøvd på eining:** fokusrørsle med ekte fjernkontroll, fokusrestaurering ved Venstre, taleinnskriving
  og Tilbake-rekkjefølgja mot `ReelstackApp` sin eigen handterar er dekte av Robolectric, ikkje av emulator.

## Kosmetisk

- Profilfargar frå namnet (åtte fargar, 45° frå kvarandre; `ProfileMenu.kt:291`, `AccountAvatar.kt:57`).
- Faste tekstfargar over bilete (#FFF/#CCC/#989898) i staden for kvitt 68–85 %
  (`TabletLibraryFeature.kt:398,511,521,529`, `HomeScreen.kt:815,1389`).
- `media_placeholder.xml` er fast FOREST-grøn; andre tema får grønlege plasshaldarar.
- Logoar i storleik etter tyngd (målareal 12 × line², 1–2,2 liner), lys silhuett for mørke logoar.
- Haptikk: tikk på brytarar og steg, klikk på spel; alltid på trykket, aldri på svaret.
- Test mot token-drift: 196 `fontSize` utanfor Typography, 50 `Color(0x…)` i ui/.
- Menyar forankra til knappen på nettbrett (`HomeScreen.kt:1204`, `LibraryFiltersUi.kt:288`).
- «Feila» ≠ «tomt» på detaljsider, offline-sjekk først (`ReelstackSheets.kt:527`).
- Angre når ein fjernar frå «Hald fram» (`MediaActionsCoordinator.kt:24`).

## Spelaren

- Neste episode frå tenaren sitt rulletekstmerke (kort ved start av rulletekst, 7 s nedtelling).
- TV-spoling: samle trykk (350 ms), hald 10/30/90 s per sekund (`TvPlaybackOverlay.kt:161`).
- Episodar inne i spelaren. Kapittel på tidslina + filtrere jamne «Kapittel N».
- Sovetimer. 5 s attende etter >10 min pause. Førebu neste episode siste 180 s.
- PiP ved Heim-gest på telefon. «−12:34» ↔ «Endar kl.». Neste-knappen på fjernkontrollen.
- DVB-undertekstar som innebygde i `DevicePlaybackProfile.kt:108` (sjekk Media3 først).

## TV og funksjonar

- «Hald fram her»: overtaking frå eiga anna eining (øktene finst alt i `ServiceClients.kt:330`;
  filtrer bort økter stille i >90 s, `LastActivityDate` blir ikkje lese i dag).
- Rader på tittelsida: «Del av samlinga», liknande frå eigen tenar, ekstramateriale.
- Alfabetstolpe ved «Etter namn» (stort). Neste episode i Google TV «Sjå vidare» (`WatchNextSync.kt`).
- Innstillingar: dim høgre rute til ein går inn; val inline. QR-kode for lenkjer på TV.
- Innstillingar som følgjer kontoen via `DisplayPreferences` CustomPrefs (GET, endre eige prefiks,
  POST alt; nyaste `changed`-stempel vinn). Aldri PIN.
- Brukarinstallerte sertifikat + namngje sertifikatfeilen. Eigne HTTP-hovud per tenar.
- «Kva er nytt» éin gong etter oppdatering. Gullliste over lagra nøklar.

## Vraka

Glød bak sidene, forstørring som einaste fokusmerke, topptekst som skiftar ved rulling,
personbanner kvart 6. sekund, Plex, SyncPlay, donasjonar, app-ikon knytt til kjøp,
60 s-grense før «hald fram», svak farge frå biletet bak detaljsider (fint, men stort og mot
«ingen dekorative gradientar»).
