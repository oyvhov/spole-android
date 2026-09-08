# Søkeinngang frå Heim

## Utforming

Ei diskret søkelinje rett under Spole og profilen: «Søk etter filmar og seriar». Heile lina er
éi handling, ikkje eit skrivefelt som opnar tastaturet før navigering. Ho brukar sidebakgrunnen,
nedtona tekst, søkeikon og ein tynn skiljestrek. Ingen limeknapp, eigen kortramme eller hjelpetekst.
Dette erstattar den første, meir framheva skissa etter brukaren si tilbakemelding.

Oppdag forklarer no føremålet konkret: «Finn noko nytt å sjå. Legg til det du vil ha i biblioteket.»
Dei eksisterande bibliotek- og Seerr-søka, sesongvala og stadfestingane er uendra.

## Overgang og tilstand

- Feltet deler visuelle grenser mellom Heim og Oppdag gjennom Compose SharedTransitionLayout.
- Flytting/endring av storleik brukar ein 320 ms kurve utan fjør; innhaldet tonar over.
- Tastaturet blir opna etter at navigeringa er ferdig og søkefeltet er montert. Ingen fast
  tidsforsinking prøver å gjette når animasjonen er ferdig.
- Eit eksplisitt søk frå Heim går til toppen og nullstiller type-/statusfilter, men bevarer søketeksten.
- Vanlege fanebesøk bevarer filter og rulleposisjon, utan å be om tastaturfokus.
- Tilbake/fanebyte avbryt uteståande fokus og lukkar tastaturet. Raske ekstra trykk på utgåande
  Heim kan ikkje starte fleire søkeovergangar.
- Compose følgjer Android si animasjonsskala. Ingen endring av innlogging eller serverdata.

## Visuell kontroll

Den eksisterande Spole-designen er referansen, ikkje ein ny nett- eller bildegenerert app.
Kontrollpunkta er ordlyd og plassering, typografihierarki, eksisterande fargar, avrunding og
treffflate, samanhengande feltposisjon og tastatur etter overgang. Android-emulatoren er
verifiseringsflata; nettlesaren kan ikkje gjengi denne innfødde Compose-overgangen.

## Testdekning

HomeSearchNavigationTest dekkjer synleg inngang, fokus/skriving, vanleg fanebesøk utan autofokus,
tilbakeveg, nullstilling av filter og bevaring av søketekst. HomeSearchEntryTest dekkjer éi
trykkhandling og lesbar tekst på 300 dp breidd med dobbel skriftstorleik.

Primærreferanse for overgangsmønsteret:
[Android Developers – Shared element transitions](https://developer.android.com/develop/ui/compose/animation/shared-elements).
