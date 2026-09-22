# Spole 0.13.0

Åtte nye funksjonar, og dei svake ledda som kom fram under gjennomgangen av appen.

## Nye funksjonar

- **Hald fram å sjå.** Ny rad på Heim med halvsette filmar og episodar, henta frå Jellyfin og Emby
  si eiga resume-liste. Avspelingsposisjonen låg alt i modellen, men vart aldri vist. Rada spør
  kvart bibliotek for seg, så barnebiblioteka aldri kan kome inn denne vegen, og kan skruast av
  i Innstillingar.
- **Søk i eigne bibliotek.** Oppdag søkjer no i Jellyfin og Emby i tillegg til Seerr. Titlar du alt
  eig kjem øvst under «I biblioteka dine». Dei to kjeldene svarar uavhengig, så eit søk verkar
  sjølv om den eine er nede.
- **Trekk tilbake ein førespurnad.** Aktive førespurnader i Aktivitet kan trekkjast tilbake, med
  stadfesting. Appen kontrollerer at førespurnaden er din før slettinga blir send.
- **To adresser per teneste.** Legg til ei alternativ adresse under avanserte val, til dømes
  heimenettet og ein proxy utanfrå. Appen byter automatisk når den vanlege adressa ikkje svarar,
  og berre eit mislukka forsøk kostar noko ekstra. Tilgangsteiknet høyrer til tenaren, så du
  treng ikkje logge inn på nytt.
- **Opne i medieappen.** Detaljpopupen finn ein installert Jellyfin-/Emby-klient som handterer
  tenaradressa og opnar han direkte. Knappen namngjev appen han opnar.
- **Varselkanalar per hending.** «Klart i biblioteket», «lastar ned» og «stoppa» er tre kanalar i
  Android-innstillingane, så dei kan stillast eller slåast av kvar for seg.
- **Mellomlager i SQLite.** Dashbordet ligg no i ein Room-database i staden for éin JSON-tekst.
  Rader kan lesast sidevis, og ei forelda kopi blir sletta i staden for tolka.
- **Paginering og widget.** Søkjeresultat frå Seerr stoppar ikkje lenger på dei første 20, og
  «Spelar no» finst som heimeskjermwidget.

## Rettingar

- Innloggings- og tilkoplingsfeil viser nynorsk tekst med neste steg i staden for rå
  Java-nettverkstekst. Eit vertsnamn som ikkje svarar, ein tenar som er av og eit sertifikat
  Android ikkje stolar på får kvar si melding.
- Vanleg HTTP er berre tillate for ekte private adresser igjen. Sjekken var eit prefikstest på
  vertsnamnet, så namn som `fcbarcelona.com` og `192.168.1.5.nip.io` opna for ukryptert trafikk
  over det opne internettet.
- Heim viser den sist stadfesta feeden ved kald start. Mellomlageret vart skrive ved kvar
  oppdatering, men aldri lese.
- Aktivitet grupperer etter faktisk tidspunkt. «For 5 dagar sidan» hamna under «I DAG».
- Detaljpopupen viser igjen kvar tittelen kjem frå.
- Ei uventa feil under oppdatering krasjar ikkje lenger appen.
- Følgjing av førespurnader går i lågt tempo når Aktivitet ikkje er open.
- Éin uventa oppføring frå ei teneste tømmer ikkje lenger heile rada.

## Oppgradering

Same pakkenamn (`app.reelstack`) og same signeringsnøkkel som 0.12.4, så APK-en kan installerast
som ei vanleg oppdatering. Mellomlageret blir bygd opp att ved første oppdatering; innloggingar og
følgde førespurnader er bevarte.

Bygget har fått to nye avhengigheiter: KSP 2.3.10 og Room 2.8.4.
