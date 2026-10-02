# TV: Hero, spolemelding og serieside · 2. oktober 2026

## Vurdering av seriesida

Brukarbiletet viser eit godt hierarki øvst: tittel, episode, kort omtale og ei tydeleg
«Hald fram»-handling. Problemet er den store tomme avstanden etter handlingane. Sesongvalet
hamnar nær skjermbotnen, og episodane blir kutta før brukaren har fått sjå kva dei inneheld.

Sesongane bør stå under handlingane, på venstresida saman med tittelen. Flytt dei nærare ved å redusere
minimumshøgda på toppområdet for seriar og episodar. Tittel og avspeling er framleis første
val; sesongane og første episoderad blir synlege tidlegare. Film beheld den romslege toppflata.
Innhaldet kan framleis vekse ved større skrift eller utvida omtale, og heile sida rullar samla.
Etter tilbakemeldinga 3. oktober har toppområdet for seriar og episodar minimum 52 % av
høgda. Logoen har 16 dp luft under seg, og avstanden før omtale og handlingar er auka.
Den nye balansen prioriterer roleg hierarki og synlege episodebilete framfor å presse alle
tekstlinjer inn på første skjerm. Speletida står i eit diskret, nøytralt merke på episodebiletet;
spelesymbolet viser kva episode som står for tur utan ei ekstra tekstlinje under tittelen.

## Endringar

- TV-Hero tonar inn det nye biletet over 1,1 sekund. Det førre biletet held seg synleg under
  overgangen. Logoar og tekst blir klargjorde for den avgrensa karusellen, og tekstfeltet held
  stabil høgd. Handlingane flyttar seg ikkje ved byte mellom film og episode.
- Spoling viser eit enkelt retningsikon utan tal, og éi nøytral tidsmelding. Gjentekne trykk
  summerer faktisk flytta tid; retningsbyte startar ei ny melding. Ved starten og slutten blir
  avgrensa rørsle vist korrekt. Meldinga tonar ut utan skalering og utan å bli nullstilt i utgangen.
- «Fyll skjermen» er fjerna frå TV-OSD. TV bruker originalt bildeformat, slik at delar av
  biletet ikkje blir kutta for å fylle skjermen. Mobilkontrollen er framleis tilgjengeleg.

## «Hald fram» nær slutten

Jellyfin-klienten sender posisjon til tenaren. Tenaren bruker som standard `MinResumePct=5`,
`MaxResumePct=90` og `MinResumeDurationSeconds=300`. Over maksimumsprosenten, eller ved siste
sekund, blir videoen merkt som sett og posisjonen nullstilt. Tenarinnstillingane kan endrast.

Kjelder: [Jellyfin UserDataManager](https://github.com/jellyfin/jellyfin/blob/master/Emby.Server.Implementations/Library/UserDataManager.cs),
[standardkonfigurasjonen](https://github.com/jellyfin/jellyfin/blob/master/MediaBrowser.Model/Configuration/ServerConfiguration.cs),
[rapportering i Jellyfin Web](https://github.com/jellyfin/jellyfin-web/blob/master/src/components/playback/playbackmanager.js).

Spole si lokale journalføring kunne tidlegare leggje tilbake ein nesten ferdig episode sjølv
om tenaren hadde fjerna han frå si rad. Lokal fullføring bruker no Jellyfin si standardgrense
ved slutten, også for eldre journaloppføringar. Etter ei vellukka stopprapportering, og ved
framdriftsrapportering nær slutten, blir den aktuelle brukaren sin spoleposisjon og sett-status
henta frå tenaren og får siste ord. Dette krev ikkje administratortilgang. Ved nettverksfeil
står den lokale journalen att. Ei forseinka stadfesting får ikkje overskrive ei nyare spoling
eller ny avspeling, fordi kvar journaloppføring har ein eigen identitet.

## Verifisering

821 av 821 JVM-testar er beståtte. `lintDebug` har 0 feil, 168 åtvaringar og 1 hint.
Debug-APK og instrumenterings-APK er bygde. Sluttkøyringa av `testDebugUnitTest lintDebug
assembleRelease` er vellukka. Produksjons-APK-en har den eksisterande Spole-signaturen
(`36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`).
Denne kontrollen gjeld det lokale bygget før beta14. Sluttkontrollen av beta14 er dokumentert
i `VERIFICATION_v0.18.0-beta14.md`.
APK-en er installert med `-r` på den lagra TV-profilen. Opphavleg installasjonsdato er
bevart, og appen opnar den eksisterande barneprofilen med faktisk bibliotekinnhald.

28 ulike Android-testar er beståtte på den isolerte TV-emulatoren `emulator-5566`:

- 14 avspelingskontrolltestar: fjernkontroll, spoling, buffer, menyar og skriftstorleik 2,0.
- 5 testar av journal og framdrift: fem sekund att, eigne tenargrenser, forseinka stadfesting,
  ny avspeling og skilje mellom kontoar og tenester.
- 5 Hero-testar: rotasjon, rørsleval, bakgrunn, første rad, stor skrift og stabile handlingar.
- 4 detaljtestar: film, serie, episode med logo/metadata og navigasjon med ekte skriftstorleik 2,0.

Detaljvindauget får densiteten frå sitt eige Android-vindauge. Eit Compose-overstyrt
rotvindauge gav derfor ikkje ekte 2,0-skrift inne i dialogen. Den store-skrift-testen blir no
køyrd separat med systeminnstillinga `font_scale=2.0` på den isolerte TV-eininga og kontrollerer
den faktiske tekstdensiteten. Innstillinga blir sett tilbake til 1,0 etter testen.

Skjermbilete av spolemelding, serieside og episodeside er kontrollerte visuelt. Episodetittelen
og heile første kortet fekk plass i det første, tettare oppsettet. Den endelege seriesida
gir logoen meir luft; episodebiletet og starten på tittelen er synlege før rulling. Ved
skriftstorleik 2,0 er handlingane framleis nåbare ved rulling. Bileta og testloggane ligg lokalt i `app/build`.
Tenarsvara er testa med syntetisk HTTP-transport; ingen ekte episodar er merkte som sett for
å teste regelen. Ingen instrumentering er køyrd på emulatorane med lagra brukarkontoar.
