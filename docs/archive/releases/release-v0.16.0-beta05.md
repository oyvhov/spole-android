# Spole 0.16.0-beta05

Målet med denne betaversjonen er betre førsteoppleving på kald start, med kortare
oppstartssjikt innan heimevisning og mindre venting før vi får noko synleg innhald.

- Fjerner unødig `delay` før første `refreshLiveData()`-køyring i ViewModel-init.
- `StartupReveal` blir mindre sen og løftar opningsskjerm fortare når første data eller
  cached data er tilgjengelege.
- Oppdaterte versjonsnummer/versjonskatalog for publisering.

Dette er ein testutgåve.