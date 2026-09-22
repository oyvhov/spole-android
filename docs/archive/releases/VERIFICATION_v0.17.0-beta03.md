# Verifisering · 0.17.0-beta03

## Bygg

- Pakke `app.reelstack`, versjonskode 82, minSdk 26, targetSdk 36. Ikkje debuggable.
- Endeleg køyring av assembleRelease, assembleDebug, assembleDebugAndroidTest, testDebugUnitTest og lintDebug bestod.
- 509/509 einingstestar. Lint: 0 feil, 66 åtvaringar.
- Isolert TV 5566: full runde med DesignRefreshUiTest, WatchNextTest og JellyfinPlayerTest gav OK (44 testar). To plattformavhengige mobiltestar vart hoppa over (PiP og mobilens skjermknapp for Tilbake). 42 bestod.
- Etter siste visuelle retting: 21/21 testar i DesignRefreshUiTest og WatchNextTest bestod, inkludert den nye testen for temagalleriet ved 320 dp og skrift 2.0. Samla 43 unike beståtte testar og 2 plattformavhengige som ikkje vart køyrde.
- Testar med faktisk videodekoding dekkjer spoling, pause, kvalitet, neste episode, undertekstoppvarming, retur og rapportering av framdrift. TV-menytesten dekkjer kapittelval, fart og eitt Tilbake.
- Tidlege testfeil var feil val av Compose-rot/semantikktre og eit syntetisk kontofingeravtrykk før normalisering. Desse vart retta og køyrde på nytt. Ein mellomliggjande lint-køyring krasja i Kotlin UAST; siste avslutta køyring bestod utan unntak eller deaktivering av kontrollar.

## Artefakt

- APK: `Spole-v0.17.0-beta03.apk`, 11 718 966 byte.
- SHA-256: `be507359f3c9ff1e895c9015a454fcdeb639a05fd2340e436133d18069da0d80`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og den tilhøyrande R8-mappinga er arkiverte i `app/build/release-v0.17.0-beta03`.

## Einingar og grenser

- Signert APK installert med `-r` på mobilprofil 5560; versjonskode 82 stadfesta. Profilen stod allereie på innlogging før oppdateringa. Ingen kontoar, AVD-data eller appval vart nullstilte.
- TV 5564 vart oppdatert frå beta02 (81) til beta03 (82) gjennom Spole: Sjekk no → Last ned → kontroll → Installer → Android Update → Open. Installert versjon er stadfesta. Ekte innhald, innlogging, skjult sidemeny og lagra utsjånad kom tilbake etter oppdateringa. Siste APK står open på TV-en.
- Mobiltestprofil 5562 fekk ein Android-systemfeil ved lesing av permission/access.abx før Spole kunne startast. Profilen vart stansa, ikkje sletta eller nullstilt. Mobilprofil 5560 fekk også ein Bluetooth-systemdialog, uavhengig av Spole.
- PiP er implementert, men faktisk vidare avspeling i PiP og retur er ikkje verifisert på ei støtta mobileining. Brukaren er beden om å logge inn på mobilprofilen for denne kontrollen.
- Watch Next-innsetting, framdrift, fullføring og utlogging er verifiserte mot Android TV-provider med syntetisk konto. Google TV si startskjermvising krev separat Google-godkjenning.
- Temagalleri og biblioteksnamn er visuelt kontrollerte frå isolerte skjermbilete. Ekte kontoar og skjermbilete er ikkje publiserte.

## Offentleg publisering

- [GitHub beta03](https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta03) er publisert som prerelease, ikkje draft eller stabil latest.
- Kjeldecommit og uendra release-tag: `d924fd26f7172bc237be56b43187e3eb971fc5eb`.
- Offentleg release-endepunkt er kontrollert utan innlogging. APK-en vart lasta ned på nytt frå den offentlege lenkja; SHA-256 er identisk med artefakten over.
- Heile oppdateringsflyten gjennom appen er prøvd på den ekte TV-profilen, ikkje berre installasjon via ADB.
- Endeleg visuell kontroll på ekte TV stadfesta heimesida etter oppdatering. Bibliotekoppsettet er kontrollert med isolerte UI-testar; den siste manuelle kontrollen gav ikkje fokus via dei injiserte navigasjonstastane, så han stadfestar ikkje full fjernkontrollnavigasjon på denne profilen.
