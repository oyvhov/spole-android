# Lokal Spole beta22 – faktisk varm og kald Stova-kortopning bestått
2026-10-25: Faktisk Stova-korttrykk feila i beta21 sjølv om direkte varm/kald ADB-opning fungerte: rett URI vart send, men berre Heim kom fram.
Beta22/159 tek imot rå ACTION_VIEW i Main med same parser/økt-/konto-/barnevern. Mottakaren har tom affinity og handterer onNewIntent. Seks nye regresjonar feila først og er no grøne (18/18 i målretta klassar).
Full testDebugUnitTest, lintDebug og signert assembleRelease er grøne: 1293 bestått, 1 skip; lint 0 feil, 212 åtvaringar, 2 hint. APK 28 060 364 byte, SHA-256 f309682bc23f04535877fe0a54624950037d234893f1b77b86957788447351e8, same eigarsignatur og 20 uendra native bibliotek.
Hovudagenten installerte beta22 med -r; installert SHA er identisk og firstInstallTime er framleis 10.09.2026. Faktisk Stova-kort + D-pad OK opnar rette «One Battle After Another»-detaljar varmt og etter force-stop-kaldstart, med Jellyfin, Trailer og «Hald fram · 143 min att».
Back frå detaljar går til Spole Heim; Stova vart opna uttrykkeleg etterpå. Ingen avspeling, utlogging, kontoimport eller datatømming. Emby/profilbyte/barnemodus er ikkje fysisk prøvde her. Sjå `docs/VERIFICATION_v1.0.0-beta22-local.md`; artefaktcommit er 830cd4d. Berre dokumentasjon er endra etterpå; ingen push/tag/release.

## Beta21: direkte lenkjeopning før faktisk kortprøve
2026-10-25: Brukaren logga inn. Hovudagenten stadfesta ekte Spole-heim og både varm og force-stop-kald `spole://open`-opning med `action=details` for «One Battle After Another» frå Stova sitt Jellyfin-snapshot.
Begge UI-dumpar viser rett tittel, Jellyfin, samandrag og «Hald fram · 143 min att»; den varme skjermdumpen er òg kontrollert visuelt. Ingen avspeling eller konto-/tokenendring frå agentane.
Berre dokumentasjon er endra etter artefaktcommit `92ce694`; signert beta21/158 og APK SHA-256 er uendra. Ingen nye bygg eller einingskall i denne oppdateringa.
Attstår: faktisk korttrykk frå endeleg Stova-installasjon og Back-retur, som hovudagenten tek. Avspeling, profilbyte og barnemodus er ikkje prøvde fysisk. Ingen push eller offentleg release.

## Tidlegare status før brukaren logga inn
2026-10-25: Flett beta20 inn i launcher-greina er fullført i `dd46cbe`. Signert lokal beta21/158 er bygd og installert med -r på Shield; ikkje publisert.
1288 testar totalt: 1287 bestått, 0 feil, 1 eksisterande LAN-test hoppa over. Lint 0 feil (212 åtvaringar, 2 hint), release-bygg OK. Sertifikatet er den eksisterande eigarsignaturen.
APK SHA-256 `3fe9870fa82c6f37fcfa747e44c029ac60014dfe4dc63cd69368873e7ae5c4ee` er verifisert på Shield. Native bibliotek er uendra frå beta20; APK og mapping er arkiverte lokalt.
Mottakaren for spole://open blir no funnen, men både vanleg kaldstart og faktisk detaljlenkje viser Velkomen/Logg inn. Ingen clear/uninstall/utlogging/kontoendring/tokenmigrering eller avspeling er utført. Ekte detaljopning og konto-kontinuitet er ikkje stadfesta; sjå `docs/VERIFICATION_v1.0.0-beta21-local.md`.
Shield-UI er frigjeven til hovudagenten. Etter innlogging: prøv kald/varm detaljopning frå Stova og Back-retur. Versjonskode 158 er lokalt distribuert; ikkje bruk same kode for eit anna artefakt. Ingen push eller offentleg release.
