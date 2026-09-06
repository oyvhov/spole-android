# Spole design system

## Current refinements · 0.12.0

### Popup correction · 0.12.1 (supersedes earlier sheet rules)

Every modal route, including connection steps and playback, now keeps an 82% content viewport.
There is no content-sized exception. One shared `SheetToolbar` owns the top row: 48 dp close target,
36 dp circular surface, 20 dp icon, 12 dp trailing inset. The close target is top-aligned independently
of title length and stays outside the scrolling body. Calendar retains its explicit back action.
Material handles keyboard/system insets once; the connection body no longer adds another IME inset.

Content scrolling does not drag or dismiss the modal. `sheetGesturesEnabled = false` removes the
competing parent nested-scroll gesture, and the drag handle is removed so it does not promise that
interaction. Close, scrim tap and Android Back remain available; the explicit close animates the sheet
out before removing it. Sending a request still prevents dismissal. Headers never switch title based
on pixel scroll thresholds. Test bounds during a held drag as well as after flings and async updates.

API reference: [Material modal gesture control](https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalBottomSheet.composable).

These supersede older sizing descriptions below. Title details, Calendar and Request Composer share an exact
82% content viewport. Inner text uses minimum heights, never clipping fixed boxes. Synopsis starts at four
lines with an explicit expand/collapse action; the outer modal stays still. Cast appears only from service
metadata. Discover has type and availability filters, and personal Activity has All / In progress / Ready.
Cover cards reserve top space for badges and grow at large font sizes. Onboarding uses a local vector film
strip and one short reveal. The request journey uses the same three steps before and after submission.

## Product hierarchy

Home is a service-agnostic media feed. Service names explain provenance, but never become primary navigation. The default order follows immediacy: active playback, new library items, future releases, then active downloads.

## Core patterns

| Pattern | Purpose | Important states |
| --- | --- | --- |
| Playback carousel | One card per active Jellyfin or Emby session | playing, paused, command pending, command failed |
| Media rail | Separate Jellyfin/Emby poster and episode rows | loading, newly added, small source mark in heading |
| Discovery grid | Adaptive posters with usable type filters and quieter actions | all, movies, series, searching, empty, requested |
| First-run setup | Select services, authenticate, then open Home | no connections, connection added, explicit demo preview |
| Upcoming card | Date-first Radarr/Sonarr calendar item | today, tomorrow, later date, missing artwork |
| Activity row | Request/download history with visual recognition | approved, downloading, imported, failed |
| Section switch | Controls Home visibility without disabling sync | visible, hidden |
| Quiet empty line | Confirms a valid empty state without competing with content | no releases, empty queue; empty playback is omitted |
| Cinematic detail sheet | Keeps context while revealing depth | artwork hero, layered title, metadata rail, primary action |
| Quick Connect panel | Passwordless Jellyfin sign-in | ready, creating code, awaiting approval, connecting, expired |

## Tokens and motion

- Matte charcoal `Ink` is the page foundation; raised surfaces use solid `SurfaceRaised`. No space backdrop, blur, glass panels, or decorative gradients.
- `SurfaceRaised` alone is 1.5:1 against `Ink`, which is below WCAG 1.4.11's 3:1 for identifying a control. Controls whose fill cannot carry that on its own — text fields, unselected chips, disabled buttons — take a 1 dp `ControlOutline` edge (3.5:1 against `Ink`). Plain cards are containers, not controls, and stay borderless.
- Warm white carries titles and hierarchy. Lime is reserved for selection, progress and direct actions. Service logos retain their original colours.
- Lime never carries passive information. Counts, type badges, source names and status labels are `Muted` or warm white; a checked switch puts lime in the thumb and a dim olive in the track, so a list of preferences is not the loudest surface in the app.
- Coral marks a problem the reader can act on. A title waiting in a queue is a normal state and stays neutral.
- Green confirms completed or healthy states; coral is reserved for actionable problems.
- Playback cards use 20 dp corners; posters use 12 dp and compact surfaces use 10–16 dp. Artwork is borderless, without a decorative shadow or bottom strip.
- Navigation reserves layout space instead of floating over content. System insets are handled once by the app shell. Each tab retains its scroll state.
- Navigation fades use 150–220 ms. Home cards reveal in 240 ms with at most 60 ms stagger, once per saved composition identity; touch scales use a restrained spring. Compose motion follows Android's animation scale.
- Series detail imagery keeps its aspect ratio, with title and subtitle below. Movie details keep a full poster beside the summary. Playback, Upcoming and Discover use dark image scrims solely for text legibility, not decorative glass effects.
- Every section heading on a page uses one style (`titleLarge`). Home, Discover, Activity and Settings do not each pick their own size for the same level.
- Shared dimensions live in `ReelLayout`; see [the layout specification](LAYOUT.md) for each screen's hierarchy. Upcoming uses 280 × 226 dp full-bleed cards, with date above and title below. Discover uses adaptive full-bleed posters, a top type/status strip and bottom action; content can grow with font size. Empty playback is omitted, while multiple sessions show an explicit count.
- Content is capped at `ReelLayout.ContentMaxWidth` and centred, so a wide window keeps one readable column instead of stretching each header until its trailing label loses contact with its title. At or above `RailBreakpoint` navigation moves from the bottom bar to a `NavigationRail`.
- No control has a fixed height that text has to fit inside. The bottom bar, playback cards and card text blocks grow with the font scale; at 2.0x nothing is clipped and no line is dropped. Test at 2.0x, not 1.5x: Android's non-linear scaling barely moves large sizes.
- Bottom sheets that receive remote metadata (title details, calendar, request composer) reserve a stable 90% content viewport from their first frame, plus the drag handle and system insets. Sheets whose content is already known — the connection form, the playback sheet — are sized by that content and capped at the same 90%, so a one-field form does not reserve most of the screen and leave it black. The size constraint belongs to the content, not the modal surface, so the sheet keeps its bottom anchor. Remote metadata updates only the scrollable interior; no content-size spring changes the outer anchor. Keyboard insets may resize the usable area deliberately.
- Calendar keeps title, type filters and a 28-day date strip above a lazy agenda. A selected date filters the list; tapping it again or “Alle dagar” resets it. Empty days remain selectable. Rows have borderless 16:9 episode art or uncropped 2:3 film posters, 48 dp minimum targets, and a readable source/time label. Film dates do not imply an exact release time or availability in the library.
- Home visibility has four independent library switches: Jellyfin movies/series and Emby movies/series. Older combined preferences migrate without re-enabling hidden rows. Hiding a row does not disconnect the service or disable its other features.
- Discover, Activity and Calendar share `AppFilterRow`: compact solid filters, no outline, lime selected state and Material's minimum 48 dp interaction area. The visible chip is not inflated to fill its touch target.
- `ServiceSymbol` shares source identity across setup, settings and details. Jellyfin/Emby use their real logos; TV/movie/search icons describe the other services consistently.
- Title/playback sheets have explicit close controls. The Calendar back button restores its saved filter, date and agenda position; a swipe, scrim tap or system Back dismisses the sheet. There is no close/reopen animation for the in-sheet Calendar button.
- Home has one compact row: Spole identity and a 40 dp personal avatar inside a 48 dp account-settings target. No greeting, date or calendar shortcut in the header; Calendar remains under Upcoming. Prefer verified personal Seerr, then Jellyfin, then Emby; use the selected person's initial when artwork is missing and a neutral person icon when no personal identity is verified. Never substitute a shared API-key owner. The row's dimensions stay stable as the picture loads. Settings retains version/identity at its bottom. A preference row exposes one switch action, not competing row and thumb actions. Library notifications have an actual working master switch alongside each followed request's choice.
- With no visible active sessions, Home omits the entire playback section, including the checking/empty line during refresh. Actual playback still has its title and explicit multi-session count.
- All Material color roles are explicitly assigned; default purple secondary/container colors must not leak into controls. Native startup and app icon use the same charcoal/lime palette.
- The artwork frame follows the image, not the source. Services answer an episode request with a series poster whenever no still exists, so a detail hero measures what arrived: wide art fills a 16:9 crop and portrait art keeps its own shape. Never letterbox or pillarbox into a fixed frame.
- Artwork missing from live data uses the neutral media placeholder, never preview artwork. Seerr status is attributed to Seerr and distinguishes pending, processing, partial, available, blocked and deleted; processing is not proof of a running download. Approval is not import completion.

## Accessibility

- Navigation and primary actions use at least 48 dp touch targets. Interactive rows expose button or switch semantics.
- Status never relies on color alone; labels describe source, state, and progress.
- Empty states are short factual messages, not decorative cards.
- Artwork always has a local fallback, while decorative imagery has no spoken description.

## Setup and authentication

- Ordinary users have a personal experience, without role-limit banners or administrator overview filters. Only verified Seerr administrators see the administrator role label, shared queue and cross-user Activity filters. Permissions are enforced before data becomes UI state, not only by hiding controls.

- Existing configured users enter Home directly. New users choose a service or explicitly opt into demo data; preview content is cleared when the first service connects.
- New connections ask for the server address before credentials. Optional connection names and profile IDs live under advanced settings.
- Jellyfin offers Quick Connect, account login and API tokens. Seerr offers Jellyfin account login, Seerr-mediated Quick Connect and administrator API keys.
- Seerr stores its own encrypted session cookies, not the password or a forwarded Jellyfin token. Cookie requests carry CSRF protection when supplied by Seerr. Requests run with the signed-in user's server-side permissions.
- Unsupported Quick Connect, rejected credentials and expired sessions have actionable Nynorsk messages. Cancellation stops pending login work.

### Personal sign-in pattern

- One solid account surface: Seerr's verified avatar leads, with compact Jellyfin/Emby rows underneath. Each service keeps its own account action and identity; display names do not establish a shared identity or shared permissions.
- Emby uses local server username/password by default. This is not Emby Connect cloud login. API credentials are secondary, under advanced choices. Radarr/Sonarr are hidden behind an administrator-tools expander during onboarding.
- Jellyfin or Seerr account login offers an unchecked companion-login option. The user explicitly confirms both destinations, receives a password-sharing explanation and an HTTP warning where relevant. Passwords are transient, cleared from form state on submission, and never persisted. Both credentials and probes must succeed before saving either connection; Seerr's personal media-user ID must match the authenticated Jellyfin ID. A failure leaves saved connections untouched. No automatic token forwarding or automatic server discovery.
- Quick Connect remains separately authorized per service. Copy places only the visible code (no formatting spaces or secret) on the clipboard and confirms with `Kopiert`. Instructions allow another app/browser on the same phone. Copy has a 48 dp minimum touch target through Material TextButton and a text label.
- `Logg ut` is visible at the top of a configured connection. The confirmation names the service and explains local scope. Confirm removes local credentials/identity and cached feed data, cancels pending authentication and account-related work, and retains only the server address for a later sign-in. Other services stay signed in. This does not revoke sessions on other devices. Signing out of the last service returns to onboarding.
- UI states: address → credentials → signing in → verified Home; failure remains in the form; explicit cancel stops the work; sign-out supports confirm/cancel. Shared login is available only for username/password, never Quick Connect or API keys.

Protocol references: [Seerr authentication implementation](https://github.com/seerr-team/seerr/blob/develop/server/routes/auth.ts), [Emby server authentication](https://dev.emby.media/reference/RestAPI/UserService/postUsersAuthenticatebyname.html).

Verification of this local, unreleased change: 145 JVM tests and 27 focused Android tests pass; debug app/test APK builds succeed; lint has 0 errors and 20 warnings. Loopback fixtures cover linked login in both directions with a non-admin account, second-service rejection, mismatched media identities and service-scoped sign-out. Android UI tests cover Emby account fields, explicit companion consent, clipboard code contents and sign-out confirm/cancel. The signed-in review emulator was updated without clearing data, and the consolidated account surface and Emby form were visually checked. A real Emby username/password login still needs the user's credentials; no live account was signed out or replaced during verification. No release or version bump.
