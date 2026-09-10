# Spole 0.16.0-alpha10 — TV-innstillingar, tema og sjårekkefølgje

Lokal, signert oppdatering: bygg 53. Ingen GitHub-publisering.

## Hald fram å sjå + Neste episode

Den kombinerte rada blir sortert samla etter siste sjåaktivitet, nyaste først. Ho legg ikkje lenger heile Neste episode-lista etter dei påbegynte titlane.

- Påbegynt film/episode bruker profilen sitt `UserData.LastPlayedDate`.
- Ei usett neste episode bruker datoen til den sist sette episoden i same serie. Oppslaget er avgrensa til den same profilen og ein serie frå dei valde biblioteka.
- Ein serie sett i går kan difor liggje framfor ein film pausa førre veke.
- Duplikat blir fjerna, og den påbegynte utgåva beheld framdrifta si. Like datoar har stabil rekkefølgje.
- Manglande datoar hamnar etter daterte element, i stabil rekkefølgje. Innlastingsdato eller premieredato blir ikkje framstilt som sjåaktivitet.
- Tidspunkta blir lagra i mellomlageret. Ei gamal utgåve av dette disponible mellomlageret blir bygd opp på nytt etter oppdatering; kontoar og innstillingar blir ikkje sletta.

Jellyfin gir ikkje den usette episoden eit eige sjåtidspunkt. Difor blir det gjort eit lite historikkoppslag per serie, med maksimalt fire samtidige kall og åtte sekund samla ventetid. Feil i dette oppslaget fjernar ikkje episoden frå rada.

API-grunnlag: [Jellyfin UserItemDataDto](https://typescript-sdk.jellyfin.org/interfaces/generated-client.UserItemDataDto.html) og [Jellyfin GetItems-parametrar](https://typescript-sdk.jellyfin.org/interfaces/generated-client.LibraryApiGetItemsRequest.html).

## Nytt TV-oppsett

Den gamle sideinndelinga kravde 900 dp tilgjengeleg innhaldsbreidd. Ein vanleg TV-emulator har 960 dp totalt, men berre 880 dp etter sidemenyen. Dermed fekk TV ei lang mobil-liknande liste med alle innstillingane på same side.

TV har no eit eige oppsett uavhengig av denne nettbrettgrensa:

| Kategori | Innhald |
| --- | --- |
| Utsjånad | Førehandsvising, bakgrunn, aksent, biletstorleik, kortform, fokus, kontrast, vurderingar, kvalitet og språk |
| Heim | Bibliotekval, Neste episode, kombinert sjårad, hovudbilete og synlege heimrader |
| Meny | Ei oversiktleg rad per side; eit eige panel for flytting og skjuling; biblioteksnarvegar |
| Avspeling | Automatisk gjenopptaking og roleg oppstart |
| Tenester | Tilkoplingar, kontotilgang og personverninformasjon |
| Oppdateringar | Varsel og berre Wi-Fi |
| Om appen | Versjon, kjelder og eventuell krasjrapport |

Opp/ned blar mellom kategoriar. Høgre eller OK går inn i innhaldet. Venstre eller Tilbake frå innhaldet går tilbake til den aktive kategorien. Kvar kategori beheld si eiga rulleplassering. Telefonen held på mobilnavigasjonen.

## Fleire temaval

- Fire bakgrunnar: Skog, Midnatt, Kino og Skumring.
- Åtte aksentfargar: lime, havblå, iris, korall, gull, mint, rose og perle.
- Tre storleikar på biletkort.
- Tre hjørneformer: skarpe, mjuke og avrunda.
- Tre fokusmarkeringar: kvit, aksentfarga og kraftig kvit.
- Sterkare kontrast for sekundærtekst og standardkontrollar.

Endringar blir viste med ein gong og lagra på eininga. Bakgrunn og overflater følgjer temaet i heile appen; biletkort på Heim, Bibliotek, Oppdag og Aktivitet følgjer kortforma. Førehandsvisinga viser fargane, kortstorleiken, forma og fokusmarkeringa. Tilbakestilling av utsjånaden endrar ikkje konto, menyrekkefølgje eller avspelingsval.

## Kontroll og avgrensingar

Den signerte APK-en er bygd og installert som oppdatering på både TV- og telefonemulatoren med ekte kontoar. Begge er opne for vidare prøving. Mobiloppsettet og dei nye temavala er visuelt kontrollerte; opning og lukking av episodetaljar med berøring fungerte.

317 JVM-testar, 226 ulike utførte telefon-testar og 22 målretta TV-testar bestod. To ekstra TV-testar bestod med 200 % systemskrift. Telefonresultatet inkluderer ei atterkøyring av ein paneltest som nådde tidsgrensa i fullpakken. Emulatoravbrot, atterkøyringar, lint og signatur er dokumenterte i [verifikasjonsrapporten](VERIFICATION_v0.16.0-alpha10.md).

APK: `app/build/test-alpha10/Spole-0.16.0-alpha10.apk` (bygg 53).

Fysisk TV er ikkje testa i denne arbeidsøkta. Historikksortering er avhengig av sjåtidspunkta tenaren faktisk har lagra; importert eller manuelt endra sjåhistorikk kan ha manglande datoar.
