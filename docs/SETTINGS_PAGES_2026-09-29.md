# Sider og meny (2026-09-29)

Gjennomgang av innstillingane «Heim og navigasjon» og «Meny», og det som kom ut av han.

## Funn

1. **To kategoriar for éin ting.** TV og nettbrett hadde både «Heim og navigasjon» og «Meny».
   Navigasjonen låg ikkje i «Heim og navigasjon», men i «Meny». «Vel bibliotek» stod i begge.
2. **Biblioteksida hadde tre stader.** Dei var «Vel bibliotek» (med Lagre og Avbryt), «Tilpass
   biblioteksida» (rader, rekkjefølgje og skjul bibliotek) og brytaren «Bibliotekoversikt», som bytte
   ut heile sida. To av dei kunne skjule eit bibliotek, kvar på sin måte.
3. **Utvalsvala gjorde ingenting på telefon.** Telefonen har ikkje noko utval på Heim. «Utvalt på
   Heim» slo i tillegg av utvalet på biblioteksida.
4. **Vala stod på feil side.** «Vis vurderingar», «Vis bilete- og lydkvalitet» og «Kommande
   episodar» stod under «Bibliotekvising», men styrte detaljsidene.
5. **Skjulte bibliotek kom ikkje fram før omstart.** Nøkkelen `library_hidden` mangla i
   observatøren til `AppPreferencesRepository`.

## Slik er det no

**Innstillingar → Sider og meny** er éin kategori på TV, nettbrett og telefon:

- **Sider**
  - **Tilpass framsida**: radene per teneste, som før. «Utval» står øvst på TV og nettbrett.
    «Søkelina på framsida» står under «Val» på telefon. Kvart val blir berre vist der det gjer noko.
  - **Tilpass biblioteksida**: bygd på same måte som Tilpass framsida, med same radkomponent
    (`LayoutEditorRow`).
- **Meny**: startside, «Skjul sidemenyen» (TV), rekkjefølgje og synlegheit for sidene,
  «Nedlastingar i menyen» (mobil) og «Nullstill menyen».

### Tilpass biblioteksida

- **Bibliotek**: brytaren viser eller skjuler biblioteket i Bibliotek-fana og i søk. Pilene flyttar
  det. Rekkjefølgja gjeld både sida og sidemenyen. «I sidemenyen» og ikonvalet blir berre viste på
  TV og nettbrett, der sidemenyen har biblioteksnarvegar.
- **Innhaldsrader**: Utval, Hald fram, Neste episode, Favorittar og Biblioteksrader, kvar med brytar og pilar.
- **Vising**: breie biblioteksbilete, biblioteksnamn og sideoverskrift.

Rader, rekkjefølgje og vising blir lagra med ein gong. Bibliotekvalet lastar biblioteket på nytt,
så det blir lagra éin gong, når editoren blir lukka med «Ferdig» eller tilbakeknappen. Blir ingenting
endra, blir ingenting lasta på nytt. Tilpass-knappen nedst på biblioteksida opnar den same editoren.

Utan tilkopla Jellyfin eller Emby viser editoren berre radene og visinga.

### Fjernkontroll

- Editoren gir første bibliotek fokus når han opnar på TV. Utan startfokus gjekk første trykk til
  «Ferdig» under lista, og alle trykk etter det blei ståande der.
- «Ferdig» og «Tilbakestill» i båe editorane har synleg fokusring (`focusOutline`). Ein
  Material-knapp viser ikkje fokus sjølv på TV.
- Ei rad utan eigne val har pilene på same linje som brytaren. Ved skriftstorleik 1.5 og over
  får raden to linjer, slik at tittelen får plass.
- Snarvegsknappen seier kva tilstand han er i: «✓ I sidemenyen» eller «+ Legg i sidemenyen».

## Fjerna brytarar

| Brytar | Fast åtferd no |
|---|---|
| Bibliotekoversikt | Biblioteksida er alltid oversikta. Rader kan skjulast i editoren. Den gamle mappesida (`LibraryLanding`) er sletta. |
| Roter utvalet | Utvalet byter tittel av seg sjølv og stoppar når det har fokus. Med «Rolege overgangar» eller lett TV-modus står det stille. |
| Filmlogo i utvalet | Logoen blir vist når han finst, elles tittelen. |
| Kompakt utval | Storleiken følgjer vindauget og «Biletstorleik». |
| Vis vurderingar | Vurderingane blir viste når serveren har dei. |
| Kommande episodar | Blir alltid viste når Seerr er kopla til. |

Ved neste lagring slettar `AppPreferencesRepository` dei lagra verdiane for desse nøklane
(`hero_rotate`, `hero_logo`, `hero_compact`, `library_hub`, `show_upcoming_episodes`, `show_ratings`,
`show_quality`). Eit gammalt «av» kan difor ikkje kome tilbake i ein seinare versjon.

Etter tilbakemelding vart eit nytt, tydelegare val lagt til under **Utsjånad**:
**Vis teknisk medieinfo**. Det styrer oppløysing, videokodek, lydformat og kanaloppsett i
detaljvisinga og er av som standard. **Skjul sidemenyen** på TV ligg òg under Utsjånad.

## Overgang

- **Skjulte biblioteksfliser** (`libraryHidden`): Eit bibliotek som berre var skjult på biblioteksida,
  står avslått i editoren. Første gong editoren blir lukka, blir det lagra som ikkje valt, og lista
  blir tømd. Frå då av er biblioteket òg borte frå søket.
- **Snarvegar i sidemenyen** held den gamle rekkjefølgja til eit bibliotek blir flytta i editoren. Då
  følgjer menyen rekkjefølgja til biblioteka. Ein ny snarveg kjem sist.
- **Utvalet på biblioteksida** følgjer berre rada «Utval» i Tilpass biblioteksida. Utvalet på
  framsida styrer det ikkje lenger.

## Testar

- `SettingsPagesTest` (Robolectric, 13): éin kategori og éin inngang per side, lagring av
  bibliotekvalet ved lukking, ingen omlasting utan endring, overgang frå `libraryHidden`, rader og
  vising lagra med ein gong. Testane dekkjer òg at telefonen ikkje får snarvegar, rekkjefølgja i
  menyen på nettbrett, editoren utan tenar, skriftstorleik 2.0 og val i Tilpass framsida per eining.
- `PersonalizationObserverTest`: `library_hidden` når skjermen, og nøklane som er fjerna, blir sletta.
- Instrumenteringstestane som brukte dei gamle dialogane og brytarane, er skrivne om:
  `Alpha12UiTest`, `LibraryBrowserUiTest`, `LibraryServiceDisplayTest`, `DesignRefreshUiTest`,
  `TvRefinementUiTest` og `TvSettingsRedesignTest`. `TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus`
  er ny; startfokus i ein dialog kan ikkje testast i Robolectric, som ikkje gir dialogvindauget fokus.
  Instrumenteringstestane er kompilerte, men ikkje køyrde i denne runden.

## Verifisering på eining

Signert release-APK, installert med `install -r` på TV-eininga `emulator-5564` med ekte
Jellyfin-/Emby-kontoar:

- «Sider og meny» erstattar «Heim og navigasjon» og «Meny». Kategorien «Meny» finst ikkje lenger.
- «Tilpass biblioteksida» opnar både frå innstillingane og frå tilpass-knappen nedst på
  biblioteksida. Fokuset startar på første bibliotek.
- «Samlingar» vart slått av og editoren lukka med tilbakeknappen. Biblioteket forsvann frå
  biblioteksida. Det vart slått på att og lagra med «Ferdig», og biblioteket kom tilbake.
  Snarvegane i sidemenyen var uendra heile vegen.
- Biblioteksida viser framleis utvalet med logo og vurdering (Rotten Tomatoes 92 %).
