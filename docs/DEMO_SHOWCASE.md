# Utvida offline-demo

Demoen har 10 filmar, 8 seriar, 6 hald-fram-kort, 4 neste episodar, 6 favorittar, 14 oppdagstitlar og fleire førespurnads-/kalenderhendingar. Titteldetaljar har omtale, sjanger og eksempelvurdering. Alt er demodata, også vurderingar og aktivitet; det er ikkje ei liste over faktiske utgjevingar eller brukarar.

Hero på TV og nettbrett kan bruke lokale demobilete. Levande tilkoplingar får framleis berre hero frå tilgjengeleg serverkunst og synlege bibliotekrader. Demoen bruker ingen token eller eksterne medie-ID-ar, og bilda krev ikkje nettverk.

## Bilete

Tre nye bilete vart laga med det innebygde imagegen-verktøyet 13. september 2026, som fiktiv innhaldskunst. Ingen private bilete vart brukte. Originalformatet er 1536 × 1024 PNG, lagra i drawable-nodpi og gjenbrukt mellom relevante demotitlar. Det kjem om lag 8,1 MB ukomprimert PNG til kjeldekoden; ingen nye biletkall går til nettet.

- `app/src/main/res/drawable-nodpi/demo_coast.png`
- `app/src/main/res/drawable-nodpi/demo_winter.png`
- `app/src/main/res/drawable-nodpi/demo_manor.png`

### Endelege genereringspromptar

**coast**

Use case: stylized-concept. Asset type: original fictional film/series artwork for an offline Android media app demo. Nordic coastal mystery: a small lone lighthouse on a rocky island, dramatic misty fjord mountains, one distant warm window, deep teal sea, dusk, atmospheric cinematic realism. Single full-bleed landscape image, 1536 by 1024. Main focal subject near centre-right, composition must also crop attractively to a central portrait poster. Rich photographic detail, premium film still, restrained natural colours, no text, no logos, no watermark, no UI, no collage.

**winter**

Use case: stylized-concept. Asset type: original fictional film/series artwork for an offline Android media app demo. Warm winter drama: a Norwegian wooden cabin with glowing windows beside a snowy pine forest, a red wool scarf on the fence, soft falling snow, twilight indigo and warm amber, cinematic realism. Single full-bleed landscape image, 1536 by 1024. Main focal subject near centre-right, composition must also crop attractively to a central portrait poster. Rich photographic detail, premium film still, restrained natural colours, no text, no logos, no watermark, no UI, no collage.

**manor**

Use case: stylized-concept. Asset type: original fictional film/series artwork for an offline Android media app demo. Elegant supernatural mystery: a Victorian manor half hidden in autumn forest and violet fog, a small distant human silhouette on a path, warm orange window light, subtle moonlight, suspenseful cinematic realism, not graphic. Single full-bleed landscape image, 1536 by 1024. Main focal subject near centre-right, composition must also crop attractively to a central portrait poster. Rich photographic detail, premium film still, restrained natural colours, no text, no logos, no watermark, no UI, no collage.

## Visuell kontroll

PublicScreenshotsTest viser den faktiske appen med den utvida demoen, standardtema, jul og Halloween. Sesongane blir også testa ved skriftstorleik 2.0. Skjermbileta bruker innstillinga for redusert rørsle, slik at lokal dekor er synleg utan tilfeldig partikkelposisjon mellom opptaka. Dette er ein visuell test av den statiske sesongvisinga, ikkje ei måling av animasjonsyting på ein fysisk TV.

Verifisert 13. september 2026: 476 einingstestar bestått; lint har 0 feil og 31 eksisterande åtvaringar.
PublicScreenshotsTest sine fem scenario er køyrde på TV, nettbrett og telefon. To TV-navigasjonstestar er også bestått.
Telefonopptak: 1080 × 2400 ved 420 dpi. Nettbrettopptak: 1920 × 1200 ved 240 dpi.
Testprofilane er isolerte frå dei lagra kontoane. Galleriet viser 15 utvalde bilete; opptak av stor tekst blir brukte til kontroll.
Tre ekstra aktivitetstestar for demoførespurnad, Oppdag-detaljar og filmdetaljar er bestått på telefon.
Totalt: 20 Android-testkøyringar bestått (5 × 3 skjermformat, 2 TV-navigasjon og 3 telefonflytar).
