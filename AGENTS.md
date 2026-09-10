# Spole: lokal arbeidsrettleiing

Les `docs/AI_INSTRUCTIONS.md` for prosjektreglane. Bevar eksisterande endringar i arbeidsmappa.

Når brukaren ber om ein ny APK-release som appen kan oppdatere frå, følg `docs/RELEASE_WORKFLOW.md`. Bruk same signeringsnøkkel, høgare versjonskode og éin universal-APK i ein publisert GitHub Release; verifiser digest og oppdateringsflyt.

Når brukaren ber om TV-/mobilemulatorar med ekte data, bruk `scripts/Start-SpoleEmulators.ps1` og les `docs/EMULATORS_WITH_REAL_DATA.md`. Bruk dei eksisterande innlogga AVD-profilane; ikkje lag tomme erstatningar, slå på demo, slett appdata eller køyr instrumentering på desse profilane.
