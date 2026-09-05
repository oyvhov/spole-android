# Spole — filmrute-S

Status: included in version 0.11.0. Publication authorized by the user.

The selected direction combines an upright S with two open film-frame counters and a connecting strip. The native artwork is flat pale lime on charcoal: no decorative glass, texture, tiny sprocket holes, extrusion or play triangle. The compact wordmark is rendered as real app text, not baked into the icon.

## Research and inspiration

- [Pentagram — Film Independent](https://www.pentagram.com/work/film-independent): film heritage expressed with flexible graphic forms, clean softened typography and strong colour. Used as a principle, not copied artwork.
- [SPIN — MUBI](https://spin.co.uk/projects/mubi): a simple recognisable identity carried across digital assets and motion. Used for clarity and restraint, not the MUBI dot arrangement.
- [It's Nice That — MUBI Go](https://www.itsnicethat.com/news/spin-mubi-go-graphic-design-290124): a reference for translating a physical cinema cue into a small graphic symbol.
- [Android adaptive icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive): separate foreground/background, clean edges and a 66 × 66 dp central safe area in a 108 × 108 dp container; a monochrome layer supports themed icons.

## Assets and implementation

- `design/brand/spole-mark.svg`: clean editable symbol, transparent.
- `design/brand/spole-icon.svg`: rounded-square preview of the colour launcher icon.
- `design/brand/spole-icon-preview.png`: actual Android-rendered adaptive icon with the emulator's circular mask (512 px).
- `app/src/main/res/drawable/spole_mark.xml`: production native in-app vector.
- `app/src/main/res/drawable/spole_launcher_foreground.xml`: native launcher/themed layer. The actual mark is within x=28.08..79.92 and y=22.72..85.28 in the 108 dp container, inside the safe area.
- `app/src/main/res/mipmap-anydpi-v26/ic_spole.xml`: adaptive icon with colour background, foreground and monochrome layer.
- `design/brand/spole-concept.png`: the selected ImageGen exploration, retained as a design reference only. It has raster artefacts and is not the production icon.

The native vectors are a simplified implementation of the explored film-frame S concept, with clean paths and only the two main counters. The same path is used in SVG and Android resources. The app label, Home and Settings use Spole. App ID, credentials, server identities and signing are unchanged. Historical Reelune assets and release notes are preserved.

## Generation mode and final exploration prompt

Built-in ImageGen was used for exploration (not fallback CLI). A ribbon-coil draft and one refinement were rejected because they retained texture/depth. The final flat film-frame exploration used this prompt:

Use case: logo-brand. Create a pristine FLAT TWO-DIMENSIONAL typographic symbol for an Android movie app named Spole. No text. One thick, compact, upright letter S constructed as a film-strip monogram, viewed exactly straight-on, all points in the same flat plane. Broad geometric S silhouette with softly rounded outside corners and two large square negative-space film frames cut into the upper and lower strokes. Very simple custom Swiss graphic-design geometry, balanced at 24 pixels, no thin lines. Absolutely NO perspective, coils drawn as ellipses, ribbon extrusion or 3D. Only one uniform opaque spot colour pale lime #D5F675. No other colours anywhere. Perfect clean edges, no grain, noise, speckles, brushwork or texture. Generous transparent negative spaces and genuinely transparent background. One centered mark using 64% of a square canvas. No enclosing tile, no typography, no wordmark, no brand references, no gradients, no highlights, no shadows, no glow. A bold minimal print-ready icon, like a precisely cut piece of flat paper.

Production vector implementation intentionally removes the generated small perforations and raster noise; the generated PNG is not represented as a clean production master. No trademark clearance is claimed.

## Verification

- Debug app and Android test APK build successfully; lint reports 0 errors and 20 warnings.
- 20 focused Android tests passed on the isolated API 36 emulator: brand rendering, Home header/rows, viewer experience and smoke tests.
- Native adaptive icon drawn at 24, 48 and 512 px; colour foreground/background and API 33+ monochrome layer verified. The 512 px production rendering was visually inspected.
- Updated the signed-in review emulator with a data-preserving install; visually checked Spole, the personal avatar and the absence of the header calendar shortcut. Account data was not cleared.
- Original design verification was local and unpublished. Release verification is recorded separately in `VERIFICATION_v0.11.0.md`.
