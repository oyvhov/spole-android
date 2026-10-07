# Oppstart med tenarar på nettet · 6. oktober 2026

Jellyfin-innlogginga var vanskeleg med fjernkontroll. TV-en fekk telefonskjermen ved første start,
oppsettsarket hadde ingen startfokus, og dei første kontrollane var to adressefelt. Knappen
«Godkjenn på mobilen» var grå til begge adressene var fylte ut, og ein grå knapp kan ikkje få fokus.
Ei naken LAN-adresse som `192.168.1.20` vart gjort om til `https://192.168.1.20` utan port.

## Endra

- **Tenarar på nettet.** `LanServerDiscovery` sender `who is JellyfinServer?` og `who is EmbyServer?`
  til UDP 7359 på kvart lokalnett og til 255.255.255.255, éin socket per tenartype. Svar kjem fram
  etter kvart som dei blir mottekne. Peikar svaret på ei IP som ikkje er avsendaren, til dømes eit
  virtuelt WSL/Hyper-V-nettkort, blir avsendaradressa brukt med same skjema og port. Mobil og VPN
  blir hoppa over.
- **Første skjerm** viser «Tenarar på nettverket» som store kort med namn, tenartype og adresse. På
  TV får første tenar fokus når han dukkar opp, men berre om fjernkontrollen ikkje alt er i bruk.
  Utan funn står det kva som skjedde, og «Søk på nytt» er tilgjengeleg. «Skriv inn adressa» og
  «Andre innloggingsmåtar» er der som før.
- **Ein vald Jellyfin-tenar** opnar «Logg inn på Stova» med adressa låst og fokus på «Godkjenn
  på mobilen». Ingenting må skrivast. Svarar Seerr på same maskin (port 5055), blir adressa fylt
  inn. Seerr blir aldri gjetta for offentlege namn, og Seerr er elles av som standard.
- **Ein vald Emby-tenar** går rett til innlogging. Brukarar som tenaren viser på innloggingssida,
  blir viste som brikker; ei brikke fyller inn namnet og flyttar fokus til passordet. Jellyfin skjuler
  brukarane sine som standard, og då blir det ikkje vist nokon brikker.
- **Adresser som blir skrivne inn** blir prøvde med `/System/Info/Public` før innlogging.
  `192.168.1.20` prøver `http://…:8096`, `https://…:8920` og deretter utan port; ein skriven port
  eller ei skriven skjema blir respektert. Offentlege namn blir berre prøvde over HTTPS. Ei Emby-adresse
  i Jellyfin-flyten blir namngitt som Emby. Ei fullstendig adresse som ikkje svarar på prøva, held fram
  til vanleg innlogging, så trege mobilnett ikkje får ein ny feil.
- **Fjernkontroll:** startknappar og «Logg inn» er aldri grå; tomme felt får ei melding når knappen
  blir trykt. Steg 2 i tilkoplingsarket får fokus på første brukarbrikke eller hovudhandlinga, ikkje
  i eit tekstfelt. Adressefelt opnar tastaturet når OK blir trykt, ikkje kvar gong fokus passerer.
  Brytaren for Seerr og «Endre» har synleg fokusring. Den oppdikta eksempeladressa er fjerna for
  Jellyfin og Emby.
- **Animasjon utan tekst.** Venstre kolonne på TV viser éi samanhengande scene med tenar, mobil og
  TV på ei felles grunnlinje. Scena blir teikna som vektorgrafikk, ikkje spelt av frå ei fil.
  Rekkjefølgja er denne:
  1. To mjuke ringar breier seg frå tenaren.
  2. Ein låg boge teiknar seg til mobilen.
  3. Mobilskjermen lyser opp, og ein hake teiknar seg.
  4. Ein like stor boge går vidare til TV-en.
  5. TV-skjermen fyller seg, og spel-symbolet kjem fram. Ein svak aksentglød på 14 % stig bak TV-en,
     same uttrykk som oppstartsmerket.

  Eitt løp tek 7 sekund med FastOutSlowIn per fase. Animasjonen spelar tre gonger og blir ståande
  på sluttbiletet. Med rørsle skrudd av vises sluttbiletet med ein gong. Skjermlesaren les éi setning.
  Bilettakta går gjennom `withInfiniteAnimationFrameNanos`, slik at UI-testar ser eit stilt bilete
  og ikkje må vente på animasjonen.
- Hardkoda «Brukarnamn og passord», «Avbryt» og «Tilbake» er flytte til ressursar på nynorsk,
  bokmål og engelsk. Dei ubrukte «Kom i gang»-tekstane er fjerna.

## Verifisering

- 880 einingstestar: 879 bestått, 1 hoppa over (valfri prøve mot ekte nett), 0 feil. Nye testar:
  `LanServerDiscoveryTest`, `ServerAddressCandidatesTest`, `ServerProbeTest`.
- Den valfrie prøva (`SPOLE_LAN_DISCOVERY=1`) køyrde mot heimenettet frå arbeidsstasjonen og fann
  Jellyfin- og Emby-tenaren der. Begge adressene svara 200 på `/System/Info/Public`.
- Lint: 0 feil, 188 åtvaringar, ingen i nye eller endra filer.
- Animasjonen er fanga bilete for bilete på TV-emulatoren. Han har òg vore kontrollert i
  sluttbiletet etter tre løp. `CombinedSetupUiTest` og `TvSetupTest` består med animasjonen på.
  Emulatoren teiknar med programvare og kjem berre opp i om lag 9 bilete i sekundet, så eit opptak
  derifrå viser ikkje kor jamt animasjonen går på ein ekte TV.
- Isolert TV-profil `Spole_TV_Instrumentation` (5566): `CombinedSetupUiTest` 10/10 (fire nye),
  `TvSetupTest` 4/4, `LinkedLoginTest` 7/7, `AccountOptionsTest` 3/3, `SetupAndDiscoverTest` 5/5,
  `LoginExperienceTest` 8/9, `TvRefinementUiTest` 37/38.
- Dei to feila over, og `SheetInteractionTest` (7 av 10), `ReelstackSmokeTest` (8 av 8) og
  `HomeSearchNavigationTest` (4 av 4), feila likt på uendra HEAD på same TV-profil. Dei er ikkje
  regresjonar frå dette arbeidet, men står att som eldre TV-feil.
- Ende til ende på TV-emulatoren med fjernkontrolltastar: «Skriv inn adressa», `10.0.2.2:8097`, OK.
  Appen fann den ekte Jellyfin-tenaren bak adressa, og tenaren laga ein Quick Connect-kode som
  vart vist. Godkjenninga krev ein innlogga mobil og er ikkje gjord.

## Avgrensingar

- Emulatoren sit bak QEMU sin NAT. Kringkastinga når verten, men svaret kjem ikkje tilbake til
  appen. Oppdaginga er difor verifisert mot ekte nett frå arbeidsstasjonen og med ein injisert tenar
  i UI-testane, ikkje på ein fysisk TV.
- Fysisk SHIELD, full Quick Connect-godkjenning, Emby-innlogging med brukarbrikker mot ekte konto og
  automatisk Seerr på ekte nett er ikkje prøvde. Profilane med ekte kontoar (5560/5564) er ikkje rørte.
- Endringane er med i 1.0.0-beta4. Sjå [verifiseringa av beta4](VERIFICATION_v1.0.0-beta4.md).
