# Fase 4 – særpreg

Omfanget vart stadfesta av brukaren 15. september 2026: mini-spelar, Watch Next, temagalleri, trailerførehandsvising, hurtighandlingar og meir avansert OSD.

## Implementert i 0.17.0-beta03

| Funksjon | Flyt og avgrensing |
| --- | --- |
| Mini-spelar | Eigen knapp i den innebygde spelaren på telefon/nettbrett med Android bilete-i-bilete. Videoen held fram under overgangen; Android tilbyr retur til fullskjerm. Knappen blir berre vist på støtta einingar. Full stenging stoppar avspelinga. |
| Watch Next | Val under Avspeling på TV, av som standard. Uferdige Jellyfin-videoar med minst eitt minutt sett blir publiserte med framdrift. Ferdige videoar og gamle kontooppføringar blir fjerna. Startskjermen får lokale bilete og ei kontoavgrensa opningslenkje, aldri tilgangsteikn eller tenaradresse. |
| Temagalleri | Seks lokale uttrykk, med to kolonnar på breie flater og éi på telefon. Førehandsvising endrar ikkje appen før «Bruk uttrykket». Jul/Halloween vel både bakgrunn og aksent. Tilpassa variantar kan namngjevast og lagrast under Mine uttrykk. |
| Trailerførehandsvising | Detaljar viser Trailer når serveren tilbyr ei støtta YouTube-lenkje. Eit bilete og eit tydeleg avspelingsval kjem først; avspeling opnar i videoapp/nettlesar. Ingenting startar automatisk når ein blar. Andre trailerformat og lokale trailerfiler er ikkje med i denne leveransen. |
| Hurtighandlingar | Langtrykk på kort i Heim og bibliotekhyllene: spel av/hald fram, detaljar, favoritt og sett-status. Fjerning frå Hald fram finst berre der handlinga er relevant. Serie-/sesongkort opnar detaljar; dei blir ikkje framstilte som ei enkelt videofil. |
| Avansert OSD | Fart 0,75–2×, kapittelval når serveren har kapittel, kapittelbilete under direkte TV-spoling, direkte venstre/høgre-spoling og eitt Tilbake for å lukke TV-OSD med meny. Kapittelbilete er ikkje nøyaktige biletruter frå kvar posisjon. |
| Undertekstutsjånad | Rein, Kino, Høg kontrast og Stor. Faktisk undertekstførehandsvising i innstillingar og tilgang frå OSD. Same stilfunksjon på telefon, nettbrett og TV. Gjeld tekstbaserte undertekstar i Spole; eksterne spelarar og innebrend tekst har eigen utsjånad. |

Bibliotekfiltera Alt/Filmar/Seriar/Samlingar er fjerna frå oversikta på alle skjermstorleikar. Biblioteksnamn ligg under biletet og kan slåast av. Tekst som er teikna inn i sjølve serverbiletet, er ein del av biletet.

## Plattformavgrensing

Android TV-startskjermen avgjer om Watch Next blir synleg. Google TV krev i tillegg godkjenning gjennom Google sin sertifiseringsprosess. Koden og skrivinga til Android sin TV-provider kan testast lokalt; dette er ikkje ei godkjenning frå Google. Sjå [Google si rettleiing](https://developer.android.com/training/tv/discovery/watch-next-add-programs).

Temagalleriet er lokalt, utan konto, betaling, nedlasting av kode eller eksterne temapakkar. Traileravspeling brukar den offentlege, normaliserte videolenkja og sender ingen Spole-innlogging til videoappen.

## Grunnlag og kontroll

- [Android TV-spelarkontrollar](https://developer.android.com/training/tv/playback/controls): OK for avspeling/pause, venstre/høgre for spoling og opp/ned for informasjon.
- [Media3 SubtitleView](https://developer.android.com/reference/androidx/media3/ui/SubtitleView): relativ tekststorleik og felles stil i video og førehandsvising.
- Testar dekkjer kapittelval, fartmeny, Tilbake, temaførehandsvising utan lagring, bevaring av spelarval ved temabyte, langtrykk og valfrie biblioteksnamn.
- Watch Next blir prøvd med syntetisk konto på isolert TV. Innsetting, fullføring, utlogging og fråvær av innloggingsdata i lenkja blir kontrollerte.
- Mini-spelar krev manuell kontroll på ei støtta, innlogga mobileining. TV-testemulatoren har ikkje PiP. Den isolerte mobil-emulatoren stoppa med ein Android-systemfeil før appstart; den vanlege mobilprofilen stod på innlogging. Ingen av profilane vart nullstilte.

Fase 4 er implementert og publisert i [0.17.0-beta03](https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta03). Offentleg APK og oppdatering gjennom appen på TV er kontrollerte. Sjå [verifiseringsrapporten](archive/releases/VERIFICATION_v0.17.0-beta03.md) for testresultat og dei attståande grensene for manuell mobilkontroll.
