# Verifisering av Spole 1.0.0-beta2

4. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 139.

## Testgrunnlag

- 84/84 relevante einingstestar bestod under bibliotekarbeidet: visingsval og migrering, bileteval, nettverksmapping, samanslåing av «Neste», filtrering og uavhengig lasting.
- Hovudimplementasjonen: 16/16 avgrensa Android-testar ved normal skrift og 12/12 ved systemskrift 2.0 på den isolerte profilen 5562.
- Første justering av tittellinjer: 5/5 ved normal skrift og 12/12 ved 2.0.
- Siste justering: 3/3 ved normal skrift og 3/3 ved 2.0. Filmårstalet er synleg rett under tittelen; seriekort og listevising er òg kontrollerte.
- Kald lasting av breie miniatyrbilete er kontrollert med ein test av bildekantane. TV-fokus, visingsval, sett-merke, vurderingar og faktiske usett-tal er dekte.
- Ekte Jellyfin-film og -serie er viste på den innlogga TV-profilen. Kontoar og appval er bevarte. Mobilkontrollane brukar syntetiske lokale bilete på isolert profil.
- Omsetjingar: 1432 standardnøklar, ingen manglande nynorsk-/bokmålsnøklar og éin tillaten standardnøkkel utan omsetjing.
- Sjå [arbeidsrapporten](LIBRARY_PRESENTATION_2026-10-03.md).
- Heile testsuiten blir ikkje repetert for versjonsauken, i tråd med ønsket om kjappe releasear. Endeleg produksjonsbygg, lint, versjonspolitikk, signatur og offentleg nedlasting blir kontrollerte for denne utgåva.

## Produksjonsbygg og publisering

- Endeleg bygg avslutta med BUILD SUCCESSFUL og kode 0 etter 5 minutt 51 sekund.
- Versjonspolitikk: 9/9 einingstestar bestått. Endeleg lint: 0 feil.
- Universal-APK: 11 473 574 byte, `app.reelstack`, kode 139, `1.0.0-beta2`, minSdk 26 og targetSdk 36; ikkje debuggable.
- SHA-256: `0b2955cb1a6a9d49318d35460147c3692dc753b14631f3bd62c9068de2fdfc77`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og nøyaktig R8-mapping er frosne i `app/build/release-v1.0.0-beta2`. Dei 640 kjeldefilene er kontrollert uendra gjennom bygginga.
- Fire FFmpeg-bibliotek og begge lisensfilene er byteidentiske med beta1. Tilhøyrande kjelde-/relenkingsarkiv er med, med SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Signert `install -r` frå beta1 til beta2 lykkast på lagra TV-profil 5564. Kode 139 og bevarte kontoar med ekte Jellyfin-/Emby-innhald er kontrollerte. Første installasjonstid er framleis 9. september 2026 kl. 20:50:58.
- Den lagra mobilprofilen 5560 starta med beta16. Android fekk mellombels «Broken pipe» og eit System UI-varsel under kald oppstart; etter venting kunne den eksisterande Spole-installasjonen opnast. Ingen data er sletta eller kontoar endra.

## Offentleg release

- Publisert som prerelease, ikkje draft, med nøyaktig éin universal-APK og fem assets: https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta2 . Stabil latest er ikkje endra.
- Kjelde/tag: `16c81c1e173dfe90b9d924ea551b75660d019f5a`. `SOURCE_COMMIT.txt` peikar på denne kjelda.
- Alle fem asset-digestar og storleikar samsvarer med dei lokale arkivfilene.
- Uautentisert release-liste og tag-endepunkt viser beta2. Den offentleg nedlasta APK-en har same SHA-256 som bygget.

## Oppdatering gjennom appen

- På den lagra mobilprofilen 5560, med `0.18.0-beta16` / kode 136: Innstillingar → Notifications and updates → App updates → Check now fann `v1.0.0-beta2`.
- «Download update» lasta ned den offentlege APK-en gjennom Spole. Etter appen sine kontrollar vart «Install update» tilgjengeleg.
- Android sin oppdateringsdialog vart godkjend. Play Protect fullførte skanninga med «This app looks safe», og Android viste «App installed.».
- Installert `app.reelstack` er no `1.0.0-beta2` / kode 139. Første installasjonstid er framleis 15. september 2026 kl. 10:09:51. Appen er opna att; språk, menyval og den lagra mobilkonfigurasjonen er bevarte, utan nytt førstegongsoppsett.
- Dette er ein faktisk nedlastings-/installasjonstest gjennom appen, i tillegg til den separate `install -r`-kontrollen på TV. Ingen appdata er nullstilte.
