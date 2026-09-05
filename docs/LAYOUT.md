# HomeReel layout

## Navigation and hierarchy

Four persistent destinations: **Heim**, **Oppdag**, **Aktivitet**, **Innstillingar**. The navigation owns space rather than covering the feed. Each tab retains its scroll position. App identity belongs in Settings, not in every page header.

- **Heim:** date and greeting, an always-reachable calendar shortcut, quiet playback status (or active session cards), separate recently-added movie/episode rails for each library service, upcoming releases, download queues. No continue-watching feed and no connection-count banner.
- **Oppdag:** search, media-type filter, adaptive poster grid. Availability is text below artwork, never an overlay obscuring a face. The card opens details; the explicit action either opens details or adds a title through Seerr.
- **Aktivitet:** title first, status second, time/source third. Posters remain unobstructed. Source filters keep request and download activity easy to isolate.
- **Innstillingar:** personal accounts first, then connections and independent service/media-type Home switches, preferences and local app identity last.
- **Konto:** authenticated Jellyfin and Seerr portraits/names lead Settings. Discover and the final add action show the Seerr actor. Administrator API keys are visibly read-only; personal Seerr sessions are reverified before writing. A Jellyfin profile alone does not imply a Seerr login.
- **Kalender:** type filter, dated strip with release counts, chronological agenda. Selection resets the agenda to its beginning. Opening details and explicitly returning preserves the date/filter; closing actually closes the sheet. Film dates are digital/physical home releases, not cinema premieres.
- **Detaljar:** full portrait poster beside title/facts for films and Seerr; wide artwork and episode subtitle for library episodes. Synopsis precedes the availability explanation. Additional facts wrap instead of being hidden offscreen.

## Visual rhythm

Use the existing matte charcoal palette, warm text and restrained lime action color. No glass blur, decorative space background, borders around posters or permanent artwork badges. The source logo/name in each Home heading identifies its rail.

`ReelLayout` is the shared source for page gutters, top spacing, artwork corner radius and Home movie/episode dimensions. Skeletons use the same artwork dimensions. Text may grow with font scaling; do not force titles into fixed pixel-height containers.

## Motion and changing data

- Preserve the fixed sheet viewport while metadata arrives; scroll the inner content. Never resize the modal anchor to fit each network response.
- Brief card reveal and spring press feedback remain; avoid long entrance delays or repeating animation on every refresh.
- Empty playback is a small status line, including while checking. A refresh must not temporarily insert a full-height playback skeleton above the library.
- Keep real artwork placeholders when a source is missing; do not substitute demo images into authenticated feeds.

## Data and verification boundaries

Prefer Norwegian Seerr descriptions, with an English fallback when the translation is empty. Translate known series-status labels into Nynorsk. Counts and networks appear only when supplied by the service. Account-scoped, bounded metadata caching avoids repeated request-title lookups; current availability still comes from fresh request responses.

Authenticated UI checks are read-only: no playback changes, requests or deletes. Keep screenshots and account data local. Run destructive storage/instrumentation tests only on a separate clean emulator, and update the authenticated emulator in place with the established signing key.
