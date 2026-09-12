# TV: navigasjon, knappar og neste episode

Lokal gjennomgang 12. september 2026. Endringane er bygde og installerte som signert produksjonspakke på den eksisterande TV-review-emulatoren. Dei er ikkje publiserte som ein ny release.

## Navigasjon og samanheng

- Framsida hadde både automatisk fokusrulling og ei eiga brå rulling til toppen. Toppfeltet ber no om å bli synleg gjennom same fokusrulling som resten av innhaldet.
- TV-radene har stabile nøklar og kjem fram utan forseinka innflyging. Fokus blir gjenoppretta dersom første datalasting byter ut den opphavlege fokusknappen.
- Kort på framsida, i biblioteket og i Oppdag veks ikkje lenger når dei får fokus. Fokusramma viser valet utan å klippe kort eller skyve dei visuelt ut av rada.
- Titlar får jamn høgd innanfor kvar TV-rad. Breie episodekort brukar éi tittellinje; plakatkort med to linjer reserverer same plass for korte og lange titlar. Aktivitet har òg jamn tittelplass.
- Felles sekundærknapp gir dei tidlegare reine tekstknappane på TV ein diskret kant, minst 48 dp høgd og synleg fjernkontrollfokus. Mobil held fram med sin eksisterande tekstknapp. Endringa omfattar mellom anna innstillingar, filter, konto, oppdatering og dialoghandlingar.

## Neste episode

Innstillingar → Avspeling har eigne val for tilbodet og automatisk avspeling.

| Val | Standard | Tilgjengelege tider |
| --- | --- | --- |
| Vis neste episode | På | Når episoden sluttar, eller 15/30/60/90/120/180/300 sekund før |
| Tid før slutten | 60 sekund | Som over |
| Spel neste automatisk | På | Kan slåast av uavhengig av tilbodet |
| Vent etter slutten | 12 sekund | 5/10/12/15/20/30/60 sekund |

Kortet ligg øvst til høgre med tittel, episodeinformasjon, «Spel no» og «Sjå ferdig». Det kan visast medan dei vanlege avspelingskontrollane er skjulte. Store tekststorleikar får knappane til å bryte til neste linje.

Tilbodet krev at tenesta har levert ein faktisk neste episode. Ukjend spilletid gir ikkje eit tidleg tilbod. «Sjå ferdig» eller Tilbake før slutten skjuler tilbodet og avbryt automatisk overgang for den episoden. Automatisk overgang startar først etter faktisk avspelingsslutt. Appen stoppar nedteljinga i bakgrunnen og held fram når spelaren kjem tilbake. Kontobyte, feil og raske dobbelttrykk er verna i avspelingsmodellen.

Dette gjeld den innebygde spelaren; eksterne avspelingsappar styrer sine eigne episodetilbod.

## Tema og tregare TV-ar

Innstillingar → Utsjånad har dei eksisterande seks bakgrunnstemaa, sesongval, aksentfargar, kortstorleik, hjørne, fokus og kontrast. Den nye innstillinga «Lett TV-modus» slår av roterande toppbilete, biletovergangar, glød, bevegelege lastefelt og sesongrørsler. Toppfeltet held då berre det valde biletlaget aktivt. Fokusramma er framleis synleg. Modusen er av som standard og blir lagra lokalt.

Avspelaren oppdaterer tidsposisjonen kvart sekund i staden for to gonger i sekundet. Rapportering til tenesta held same intervall på ti sekund.

## Verifisering

- 448 av 448 einingstestar bestod, ingen hoppa over.
- 37 av 37 utvalde Android-testar bestod på isolert TV-emulator: avspelingskontrollar, neste episode, innstillingar og lagring, tekststorleik 2.0, kortgeometri, navigasjon, oppsett, medierader, bibliotek og kontoalternativ.
- Mobil: bibliotek og kontoalternativ bestod 6 av 6. Detaljpanelet bestod 6 av 6 i separat ny køyring. Første samla mobilkøyring hadde to tidsavbrot i detaljpanelet; ny køyring bestod utan kodeendring. Dette er ikkje dokumentasjon på at testen er fri for tidsavhengig ustabilitet.
- `lintDebug`: 0 feil, 28 åtvaringar.
- `assembleRelease`, `assembleDebug`, `assembleDebugAndroidTest` og `git diff --check` fullførte.
- Manuell visuell kontroll med ekte data: framside, oppover til toppfeltet, bibliotek, Oppdag, Aktivitet og innstillingar. Lett TV-modus vart slått på og av; opphavleg kontrast og lettmodus vart gjenoppretta.
- Neste-episode-kortet er visuelt kontrollert med syntetisk avspelingstilstand ved normal og dobbel tekststorleik. Full overgang mellom to verkelege mediefiler er ikkje køyrd i denne gjennomgangen.
- Ingen fysisk treg TV var tilgjengeleg. Emulatoren gir ikkje grunnlag for eit lova bilettal eller prosentvis ytelsesgevinst. Dette er konkrete reduksjonar i UI-arbeid, ikkje ein kontrollert maskinvarebenchmark.

Skjermbilete og køyringsloggar ligg lokalt i den Git-ignorerte `app/build/`-mappa. Den lokale visuelle rapporten er `app/build/tv-ui-review.md`. Skjermbilete med ekte kontoinnhald skal ikkje leggjast i Git.

## Lokal APK

- Pakke: `app.reelstack`, versjon `0.16.0-alpha19`, versjonskode 62 (ikkje auka for denne lokale gjennomgangen).
- Fil: `app/build/outputs/apk/release/app-release.apk`, 3 964 362 byte.
- SHA-256: `30f9dd138876038690b10f5332da612ed8e5a7c5e6fc9bf338e30e3204d59ef0`.
- Verifisert signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Installert med oppdatering over eksisterande pakke, med kontoar og appdata bevarte.
