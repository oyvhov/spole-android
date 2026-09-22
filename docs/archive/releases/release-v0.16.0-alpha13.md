# Spole 0.16.0-alpha13

Oppdateringar frå GitHub, eit betre bibliotek og meir samanhengande utforming på TV, telefon og nettbrett.

- **Oppdatering i appen:** automatisk sjekk, varsel, versjonsnotat og nedlasting. Fil, versjon og signatur blir kontrollerte før Android ber om installasjonsgodkjenning.
- **Bibliotek:** nytt bibliotekval, ti menyikon og serverbaserte filter for søk, sjanger, år, sett-status, favorittar, oppløysing og sortering.
- **TV:** fokusramma ligg rundt biletet med teksten under. Førespurnadsknappen ligg tidleg i detaljsida og får fokus. Appen har TV-startpunkt og banner.
- **Utsjånad:** felles skrift- og avstandsskala, betre temafargar, tematilpassa lasteskjelett og widget, og meir einsarta avspelingskort på breie skjermar.
- **Oppstart:** animert Spole-logo med innlasting medan animasjonen køyrer.
- **Utvikling:** dokumentert release-flyt og oppstartsskript for emulatorane med lagra data.

Dette er ei **testutgåve**. Frå alpha12: slå på Testutgåver og vel Sjekk no under Appoppdateringar. Eldre versjonar utan oppdateringsmeny må få denne APK-en manuelt éin gong. Installer over eksisterande Spole; ikkje avinstaller først. Kontoar og preferansar blir bevarte.

Pakke `app.reelstack`, versjonskode **56**, Android 8.0 eller nyare. Signert med den eksisterande Spole-nøkkelen. Automatisk sjekk betyr ikkje stille installasjon; Android krev godkjenning.

Mottak av «spel på»-kommandoar frå andre Jellyfin-klientar er ikkje implementert. Fysiske TV-ar og alle maskinvarekodekar er ikkje fullstendig verifiserte i denne runden.

Verifisert: 341 einingstestar, lint utan feil og signert release-bygg. 29 TV-testar fullførte utan feil før emulatorplattformen stoppa. Full mobiltest og installasjon gjennom appen står att på grunn av WSL-feil; denne utgåva er derfor publisert som testutgåve.
