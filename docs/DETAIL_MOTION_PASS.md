# Detaljar og rørsle · første del

Implementert:

- Opningsbiletet og tittelen er synlege medan metadata lastar. Hero-layouten blir ikkje bytt når svaret kjem.
- Film og seriar med poster bruker poster til venstre og tekst til høgre. Episodar beheld breitt bilete.
- Metadata tonar inn under heroen; skeletons erstattar ikkje lenger coveret.
- Nye fakta og eventuell oppdatert tittel blir tekne vare på i detaljinnhaldet.
- Systemstyrt haptikk ved opning og ingen Coil-biletovergang når Android-animasjonar er slått av.
- Eksisterande fast popuphøgd og fast lukkeknapp er bevarte.

Verifisert: debug-bygg, 203 JVM-testar og 10/10 SheetInteractionTest på emulator-5562. Testane dekkjer opningsretning, sein metadata, cover under lasting, stabil ramme, scrolling og lukkeknappen. Ingen ekte kontoar eller fysisk haptikk er testa.

Står att frå ønsket om 2, 3 og 6:

- Ekte delt cover-overgang frå kortet til detaljpanelet. Android-dialogen har eige vindauge; denne runden innfører ikkje koordinatoverføring mellom vindauga.
- Rikare sesong-/episodevising og brei serieillustrasjon der kjelda tilbyr dette.
- Fargetoning frå omslag og meir finpuss på kortrespons/avspelingsframdrift.

Ingen ny release i denne runden.
