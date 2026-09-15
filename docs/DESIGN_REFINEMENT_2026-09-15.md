# Visuell finpuss etter emulatorgjennomgang

Oppfølging av dei tolv tilbakemeldingane med skjermbilete, og ønskja om synleg logoanimasjon og samanheng mellom episode og serie. Dette byggjer vidare på fase 1–3. Ingen GitHub-release er publisert i denne oppgåva.

## Innstillingar og kontrollar

- Innstillingssøk og den tilhøyrande fokushandteringa er fjerna.
- Heimskjermvala er grupperte i «Ved oppstart», «Utval» og «Innhaldsrader». Alle utvalsvala har korte forklaringar. Utsjånad skil mellom uttrykk, fargar/sesong, bilete/omslag og lesing/rørsle.
- Kontrollane har mindre luft: 40 dp minste visuelle høgd for vanlege kontrollar, 48 dp for innstillingsrader og 10 dp kontrollhjørne. Material-kontrollar bevarer sine større berøringsflater. Tekst kan utvide ei rad ved stor skrift.
- Menyval har vis/skjul og flyttepiler direkte i lista. Identitet og fokus følgjer rada gjennom ei flytting. Heim og Innstillingar kan ikkje skjulast; Bibliotek er også verna når det er startside.
- Venstre inne i ei rad skal flytte mellom kontrollane. Først når fokus forlèt innhaldspanelet mot venstre, går det til vald kategori. Dette nyttar [Compose sin onExit-mekanisme](https://developer.android.com/reference/kotlin/androidx/compose/ui/focus/FocusEnterExitScope).
- «Mine uttrykk» kan lagre, laste og slette opptil tolv namngjevne kombinasjonar. Same namn erstattar eit tidlegare uttrykk. Berre visuelle val blir lagra; avspeling, kontoar og menyval blir ikkje endra når eit uttrykk blir lasta.
- Komfort nyttar mjuke hjørne. Hjørna i miniatyrførehandvisinga blir skalerte i høve til kortstorleiken.

## Bibliotek og bilete

- Bibliotekveljaren brukar bildekort med bibliotekbilete frå serveren og namn over ein mørk gradient. Ved manglande bilete viser kortet eit nøytralt bibliotekikon.
- Tilpassingsknappen på biblioteksida opnar radrekkjefølgje, vis/skjul, bibliotekrekkjefølgje, breie/kvadratiske biblioteksbilete og sideoverskrift. Dei same vala finst under Heimskjerm i innstillingane.
- Ordet «Bibliotek» er skjult som standard og kan visast igjen. Navigasjonsnamnet er uendra.
- Favorittkort får eigne posterreferansar frå servermetadata, også for seriar. Breie kort nyttar landskapskunst når tilgjengeleg. Formatvala er bevarte.
- Heroen prioriterer bakgrunnsbilete frå film eller serie, deretter tilgjengeleg serieminiatyr. Ein episode sitt eige bilete blir ikkje valt som hero; metadata for seriesbakgrunn eller serieminiatyr blir brukte. Bilete blir ikkje gjetta frå tittelen.
- Bakgrunns- og posterreferansar blir lagra saman med mellomlagra kort. Biletetaggar følgjer rett bilettype. Posterførespurnader er avgrensa til 480 pikslar; hero nyttar opptil 1920 pikslar, eller 1280 i lett TV-modus.

## Episodar og oppstart

- Kommande episodar har eit tydeleg «Kommande»-merke på biletet og premieredato under tittelen. Episodar som ikkje er tilgjengelege etter premieredatoen er merkte «Manglar». Dei kan ikkje startast som om fila var tilgjengeleg.
- «Gå til serien» knyter episodesida til seriesida. Ho opnar same sesong, og Tilbake gjenopprettar episoden og sesongvalet ein kom frå. Returen er avgrensa til same innlogga konto.
- Den originale logoen er bevart. Formingsanimasjonen tek 900 ms, med minst 300 ms etterpå før ein mjuk uttoning. Langsam oppstart nyttar 1200 + 400 ms. Data og bilete lastar parallelt bak logoen.
- Dei fire første kortbileta blir klargjorde før dekselet blir løfta når det let seg gjere. Nettverksventinga har ei grense på fem sekund, slik at tenestefeil ikkje held brukaren fast ved logoen. Dette er ikkje ein lovnad om at heile biblioteket er lasta ved overgangen.

## Avspeling og retur til appen

- Ved episodeskifte blir ikkje fil-ID eller sporindeksar frå førre episode sende til den nye fila. Vald versjon og spor er framleis bevarte når kvaliteten blir endra i same tittel. Testtenaren brukar no ulike fil-ID-ar for kvar episode, og testen krev at neste video faktisk spelar.
- Eit avgrensa minnelager held opptil fire tekstfiler på maksimalt 2 MiB kvar per avspelingsplan. Eitt sannsynleg tekstspor blir klargjort etter videostart, også når tekst er avslått. Tekstval brukar same avspelingsøkt. Biletbasert tekst kan framleis krevje omkoding på serveren.
- Eit lite lokalt framdriftslager oppdaterer «Hald fram» straks og vernar mot eldre serversvar. Det blir også lese etter at appen er oppretta på nytt. Spelaren brukar lokal posisjon når serveren enno ikkje har teke imot henne.
- Lageret er avgrensa til dei siste 20 titlane i 24 timar, er knytt til server/konto/innlogging og inneheld ikkje tilgangsteikn. Nyare serverframdrift vinn. Ferdige episodar blir fjerna; manuell sett/ikkje sett fjernar det lokale overstyret. Utlogging og endring av bibliotekutval ryddar lageret.
- Spelaren lagrar og pausar ved `onPause`, før hovudsida kjem tilbake. Automatisk vidareavspeling blir ikkje gjenopptatt i bakgrunnen.

## Endeleg kontroll

- Endeleg bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintDebug` fullført utan feil.
- 507 JVM-testar bestått. Lint: 0 feil, 57 åtvaringar.
- Endeleg Android-kontroll: **49/49 bestått** på isolert TV-profil 5566. Omfattar lokal framdrift, manuelt episodeskifte, innstillingsgrupper, lagra uttrykk, menyfokus, bibliotek, episode/serie-retur, oppstartslogo og hero. Logg: `app/build/design-review/refinement-final-all.txt`.
- Eigen avspelingsrunde: **39 testar utan feil**, med éin telefonspesifikk skjermknapptest hoppa over på TV. Resten bestod, inkludert dekoding, automatisk/manuelt episodeskifte med ulike fil-ID-ar, undertekst av/på, klargjering medan tekst er av, bakgrunn/retur, kvalitet/lyd og kontoendring. Logg: `playback-refinement-final.txt`. Fyrste runde avdekte gamle telefonforventningar i TV-testen og eit testløp som valde lyd medan søking framleis bufra; dette vart retta før den vellukka runden.
- Ekte TV-profil: visuelt kontrollert roterande seriebakgrunnar, kort og episodesida med «Gå til serien». Eldre cache utan hero-felt gav først eit stort reserveikon. Dette er retta: gamle kort blir berre brukte når URL-en stadfestar landskapskunst frå sjølve serien; andre kandidatar blir utelatne. Bileta er kontrollerte på nytt etter rettinga.
- Mobil/nettbrett og stor skrift er kontrollerte med syntetiske data på den isolerte emulatoren. Denne kontrollen stadfestar ikkje ekte innlogging eller avspelingsyting på den faste mobilprofilen.
- Signert produksjonspakke er installert med `-r` på dei lagra TV- og mobilprofilane (5564/5560). Kontoar og appdata er bevarte. Ingen GitHub-publisering eller versjonsauke er gjort.

APK: lokal testutgåve **0.16.1 (79)**, pakke `app.reelstack`, **11 690 898 byte**.
SHA-256: `5757b42c3d765ac6912b23df132aef250d9ca9a9b4e1b0aaa6a2fd3532776497`.
Signeringssertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
Fila er `app/build/outputs/apk/release/app-release.apk`.

Private skjermbilete og loggar ligg berre i den ignorerte mappa `app/build/design-review`. `git diff --check` er rein. Logo-/ikonressursane er uendra.
