# HomeReel design system

## Product hierarchy

Home is a service-agnostic media feed. Service names explain provenance, but never become primary navigation. The default order follows immediacy: active playback, resume, new library items, future releases, then active downloads.

## Core patterns

| Pattern | Purpose | Important states |
| --- | --- | --- |
| Playback carousel | One card per active Jellyfin or Emby session | playing, paused, command pending, command failed |
| Media rail | Compact poster browsing across sources | progress, newly added, source badge |
| Upcoming card | Date-first Radarr/Sonarr calendar item | today, tomorrow, later date, missing artwork |
| Activity row | Request/download history with visual recognition | approved, downloading, imported, failed |
| Section switch | Controls Home visibility without disabling sync | visible, hidden |
| Quiet empty line | Confirms a valid empty state without competing with content | no sessions, no releases, empty queue |

## Tokens and motion

- Near-black `Ink` is the page foundation; raised surfaces use `SurfaceRaised`.
- Violet is reserved for navigation, progress, source context, and direct actions.
- Green confirms completed or healthy states; coral is reserved for actionable problems.
- Major cards use 20–34 dp corner radii. Compact media and status elements use 13–20 dp.
- Navigation fades use 150–220 ms. Playback progress uses a low-stiffness spring so live changes remain legible.

## Accessibility

- Interactive rows and cards expose button or switch semantics and use at least 44 dp touch targets.
- Status never relies on color alone; labels describe source, state, and progress.
- Empty states are short factual messages, not decorative cards.
- Artwork always has a local fallback, while decorative imagery has no spoken description.
