# HomeReel design system

## Product hierarchy

Home is a service-agnostic media feed. Service names explain provenance, but never become primary navigation. The default order follows immediacy: active playback, new library items, future releases, then active downloads.

## Core patterns

| Pattern | Purpose | Important states |
| --- | --- | --- |
| Playback carousel | One card per active Jellyfin or Emby session | playing, paused, command pending, command failed |
| Media rail | Separate Jellyfin/Emby poster and episode rows | loading, newly added, source badge |
| Discovery grid | Adaptive posters with usable type filters and quieter actions | all, movies, series, searching, empty, requested |
| First-run setup | Select services, authenticate, then open Home | no connections, connection added, explicit demo preview |
| Upcoming card | Date-first Radarr/Sonarr calendar item | today, tomorrow, later date, missing artwork |
| Activity row | Request/download history with visual recognition | approved, downloading, imported, failed |
| Section switch | Controls Home visibility without disabling sync | visible, hidden |
| Quiet empty line | Confirms a valid empty state without competing with content | no sessions, no releases, empty queue |
| Cinematic detail sheet | Keeps context while revealing depth | artwork hero, layered title, metadata rail, primary action |
| Quick Connect panel | Passwordless Jellyfin sign-in | ready, creating code, awaiting approval, connecting, expired |

## Tokens and motion

- Matte charcoal `Ink` is the page foundation; raised surfaces use solid `SurfaceRaised`. No space backdrop, blur, glass panels, or decorative gradients.
- Warm white carries titles and hierarchy. Lime is reserved for selection, progress and direct actions. Service logos retain their original colours.
- Green confirms completed or healthy states; coral is reserved for actionable problems.
- Playback cards use 20 dp corners; posters use 12 dp and compact surfaces use 10–16 dp. Artwork is borderless, without a decorative shadow or bottom strip.
- Navigation reserves layout space instead of floating over content. System insets are handled once by the app shell. Each tab retains its scroll state.
- Navigation fades use 150–220 ms. Home cards reveal in 240 ms with at most 60 ms stagger, once per saved composition identity; touch scales use a restrained spring. Compose motion follows Android's animation scale.
- Series detail imagery keeps its aspect ratio, with title and subtitle below. Movie details keep a full poster beside the summary. Gradients are used only to make overlaid playback/upcoming text legible.
- Bottom sheets reserve a stable 90% content viewport from their first frame, plus the drag handle and system insets. The size constraint belongs to the content, not the modal surface, so the sheet keeps its bottom anchor. Remote metadata updates only the scrollable interior; no content-size spring changes the outer anchor. Keyboard insets may resize the usable area deliberately.
- Calendar keeps title, type filters and a 28-day date strip above a lazy agenda. A selected date filters the list; tapping it again or “Alle dagar” resets it. Empty days remain selectable. Rows have borderless 16:9 episode art or uncropped 2:3 film posters, 48 dp minimum targets, and a readable source/time label. Film dates do not imply an exact release time or availability in the library.
- Home visibility has four independent library switches: Jellyfin movies/series and Emby movies/series. Older combined preferences migrate without re-enabling hidden rows. Hiding a row does not disconnect the service or disable its other features.

## Accessibility

- Navigation and primary actions use at least 48 dp touch targets. Interactive rows expose button or switch semantics.
- Status never relies on color alone; labels describe source, state, and progress.
- Empty states are short factual messages, not decorative cards.
- Artwork always has a local fallback, while decorative imagery has no spoken description.

## Setup and authentication

- Existing configured users enter Home directly. New users choose a service or explicitly opt into demo data; preview content is cleared when the first service connects.
- New connections ask for the server address before credentials. Optional connection names and profile IDs live under advanced settings.
- Jellyfin offers Quick Connect, account login and API tokens. Seerr offers Jellyfin account login, Seerr-mediated Quick Connect and administrator API keys.
- Seerr stores its own encrypted session cookies, not the password or a forwarded Jellyfin token. Cookie requests carry CSRF protection when supplied by Seerr. Requests run with the signed-in user's server-side permissions.
- Unsupported Quick Connect, rejected credentials and expired sessions have actionable Nynorsk messages. Cancellation stops pending login work.
