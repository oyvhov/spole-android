# Spole layout

Mobilpresisering (1. oktober 2026): telefonens titteldetaljar bruker eit fast
96 %-ark med kunst heilt ut til kantane, tittel/serielogo på ein mørk overgang og
sentrerte metadata. Lukkeknappen ligg fast over kunsten, utan generisk overskrift.
Avspeling ligg i eit eige fast botnfelt over systeminnfellinga; lesekolonnen held
av plassen og kan rullast uavhengig. Andre popupoverskrifter, nettbrett og
TV-detaljar held på oppsettet sitt.
Sjå [detaljvising på mobil](MOBILE_DETAILS_2026-10-01.md).

Visuell presisering (8. oktober 2026): «Spelar no»-korta har eit heilt cover med `Fit`, og teksten
ligg ved sida av ved normal skrift. Stor skrift stablar innhaldet. Toppfeltet er lågare, og
hald-fram-rader held av to tittellinjer. «Neste» i Bibliotek bruker kompakte bilde-/tekstrader på
TV og breie vindauge; skriftstorleik 2.0 bruker den romslegare, stabla forma. TV-fokus har svakare
glød og mindre løft. Eit fokusert ikon i ein samanfalda meny kan vise namnet utan å ta fokus.
Sesongførespurnader i Aktivitet deler cover når medie-ID, kvalitet og type er like. Ein veljar
viser dei opphavlege førespurnadene og statusane; handlingar gjeld berre valt førespurnads-ID.

Kontroll 8. oktober 2026: 62 relevante einingstestar og 37 Android-testar bestod; lint hadde
0 feil, 189 åtvaringar og 1 hint. Det signerte lokale bygget vart installert med `-r` på dei lagra
review-profilane. TV vart kontrollert med ekte Jellyfin-, Emby- og Seerr-innhald; mobilen brukte
sin eksisterande, eksplisitte demomodus. Heim, Bibliotek, sesongval med fjernkontroll og
«Spelar no»-visinga vart kontrollerte med skriftstorleik 2.0. Begge einingane fekk tilbake 1.0.
Ingen aktive tenarøkter var tilgjengelege under denne gjennomgangen; ekte øktbilde vart
kontrollerte 7. oktober, og dei nye korta vart kontrollerte med demo og isolerte Android-testar.
Bygget er ein lokal review-APK med versjon 1.0.0-beta5 (142). Skjermbilete ligg berre lokalt.

Current override (alpha13): every width threshold lives in `WindowLayoutPolicy` —
`useSideBySideMedia` 600, `useNavigationRail` 640, `useInlineHeader` 680, `useCenteredDialog` 840,
`useTabletCanvas` 900, `expandSidebarByDefault` 1000 dp. A screen never writes its own dp
comparison. Section headings on a page use `ReelLayout.SectionTop` (22 dp) and
`ReelLayout.SectionBottom` (10 dp); page titles are `displaySmall` everywhere. Loading skeletons
take their colour from the theme and their corners from `ReelLayout.ArtworkCorner`. This supersedes
the threshold numbers quoted below.

Current override (alpha03, 9 September 2026, unreleased): media pages fill the available width
after navigation. Home media rails bleed to the trailing edge on wide windows; profile/header
controls keep 24 dp padding. Reading/settings pages stay capped at 840 dp. Navigation still switches at a 640 dp window. A wide dialog window (at least
840 × 480 dp) centres a 720 dp panel capped at 860 dp height. Narrow windows keep the bottom sheet.
Dialog height is calculated inside safe/IME constraints, independent of metadata. Full physical
tablet support is not yet verified. See [the roadmap](../ROADMAP.md) and [language implementation](LOCALIZATION.md).

Current override (Spole 0.12.4): opening an already configured service shows a compact verified
summary first. The credential method, address change and sign-out action are progressively disclosed
from that summary. Expansion animates only the inner content; the fixed popup viewport does not move.

Current override (Spole 0.12.3): Home episode cards no longer reserve a second title line for
every item just because one title is long. Discover and Activity put the verified personal Seerr
portrait in the top-right account target. Discover has one horizontal filter rail: type choices stay
visible and availability opens from one status chip. Activity shows a compact source menu for admins;
personal completed requests are compact summary cards, while only active requests expose progress
and notification controls. Avatar bytes are retained in a small memory cache across lazy-list reuse.

Popup override (Spole 0.12.2): `StableSheetDialog` now owns the fixed 82% surface, system/keyboard
insets and non-bouncing entrance/exit. There are no Material drag anchors. Detail loading is a
single skeleton-to-content fade after the entrance; no partial-text reflow while the surface moves.

Popup override (Spole 0.12.1): all five modal routes share a fixed viewport and one pinned toolbar.
The X sits at the top-right trailing inset, never within scrolling content. Body scroll gestures no
longer drag the outer sheet; close with X, scrim tap or Android Back. No drag-handle affordance remains.
The calendar's explicit back button still returns to the saved date/filter. These rules replace the
older content-sized connection/playback exceptions and swipe-to-dismiss references below.

Current overrides (Spole 0.12.0): Home uses Spole identity + personal avatar, no date/greeting/calendar shortcut,
and no empty playback section. Calendar stays under Upcoming. Discover uses image overlays and embedded request
buttons, with separate type/availability filters. All remote-content sheets share the same 82% viewport;
text expands inside it. See [the current review](REVIEW_v0.12.0.md) for the latest changes.

## Navigation and hierarchy

Four persistent destinations: **Heim**, **Oppdag**, **Aktivitet**, **Innstillingar**. The navigation owns space rather than covering the feed. Each tab retains its scroll position. Home carries the compact Spole identity; the other pages lead with their task and a personal account target where relevant.

- **Heim:** compact Spole identity and personal avatar, active session cards only when something plays, separate recently-added movie/episode rails for each library service, upcoming releases, download queues. No greeting, date, continue-watching feed or connection-count banner.
- **Oppdag:** search, media-type filter, adaptive poster grid. Availability is text below artwork, never an overlay
  obscuring a face. The card opens details. A second, loud button that also opens details is not an action, so a
  title that cannot be requested shows its status as a line instead; only a requestable title gets a filled button.
  One classification (`DiscoverMedia.isSeries`) drives the type filter, the type badge and the request wording.
- **Aktivitet:** personal requests first, using compact completed cards and expanded progress only for work in flight. Administrator source scope is a single menu rather than another chip row. Service activity is grouped by day, then title first, status second, time third. The status line already names the
  service, so the meta line does not repeat it. Every thumbnail has the same width; films keep 2:3 and episodes
  16:9, so titles share a left edge without either format being cropped. Source filters keep request and
  download activity easy to isolate.
- **Innstillingar:** «Kontoen din» says who you are signed in as and appears only once something is configured;
  «Tenestene dine» is the single place to add an address or sign out. Every row in the account card puts its
  action in the same trailing position, and names the service in its spoken label. Home switches appear only for
  services that actually have a connection. Preferences and local app identity last.
- **Konto:** authenticated Jellyfin and Seerr portraits/names lead Settings. Discover and the final add action show the Seerr actor. Administrator API keys are visibly read-only; personal Seerr sessions are reverified before writing. A Jellyfin profile alone does not imply a Seerr login.
- **Kalender:** type filter, dated strip with release counts, chronological agenda. Selection resets the agenda to its beginning. Opening details and explicitly returning preserves the date/filter; closing actually closes the sheet. Film dates are digital/physical home releases, not cinema premieres.
- **Detaljar:** full portrait poster beside title/facts for films and Seerr; wide artwork and episode subtitle for library episodes. Synopsis precedes the availability explanation. Additional facts wrap instead of being hidden offscreen.

## Visual rhythm

Use the existing matte charcoal palette, warm text and restrained personal accent (lime by default). No glass blur, decorative space background, borders around posters or permanent artwork badges. The source logo/name in each Home heading identifies its rail.

`ReelLayout` is the shared source for page gutters, top spacing, artwork corner radius, Home movie/episode
dimensions, the maximum content width and the navigation-rail breakpoint. Every screen wraps its scrolling
container in `ReelPage`; media fills the window while reading columns stay centered. Personal
artwork size scales both cards and skeletons without scaling text. Text may grow with font scaling; do not force titles into fixed pixel-height containers.

## Motion and changing data

### Meny på telefon · september 2026

Ståande telefonvising bruker botnmenyen. Breie, korte berøringsvindauge (minst 640 dp
breie og under 600 dp høge) bruker ei kompakt sidemeny på 80 dp. Ho utvidar seg ikkje
til TV-/nettbrettmenyen og viser ikkje bibliotek-snarvegar. Innstillingar ligg fast
nedst; resten kan rullast uavhengig ved liten høgd. TV held på fjernkontrollmenyen.

Menytilpassinga har brytarar for synleg/skjult, separate flytteknappar og nullstilling.
Heim og Innstillingar kan ikkje skjulast. Bibliotek er òg obligatorisk når det er
startside. Reglane gjeld både rendering, endring og lagring, også med gamle lagra val.
Skjuling gjeld både botnmeny og sidemeny med ein gong.

- Preserve the fixed sheet viewport while metadata arrives; scroll the inner content. Never resize the modal anchor to fit each network response.
- Brief card reveal and spring press feedback remain; avoid long entrance delays or repeating animation on every refresh.
- Empty playback is a small status line, including while checking. A refresh must not temporarily insert a full-height playback skeleton above the library.
- Keep real artwork placeholders when a source is missing; do not substitute demo images into authenticated feeds.

## Data and verification boundaries

Prefer Norwegian Seerr descriptions, with an English fallback when the translation is empty. Translate known series-status labels into Nynorsk. Counts and networks appear only when supplied by the service. Account-scoped, bounded metadata caching avoids repeated request-title lookups; current availability still comes from fresh request responses.

Authenticated UI checks are read-only: no playback changes, requests or deletes. Keep screenshots and account data local. Run destructive storage/instrumentation tests only on a separate clean emulator, and update the authenticated emulator in place with the established signing key.

## «Spelar no» — popup og direkte oppdatering

Popupen viser ekte tenarbilete i sitt eige format, med `Fit` og utan tekst over coveret.
Biletplassen, lukkeknappen og pause-/hald fram-knappen held seg i ro når bilete og metadata kjem inn.
Valet mellom fleire økter ligg øvst. Ved skriftstorleik 2.0 ligg metadata under kvarandre og
innhaldet kan rullast, medan handlinga ligg fast nedst. Ein avslutta økt får ein tomtilstand;
eit mislukka nettverkskall får ei melding om automatisk nytt forsøk.

Emby blir sjekka kvart tredje sekund når Heim er synleg, òg når ingen spelar, og begge tenestene
kvart andre sekund i avspelingspopupen. Opning eller byte av økt utløyser ein sjekk med ein gong.
Jellyfin sine direktevarsel gjeld berre Jellyfin. Framsidelasting og diskcache skal ikkje
overskrive nyare økter. Kontrollen brukar dei eksisterande verifiserte konto- og tilgangsreglane.
Nettverkstida kjem i tillegg til sjekkintervallet. Bakgrunnsappen pollar ikkje avspelingar.

Kontroll 7. oktober 2026: ekte Jellyfin- og Emby-økter og cover vart viste på den lagra TV-profilen.
Emby sitt automatiske episodebyte oppdaterte innhaldet medan popupen var open. Start, stopp,
pause, byte av økt, nytt forsøk og stor skrift blir også kontrollerte på den isolerte testeininga.
Skjermbilete med persondata ligg berre lokalt i den ignorerte byggmappa.
