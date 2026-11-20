# Verifisering av Spole 1.1.0

Produksjonspakke `app.reelstack`, versjonskode 178. Bygd på beta40.

Søkjeforbedringane er prøvde med einings- og Compose-testar. Før versjonsendringa bestod 1 696 testar, éin valfri nettverkstest vart hoppa over, og lint rapporterte null feil.

Dei lagra mobil- og TV-emulatorane starta, men ADB viste `unauthorized`. Kontroll med ekte kontoar og oppdatering gjennom appen er difor ikkje stadfesta.

1.1.0 er bygd med 1 696 beståtte einingstestar, éin hoppa over og null lint-feil. Signaturen samsvarer med Spole-nøkkelen. Dei 20 native biblioteka er byte for byte like beta40, og kontrollen for native undertekstar består.

På fysisk Shield vart den signerte APK-en installert over beta40 med `install -r`. Versjonskode 178 og uendra installasjonsdato vart stadfesta. Innlogga profil og ekte bibliotek kom opp, og fjernkontrollfokus på framsida vart kontrollert. Søkeskjermen og oppdatering via appen er ikkje prøvde på Shield.
