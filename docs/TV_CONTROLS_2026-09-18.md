# Sesongar, bibliotekval og fokus

## Komponentgjennomgang

Design-system-gjennomgangen fann to ulike fokusproblem: låste sesongrader mangla
ein fokusstad, og Material-kontrollane teikna fokus rundt eit større usynleg
minsteområde enn den synlege flata. Endringa brukar dei eksisterande tema-tokena
og endrar ikkje valde aksentfargar, kontorettar eller bibliotekspørjingar.

| Komponent | Endring | Tastatur og fjernkontroll |
| --- | --- | --- |
| Sesongrad | Same fokusramme på valbare og låste rader | Opp/ned flyttar fokus og rullar; låste rader kan ikkje veljast eller sendast |
| Konto i sesonghovud | Kompakt `labelSmall`, til høgre på breie vindauge | Framleis ei eksplisitt kontohandling; ingen automatisk kontobyting |
| Bibliotekfilter | Éin verdi per kategori, alternativ i nedtrekksmeny | Fokus endrar ikkje verdi; stadfesting vel og lukkar menyen |
| Felles filter-/navigasjonsknappar og sekundærknappar | Synleg 48 dp minsteflate og samsvarande fokusramme | Trefflate og fokus er bevarte; ingen skalering ved fokus |
| Endringslogg | Overskrifter, punkt og avsnitt i native tekst | Rullbar tekst utan nettinnhald eller HTML-køyring |

## Tilstandar og token

- Fokus og val er ulike tilstandar. Fokusramma følgjer `focusStyle`, `onSurface`
  eller aksenten. Den skarpe streken blir teikna innanfor komponentgrensa.
- `ControlCorner` styrer knappforma. Teksthøgd kan vekse; ingen fast teksthøgd.
- Material sitt ekstra minsteområde er slått av berre inne i dei felles
  kontrollane som no sjølve held minst 48 dp synleg høgd. Kunstkort beheld sitt
  eige skalering-/glødmønster.
- Smale sesongvindauge legg den kompakte kontohandlinga under tittelen for å
  unngå å presse tittelen inn i ei uleseleg kolonne. Feil og innloggingskrav
  beheld den eksisterande forklaringa, ikkje ein oppdikta identitet.
- Sortering, visingsstatus og oppløysing har kvar sin veljar. Årstal og sjanger
  beheld listene sine. Søk opnar framleis berre etter eksplisitt handling.
- Endringsloggen støttar overskrifter, punkt/nummerlister, vanleg vektlegging,
  lenkjetekst med adresse og kodegjerde. Dette er ei avgrensa tekstvising av
  release-notat, ikkje ein nettlesar eller full CommonMark-motor.

## Verifisering

- JVM: 636/636 bestått, ingen feil.
- Endeleg bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`
  gav `BUILD SUCCESSFUL` etter 6 minutt og 9 sekund. Lint: 0 feil, 93 åtvaringar
  (uendra tal). `git diff --check` er rein.
- Android på endeleg debug-APK: 32/32 bestått. 23 testar i telefonformat
  (`RequestFlowUiTest`, `AccountPanelsTest`, `LibraryBrowserUiTest`, 40,854 s),
  og 9 testar i breitt 960 × 540 dp-vindauge (`TvControlsRegressionTest`,
  `FocusOutlineTest`, 22,850 s).
- Fjernkontrolltestane bruker TV-konfigurasjon og ekte tastetrykk på den
  isolerte `emulator-5562`. Dei kontrollerer rulling gjennom 18 låste sesongar,
  ingen utilsikta sending, inline konto, filtermenyar, nullstilling og fokus.
- Sesongar, bibliotekfilter og endringslogg er kontrollerte ved 2,0× skrift.
  Ein pikseltest stadfestar at den fokuserte filterknappen ikkje har ei tom
  stripe mellom synleg fyll og fokusramme. Fokus endrar ikkje mål eller val.
- Visuell kontroll av syntetiske skjermbilete avdekte at sesongteksten trong
  innvendig innrykk frå ramma. Dette er retta og testane køyrde på nytt.
- Ein eldre korttest venta på «Sesong 2 · Berre varsel», medan også HEAD-koden
  alt viste sesong og Følgjer-merke kvar for seg. Testen kontrollerer no begge
  dei eksisterande elementa. Ein teststart under APK-installasjon vart avbroten;
  heile køyringa vart teken på nytt etter stadfesta installasjon.
- Ingen fysiske TV-ar, ekte kontoar eller serverhandlingar er brukte. Emulatoren
  er sett tilbake til opphavleg oppløysing og tettleik etter kontrollen.

Tidlegare lokal retting av behaldarfeil i avspelaren er bevart. Ingen ny
versjonskode eller GitHub-release er laga i denne UI-oppgåva.
