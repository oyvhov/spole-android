# Animert oppstart

- `StartupReveal` legg eit matt dekkje over appen medan innhaldet startar å laste.
- «Spole» tonar inn med 12 dp rørsle i 360 ms, og ei diskret lastelinje går under teksten.
- Etter innfadinga ventar dekkjet høgst 1 200 ms på første synkronisering. Deretter tonar det ut i 240 ms. Nettverksfeil kan ikkje halde brukaren fast.
- Compose sin animasjonsskala gjeld òg her. Faneval startar ikkje oppstarten på nytt.
- Android sin system-splash viser berre den sams bakgrunnen, ikkje det gamle launcher-symbolet. Ordmerket blir animert når første Compose-ramme er klar.
- Launcher-symbolet er redusert med om lag 20 prosent; Android eig framleis forma på sjølve ikonflata.
- Eiga nedlastingsrad og tilhøyrande heimeinnstilling er fjerna. Det gamle lagringsvalet blir tolerert, og Aktivitet beheld førespurnadsframdrift.

Visuell kontroll på emulator/eining står att; ikkje rekn kompilering som ei stadfesting av animasjonsflyten.
