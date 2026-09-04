# HomeReel product and implementation plan

## Product promise

One reliable place to answer three questions: **What is playing? What was just added? What is arriving next?** HomeReel should feel like a premium media app, while making the status of a five-service automation stack understandable to people who did not configure it.

## Core principles

1. **Media first.** Artwork and titles lead; server mechanics stay contextual.
2. **One action at a time.** Every screen has a clear primary action and immediate feedback.
3. **Progressive depth.** Everyday use is simple, while queue, profile, and error details remain one tap away.
4. **Honest state.** Requested, approved, searching, downloading, importing, available, and failed are never collapsed into an ambiguous spinner.
5. **Private by default.** Secrets remain encrypted on-device, HTTPS is preferred, and local HTTP is limited to trusted LAN addresses.

## Navigation and key flows

### Home

- All connected servers at once, with no server/profile switcher
- Current playback with seek progress and transport controls
- Separate recently-added movie and series rows for each Jellyfin and Emby server
- Compact incoming movie and episode status
- Tap any item for a detail sheet instead of losing dashboard context

### Discover

- Live Seerr title search
- Movie/series, service, availability, and genre filters
- Clear availability badge: already available, requestable, or in progress
- Add-to-library action through Seerr with optimistic state and recoverable failure feedback
- Rich title details with overview, year, runtime, rating, genre, availability, and source status

### Activity

- One normalized timeline across Seerr, Radarr, Sonarr, Jellyfin, and Emby
- Human-readable status language with optional technical detail
- Filters for requests, downloads, imports, playback, warnings, and failures
- Retry or open-source-service actions when the corresponding API supports them

### Settings

- Multiple named connection profiles, including home and remote endpoints
- Test-before-save feedback with latency and server version
- Jellyfin Quick Connect, account sign-in, and advanced API-key setup; passwords are never persisted
- Library, quality profile, root folder, and notification preferences
- Diagnostics export that excludes credentials

## Delivery sequence

### Milestone 1 — Native experience foundation (complete)

- Four-destination Compose app and cinematic visual system
- Interactive playback/request/navigation states
- Connection editor for all five services
- Encrypted API-token storage and service health probes
- URL hardening, tests, lint, and emulator validation

### Milestone 2 — Live read model

- [x] Jellyfin and Emby active playback sessions
- [x] Jellyfin and Emby recently added movies and series across all accessible libraries
- [x] Seerr trending discovery, request submission, and request-status feeds
- [x] Radarr and Sonarr queue feeds
- [x] Radarr and Sonarr calendar feeds
- [ ] Radarr and Sonarr history feeds
- [x] Stable domain mapping, empty states, pull-to-refresh, and partial-service failures
- [x] Persistent non-secret dashboard cache with last-updated state
- [x] Section-shaped loading skeletons that preserve cached content during refresh
- [x] Live Seerr title search and service-enriched title detail sheets
- [x] Native Jellyfin Quick Connect with automatic approval polling and secure token exchange
- [x] Cinematic, artwork-led detail sheets for playback, library, discovery, and activity
- [ ] Pagination and migration to a local Room cache

Acceptance: replacing demo mode with a valid profile populates every screen from real services; one offline service does not break the others.

### Milestone 3 — Actions and playback

- [x] Submit Seerr requests
- [ ] Cancel Seerr requests
- [x] Pause and resume active Jellyfin/Emby sessions
- [x] Aggregate and control multiple simultaneous media-server sessions
- Pause, resume, and inspect Radarr/Sonarr queue items where permitted
- Deep-link or cast playback into Jellyfin/Emby clients
- Quality-profile and monitored-state selection before advanced requests
- Confirmation and undo patterns for destructive actions

Acceptance: every mutation shows pending, success, and actionable failure states, and never repeats because of navigation or process recreation.

### Milestone 4 — Background reliability

- [x] WorkManager refresh with network and optional Wi-Fi constraints
- [x] Per-section Home visibility preferences
- Optional notifications for approval, completion, stall, and failure
- Token-expiry and server-certificate guidance
- Exponential backoff, rate-limit handling, and per-service diagnostics
- Tablet/foldable adaptive layout and TalkBack/accessibility pass

Acceptance: updates are useful without being noisy, stale data is visibly identified, and background work recovers after connectivity returns.

### Milestone 5 — Release readiness

- Onboarding and migration-safe local schema
- Release signing, R8 verification, privacy disclosure, and store assets
- Unit, integration, screenshot, accessibility, and physical-device test matrix
- Crash reporting kept optional and scrubbed of endpoints/media history

## Service boundaries

| Service | Primary role | First live reads | First live actions |
| --- | --- | --- | --- |
| Jellyfin | Playback and library | sessions, resume, recent items | playback handoff/control |
| Emby | Playback and library | sessions, resume, recent items | playback handoff/control |
| Seerr | Discovery and requests | discover, request status | create/cancel request |
| Radarr | Movie acquisition | calendar, queue, history | queue retry/remove |
| Sonarr | Series acquisition | calendar, queue, history | queue retry/remove |

## Design system direction

- Near-black midnight surfaces with restrained indigo and violet light
- Artwork remains full-color; controls and labels stay neutral and high contrast
- Rounded, layered cards with shape continuity into detail sheets
- Motion durations around 180–320 ms, using spring motion only for direct manipulation
- Bottom navigation stays visually detached and thumb-reachable
- Phone-first portrait design, then two-pane expansion for larger screens

## Explicit non-goals for the first release

- Replacing the full administration UI of any connected service
- Hosting or proxying media through HomeReel
- Storing server API tokens in a cloud account
- Silent destructive queue or request changes
