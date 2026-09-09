# Nettbrett og engelsk — 9. september 2026

Arbeidsversjon: 0.16.0-alpha02 / 45. Vidareføring av språk-/nettbrettgrunnlaget i alpha01, ikkje ei ferdig TV- eller Play-utgåve.

## Nytt mønster: bibliotekframheving på nettbrett

**Behov:** Ei brei samling av like rader mangla eit tydeleg visuelt tyngdepunkt. Nettbrettet skal gi meir plass til ei historie, utan å finne på tilrådingar eller endre datakjeldene.

`TabletLibraryFeature` viser éin tittel frå ei synleg serie-/episoderad, med faktisk kjeldemerke, tittel, episode, omtale når ho finst, og ei handling som opnar detaljane. Ho sender ikkje førespurnader eller avspelingskommandoar.

| Tilstand | Åtferd |
| --- | --- |
| Brei medieside, tittel med bilete | Filmatiske bilete til høgre; tekst på mørk, lesbar flate til venstre. |
| Smalt vindauge | Ingen ekstra framheving; den eksisterande mobilflyten blir bevart. |
| Skjult bibliotekrad | Tittelen kan ikkje veljast frå denne rada. |
| Ingen kvalifiserte titlar | Ingen tom banner eller erstatning med demodata. |
| Stor skrift | Innhaldet får vekse; knappen har minimum 48 dp treffhøgd. |
| Oppdatering | Ingen tidsstyrt karusell eller gjenteken inngangsanimasjon. |
| Trykk | Opne nøyaktig bibliotek-ID i eksisterande detaljflyt. |

Fargar og rytme følgjer Spole: `Ink`, varm kvit, nøytrale kjeldetekstar, 24 dp bilethjørne og 28 dp indre marg. Overlegget finst for tekstkontrast, ikkje for glas/glød. CTA er ei kvit, roleg sekundærhandling; lime er framleis hovudfargen for handlingar elles. Biletet er dekorativt for skjermlesar, medan tittel, kjelde og knapp er tilgjengelege.

Nettbrettplakatar er 158 × 237 dp og episodar 292 × 164,25 dp. Lasteplasshaldarane brukar dei same biletmåla; den eksisterande hald-fram-rada bevarer sine eigne mål. Oppdag brukar større gridceller. Breie detaljpanel får 164 × 246 dp plakat og større tittel; telefonmåla blir bevarte. Manglande kunst får ei lågkontrast filmrute, ikkje den gamle HomeReel-huslogoen eller eit falskt mediebilete.

## Engelsk

54 nye ressursnøklar per språk dekkjer fleire Heim-/lastetilstandar, Oppdag-handlingar, Seerr-statusar, detaljoverskrifter, omtaleutviding og opning/avspeling. Statuspresentasjonen brukar den same prioriteringa som før; blokkert, delvis, ventande og tilgjengeleg blir ikkje slått saman. Ingen førespurnadsrettar er endra.

Engelsk er framleis førehandsvising. Tenartekstar og filmomtalar blir ikkje automatisk omsette. Nokre modell-, konto-, kalender- og varseltekstar står framleis att.

## Verifisering og skjermbilete

Ekte kontoar blir berre brukte til lesing og visuell kontroll på review-emulator 5560. Automatiske testar køyrer separat på 5562. Bilete med ekte kontodata blir verande i lokal ignorert byggmappe og blir berre delte med brukaren i denne oppgåva; ikkje lagde i Git eller releasevedlegg.

- [x] Bygg av debug, Android-test-APK og signert release-APK. Endeleg logg: `app/build/tablet-polish-2026-09-09/build-final.txt`.
- [x] 272 einingstestar, ingen feil. Lint: 0 feil, 28 åtvaringar.
- [x] Visuell kontroll på 1920 × 1200 / 240 dpi (1280 × 800 dp) med ekte Jellyfin-, Emby- og Seerr-kontoar, ikkje demo- eller mockupinnhald.
- [x] Heim: ekte Ted Lasso-bilete, episodetekst, omtale og innlogga profil. Engelsk Oppdag: faktiske Seerr-titlar og statusar. Detaljpanel: faktisk Moana/Vaiana-innhald med større plakat.
- [x] Sluttbygget vart installert med bevarte kontoar, nynorsk vart kontrollert i innstillingane og Heim-biletet teke på nytt. Framhevinga opna den faktiske Ted Lasso-episoden. Etter sveip ned ved toppen og sveip opp til omtalen låg lukkeknappen på same koordinatar; X vart brukt for å gå tilbake. Dette erstattar ikkje den uavklarte automatiske testen med halden peikar.
- [x] Språkbyte til engelsk for skjermbileta, deretter val av nynorsk igjen. Ingen førespurnader eller avspelingar vart starta i den visuelle kontrollen.
- [x] Full første Android-runde: 141 køyrde, 140 bestod. Éin test fanga at kjeldeteksten «Oppdag i Seerr» var erstatta av status. Kjeldeteksten er gjenoppretta; testkravet vart ikkje svekt. Logg: `android-full.txt`.
- [x] Endeleg bygg: alle 24 målretta mobiltestar bestod (ReelstackSmokeTest, HomeMediaRowsTest, SheetInteractionTest), inkludert den tidlegare feilen. Logg: `android-recheck.txt`.
- [ ] Brei 42-test-runde vart ikkje fullført: éin 1000 ms tidsavgrensa feil ved lukking med tastatur, deretter stans under testing med ein halden dragerørsle i SheetInteractionTest. Køyringa vart avbroten i den isolerte testappen. Dette blir ikkje registrert som ein fullstendig bestått nettbrett-popupkontroll. Logg: `android-tablet.txt`.
- [x] Separat nettbrettkontroll: 32/32 bestod på 1920 × 1200 / 240 dpi. Dekker framheving/stor skrift, status, dialogramme, lukking med tastatur, vindaugsbreidd, språk, Heim, Oppdag og konsistens. Den førre tastaturtidsavgrensinga vart ikkje gjenskapt. Dragerørsle-testen er ikkje med i denne køyringa og står framleis uavklart. Logg: `android-tablet-recheck.txt`.
- [ ] Fysisk nettbrett, full rotasjons-/multivindaugsflyt og full engelsk omsetjing står att. Ingen ny verifisering av Radarr-/Sonarr-kalender med ekte kontoar i denne runden.

Lokale skjermbilete i `app/build/tablet-polish-2026-09-09/`: `01-home-live.png`, `02-discover-live-en.png`, `03-details-live-en.png`. Review-emulatoren brukar mellombels nettbrettmål; opphavlege mål er 1280 × 2856 / 480 dpi og kan gjenopprettast med `wm size reset` og `wm density reset`.

Dette er ei lokal alpha02-utgåve med same pakke og eksisterande signeringsnøkkel. Ho er ikkje publisert på GitHub eller Google Play.
