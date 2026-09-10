# TV-kort, mobilkantar og innlasting — alpha14

## Rettingar

- Mobilens mediarader blir ikkje lenger klipte 24 dp før høgre skjermkant. Radene går til kanten, medan søk, profil, overskrifter og siste kort behaldar luft. Radene kan framleis sveipast; eit neste kort kan vere delvis synleg ved sjølve skjermkanten.
- Material sin fokusindikasjon låg på heile kortet, sjølv etter at den eigne ramma var flytta til kunsten. På TV er denne indikasjonen no slått av for mediekort. Biblioteket brukar ei klikkbar kolonne utan Material Card sitt farga fokuslag. Ramma følgjer berre biletet og vald avrunding. Søkeresultat har same regel. Vanlege menyval og knappar beheld si fokusmarkering, og mobil beheld trykktilbakemelding.
- Den feilforma S-en i TV-startbanneret er teikna om med ein samanhengande, lesbar bokstavkontur. Sjølve Spole-symbolet er bevart.
- Ei manglande verifisert bibliotekidentitet vart tidlegare returnert som eit vellukka, tomt bibliotek. Dette blir no rapportert som ei ufullstendig innlasting, utan å hente innhald for ein annan brukar.
- Medan Heim er open, blir mislukka eller delvis innlasta Jellyfin-/Emby-data prøvde igjen, høgst ein gong per minutt. Det eksisterande avspelingspolling-kallet oppdaterte berre aktive økter og kunne ikkje hente tilbake «Hald fram å sjå». Vellukka tomme bibliotek utløyser ikkje nye bibliotekskall.
- Ei tom «Hald fram å sjå»-rad ved innlastingsfeil har no ei diskret forklaring i staden for å forsvinne. Reelt tom rad er framleis skjult etter vellukka innlasting.

## Kontroll

Regresjonstestane dekkjer manglande profil utan datalekkasje, ny prøve etter ventetida, vern mot overlappande oppdatering og mot unødvendige kall ved vellukka tomt resultat. Android-testane samanliknar pikslane i bildeteksten før/etter TV-fokus og kontrollerer mobilkanten, siste kort, dobbel skriftstorleik og forklaringa ved mislukka innlasting. Faktiske køyreresultat blir førte i release-rapporten.
