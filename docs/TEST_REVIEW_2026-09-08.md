# Testgjennomgang · 8. september 2026

Kjelde: commit 1669b5e, Spole 0.13.3. Ingen appkode er endra i denne gjennomgangen.

## Resultat

- JVM-testar køyrde på nytt med testDebugUnitTest --rerun: 203/203 bestod.
- Byggkontroll: assembleDebug, assembleDebugAndroidTest, lintDebug og bundleRelease bestod. Uendra byggoppgåver kunne bruke eksisterande resultat.
- Lint: 0 feil, 26 åtvaringar. Mellom anna maskinvare-ID, nettverksgrunnkonfigurasjon og avhengigheiter må vurderast før butikkinnsending.
- Full Android-testpakke på isolert emulator-5562: 82 testar, 78 bestod og 4 feila (311,583 sekund).
- Tre feil gjaldt Quick Connect-utklippstavla og to tilbakeknapp-testar. Android System UI hadde ein ANR-dialog som tok vindaugsfokus; dette vart stadfesta med skjermbilete og dumpsys window.
- Etter at systemdialogen var borte, bestod alle desse tre testane utan kodeendringar (11,488 sekund).
- Eitt attverande avvik: ReelstackSmokeTest.homeSectionsCanBeEditedInSettings leitar etter Emby-brytarane utan å opne «Tilpass framsida». Testen må oppdaterast til den nye flyten. UiConsistencyTest, som opnar gruppa før endring, bestod i fullkøyringa.
- Fullpakken er ikkje køyrd om att etter dette. Resultatet skal ikkje presenterast som 82/82 grøne i ei rein køyring.

## Ekte kontoar

Den faste Spole_Review-emulatoren (5560) vart starta med eksisterande data. Produksjons-APK 0.13.3 vart installert med -r og opna. Brukaren logga deretter inn på Jellyfin, Emby og Seerr direkte i emulatoren.

Visuelt verifisert med ekte data:

- Profilbilete og separate filmrader frå Jellyfin og Emby lastar.
- Jellyfin-filmen Green Zone opnar med poster, omtale, vurdering, spilletid og bibliotekstatus.
- Oppdag lastar titlar og søk etter Silo gir bibliotektreff.
- Avvik: Silo blir vist fleire gonger med svært like omslag og utan tydeleg skilje mellom treffa. Årsaka er ikkje diagnostisert; dette bør ryddast før lansering.
- Aktivitet viser eksisterande førespurnader med bilete og status, med «Mine» valt. Dette stadfestar ikkje tilgangskontroll for ein ikkje-administrator.
- Innstillingane viser tre HTTPS-tilkoplingar. Radarr og Sonarr er ikkje tilkopla i denne emulatoren; kalender/køar er difor ikkje verifiserte.

Ingen kontoar vart sletta, og ingen instrumenteringstestar vart køyrde på review-emulatoren. Ingen førespurnader vart sende eller trekte tilbake, og inga avspeling vart endra. Vanleg brukar/admin, førespurnad → nedlasting → bibliotek/varsel, utgåtte sesjonar og måling av popup-animasjonar står att. Skjermbilete med ekte data er berre lagra lokalt under app/build, ikkje publiserte.

## Konklusjon

Det automatiske grunnlaget er godt, og grunnflyten lastar ekte data frå Jellyfin, Emby og Seerr. Testen stadfestar ikkje at alle integrasjonar fungerer. Før Play Store-beta: oppdater den utdaterte testen, få ei rein fullkøyring på stabil emulator, undersøk dei utydelege søketreffa og fullfør dei nemnde ende-til-ende-testane. AAB-bygg åleine betyr ikkje Play Store-godkjenning eller verifisert 16 KB-kompatibilitet.
