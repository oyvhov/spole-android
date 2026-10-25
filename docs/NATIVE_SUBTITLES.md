# Lokal ASS/SSA-rendering

Spole brukar `io.github.peerless2012:ass-kt:0.5.1` frå Maven Central. AAR-en har med seg
`libass.so`; den separate `ass`-AAR-en har dei same filene og ein tom `classes.jar`, og er ikkje med.
Berre Kotlin/JNI-kjernen er brukt. Media3-integrasjonen er Spole sin eigen, mot Media3 1.11.1;
oppgradering av Media3 krev dei native prøvene for spoling og sporbyte.

Kjelde: [libass-android v0.5.1](https://github.com/peerless2012/libass-android/tree/504f64a325059ab5188c4c9d0eddc99c9356a23a).
Native bygg: NDK 28.1.13356709, CMake 3.22.1, fire Android-ABI-ar. ARM64 og x86_64 har
16 KB ELF-segmentjustering; 32-bit bibliotek har 4 KB. Maven-AAR SHA-256:

```
ass-kt 1051212faf98ef956e06992b002d43cfdc81b6c60dcd32662e8d3ff52d584d65
```

## Kjelder og lisensar

Full lisens-/opphavstekst følgjer APK-en i `assets/licenses/libass/`.
FreeType blir brukt etter FreeType License. FriBidi blir brukt etter LGPL 2.1 eller nyare.
Wrapperen er MIT, libass ISC, HarfBuzz/Expat/Fontconfig har sine medfølgjande permissive
lisensar, og libunibreak har zlib-lisens. FreeType-prosjektet har copyright på delar av programvara.

Native avhengigheiter er låste i
[libass-cmake](https://github.com/peerless2012/libass-cmake/tree/d3f00a43ca66e42a2c34de964b1a7dbbfa9dbc8b):

| Del | Kjeldecommit |
|---|---|
| libass | `bbb3c7f1570a4a021e52683f3fbdf74fe492ae84` |
| FriBidi | `68162babff4f39c4e2dc164a5e825af93bda9983` |
| FreeType | `42608f77f20749dd6ddc9e0536788eaad70ea4b5` |
| HarfBuzz | `c3fcbffa651cea70400552f2a8bd695ad11023c1` |
| Fontconfig | `daa175d234b8a362eedd4c18c33537cc2d19cd98` |
| Expat | `f9a3eeb3e09fbea04b1c451ffc422ab2f1e45744` |
| libunibreak | `304585d8e2d63187507368d612c3d5fff1486368` |

Ved publisering skal komplett tilhøyrande kjelde og bygginstruksar for desse native
biblioteka vere tilgjengelege saman med releasen, slik FFmpeg-kjelda allereie er.
Klon wrapperen på commiten over med alle rekursive submodular. Bevar submodulane og
byggfilene i kjeldearkivet; GitHub sitt vanlege kjeldearkiv inneheld ikkje submodulane.
Ein endra kjerne kan byggjast som dei same AAR-modulane, bytast inn i appens Gradle-
avhengigheiter og byggjast til ein ny APK med eigen signeringsnøkkel.

`scripts/package-native-subtitle-sources.py <rekursiv-klone> <ny-arkivsti>` pakkar dei
låste Git-objekta, inkludert FreeType sin rekursive `dlg`-avhengigheit. Han tek ikkje
med `.git`, uregistrerte filer eller bygg-/signeringsdata. Bruk ein ny arkivsti; eit
eksisterande arkiv blir ikkje overskrive. Arkivet inneheld `SOURCE-MANIFEST.json` og
`BUILDING.md` med konkrete AAR-byggje- og relenkingssteg.

Kontrollert lokalt 17. oktober: ti låste kjeldetre og 5923 arkivoppføringar. Arkiv:
`app/build/native-subtitle-source/spole-native-subtitles-0.5.1-source.tar.gz`,
50 060 685 byte, SHA-256
`63d16b20ba39a14789633abf372eed82fa0061c8c974e164c6fe8cbd86efd935`.
Arkivet er kontrollert for innhald; dei native biblioteka er ikkje bygde på nytt her.

## Driftsgrenser

- ASS blir teikna på eigen bakgrunnstråd, maksimalt 30 oppdateringar per sekund og 720p
  teikneflate. Videoen og HDR-signalet går framleis gjennom den vanlege videosurface.
- Native cache: 24 MB bitmap / 2000 glyfar. Dokument: 2 MB. Fontar: 2 MB per fil, 8 MB totalt.
- Eit originalt ASS-dokument over 2 MB, eller eitt som ikkje kan hentast, blir bytt ut med
  tenaren sin VTT av same spor (`Stream.vtt`, som beta17 alltid brukte): rein tekst i staden
  for ingen. VTT-varianten ligg som eit lat sidespor og blir berre henta når han trengst.
- Tekst kan ikkje halde ein ferdig video i gang. Media3 ventar på at siste line er over før
  avspelinga sluttar; ei line til 9:59:59 heldt episoden ved slutten utan nedteljing.
  `MediaBoundTextRenderer` reknar teksten som ferdig når han er lesen til endes og posisjonen
  har nådd lengda på perioden.
- Feil frå Spole sine eigne lesarar (SUP, MKS, ASS-dokument) er `SubtitleSourceException`. Ein
  slik feil, og ein HTTP-feil på ei undertekstrute, slår av teksten i staden for å pakke om
  eller stoppe filmen.
- Originale stilar, plasseringar, teikningar, animasjonar og innebygde fontar blir bevarte.
  Manglande fontar får ein registrert systemfont som reserve.
- Fråkopla MKV les vedlegg via SeekHead, med avgrensa I/O; video-cluster blir ikkje skanna.
- `SpoleSubtitleSources` er ein liten pakkeintern kompatibilitetsadapter for Media3 sin
  late undertekstlasting. Han må kontrollerast ved endring av Media3-versjon.

## Bitmaptekst

Innebygd PGS og DVD/VobSub går gjennom Media3. Ekstern PGS/SUP og MKS med PGS eller
DVD/VobSub blir brukte lokalt når tenaren annonserer ei gyldig filadresse på same opphav.
MKS-adapteren krev eitt undertekstspor med den annonserte kodeken og bevarer serverens
spor-ID og språk gjennom Media3 si late lasting. Uvalde sidefiler blir ikkje opna.
SUP-parseren lagrar avgrensa epokepunkt for spoling.

Bitmap-sidefiler blir førehandslasta med ein 16 KiB kopieringsbuffer til appens private
cache før Media3 får velje sporet. Dermed held videoen fram medan tenaren trekkjer ut
underteksten. Taket er to filer og 256 MiB per spelar; storleiksbudsjettet trekkjer frå
64 MiB av den ledige plassen før nedlasting. Namna inneheld ikkje nettadresse eller
tilgangsteikn. Filene blir fjerna ved
utkasting/lukking; neste lasting ryddar foreldrelause filer etter prosessavslutting og
bevarer andre aktive spelarar sine filer. Vanleg tekst ligg framleis i ein avgrensa
minnecache. Eit utkasta språk blir førehandslasta på nytt før det blir valt.

Serverar utan ei støtta filadresse beheld forhandlinga om brenning/transkoding. Full
kompatibilitetsreserve annonserer ikkje den same bitmap-ruta på nytt. Separate IDX/SUB-
par blir ikkje henta direkte; serveren kan levere dei som MKS. Det er ikkje full dekning
av alle format. [Jellyfin si uthenting](https://github.com/jellyfin/jellyfin/blob/master/MediaBrowser.MediaEncoding/Subtitles/SubtitleEncoder.cs)
viser MKS-ruta; eldre serverversjonar og Emby må framleis gi ei eksplisitt støtta adresse.
