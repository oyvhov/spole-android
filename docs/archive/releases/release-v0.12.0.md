# Spole 0.12.0 · Rett innhald og stødigare flyt

## Dette er nytt

- **Nyleg tilgjengeleg:** filmar må både liggje i biblioteka dine og ha ein stadfesta digital utgjevingsdato dei siste 28 dagane. Nye episodar bruker sin eigen premieredato. Gamle filmar som nett er lagde til, kinodatoar og berre fysiske datoar tel ikkje som nye digitale filmar.
- **Kjem snart:** komande heimeutgjevingar frå Radarr og episodar frå Sonarr. Manglande kalenderkjelde blir skild frå ein faktisk tom kalender.
- **Stødigare popup-vindauge:** detaljar, kalender og førespurnader bruker same faste ramme, med rulling og utvidbar omtale inni. Meir informasjon om skodespelarar og roller når tenesta har desse opplysningane.
- **Enklare oversikt:** tilgjengefilter i Oppdag og Alle / På veg / Klare for eigne førespurnader, med ein felles visuell framdrift.
- **Innlogging:** store bokstavar i protokoll og domenenamn blir normaliserte. Adressefelt unngår automatisk retting og gir meir presise feil.
- **Visuell finpuss:** lokal animert filmstripe i førstegongsoppsettet, ryddigare avstandar og betre plass til covermerke og stor tekst. Dette er ei kvalitetsoppdatering, ikkje ein full visuell redesign.
- **Anbefalingar:** rett kjeldetekst for GitHub-lista og bevaring av Oppdag-søket når ein opnar ei anbefaling.

## Kontrollert

- 168 einingstestar og 69 Android-testar bestod. Lint: ingen feil, 20 åtvaringar.
- Ekte kontoar på review-emulatoren: Seerr, Jellyfin, Emby og Radarr. Nye bibliotekepisodar og to komande Radarr-filmar vart viste på Heim og i kalenderen.
- Same produksjonspakke og signeringsnøkkel som 0.11.7; oppdatering bevarer appdata.

## Viktige avgrensingar

- Trygg delt kalender er **planlagd, ikkje implementert**. Kalenderen krev framleis direkte Radarr-/Sonarr-tilkoplingar. Ikkje del administratornøklar med vanlege brukarar.
- Sonarr var ikkje tilkopla review-emulatoren; episodekalenderen er automatisk testa, ikkje verifisert med ekte Sonarr-data i denne runden.
- Digital filmfiltrering er testa med kontrollerte tenestesvar. Ein konkret digital film frå den ekte kontoen er ikkje individuelt etterprøvd.
- Profilbilete som blinkar ved rulling står framleis som eit eige oppfølgingspunkt.

## Installasjon

Last ned `Spole-v0.12.0.apk` og installer som oppdatering til produksjonsappen frå 0.11.7. Ikkje avinstaller eller slett appdata først. Den gamle debug-appen er ei anna pakke.

Repoet er privat, så nedlasting krev GitHub-innlogging med tilgang.

SHA-256: `3271735407faf264e4786a278807002d4b36584b89d75639c272b38e956095f8`
