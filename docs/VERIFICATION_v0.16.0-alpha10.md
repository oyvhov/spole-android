# Verifikasjon — Spole 0.16.0-alpha10

Dato: 10. september 2026. Lokal arbeidskopi; ingen ny GitHub-publisering. [Endringar og brukarflyt](SETTINGS_THEMES_ALPHA10.md).

## Produksjons-APK

- Pakke: `app.reelstack`, versjon `0.16.0-alpha10`, versionCode `53`.
- Signert med eksisterande Spole-nøkkel, R8-minifisert, ikkje debuggable. Minimum API 26, target API 36.
- APK: `app/build/test-alpha10/Spole-0.16.0-alpha10.apk`, 3 828 865 byte.
- SHA-256: `3c66c2339818162460bf738f34b99c5e268e044e0593f5e54a90a208911b1f0f`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping: `app/build/test-alpha10/mapping-0.16.0-alpha10.txt`.

## Automatiske kontrollar

- **317 JVM-testar, ingen feil.** Nye testar dekkjer blanda kronologisk rekkefølgje, duplikat, manglande dato, parsinga av sjåhistorikk, same profil og bibliotekavgrensing, og feil ved henting av seriehistorikk.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` og `assembleRelease` bestod på endeleg kjeldekode. Logg: `app/build/alpha10-final-build2.log`.
- Lint: **40 åtvaringar, ingen feil**. Ikkje eit åtvaringsfritt prosjekt.
- Fargekontroll av dei fire temaa: hovudtekst har minst **11,37:1**, sekundærtekst minst **5,48:1**, mot temaets bakgrunn, vanlege overflate og heva overflate. Dette er ein kontroll av desse fargepara, ikkje ein full tilgjengelegheitssertifisering av appen.
- APK-identitet, signatur og fråvær av debuggable-flagg er kontrollerte med Android-verktøya.
- **22/22 målretta TV-testar bestod** på endeleg kode. Omfattar kategoriar på faktisk TV-breidd, fjernkontroll, temalagring og tilbakestilling, menyredigering, navigasjon, tidlegare TV-forbetringar og bevaring av datoar gjennom mellomlageret. Logg: `app/build/alpha10-tv-final.txt`.
- **2/2 ekstra TV-testar bestod med systemskrift på 200 %.** Alle kategoriar og temaval var tilgjengelege. Logg: `app/build/alpha10-tv-large.txt`.
- Telefon: **226 ulike utførte Android-testar bestod**, med atterkøyringa nedanfor. Fullpakken hadde 234 tilfelle: 225 bestod, éin fekk tidsavbrot, og åtte TV-spesifikke tilfelle vart hoppa over. Deretter bestod heile panelklassen **6/6**, inkludert testen som hadde fått tidsavbrot. Loggar: `app/build/alpha10-phone-final.txt` og `app/build/alpha10-phone-sheet-rerun.txt`.

## Testisolasjon og avvik

Automatiske Android-testar brukar eigne emulatorprofilar med syntetiske kontoar. Review-profilane med ekte kontoar får berre signert produksjonsoppdatering; dei blir ikkje sletta eller instrumenterte.

Utviklingsrundane på TV avdekte to reelle fokusproblem: venstre frå eit langt innhaldspanel kunne velje feil kategori, og utskifting av eit fokusert panel kunne flytte fokus tilbake til ein annan kategori. Navigasjonen ankrar no fokus i rett kategori og held fokusgruppene skilde. Endeleg 22-testpakke bestod etter rettingane.

To telefonkøyringar vart avbrotne ved same test då emulatoren sin grafikkprosess fekk signal 11. WSL-loggen peikar på emulatorens `RenderThread`; desse køyringane er ikkje rekna som bestått. Å slå av Vulkan åleine løyste ikkje problemet. Med ANGLE-grafikkmotor og berre telefonemulatoren aktiv bestod heile den aktuelle `HomeMediaRowsTest`-klassen, **6/6**, utan endring av appen eller testane. Logg: `app/build/alpha10-phone-render-check.txt`.

Den påfølgjande fullkøyringa fullførte alle 234 tilfella. `SheetKeyboardFlowTest.touchOpeningDoesNotForceKeyboardFocus` nådde eittsekundsgrensa medan han venta på at panelet skulle lukke. Heile seks-testklassen bestod umiddelbar atterkøyring utan kode- eller testendring. Dette er registrert som eit ikkje reprodusert tidsavbrot; resultata blir ikkje framstilte som éi feilfri fullkøyring. Manuell kontroll av lukking på den signerte telefonappen kjem i tillegg.

## Signert oppgradering og ekte data

Både TV- og telefonemulatoren er oppgraderte med den arkiverte APK-en utan å slette data. Eksisterande innlogging og visingsval er bevarte. TV-Heim viste ekte titlar og framdrift i den kombinerte rada. Telefonen heldt på sitt separate radval, nynorsk og den eksisterande botnmenyen.

TV-innstillingane er visuelt kontrollerte på produksjons-APK-en. Heim-panelet viser bibliotekval, Next Up, kombinering, hovudbilete og heimrader samla. Endring frå Skog til Midnatt endra både bakgrunn, sidemeny, overflater og førehandsvising. Det opphavlege Skog-temaet vart sett tilbake etter kontrollen.

Telefonen viste dei nye temavala i den eksisterande mobilvisinga. Taskmaster frå Hald fram å sjå opna detaljar med 49 % framdrift, 25 minutt att og mediefakta. Berøring på Lukk fjerna panelet og returnerte til Heim. Produksjonspakka vart kontrollert som bygg 53 / alpha10. Ved kald oppstart av begge emulatorane gav Android-systemgrensesnittet på telefonen éi melding om at det ikkje svarte; etter Vent fungerte kontrollen normalt. Dette var System UI, ikkje ein rapportert Spole-krasj.

Begge emulatorane står opne i eigne vindauge, med dei nye utsjånadsinnstillingane viste. Dei har dei same ekte kontoane som før oppdateringa.

Private skjermbilete ligg lokalt under `app/build/alpha10-*`; dei er ikkje publiserte eller lagde i kjeldekontrollen.

## Avgrensingar

Fysisk TV og fysisk nettbrett er ikkje testa i denne økta. Dette er ikkje ein ny langtidskontroll av avspeling. Kronologisk plassering krev at tenaren har lagra sjåtidspunkt; titlar utan kjent dato blir verande synlege etter dei med kjent dato. Den faktiske fordelinga mellom påbyrja innhald og neste episodar varierer med profilen sin historikk.
