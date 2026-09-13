# Rekkefølgje på Heim

Under **Innstillingar → Heim → Rekkefølgje på Heim** kan brukaren flytte radene med opp-/nedpilene.
Valet finst på TV, mobil og nettbrett. Det gjeld radene under toppbiletet; søk og toppbilde held plassen sin.

- Endringar blir lagra straks på eininga og blir lesne ved neste oppstart.
- Skjulte rader kan flyttast og hugsar plassen sin når dei blir slått på att.
- Tomme rader hugsar plassen sin medan innhaldet blir henta frå tenestene.
- Jellyfin og Emby har framleis separate film- og serierader. Ei teneste som ikkje er sett opp,
  tek ikkje plass i redigeringa, men den lagra plasseringa blir bevart.
- Samanslått «Sjå vidare» blir flytta som éi rad. «Neste episode» får att si eiga lagra plassering
  når samanslåinga blir slått av.
- **Standardrekkefølgje** nullstiller berre rekkefølgja, ikkje synlege rader eller andre innstillingar.

## Lagring og kompatibilitet

`HomeRow` har stabile identifikatorar, uavhengig av språk, synlege rader og serverresponsar.
`home_row_order` i `AppPreferencesRepository` lagrar ei ordna liste. Gamle installasjonar får
same rekkefølgje som før. Ukjende identifikatorar og duplikat blir ignorerte; nye rader blir lagde til
etter dei lagra radene. Den eksisterande migreringa av synlege rader er uendra.

Flytteknappane bevarer fokus på rada etter flytting. Ved øvste/nedste plass får den andre pila fokus,
slik at fjernkontrollen ikkje blir ståande på ein deaktivert knapp.

## Verifisering 13. september 2026

- 469/469 einingstestar bestått, inkludert fire nye testar for standardrekkefølgje,
  gamle/ukjende verdiar, flytting og bevaring av plassen til fråkopla tenester.
- 10/10 Android-testar på isolert TV (5566), 18,871 sekund.
- 10/10 Android-testar på isolert mobil (5562), 34,483 sekund.
- Testane dekkjer faktisk rekkefølgje på Heim, gjenteken flytting med fjernkontroll på TV
  og trykk på mobil, fokus ved øvste grense, nullstilling med vising av toppen, lukking/opning,
  lagring ved ny repository-instans og uendra val for skjulte rader.
- Visuell kontroll av dialogen på TV og mobil; skriftstorleik 2.0 er med i Android-testane.
- Dei seks eksisterande `HomeMediaRowsTest`-testane bestod på begge einingar.
- `assembleDebug`, `assembleDebugAndroidTest` og `lintDebug` bestod. Lint: 0 feil og 31 eksisterande åtvaringar.
- Testane brukar syntetiske data. Ingen instrumentering eller sletting på profilane med ekte kontoar.

Dette er ei kodeendring for neste APK; ingen ny produksjonsrelease er publisert som del av oppgåva.
