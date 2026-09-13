# TV: oppfølging av tolv punkt frå brukartest

Dato: 13. september 2026. Byggjer vidare på `v0.16.0-alpha20` (`d804297`).
Gjennomgangen vart først levert som lokal testversjon. Endringane er deretter tekne med
i `v0.16.0-alpha21`; sjå [release-verifiseringa](verification-v0.16.0-alpha21.md) for endeleg APK-identitet og publiseringskontroll.

## Endringar

| Punkt | Resultat |
| --- | --- |
| 1. Sesongtema | Statiske granbar, julelys, spøkelse, edderkoppar og spindelvev i appbakgrunnen, sidemenyen og TV-detaljsida. Innstillingane slepper bakgrunnen gjennom. Pynt kan framleis slåast av. |
| 2. Lastesymbol i hero | TV bruker ikkje mobilens dra-for-å-oppdatere-lag. Lastesymbolet kan derfor ikkje leggje seg over heroen. |
| 3. Lang radtittel | Den samla rada heiter **Sjå vidare**. |
| 4. Sjå meir | Ingen ekstra aksentfarga fokusramme på hero-knappen. Knappen kan framleis nåast og aktiverast med fjernkontrollen. Fokusasserten på denne knappen er teken ut av testen. |
| 5. Spelar | Eiga TV-utforming med transportikon, tydeleg spolelinje og korte verktøyknappar. Pil ned går frå avspeling til spolelinje og vidare til lyd/tekst/kvalitet. Pil venstre/høgre spol direkte når kontrollane er skjulte. Raske trykk blir samla før søk i straumen. Mellomlagring opnar ikkje automatisk heile OSD-en og flyttar ikkje fokus frå spolelinja. |
| 5. Neste episode | Lite, mørkt kort i hjørnet med dempa knappar. Nedtelling startar ved valt tidspunkt før slutten, dersom automatisk neste episode er på. Pause stoppar nedtellinga; spoling ut av tidsvindauget nullstiller henne. **Sjå ferdig** avbryt tilbodet for episoden. Avslutta film utan neste episode behaldar avspelingskontrollane. |
| 6. Denne eininga | Aktive økter frå same installasjons-ID blir filtrerte bort. Andre einingar med likt namn blir framleis viste. |
| 7. Oppdag | Tittel, metadata og handling/status ligg inne nedst i omslaget. Ingen separat tekstrad under. Handlings- og statusfelta bruker lik minimumshøgd. |
| 8. Aktivitet | Tittel og status er flytta inn i omslaget med mørk overgang for lesbarheit. På TV ligg varsel og trekk-tilbake i ein liten valmeny på omslaget. Stadfesting ved tilbaketrekking er bevart. |
| 9. Fokusfelt | Filterknappens synlege flate og fokusramme bruker same høgd. Den store usynlege margen rundt ein mindre chip er borte. |
| 10. Bibliotek | Hovudknappen Bibliotek opnar rota, også etter at eit festa bibliotek har vore opna. |
| 11. Filmdetaljar | Mindre, forankra omslag til venstre. Fakta, handlingar, omtale og sporval er samla i lesekolonnen til høgre. Automatisk fokus på spel av flyttar ikkje overskrifta ut av skjermen. |
| 12. Favorittfilmar | Oppslag etter detaljar og oppdatering av sett-/favorittstatus inkluderer favorittlista, også når filmen ikkje finst i nyleg lagt til eller søkjeresultat. |

Ved skriftstorleik 2.0 bruker mobilen ein brei menyknapp med namnet på den opne sida.
Alle synlege menyval ligg i den tilhøyrande lista, slik at namna ikkje bryt midt i orda.
Normal skriftstorleik behaldar botnmenyen med fem ikon.

## Grunnlag for spelarflyten

[Android TV sine avspelingsråd](https://developer.android.com/training/tv/playback/controls)
beskriv venstre/høgre som direkte spoling og opp/ned som tilgang til kontrollane.
Den nye fjernkontrollflyten følgjer dette. Kortet før neste episode er inspirert av
[Plex sin post-play-flyt](https://support.plex.tv/articles/202605013-play-queue-post-play-screen/)
og [Netflix sitt val for automatisk neste episode](https://help.netflix.com/en/node/121518).
Spole bruker brukarens tidsinnstilling; dette innfører ikkje automatisk deteksjon av rulletekst.

## Visuell kontroll

Utgangspunktet var dei to fotografia frå den fysiske TV-en, saman med skjermbilete frå
den eksisterande TV-emulatoren med ekte Jellyfin-/Seerr-data. Kontoar er bevarte.

Lokale bilete ligg under `app/build/` og skal ikkje leggjast i Git:

- `tv-pass2-before-movie.png` og `tv-pass2-after-movie.png`: mindre omslag og lesbar detaljside.
- `tv-pass2-after-home.png`: kort radtittel og roleg hero-knapp.
- `tv-pass2-after-discover.png` og `tv-pass2-after-activity.png`: informasjon inne i omslaga.
- `tv-pass2-filter-focus.png`: fokusramme som følgjer filterknappen.
- `tv-pass2-christmas-settings.png` og `tv-pass2-halloween-settings.png`: sesongtema i bakgrunn og meny.
- `tv-pass2-player-seek-live.png`, `tv-pass2-player-timeline-live.png` og `tv-pass2-player-audio-live.png`: direkte spoling og OSD med ein faktisk film.
- `mobile-pass2-menu-large.png` og `mobile-pass2-settings-large.png`: lesbare menyval og innstillingar ved skriftstorleik 2.0.
- `mobile-pass2-discover-large.png` og `mobile-pass2-activity-large.png`: tekst inne i omslaget ved skriftstorleik 2.0, utan ekstra svart botnfelt.

## Yting og avgrensingar

Ny bakgrunnspynt er statisk vektorteikning utan nettverkskall, tidsstyrt animasjon eller blur.
TV-detaljsida bruker eit mindre omslag utan blur. Direkte spoling ventar 180 ms etter
siste raske piltrykk før ho sender posisjonen til spelaren. Dette reduserer omstartar av
søk i straumen når brukaren trykkjer fleire gonger raskt.

Gjennomgangen omfattar emulator, automatiske testar og ekte videoavspeling. Ho er ikkje
ei FPS- eller minnemåling på brukarens fysiske, treige TV. Server, nettverk og videokodek
kan framleis påverke kor raskt videoen kjem att etter spoling.

## Verifisering

- 450/450 einingstestar bestod, utan feil eller hoppa-over testar.
- 37 ulike TV-testar bestod: først 35 testar for navigasjon, layout, innstillingar,
  favorittfilm, bibliotek og avspeling; deretter 14 spelar-/OSD-testar etter siste
  fokusretting, inkludert to nye tilfelle. Gjentekne testar er ikkje talde dobbelt.
- 24 ulike mobiltestar bestod for aktivitet, spelar, innstillingar og personalisering.
  Første løp fekk éin vindaugsfokusfeil medan Android viste «System UI isn't responding».
  Etter at systemdialogen vart lukka, bestod alle tre innstillingstestane utan kodeendring.
- `lintDebug`: 0 feil, 30 åtvaringar.
- `assembleDebug`, `assembleDebugAndroidTest` og signert `assembleRelease`: vellukka.
- `git diff --check`: rein.

Dei siste justeringane av stor mobilskrift og omslagsproporsjonar er i tillegg kontrollerte
manuelt i den signerte APK-en. Begge emulatorane med ekte kontoar er oppdaterte med `install -r`.
TV er sett tilbake til Skog/Korall, mobil til skriftstorleik 1.0 med Skog/Lime.
Dei isolerte instrumenteringsemulatorane er stoppa; dei to innlogga profilane er opne.

Lokale loggar: `app/build/pass2-delivery-build.log`, `pass2-tv-final-tests.log`,
`pass2-tv-osd-recheck.log`, `pass2-mobile-final-tests.log` og `pass2-mobile-settings-retry.log`.

Lokal test-APK: `app/build/outputs/apk/release/app-release.apk`, 4 000 322 byte.
Pakken er `app.reelstack`, framleis versjonskode 63 / `0.16.0-alpha20` fordi denne
oppfølginga ikkje er ein ny release. APK-en er derfor **ikkje identisk** med publisert alpha20.

- SHA-256: `c21c018ae0a8dcf5963404a67c9d19aba134a760e71d82521add901dd395ebed`.
- Verifisert signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
