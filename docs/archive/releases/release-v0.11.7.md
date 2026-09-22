## Spole 0.11.7 · Installerbar produksjonsvariant

### Kvifor denne versjonen finst

Dei tidlegare GitHub-APK-ane var debug-bygg med pakkenamnet `app.reelstack.debug`. Android bind slike
oppdateringar til debug-nøkkelen på maskina som bygde APK-en. Etter at nøkkelen skifta, kunne telefonen
avvise den nye APK-en sjølv om versjonsnummeret var høgare.

### Endringar

- Byggjer no ein produksjons-APK med pakkenamnet `app.reelstack`.
- Brukar ei stabil releasesignering for vidare oppdateringar.
- Held fram med endringane frå 0.11.6 for live anbefalingsstatus og fast detaljpopup.

### Viktig ved første installasjon

Dette er ein ny produksjonspakke og kan liggje ved sida av den gamle debug-appen. Installer 0.11.7,
kontroller at innloggingane fungerer, og avinstaller deretter den gamle Spole-appen dersom Android
viser to ikon. Frå 0.11.7 skal framtidige produksjonsoppdateringar kunne installerast direkte.
