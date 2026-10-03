# Verifisering av Spole 1.0.0-beta1

3. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 138.

## Testgrunnlag

- Før versjonsauken: `assembleDebug assembleDebugAndroidTest lintDebug` godkjende; null lint-feil.
- Isolert emulator-5562: 17/17 spelartestar ved systemskrift 1.0 og 6/6 ved systemskrift 2.0.
- Buffering før/under/etter spoling er dekt i ståande og liggjande mobilformat. Pauseknappen er kontrollert for storleik og nøytrale fargar. Lokale skjermbilde er gjennomgåtte.
- Sjå [arbeidsrapporten](PLAYER_BUFFERING_LAYOUT_2026-10-03.md).
- Testsuiten blir ikkje repetert etter versjonsauken, i tråd med brukarens ønske om kjapp release. Ekte videostrauming og fysisk fjernkontroll er ikkje testa i denne runden.
- Full nedlasting/installasjon gjennom appen blir ikkje repetert. Produksjonsoppdatering med `install -r`, offentleg nedlasting og GitHub-digest blir kontrollerte ved publisering.

## Tillegg etter ny brukarstyring

- Innstillingane deler éin radkomponent. Nedlastingar ligg etter varsel på mobil og nettbrett. Sjå [innstillingsrapporten](SETTINGS_INDEX_ALIGNMENT_2026-10-03.md).
- Endeleg debug-/testbygg: 2/2 innstillingstestar ved systemskrift 1.0 og 2/2 ved 2.0. Innrykk, rekkjefølgje og klikk er dekte. Lokale skjermbilde er gjennomgåtte.
- `ReleasePolicyTest`: 9/9, ingen feil. Overgangen frå 0.18.0-beta17 til 1.0.0-beta1, framtidige betaer og stabil kanal er dekte.
- Endeleg lint: null feil. Heile testsuiten er ikkje køyrd.

## Produksjonsbygg

- Endeleg bygg avslutta med BUILD SUCCESSFUL og kode 0, på 11 minutt 29 sekund.
- Universal-APK: 11 453 702 byte, kode 138, minSdk 26, targetSdk 36, fire ABI-ar, ikkje debuggable.
- SHA-256: `6a8b494387fc3c7732c9042c57aaf4ab1807a5f7719c00e8cfb3566ad534c965`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og nøyaktig R8-mapping frosne i `app/build/release-v1.0.0-beta1`.
- Fire FFmpeg-bibliotek og to lisensfiler er byteidentiske med beta17. Kjelde-/relenkingsarkivet har SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Signert `install -r` frå beta17 til 1.0.0-beta1 lykkast på lagra TV-profil 5564. Installert kode er 138; første installasjonstid er framleis 9. september 2026 kl. 20:50:58. Ingen appdata sletta.

## Publisering

- Publisert som prerelease, ikkje draft: https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta1 . Stabil latest er ikkje endra.
- Kjelde/tag: `3ac6d92`. Fem assets og nøyaktig éin universal produksjons-APK.
- Uautentisert offentleg metadata og offentleg APK-nedlasting er kontrollerte. GitHub-digest og nedlasta SHA-256 samsvarer med det frosne bygget.
