# Designgjennomgang · 10. september 2026

Gjennomgang av Spole 0.16.0-alpha12 på telefon, nettbrett og TV, med etterfølgjande retting i
alpha13. Alle tal er målte i kjeldekoden; kontrastverdiane er rekna etter WCAG 2.1 relativ luminans
frå dei faktiske hex-verdiane i `ReelstackTheme.kt` og `Personalization.kt`.

## Avgrensing

Emulatorane starta ikkje under gjennomgangen. Seks forsak — kald oppstart og snapshot,
`swiftshader_indirect` og `guest`, med og utan vindauge, to ulike AVD-ar — stoppa alle rett etter
«Windows Hypervisor Platform accelerator is operational», med under eitt sekund CPU-bruk og
`hanging thread 'QEMU2 main loop'` i loggen. `emulator -accel-check` melder likevel at WHPX er
brukande. AVD-ane frå tidlegare økter har `path=/mnt/c/…` og blei køyrde frå WSL, som her har
både SDK og `/dev/kvm`, men der brukaren ikkje er i `kvm`-gruppa.

Funna er difor lesne ut av koden, ikkje observerte på ei køyrande eining, og rettingane er
verifiserte ved gjennomlesing. Visuell stadfesting på telefon, nettbrett og Google TV står att.

## Funn og status

| Id | Funn | Målt | Status i alpha13 |
| --- | --- | --- | --- |
| T1 | Fem typestilar blir brukte utan å vere definerte og fell til Material sine Roboto-standardar | 26 bruk av 53 stil-referansar | Retta — alle tolv nivå definerte |
| T2 | `fontSize` sett direkte utan `lineHeight`, så 12 sp tekst arvar bodyLarge sine 23 sp | 198 inline-storleikar, 111 utan linjeavstand | Delvis — nye stilar finst; opprydding i bruksstadene står att |
| T3 | Fire ulike kvitfargar; `Color.White` og `Text`-tokenet møttest på Heim | ΔE 5,6 mellom dei to | Retta for kortitlar og widget; kunstoverlegg beheld rein kvit |
| T4 | Ingen formskala; 16 hjørneradiusar | 124 hardkoda bruk | Delvis — skjeletta følgjer `ArtworkCorner`; full skala står att |
| T5 | Ingen romskala; seksjonsrytmen på Heim varierte | 102 distinkte dp; 24/25/28 topp | Retta — `SectionTop`/`SectionBottom` |
| T6 | To ikonspråk: sju strekglyffar mot 140 fylte Material-ikon | 7 mot 140 | Ope — framlegg om tolv glyffar til |
| F1 | Nøytralane låste i grønt medan tre av fire stemningar er blå, lilla eller nøytrale | fargetone 112–115° mot 223°/276° | Retta — `VisualTheme` ber sine eigne nøytralar |
| F2 | Skjelett og widget følgde ikkje temavalet | 2 flater | Retta |
| F3 | Deaktivert send-knapp hardkoda grøn uansett aksent | 1 knapp | Retta |
| K1 | Innstillingsrader er kontrollar på ei flate som ikkje skil seg frå sida | 1,09:1 | Retta med flatenivå, ikkje kantlinje |
| K2 | Botnlinja klipper «Innstillingar» ved stor skrift | 10 sp, `maxLines = 1` | Retta — `labelSmall`, to linjer |
| K3 | Tynnaste fokusramme er standard også på TV | 3 dp = 6 px på 1080p | Ope — framlegg om BOLD som TV-standard |
| B1 | Seks brytpunkt spreidde over sju filer | 600/640/680/840/900/1000 | Retta — alle i `WindowLayoutPolicy` |
| B2 | Fire ulike sidetittel-behandlingar | displaySmall / headlineLarge / headlineSmall / ingen | Retta — `displaySmall` |
| B3 | Manifestet manglar TV-startpunkt, banner og valfri berøringsskjerm | 0 TV-oppføringar | Retta — pakking på plass, TV-test står att |
| S1 | Ingen kapabilitetsregistrering; Spole kan ikkje vere «spel på»-mål | 0 kall | Delvis — video blir registrert, men mottak av fjernkommandoar manglar og blir ikkje annonsert. Retta under release-kontrollen. |
| S2 | `Device="Android"` hardkoda; alle einingar like i dashbordet | 1 streng | Retta — `Build.MODEL` |
| S3 | Sett- og favorittstatus blir lesen, aldri skriven | 0 skrivekall | Ope |
| S4 | Øktpolling der Jellyfin tilbyr WebSocket | 720 kall i timen ved avspeling | Ope |

## «Spelar no» på breie skjermar

Rapportert under gjennomgangen: kortet såg ikkje ut som resten av appen. `CompactSessionCard` var ei
grå `SurfaceRaised`-plate med eit 128 dp miniatyrbilete ved sida av ei tekstkolonne — den einaste
karda rada på ei side der Hald fram, biblioteksradene og Aktivitet alle er kantlaus kunst på
sidegrunnen. To ting følgde av plata: kvart kort var like høgt som sin eigen tekst, så ei rad fekk
ujamne botnar, og framdriftslinja låg på ulik høgd i kvart kort.

Kortet er no telefonhelten i radskala: 16:9-kunst, same skrim, tittel over botnen og framdrift på
kunstflata si eiga kant. Kvar tekstlinje er éi linje, så alle korta i ei rad måler likt ved kvar
skriftskala. Breidda følgjer kunststorleik-valet som dei andre radene, i staden for den faste
480 dp-en som var laga for plata.

## Ikkje endra

Avspelingsrapporteringa mot Jellyfin er uendra. Ho var korrekt frå før: `Sessions/Playing` ved
første avspeling, `/Progress` kvart tiande sekund og ved pause, spoling og bakgrunning, `/Stopped`
ved slutt og timeout, rett tikk-omrekning og `StartTimeTicks = 0` slik at Media3 og tenaren er samde
om posisjon. Resume les tenaren gjennom `UserItems/Resume` med fallback til det eldre endepunktet.

FOREST-stemninga har nøyaktig same verdiar som før, så standardutsjånaden er uendra for alle som
ikkje har valt noko anna.
