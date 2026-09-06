# Changelog

## 0.11.6

- Sjekkar anbefalingar mot Seerr under synkronisering, slik at Home viser om tittelen alt ligg i
  biblioteket eller er førespurd før ein opnar detaljane.
- Låser detaljpopupen til ei føreseieleg standardramme med fast metadata- og omtaleplass, skeleton
  medan data blir henta og avgrensa tekstlinjer for å unngå hopp når ekstra informasjon kjem inn.

## 0.11.5

- Peikar den felles anbefalingslista til det offentlege `spole-recommendations`-repoet, slik at alle
  installasjonar kan hente henne utan GitHub-innlogging.

## 0.11.4

- Rettar Anbefalingar til å bruke den felles, statiske `recommendations.json`-lista frå GitHub.
- Seerr blir brukt for live detaljar, bibliotekstatus og førespurnader når ein opnar ei anbefaling,
  men ikkje som kjelde for sjølve Home-rada.
- Cache-ar GitHub-lista lokalt slik at Home ikkje blir tom ved eit mellombels nettverksbrot.

## 0.11.3

- La til Anbefalingar frå Seerr som ei eiga, valfri Home-rad med moderne overlay-kort og detaljvising.
- La til Nyleg tilgjengeleg som ein separat release-feed frå Radarr og Sonarr, sortert etter digital/heime- eller episode-release.
- Avgrensa release-historia til dei siste 28 dagane og ignorerer filmar som berre har kinodato, slik at gamle bibliotekfilmar ikkje dukkar opp som nye.
- Held fram med eigne Jellyfin- og Emby-rader for sist lagde bibliotekinnhald, utan å blande kjeldene.
- La til skeleton-lasting, cache-støtte, detaljpopup og Innstillingar-migrering for dei nye Home-delane.

## 0.10.1

- Removed the Home greeting and date. The compact brand header now includes a personal profile picture that opens account settings.
- Prefer the verified personal Seerr account, then Jellyfin, then Emby. Missing pictures use that person's initial; shared API keys are never presented as personal accounts.
- Hide the entire Now Playing section when there are no active sessions, including during refresh. The multi-session count is preserved when playback exists.
- Added native regression coverage for profile priority/fallback, profile navigation, no greeting/date and silent empty playback.

## 0.10.0

- Renamed the app to Reelune, with a new film-ribbon R mark, adaptive launcher icon and restrained Home header. Existing app ID and update signature are preserved.
- Added a server-verified Seerr administrator boundary: ordinary users see only their own playback and personal request Activity, without administrator filters or explanatory restriction banners.
- Applied session filtering by actual media user ID, owner filtering to Seerr requests, fresh permission checks before requests/playback commands, and administrator-only shared Radarr/Sonarr queues.
- Removed persisted shared activity/sessions and stale personal-library fallback. Account changes cancel old refresh work and clear the previous account's feed.
- Excluded Barneserier/Barneseriar, Jellyfin's Barne-TV and Emby's Barne-Tv Serier libraries from recent media queries. Other libraries, including children's films, remain untouched.
- Redesigned Upcoming as full-bleed image cards with readable bottom overlays, and Discover as poster cards with type/status tags and embedded actions. Multiple active playback sessions now have an explicit count.
- Added role, owner, library-selection and native UI regressions; aligned loading placeholders and notification settings with the actual experience.

## 0.9.0

- Added a personal Seerr request sheet with explicit missing-season selection, fresh availability checks and visible account identity before submission.
- Added a default-on library notification choice, optional poster artwork and a text-only fallback when artwork is unavailable.
- Added personal request tracking in Activity: requested, actual downloading, waiting for library import and confirmed library availability. Approval alone never means downloading.
- Isolated local follows by server and verified account, preserved notification choices across app restarts, and kept existing requests notification-off until explicitly enabled.
- Replaced ambiguous availability copy with “I biblioteket ditt” and a library icon. Kept series season selection available even when existing seasons are already in the library.
- Added regression coverage for season eligibility, request identity, 4K status, persistence, notification artwork and the native confirmation flow.

## 0.8.0

- Added authenticated Jellyfin/Seerr account panels with server-provided names and profile pictures, and a visible request identity in Discover and title details.
- Personal requests now require a Seerr session and recheck its actual user ID immediately before posting. Administrator API keys remain read-only for requests; no impersonation field is sent.
- Profile pictures use origin-scoped credentials and do not follow redirects; missing pictures use an initial rather than demo artwork.

- Refined the complete native layout against authenticated service data: shorter source-labelled Home headings, clean poster art, room for longer titles and episode subtitles, and a calendar shortcut beside the greeting.
- Kept inactive playback compact during refresh, with matched artwork dimensions for skeletons and loaded cards.
- Moved Discover availability below posters and Activity status beside text; improved hierarchy across all four tabs.
- Reworked upcoming cards around readable dates and artwork; added daily calendar counts and predictable filter scrolling.
- Prioritized synopsis in detail sheets, removed repeated type/year information and made extra facts wrap without changing the sheet height.
- Added Norwegian/English synopsis fallback, richer Seerr series facts, translated statuses and bounded account-scoped request-metadata caching.
- Preserved separate Jellyfin/Emby film and episode controls, existing account sessions and the established APK upgrade identity.

## 0.6.0

- Rebuilt the visual foundation with matte charcoal surfaces, warm white typography and a restrained lime accent; removed the decorative space background and floating navigation overlay.
- Reserved real screen space for navigation and system bars, preserved tab scroll positions and shortened card reveals and touch feedback.
- Replaced Discover's oversized boxed rows with an adaptive poster grid, working movie/series filters, clear-search control and functional detail actions.
- Added a first-run service checklist, explicit demo preview, persistent setup completion and a two-stage address/account login flow with optional advanced settings.
- Added Seerr sign-in using a Jellyfin username/password or Seerr's native Jellyfin Quick Connect endpoints, with encrypted per-account session cookies and CSRF support. Administrator API keys remain supported.
- Improved login cancellation, password visibility, keyboard actions, account/permission errors and compatibility guidance for Seerr versions without Quick Connect.
- Stopped mixing sample feeds into a connected setup; real connections clear preview content before loading.
- Refined playback controls, borderless artwork, detail typography, uncropped series imagery and matching loading skeletons.
- Added network and Android UI regression coverage for account sessions, Quick Connect, setup and Discover filters.

## 0.5.3

- Added more breathing room above the Home greeting and a quiet, localized date line.
- Removed hard outlines and the dark bottom veil from Home artwork so posters and thumbnails keep their natural color and edge.
- Refined media cards with soft depth, asymmetric corners, and taller movie-poster proportions.
- Added brief staggered reveal motion to Home rails and responsive spring feedback when cards are pressed.
- Kept loading skeletons aligned with the updated card proportions to prevent layout jumps.

## 0.5.2

- Redesigned «Kjem snart» as artwork-led cards with calm date chips and a dedicated 28-day agenda calendar.
- Radarr now excludes cinema-only dates and shows only digital or physical home releases; Sonarr continues to show upcoming episodes.
- Added full-poster movie detail layouts with the poster on the left and title information on the right, without cropping the artwork.
- Switched recently added series to wide Jellyfin/Emby Thumb artwork and exact ungrouped episode results with season, episode number, and title.
- Removed the leftover playback-progress strip from recently added cards.
- Added clearer «Om filmen», «Om serien», and «Om episoden» sections, taglines, richer metadata, and studio/status details when supplied by a service.
- Removed the HomeReel name and logo from Home and moved the app identity and version into Settings.
- Updated loading skeletons to match the new portrait, landscape, and upcoming card shapes.

## 0.5.1

- Fixed Jellyfin account sign-in on servers that disable deprecated Emby authorization headers.
- Send exactly one standards-based Jellyfin authorization header during sign-in, avoiding ambiguous duplicate credentials that could surface as a server error only for valid passwords.
- Use the same modern Jellyfin authorization scheme for connection checks, library loading, authenticated artwork, details, and playback sessions after sign-in.
- Added Jellyfin Quick Connect with a native six-character code, automatic approval checking, and secure token exchange without entering a password in HomeReel.
- Redesigned playback and title sheets around cinematic artwork, layered information, smoother content changes, lighter metadata, and pill-shaped actions instead of a grid of heavy boxes.
- Refined every bottom sheet with a softer floating shape, quieter surface, and more consistent spacing.
- Keep Emby on its compatible token header and added regression coverage for the two distinct authorization paths.

## 0.5.0

- Added subtle, section-shaped shimmer skeletons for initial Home, Discover, and Activity loading without replacing useful cached content during background refreshes.
- Added Jellyfin username/password sign-in as the default setup path; only the returned access token and profile ID are stored, and the password is never persisted.
- Replaced local-only Discover filtering with debounced, authenticated Seerr search for new movies and series.
- Added richer title sheets across library cards, Discover, upcoming releases, downloads, and Activity, with live Jellyfin/Emby or Seerr detail enrichment where available.
- Added overview, runtime, year, rating, certification, genre, source, date, and status metadata to supported title details and the offline cache.
- Reworked user-facing request language from “order” terminology to the calmer “add to the media collection” flow.
- Expanded network, parser, security, loading-state, and on-device UI regression coverage.

## 0.4.6

- Fixed automatic profile detection so active child sessions or the first returned user can no longer select a restricted child profile for Home.
- Prefer an enabled administrator or full-library profile when a Jellyfin or Emby API key is not tied to a user.
- Read the available movie and series library views and interleave their newest items, preventing one busy library from filling an entire row.
- Added regression coverage for restricted child profiles and multiple movie and series libraries.

## 0.4.5

- Gave Jellyfin and Emby separate recently-added movie and series rows instead of mixing their libraries.
- Replaced letter badges with tiny service logos on media cards and row headings.
- Added Jellyfin and Emby brand marks to their Settings connection rows.
- Removed the connection count and sync warning from the Home header.
- Localized navigation, controls, states, empty messages, and errors into Nynorsk.
- Added an Android regression test that verifies both servers render as separate Home rows.

## 0.4.4

- Removed Continue Watching from Home, Settings, syncing, and cached dashboard data.
- Split recently added media into dedicated movie and series rows sourced from both Jellyfin and Emby.
- Switched Jellyfin to direct, type-filtered Latest Media calls that do not require a Profile ID.
- Kept the current and legacy user-scoped routes as fallbacks for Jellyfin and Emby compatibility.
- Migrated existing Home-section preferences automatically and made partial-service warnings amber instead of error red.

## 0.4.3

- Fixed the launch crash caused by an unsupported live-media placeholder drawable.
- Prevented authenticated Emby and Jellyfin artwork from repeatedly opening Android's secure key store while the home screen is rendered.
- Added a crash-safe artwork fallback so a malformed or rejected image request cannot close the app.
- Added an Android regression test for authenticated artwork failures.

## 0.4.2

- Kept Jellyfin and Emby marked as connected when only a personal feed or playback-session call is unavailable.
- Added partial media-server refreshes so Recently Added can still load when Continue Watching needs a Profile ID.
- Added a global Jellyfin Recently Added fallback when no media profile can be detected.
- Distinguished connected-with-limited-data warnings from actual connection failures throughout Home and Settings.
- Replaced demo artwork on Jellyfin and Emby library/session cards with authenticated server artwork while keeping tokens out of image URLs.
- Added a neutral HomeReel placeholder for media that genuinely has no server artwork instead of showing an unrelated demo poster.

## 0.4.1

- Fixed Jellyfin library loading by using the current `/UserItems/Resume` and `/Items/Latest` routes with legacy fallbacks.
- Fixed Emby library loading by using its user-scoped resume and latest-media routes.
- Added automatic media-profile discovery for API-key connections without a configured profile ID.
- Added visible, actionable Home states when media-library refresh fails or returns no items.

## 0.4.0

- Renamed the app to HomeReel and introduced a new home-and-play launcher icon.
- Aggregated Jellyfin and Emby sessions, continue-watching items, and recently-added media on one Home screen.
- Added a multi-session carousel with independent pause and resume controls.
- Added a 28-day Upcoming rail backed by the Radarr and Sonarr calendar APIs.
- Added Home-section visibility controls under Settings.
- Added artwork-rich Activity rows and Seerr media-detail enrichment for recent requests.
- Replaced the empty playback card with a compact, subdued status line.
- Preserved cached content per service during partial refresh failures.

## 0.3.0

- Added live Jellyfin and Emby continue-watching and recently-added feeds.
- Added remote pause and resume for active Jellyfin and Emby sessions.
- Added a persistent, non-secret dashboard cache for useful startup and offline state.
- Added 30-minute background refresh with an optional Wi-Fi-only constraint.
- Added an optional Jellyfin/Emby user ID for server API-key setups.
- Added animated library rails, progress artwork, and media detail sheets.
- Expanded parser, client, cache, and on-device UI coverage.

## 0.2.0

- Added live playback, queue, Seerr discovery/request feeds, remote artwork, pull-to-refresh, and partial-service failure handling.

## 0.1.0

- Added the native Compose foundation, cinematic visual system, connection editors, encrypted token storage, and service health checks.
