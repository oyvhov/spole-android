# Logg ut av alle tenester

Under Innstillingar → Tenester finst «Logg ut av alle tenester» på TV, nettbrett og telefon.
Eitt trykk startar utlogginga, utan stadfestingsdialog. Teksten seier uttrykkeleg at dette gjeld
denne eininga og at utsjånad og Heim-oppsett blir bevarte.

Alle fem tenester blir logga ut, også administratortenester som ikkje er synlege for kontoen.
Lokale tilgangsteikn, brukar-ID-ar og øktdata blir fjerna. Tidlegare tenesteadresser blir hugsa
for enklare ny innlogging. Eit tomt tenesteoppsett overskriv ikkje ei tidlegare hugsa adresse.

ViewModel stoppar pågåande arbeid og lukkar øktkanalen. Private skjermdata blir erstatta med
ein tom tilstand. Eventuelle pågåande lagringar får avslutte før ein siste opprydding av
innlogging og den lokale medieoversikta. Brukaren ser «Loggar ut…» medan dette skjer og
kjem deretter til oppstarten. Ei ny innlogging kan ikkje starte midt i oppryddinga.

Medievarsel blir tekne bort, og widgeten blir beden om å oppdatere. Bakgrunnsoppdatering og
widget kontrollerer at kontoen framleis er den same før dei viser resultat frå eldre kall.
Tema, Heim-rekkjefølgje og andre personlege val blir ikkje nullstilte. Kontobundne bibliotekval
og lokal førespurnadssporing blir verande avgrensa til den opphavlege kontoen; serverdata blir ikkje sletta.

Dette er lokal utlogging. Det tilbakekallar ikkje økter på andre einingar og slettar ikkje brukaren på tenaren.

## Kontroll

- SignOutAllTest kontrollerer sletting for alle tenester etter ny repository-instans,
  bevaring av adresser og Heim-oppsett, og eitt trykk i tenesteinnstillingane ved skriftstorleik 2.0.
- RequestHistoryFlowTest held tilbake eit nettverkssvar under utlogging, kontrollerer tom
  kontotilstand og at oppsettet kan opnast på nytt.
- PublicScreenshotsTest tek bilete av faktisk app med berre demodata på isolerte testeiningar.

Funksjonen er ikkje del av den publiserte alpha24-APK-en.

Verifisert 13. september 2026: 473/473 einingstestar, 8/8 Android-testar på TV,
8/8 på telefon og 3/3 i nettbrettformat. Bygg og lint bestod utan feil.
Utloggingsraden er visuelt kontrollert ved skriftstorleik 2.0 i alle tre format.
Eksisterande smale kategorikolonnar på TV/nettbrett deler framleis enkelte lange ord
ved denne skriftstorleiken; sjølve utloggingsraden er lesbar og kan trykkjast.
