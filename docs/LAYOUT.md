# Spole layout

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

- Preserve the fixed sheet viewport while metadata arrives; scroll the inner content. Never resize the modal anchor to fit each network response.
- Brief card reveal and spring press feedback remain; avoid long entrance delays or repeating animation on every refresh.
- Empty playback is a small status line, including while checking. A refresh must not temporarily insert a full-height playback skeleton above the library.
- Keep real artwork placeholders when a source is missing; do not substitute demo images into authenticated feeds.

## Data and verification boundaries

Prefer Norwegian Seerr descriptions, with an English fallback when the translation is empty. Translate known series-status labels into Nynorsk. Counts and networks appear only when supplied by the service. Account-scoped, bounded metadata caching avoids repeated request-title lookups; current availability still comes from fresh request responses.

Authenticated UI checks are read-only: no playback changes, requests or deletes. Keep screenshots and account data local. Run destructive storage/instrumentation tests only on a separate clean emulator, and update the authenticated emulator in place with the established signing key.
