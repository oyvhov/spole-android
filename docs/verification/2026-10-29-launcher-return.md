# Retur til Stova frå eksterne detaljar

29.10.2026, lokal endring `720979c`. Bestilt som funn M1 i Stova-gjennomgangen.
Eigaren gav uttrykkeleg løyve til testing på både Shield TV og Loftet TV.

`RemoteDetailsTest` og alle `launcher.*`-testar: 73 bestått, 0 feil. Tre nye returtestar
feila før implementering. Dei dekkjer retur éin gong, retur under lasting, ny ekstern lenkje,
episode → serie før retur, vanleg detaljlukking og tilbakekalling ved profilbyte.
Signert `assembleRelease` og `lintRelease` består; lint: 0 feil, 214 åtvaringar, 2 hint.

Lokal APK: `1.0.0-beta26`, kode 163, 28 060 400 byte. Ingen versjonsbump eller publisering.
SHA-256: `b2b29163648f82ca57cd59e35308faee98f1e4bec9e4bbff68851e9129cef39b`.
Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

Installert med `adb install -r` på begge TV-ar. Installert base-APK har same SHA-256 som byggfila.
Første installasjonsdato står framleis som 10.09.2026 på Shield og 28.10.2026 på Loftet.
Faktisk D-pad OK på «One Battle After Another» i Stova opna rett detaljside på begge.
Eitt BACK gav same filmkort i Stova att; Android rapporterte Stova Main som framste aktivitet.
Ingen film eller trailer vart starta. Kontoar, favorittar og framdrift vart ikkje endra.
Skjermbileta ligg lokalt under `C:/Launcher/.superpowers/audit-fixes-2026-10-29/`.

Fysisk prøve gjeld den innlogga vaksne Jellyfin-profilen. Emby, barnemodus og underdetaljar
vart ikkje prøvde fysisk; desse kodegrensene er dekte av einingstestane. Ingen push/tag/release.
