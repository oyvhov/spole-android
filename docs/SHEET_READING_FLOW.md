# Detaljar: fokus og leseflyt

Arbeidsbolk 9. september 2026, etter `92e5511`. Ingen versjonsendring eller publisering.

## Reproduserte problem og rettingar

1. Den mørke flata utanfor popupen kunne få tastaturfokus utan synleg markering. Ho er no berre eit trykkmål for å lukke, ikkje eit stopp i fokusrekkjefølgja. TalkBack-handling og vanleg trykk er bevarte.
2. Detaljopning mangla eit føreseieleg, synleg fokusmål. Ved tastatur/fjernkontroll startar fokus på lukkeknappen. Berøring gjer ikkje dette. Opningsmåten kjem frå skjermen som opna dialogen, ikkje ei gissing ut frå den nye dialogvisinga. Sein metadata ber ikkje om fokus på nytt.
3. Utviding av ein lang omtale flytta «Les heile omtalen» ut av synsfeltet. Handlinga ligg no ved overskrifta, før teksten. Same kontroll bevarer fokus og posisjon når teksten veks eller blir trekt saman.

Lukk og Kalender-retur brukar den same draw-only fokusmarkeringa som mediekorta. Ingen ny høgdeanimasjon, ingen draganker og inga endring av datatilgang, førespurnadsreglar eller brukaridentitet.

## Mønster: ExpandableSynopsis

| Tilstand | Utsjånad og handling |
| --- | --- |
| Kort tekst | Full tekst; ingen unødvendig utvidingsknapp. |
| Lang tekst | Fire linjer; utviding ved overskrifta. |
| Utvida | All tekst i den eksisterande rulleflata; «Vis mindre» ligg framleis over teksten. |
| Manglande omtale | Kort, lokaliserbar tomtekst. |
| Lasting | Eksisterande stille skjelett; inga ny animasjon av storleik. |
| Fokus | Felles 3 dp kontur, 110 ms inn/ut, utan geometriendring. |

Utviding blir halden ved metadataoppdatering for same tittel og nullstilt for ein ny tittel. Tekstmåling skjer før plassering, utan tilstandsoppdatering frå layout. Overskrift og kontroll kan vekse ved stor skrift; dei overlappar ikkje. Eksisterande nynorsk/engelsk tekstressursar blir brukte.

## Verifisering og avgrensing

- 283 JVM-testar bestått; debug- og test-APK byggjer. Lint: 0 feil, 28 åtvaringar.
- Sluttkode på mobilformat 1080 × 2400 / 420 dpi: **37 Android-testar bestått** (6 fokus/retur, 3 omtale, 10 popup, 9 førespurnad og 9 innlogging).
- Sluttkode på nettbrettformat 1920 × 1200 / 240 dpi: **14 Android-testar bestått** (dei 9 nye testane og 5 språktestar). Før siste avgrensa fokusjustering bestod også den breiare 35-testpakken på nettbrett.
- Berøringstesten set Android-vindauget uttrykkeleg i berøringsmodus og kontrollerer modusen før opning; eit kall til Compose sin input-veljar åleine var ikkje nok på denne emulatoren. Dette var ein testføresetnad, ikkje bevis på feil ved faktisk fingertrykk.

Nye regresjonstestar feila først for usynleg fokus, manglande startfokus og utviding som flytta lesekontrollen ut av biletet. Testane dekkjer no òg gjenteken retur til same opphavskort, Android Tilbake, sein metadata, berøring, korte/manglande omtalar, tittelbyte og skriftstorleik 2.0 på smalt vindauge.

Automatiske testar køyrer berre på den isolerte emulatoren (5562). Lokalt skjermbilete av detaljpanelet er visuelt kontrollert med syntetisk tittel og plasshaldar, ikkje ekte kontoar. Ingen ekte førespurnad eller avspeling vart starta. Fysisk TV, fysisk nettbrett og tidsmåling av kald popup-opning står framleis att.

Fokusreglane byggjer på [Android si offisielle Compose-rettleiing](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior).
