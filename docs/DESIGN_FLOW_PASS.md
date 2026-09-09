# Design og flyt — 9. september 2026

## Levert i denne bolken

- Same fokusmarkering på bibliotek, anbefalingar, Spelar no, komande titlar og Oppdag.
- Eit felles mønster for filter og handlingar: fokus er ikkje det same som val.
- Ingen permanent biletkant. Ved tastatur/fjernkontroll tonar ein lys kontur inn over 110 ms, utan å skalere kortet eller flytte naboinnhaldet.
- Oppdag held detaljar og førespurnad som to separate handlingar.
- Detaljhandlingane på anbefalingar og Oppdag brukar eksisterande nynorsk/engelsk språkressurs.

Dette er ei avgrensa interaksjonsforbetring, ikkje ein full visuell redesign eller ei ferdig TV-utgåve. Claude sine tidlegare endringar er bevarte.

## Verifisering

- 283 JVM-testar: bestått.
- Android-bygg: debug og testpakke byggjer.
- Lint: ingen feil, 28 åtvaringar.
- 26 Android-testar bestått på mobilformat: fokus, Oppdag/oppstart, Aktivitet/innstillingar, brei navigasjon og popup-interaksjon.
- Sluttbygget: dei same 26 testane og 5 språktestar bestått på nettbrettformat (1920 × 1200, 240 dpi), totalt 31. Dette er emulatorformat, ikkje fysisk TV-godkjenning.
- Nye testar måler at fokus faktisk endrar pikslane, men ikkje kortgeometrien, at fokus ikkje klikkar, at filter krev stadfesting, og at detaljar/førespurnad ikkje utløyser kvarandre.
- To eksisterande testar vart gjorde skjermuavhengige: mobiltesten vel eksplisitt mobilformat; sidemenytesten samanliknar avrunda fysiske pikslar ved brøktettleik i staden for ein for streng dp-toleranse. Testen krev framleis uendra sidegeometri under animasjonen.

Den innlogga gjennomgangsemulatoren vart inspisert før endringane. Automatiske testar brukar den separate testemulatoren, ikkje private kontoar. Dette dokumenterer ikkje fysisk fjernkontroll, Google TV-systembilete, ekte førespurnader eller nye målingar av første popup-opning med sein metadata. Desse punkta står att. Ingen publisering eller versjonsendring i denne bolken.
