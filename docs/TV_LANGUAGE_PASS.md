# Innloggingsspråk og TV-spelarkontrollar

9. september 2026 · kjeldearbeid etter `6e31ad5` / alpha05. Ingen APK-publisering eller versjonsauke.

## Bevarte endringar frå Claude

`601b4f0` flyttar følgjevarsel-skriving bort frå hovudtråden og kontrollerer konto før UI-oppdatering. `6e31ad5` utvidar CI til alle greiner og rettar release-rettleiinga. Arbeidstreet var reint ved start; begge endringane er bevarte utan omarbeiding.

## Levert

- 24 nye nynorsk/engelsk-ressursar for innloggingssteg, adresse, brukarnamn/passord, forklaringar og innsending.
- Ni ressursar for Quick Connect. TV har større kode og forklaring om godkjenning frå ei anna eining; kopiering legg berre den synlege koden på utklippstavla.
- TV-spelar: skjult OSD har eit eige fokusmål. OK vekslar spel/pause og viser kontrollar; opp/ned viser dei; venstre/høgre spolar 10 sekund. Spoling er avgrensa til kjende videogrensar.
- Eigne play/pause-knappar er idempotente. Gjentekne key-down frå same haldne knapp blir ikkje gjentekne handlingar. Busy, feil, episodeval og framhaldsval slepper ikkje mediekommandoar gjennom.
- Synleg OSD brukar normal D-pad-fokusnavigasjon. Fokus kjem tilbake til spel/pause, transportknappane har synleg ramme og rotasjonsknappen er skjult på TV.
- På TV lukkar Tilbake først ein spor-/kvalitetsmeny, så synlege kontrollar under avspeling, så spelaren/underliggjande episodeliste. Mobilens eksisterande Tilbake-handling er bevart.

Retning: [Android sine avspelingskontrollar for TV](https://developer.android.com/training/tv/playback/controls) og [TV-navigasjon](https://developer.android.com/training/tv/get-started/navigation). Wholphin er framleis visuell/flyt-referanse frå roadmapen; ingen kode er kopiert frå prosjektet i denne bolken.

## Verifisering

- [x] 283/283 JVM-testar, inkludert fire nye testar av fjernkontrollreglane og eksisterande ressursparitet.
- [x] Debug-/test-APK bygd. Lint: 0 feil, 28 eksisterande åtvaringar.
- [x] 21/21 Android-testar: tre TV-kontrolltestar, ni eksisterande spelar-UI-testar og ni innloggingstestar (inkludert to nye språk-/TV-scenario).

Første fokusassertar feila fordi testemulatoren brukte berøringsmodus. Dei endelege fjernkontrolltestane ber eksplisitt om tastaturmodus før knappesekvensane. Testane brukar fixtures og null videospelar for UI-kontroll; dei er ikkje ei ekte videostrøyming eller fysisk fjernkontrollgodkjenning. Sjå lokale `app/build/tv-language-build.log` og `app/build/tv-language-android.txt`.

## Står att

Fullstendig omsetjing av nettverks-/modellfeil, avanserte innloggingsval og utlogging. TV-fokus i alle mediarader og detaljar, søk/innlogging utan berøring, faktisk TV-systememulator, fysisk Android TV/Google TV og langvarig avspeling. Manifestet har framleis ikkje TV-startpunkt/banner; appen blir ikkje presentert som ferdig TV-app. Ingen endring av kontoar, førespurnader, serverrettar eller bakgrunnssynkronisering.
