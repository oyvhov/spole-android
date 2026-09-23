# Spole 0.18.0-beta7

Denne testutgåva gjer at nedlasting faktisk verkar, og gjer heimeskjermen og avspelingsstarten raskare.

## Rettar

- **Nedlasting avviste nesten alt.** Spole spurde tenaren om fila med ein nøktern standardprofil
  (H.264 og stereo-AAC) i staden for det telefonen faktisk kan spele. Filer med 5.1-lyd, HEVC eller
  4K fekk difor «berre direkte filer». No gjeld same vurdering som for vanleg avspeling.
- Ein bilettekst (PGS) som standardspor gjer ikkje lenger fila til eit avslag. Alle innebygde
  tekstspor blir med i nedlastinga.
- Nedlasta filer med EAC3- eller DTS-lyd spelar med lyd.
- Ei nedlasting som starta heime, held fram over den andre adressa når du går ut av heimenettet.
- Appen kunne krasje i bakgrunnen når Android starta han for ein planlagd oppdatering. Det er
  retta, og uferdige nedlastingar held fram når du opnar appen.

## Raskare

- Heimeskjermen hentar rader og bibliotek frå Jellyfin og Emby samstundes, med eit tak på seks
  kall om gongen per tenar. Før gjekk om lag tjue kall etter kvarandre.
- Utanfor heimenettet finn Spole adressa som svarar på eit par sekund, i staden for å vente om lag
  eit halvt minutt på den lokale.
- Avspeling startar etter færre rundturar til tenaren.
- Detaljsida for ein serie viser episodane tidlegare.
- Mindre hakk når du blar i rader: innloggingstoken blir ikkje lenger dekryptert for kvart bilete,
  og breie kort hentar bilete i kortstorleik.
- Mindre trafikk: avspelingssjekken startar ikkje på nytt for kvar detaljside og hentar ikkje
  kontoprofilane på nytt kvart femte sekund.

## Avgrensingar

- Media som krev omkoding, livestream, DRM og plateavtrykk kan framleis ikkje lastast ned.
- Nedlasting er framleis avgrensa til vaksne profilar på telefon og nettbrett.
- Tekstspor som ligg i eigne filer ved sida av videoen, blir ikkje med i nedlastinga.
