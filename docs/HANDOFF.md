# Lokal Spole beta21 på Shield – ekte varm og kald detaljopning stadfesta
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
