# Designløft, fase 1–3

Dette er gjennomføringa av stabilitet, bibliotek og personlege val. Ingen ny GitHub-release er publisert som del av arbeidet.

## Fase 1: stabil geometri og felles kontrollar

- Utvalet på Heim og i Bibliotek har ei fast scene per vindauge og skriftstorleik. Tittel, filmlogo, episodeinformasjon og omtale har reserverte plassar. Skifte av metadata flyttar ikkje handlinga.
- Jellyfin-/Emby-merket er borte frå utvalet. Spole-logoen er uendra.
- Omslagsformat blir handtert før første teikna biletramme. Eit ståande reservebilete blir tilpassa ei brei ramme utan å bli blåst opp. Kunstnarleg beskjering i det store bakgrunnsbiletet er eit eige, uttrykkeleg val.
- Mellomlageret bevarer episodeidentitet, sesongnummer, filmlogo, sett-status, favorittstatus og tilgjenge. Favorittrada blir lagra. Databaseoppgradering 2 → 3 bevarer eksisterande rader og biletreferansar.
- Den tidlegare regelen som skjulte ståande episodebilete frå mellomlageret er fjerna. Dei kan no visast trygt medan oppdateringa går.
- Sjå-vidare-rader førehentar eit avgrensa tal nabobilete med eksisterande, autentisert Coil-lastar. Lett TV-modus reduserer talet frå sju til fire. Arbeidet blir avbrote når rada forlèt komposisjonen.

### Kontrollsystem

`ReelLayout` definerer 48 dp minste kontrollhøgd, 60 dp minste innstillingshøgd, 12 dp kontrollhjørne, 16 dp standardinnrykk og 24 dp ikonstorleik. Høgdene er minimum, ikkje tekstboksar med fast høgd.

Innstillingshandling, val og brytar brukar same komponentar på telefon, nettbrett og TV. Bibliotekfilter og navigasjonsknappar brukar same hjørne og minimumshøgd. Mediekort brukar framleis brukaren sitt val av omslagshjørne; runde ikonknappar og avspelingskontrollar har medvite andre former. Ein felles talverdi skal ikkje overstyre forma sin funksjon.

## Fase 2: Bibliotek som startside og adaptive innstillingar

- Bibliotekoversikta samlar typefilter, bibliotekval, utval, sjå vidare, neste episode, favorittar og førehandsvising frå kvart bibliotek.
- Biblioteka kan opnast direkte frå knappar og frå «Bla i alt». Mediekort opnar den rette tittelen også når han berre finst i ei bibliotekførehandvising.
- Filmar, seriar og samlingar er eigne filter. Kva samlingar som finst, følgjer biblioteka serveren faktisk tilbyr brukaren.
- Sluttkontrollen med ekte data avdekte at den gamle førespurnaden brukte `GroupItemsIntoCollections=false`, som ikkje styrer gruppering i Jellyfin sitt Items-endepunkt. Han brukar no `CollapseBoxSetItems=false`. Dette hindrar at serveren erstattar filmar med samlingar etter filtrering. Det eigne samlingsbiblioteket er framleis tilgjengeleg. Sjå [Jellyfin si handtering av samlingsgruppering](https://github.com/jellyfin/jellyfin/blob/master/MediaBrowser.Controller/Entities/Folder.cs).
- Underliggjande bibliotek har A–Å-filter som blir sendt til serveren. Eksisterande sortering, visingsval, paginering og bibliotekgrenser er bevarte.
- Startside kan vere Heim eller Bibliotek. Tilbake går til vald startside; eit bibliotek valt som startside kan ikkje forsvinne frå navigasjonen.
- Telefon viser éi innstillingsgruppe om gongen. Nettbrett frå 720 dp får kategoriar og innhald side om side når teksten har nok plass. Ved stor skrift går nettbrett tilbake til éin kolonne. TV bevarer kategorinavigasjon for fjernkontroll.
- Søk opnar rett gruppe og rullar til innstillinga. Skjermtastaturet blir lukka når eit resultat er valt. TV får fokus på kontrollen.
- Serieoverskrifta på telefon får eit mindre omslag ved sida av tittelen, i staden for eit høgt omslag over teksten.

## Fase 3: personlege uttrykk og kommande episodar

- Tre førehandsval: Filmrom, Oversikt og Komfort. Fargar, kortstorleik, hjørne, kontrast og rørsle kan framleis finjusterast kvar for seg.
- Førhandsvisinga nyttar tilgjengelige mediebilete. Jul- og Halloween-valet med ornament er bevart.
- Nye val for rotasjon, filmlogo og kompakt utval. Redusert rørsle gjeld også sidenavigasjon og den delte søkeovergangen.
- Heimeditoren kan flytte og skjule rader. Biletramma for medierader kan veljast som automatisk, ståande eller brei. Val blir lagra per rad; dei lastar ikkje ned nye kunsttypar som serveren kanskje manglar.
- Sesongvisinga kan supplere Jellyfin-/Emby-data med kunngjorde episodar gjennom den innlogga Seerr-brukaren. Koplinga skjer med serien sin TMDB-ID, aldri med gjetta tittellikskap. Sonarr/adminnøkkel er ikkje nødvendig.
- Lokale episodar blir viste først. Seerr-feil hindrar ikkje desse. Tillegg blir fletta etter sesong- og episodenummer, med lokale episodar først ved duplikat. Nettverkssvar frå ein eldre konto eller sesong blir forkasta.
- Kommande episodar har dato og tydeleg status, men ingen oppdikta avspelings-ID. Dei blir ikkje valde av automatisk gjenopptaking. Utgjevingsdato betyr ikkje at fila ligg på serveren.

Dette hentar tillegg for sesongar som allereie er kjende i biblioteket. Ein heilt ny sesong som serveren ikkje kjenner, universell TMDB-innlogging utan Seerr og personlege premierevarsel er ikkje del av denne gjennomføringa.

## Verifisering

- Siste bygg: `testDebugUnitTest assembleRelease lintDebug`, vellykka 15. september 2026. 500/500 einingstestar, ingen feil eller hoppa over. Lint: 0 feil, 51 eksisterande åtvaringar.
- 62 ulike relevante Android-testar er bestått på den isolerte TV-profilen 5566, med eksplisitte telefon-, nettbrett- og TV-konfigurasjonar. Dette omfattar stabil hero, skriftstorleik 2.0, innstillingssøk, radrekkjefølgje med fjernkontroll, kommande episodar, cache og eksisterande detalj-/fokusregresjonar.
- Første samla køyring gav 60/62. To testføresetnader vart retta: toleranse for avrunding frå dp til pikslar og eksplisitt startrekkjefølgje før gjenteken radflytting. Begge klassane er køyrde på nytt og bestått. Dette er ikkje oppgjeve som éi samla feilfri køyring.
- Den signerte produksjonsvarianten er bygd og installert med `-r` på dei eksisterande review-profilane. Ingen kontoar, innstillingar eller appdata er nullstilte. Ingen instrumentering er køyrd på profilane med ekte data.
- Heim, den nye bibliotekoversikta og filmrutenettet er gjennomgått visuelt med ekte TV-data. Telefon-/nettbrettgeometri er kontrollert med syntetiske skjermtestar og skjermbilete, også med stor skrift.
- Etter rettinga av `CollapseBoxSetItems` er filmrutenettet kontrollert på nytt med endeleg release-APK: tidlegare samlingskort er erstatta av enkeltfilmar med eigne omslag og vurderingar. Ny einingstest dekkjer filmar, samlingsbibliotek og filmar inni ei samling. Dei 62 Android-testane vart køyrde før denne siste endringa i nettverksparameteren; ingen skjermkode vart endra etter dei siste omkøyringane.
- Ekte innlogga mobilflyt kunne ikkje verifiserast: review-profilen 5560 viste førstegongsoppsett og Android-feilen «Bluetooth keeps stopping». Den separate instrumenteringsmobilen 5562 hadde oppstartsfeil i Android sin tilgangsdatabase. Ho vart stoppa utan nullstilling; testane nytta 5566 med eksplisitt telefonkonfigurasjon.
- Kommande episodar er testa med syntetiske svar og skjermdata. Ei faktisk kommande utgjeving frå den tilkopla Seerr-tenesta er ikkje stadfesta manuelt.
- Skjermbilete og testloggar ligg berre i den ignorerte mappa `app/build/design-review`. Bilete med ekte konto-/mediedata er ikkje lagde i Git.

Dette er eit lokalt testbygg med eksisterande versjonsnamn 0.16.1 og versjonskode 79, ikkje ein ny GitHub-oppdatering. Ein eventuell release må få ny versjon etter release-rutinen.

Endeleg APK: `app/build/outputs/apk/release/app-release.apk`, pakke `app.reelstack`, 11 660 346 byte.
SHA-256: `18f1467035237e436bb3f425ff5695854b0b9dcfb0184110614e4b6e1c4a5638`.
Signaturen er verifisert mot eksisterande sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

Ingen lovnad om ei bestemt oppstartstid på fysisk TV: bilethandteringa er kontrollert, men 1–2 sekund på alle tenarar/nettverk er ikkje målt.
