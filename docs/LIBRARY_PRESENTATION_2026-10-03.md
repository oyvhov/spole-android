# Bibliotekvising · 3. oktober 2026

## Retning

Biblioteket bruker eit fast bileteformat per vising. Filmkatalogen startar med cover
(2:3), seriekatalogen med miniatyrbilde (16:9). Fokusramma følgjer sjølve biletet,
og fjernkontrollfokus endrar ikkje høgd eller plassering av korta. Mobil får to
breie seriekort ved vanleg kortstorleik, medan TV bruker større kort og meir luft.

«Vising» opnar ein rullbar dialog som held plassen sin medan vala endrar innhaldet
bak. Kvar bibliotekvising kan lagre rutenett/liste, kortstorleik, automatisk bilete,
cover, miniatyrbilde, banner eller logo. Titlar, vurderingar, sett-merke og tal på
usette episodar har eigne brytarar. Dei fire gamle lagringsfelta blir lesne vidare;
nye brytarar får standardverdien på ved oppgradering.

Sett-merket er ei kvit hake på eit lite mørkt felt. Usette episodar har eit nøytralt
talmerke. Manglande vurdering eller usett-tal gir ikkje noko merke. Framdrift blir
ikkje vist på eit kort som tenaren har merkt som sett. Full tittel er tilgjengeleg
for skjermlesar sjølv når synlege titlar er avslått. Filmkort viser årstalet tett
under tittelen; seriekort viser berre tittelen. Ingen av dei reserverer tomme
tittellinjer. Sesong- og episodeinformasjon blir framleis vist der det hjelper
med å velje riktig episode.

## Neste

Bibliotekets «Hald fram» og «Neste episode» er samla i «Neste», både på
biblioteksframsida og inne i eit bibliotek. Ei påbyrja episode får prioritet over
ein neste episode frå same serie. Nyaste påbyrja episode vinn om tenaren returnerer
fleire frå serien. Jellyfin og Emby blir framleis viste kvar for seg.

Berre faktiske hald-fram-kort tilbyr «Fjern frå Hald fram». Neste-episode-kort
held fram med å tilby dei andre korthandlingane. Framsida sine eigne radval gjeld
framleis Framsida.

## Data og bilethenting

- Cover, miniatyr, banner og logo bruker bilete som tenaren faktisk annonserer,
  med kvar sitt bilettag. Ingen serie-thumb blir berre omskriven til ei ukjend
  Primary-adresse. Manglande format fell tilbake til kjent kunst i same kortramme.
- Tal på usette kjem frå den innlogga brukaren sitt `UserData.UnplayedItemCount`,
  aldri frå totalt tal på episodar eller sesongar. Sjå
  [Jellyfin sin brukardatamodell](https://typescript-sdk.jellyfin.org/interfaces/generated-client.UserItemDataDto.html).
- Katalogen blir publisert straks han er henta. Sjanger-/årstalfilter blir oppdaterte
  når deira eige kall er ferdig, og held ikkje bilethentinga tilbake.
- Biblioteksframsida publiserer kvar bibliotekrad når ho er klar, utan å vente på
  det første biblioteket i lista.
- Gridet førehentar berre éi neste rad og avbryt arbeid som ikkje lenger er relevant.
  Førehenting og vising bruker same storleiksval, bilettag og autentiseringshovud.
  Store hero-bilete blir avgrensa til kortstorleik i biblioteket.
- Overgangen frå plasshaldarbilete bruker det innlasta biletet sitt sideforhold.
  Ein ståande plasshaldar kan dermed ikkje zoome eller klippe ei brei miniatyr.
  Dette er eksplisitt valt for bibliotekkort med avgrensa biletstorleik; sjå
  [Coil si innstilling for bileteovergangen](https://coil-kt.github.io/coil/api/coil-compose-core/coil3.compose/prefer-end-first-intrinsic-size.html).
- Nye sidekall blir merkte som pågåande også når den gamle sida er i minnet, slik
  at fleire samtidige «Last fleire»-trykk ikkje startar same side om att.
- Seine resultat blir kontrollerte mot tenar, brukar, profil, biblioteksti og filter.

## Verifikasjon

- 84/84 relevante einingstestar bestått: lagringsmigrering, bileteval og storleik,
  brukardata frå tenaren, samanslåing av «Neste», filtrering og uavhengig lasting.
- 16/16 avgrensa Android-testar bestått for hovudimplementasjonen på den isolerte
  profilen på port 5562. Ytterlegare 12/12 bestått med systemskrift 2.0.
  Mobil- og TV-oppsett, visingsval, lagring, korthandlingar, kjeldeskilje og
  fjernkontrollfokus er dekte med syntetiske lokale bilete.
- Skjermkontrollen fann feil sideforhold i Coil-overgangen frå ståande plasshaldar
  til brei miniatyr. Den siste rettinga har ein eigen test av bildekantane ved
  kald lasting. Denne testen er bestått både med normal skrift og systemskrift 2.0.
- Endeleg debug-APK, test-APK og signert produksjons-APK bygde. Lint: 0 feil.
  Produksjons-APK-en er installert med `-r` på den innlogga TV-profilen, og
  eksisterande kontoar og appval er bevarte. Signeringssertifikatet samsvarer med
  den publiserte beta 1. Dette er eit lokalt review-bygg, med uendra versjonsnummer.
- Første justering av tittellinjer: 5/5 målretta Android-testar bestått med normal
  skrift og 12/12 med systemskrift 2.0. Tomme tittellinjer er fjerna, og testen
  kontrollerer òg avstanden til neste rad. Årstal på filmkort er tekne inn att etter
  presiseringa 4. oktober, med berre 2 dp avstand frå tittelen.
- Etter presiseringa: 3/3 målretta Android-testar bestått med normal skrift og
  3/3 med systemskrift 2.0. Årstalet er synleg rett under filmtittelen i mobilgridet,
  utan ei tom tittellinje. Seriar og listevising er kontrollerte i same gjennomgang.
  Debug-, test- og signert produksjonsbygg er oppdaterte; det siste er installert
  med `-r` på den innlogga TV-profilen. Filmtitlar med éi og to linjer og årstalet
  rett under er visuelt kontrollerte der.
- Ekte Jellyfin-film og -serie er viste på den innlogga Google TV-profilen.
  Miniatyrformat, coverformat, vurderingar, sett-hake, faktiske usett-tal og éi
  «Neste»-rad er visuelt kontrollerte. Ingen sett-status eller serverhistorikk er
  endra under review. Telefonvisinga er kontrollert med syntetiske Android-testdata.
- Omsetjingskontroll: 1432 standardnøklar og éin tillaten standardnøkkel utan
  omsetjing. Ingen manglande nøklar i nynorsk eller bokmål.

Innlogga TV- og mobilprofilar blir ikkje nullstilte. Skjermbilete frå ekte kontoar
blir berre lagra lokalt under den ignorerte byggmappa.
