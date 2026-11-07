# UI-gjennomgang av Spole på Shield mot Apple og Google

2026-11-07, Claude. Lesande gjennomgang av design og overgangar i installert Spole
1.0.0-beta36 (kode 173) på SHIELD Android TV. APK-en er byte for byte lik
`app/build/outputs/apk/release/app-release.apk` frå `4e992d5` (SHA-256 `29c5a1e508f36b26d59e…`).
Ingen appkode endra, ingenting installert. Sidemeny, nynorsk, «Rolege overgangar» av,
fokus «Løft, som Stova», Lett TV-modus av. **Animasjonsfarten på Shield er 0,5×**
(utviklarval), så alle målte tider er halve.

Full rapport med skjermbilete og opptak er levert til eigaren som lokal HTML. Bileta viser
den ekte kontoen og ligg difor ikkje i repoet.

**Hending:** eit OK-trykk for mykje starta avspeling av ein film på den ekte
Jellyfin-kontoen i om lag 49 s. Ho vart stoppa. Detaljsida sa framleis «Spel av», og
tittelen kom ikkje inn i «Sjå vidare», men økta kan stå i aktivitetsloggen til Jellyfin.

## Prioritet

1. **P1 – helt → rader har inga formgjeven overgang.** Opptak: kvart trykk gir om lag 6
   bilete på 0,08–0,13 s. Første Ned rører ikkje sida (berre om lag 5 % større kort). Andre
   Ned rullar minimalt og lèt halve heltknappar stå over skjermkanten. Opp til helten
   kneppar utan animasjon. Årsak: `FadedEdgeBringIntoView` (minimal rulling,
   `HomeScreen.kt:306–315`), `feedState.scrollToItem(0)` (`HomeScreen.kt:371`), og
   `rotationEnabled = featureVisible` som byter helt medan fokus er i radene (`:365`).
   Tilråding: to tilstandar. Ned frå helten animerer heile sida éin gong (om lag 400 ms ved
   1×, ease-out) til ei fast linje for overskrifta til rada med fokus, rett under
   tryggsona. Kvar neste rad landar på same linje. Opp animerer tilbake. Heltebytet stoppar
   når fokus forlèt helten. Tonovergang i staden for hard overkant.
2. **P1 – Tilbake på Heim avsluttar appen** frå rad 3. Apple: Menu fører fokus til toppen
   først. Tilråding: første Tilbake rullar til helten, neste avsluttar.
3. **P1 – fokus er for svakt på kort.** «Løft» gir berre om lag 5–6 % skala, utan ring,
   skugge eller lys. Apple: løft, lys og animasjon. Google: 1,025–1,1× med glød, kant eller
   farge. Tilråding: om lag 1,08–1,1×, mjuk skugge, litt dempa naboar. Sitjeprøve før val.
4. **P1 – heltebytet går via ein tom, mørk helt.** Ved 5,75 s er det gamle biletet borte,
   ved 5,80 s òg teksten, så tonar det nye inn til 6,30 s (luma 60 → 49 → 58).
   `TabletLibraryFeature.kt:331–339` skal halde det gamle synleg. Årsaka er ikkje
   fastsett.
5. **P2 – mørke mellomsider ved sidebyte.** Tittel opna frå Heim: luma 72 → 31 i om lag
   0,3 s. Oppdag-detalj: skjelett 0,6 s. Bibliotek: tom side i om lag 0,1 s, og
   fokusringen hoppar til «Heim». Kald oppstart: om lag 1,4 s mørk skjerm før logoen.
6. **P2 – avkapping og tryggsoner.** Kort skorne av mot menyen («e Celebrity Traitors»),
   sesongfane «esong 6», episodetitlar om lag 26 px frå nedre kant. Apple: 60 pt øvst og
   nedst, 80 pt på sidene, symmetrisk delvis skjult innhald, toning ved avkorting.
7. **P2 – sidemenyen dekkjer innhaldet utan dimming** og kappar heltetekst og knappar.
8. **P2 – to titlar i helten** når bakgrunnsbiletet har tittelen trykt inn (Kongen
   Befaler). **Like radtitlar** («Sjå vidare» og «Favorittar» for kvar tenar).
   **«Be om» står under** «Følg i kalenderen» og «Trailer» på Oppdag-detaljar.
9. **P3** – detaljside utan bakgrunn blir flat (Eşref Tek), undertekstar på 12 sp (24 px)
   i grått nær Apple-minimum 23 pt, og portretta under «Medverkande» poppar inn.

Bevar: Oppdag (toppen følgjer fokus, fast radlinje, ein god modell for Heim),
detaljsidene, krysstoninga tilbake og innstillingsstrukturen.

## Avgrensing

Ikkje prøvd: spelaren med vilje, søk, toppmeny, profilbyte, barnemodus, stor tekst, Rolege
overgangar, TalkBack og langt trykk. Ingen førespurnader sende, ingen innstillingar endra.
Spelet «Brua» stod framme før gjennomgangen. Shield står no på Stova, etter Tilbake-prøva.

## Kjelder, lesne i denne økta

Apple: Designing for tvOS, Focus and selection, Motion, Layout (tvOS: 60/80 pt
tryggsone), Tab bars (tvOS: Menu fører til fanelina, toning ved avkorting), Typography
(tvOS: standard 29 pt, minimum 23 pt). Google: Focus system (skala 1,025–1,1×, glød
2–32 dp), Layouts (ei tydeleg rolle per retning, rom for skala), Tabs (pille for
hovudmål).

## Retta i beta37

Alle punkta over er retta i 1.0.0-beta37; sjå `VERIFICATION_v1.0.0-beta37.md`.
