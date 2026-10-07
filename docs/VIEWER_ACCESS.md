# Kontogrenser i Spole

Gjeldande kode etter 1.0.0-beta4. Testgrunnlag og attståande prøver ligg i
[status for stabil 1.0](STABLE_1_0_PROGRESS.md). Adminvisinga krev ADMIN-retten frå Seerr sitt
ferske `/api/v1/auth/me`-svar. Ein Jellyfin-administrator åleine opnar ikkje adminvisinga.
REQUEST_VIEW eller MANAGE_REQUESTS gir heller ikkje ein vanleg brukar andre sine personlege rader.

## Identitet og data

Frå 0.16.0: Ei stadfesta personleg Jellyfin-/Emby-innlogging gir framleis tilgang til eige bibliotek og eigne økter når Seerr er nede. Manglande Seerr-profil gir aldri adminvising eller andre brukarar sine økter. Når begge profilar er tilgjengelege, blir kjende identitetskonfliktar framleis avviste. Manglande medieprofil gir ikkje tilgang på grunnlag av eit namn eller ein gjetta brukar-ID.

- Jellyfin-identiteten kjem frå `Users/Me`; Emby bruker den autentiserte `User.Id` og `Users/{Id}`. Namn og manuelt innskrivne ID-ar stadfestar ikkje eigarskap. Økter blir filtrerte etter eksakt `UserId` før mapping og vising.
- Ein vanleg brukar sin kjende Seerr-kopling må samsvare med mediekontoen. Ein delt admin-konto kan ikkje opptre som ein annan person. Manglande medieidentitet gir tomme personlege rader.
- Tidleg heimlasting held att hald fram, neste episode og favorittar medan Seerr-identiteten ventar. Vanlege bibliotekmetadata kan bli klare tidleg for ein verifisert mediebrukar. Ein kjend identitetskonflikt blir avvist; separat Seerr-svikt tek ikkje bort eit verifisert eige bibliotek.
- Ein stadfesta Seerr-administrator kan hente øktoversikt gjennom ein mediekonto med nødvendige rettar. Medietenaren avgjer framleis kva tilgangsteiknet tillèt.
- Personlege førespurnader sender `requestedBy` og filtrerer eigaren i svaret på nytt. Aktivitet og følgjeval er kontoavgrensa. Spole har ingen direkte Sonarr-/Radarr-klientar.
- Delte køkall blir hoppa over for vanlege brukarar. Kalenderen bruker eigne førespurnader, bibliotekfrø og lokale følgjeval. Ein utgjevingsdato seier ikkje at brukaren har lasta ned noko.
- Ei førespurnad stadfestar aktør, film-/serierett og sesongval før skriving. Admin-API-nøklar kan ikkje sende personlege førespurnader. Fjernkontroll av avspeling stadfestar at økta framleis er synleg.
- Profil, kontoavtrykk og generasjon eig arbeid og skjermtilstand. Kontobyte tilbakekallar gamle jobbar og spelarar, også ved byte bort og tilbake. Gamle heim-/søkprojeksjonar blir haldne att til eigarskapen samsvarar. Innlogging skriv til profilen som starta operasjonen.
- Delte køar, aktivitet og aktive økter blir ikkje lagra i dashboard-cache. Ny konto får ikkje den førre kontoen sitt mellomlager. Personlege innstillingar viser ikkje lagra nøklar i innloggingsfelt.

## Bibliotek og barn

Bibliotekspørringar bruker dei tillatne bibliotek-ID-ane og brukaren sine val per rad og teneste.
Heim og Bibliotek har separate bibliotekval. Biblioteksnamn er ikkje tilgangskontroll; tom eller
feila bibliotekoppdaging opnar ikkje ei uavgrensa spørring.

Barnemodus bruker barnet sin eigen Jellyfin-/Emby-konto. Tenaren eig bibliotek- og aldersrettane;
Spole skriv ikkje admin-policy eller filtrerer etter sjanger som erstatning for kontorettar.
Valfri PIN vernar vegen til vaksenprofilen. Deaktivering krev rett PIN eller stadfesta
passordgjenoppretting for vaksenkontoen. Leggetid er ei lokal familierutine, ikkje kioskvern.

## Avgrensingar

Bruk personlege tenarkontoar. Ein klient med ein delt adminnøkkel kan ikkje erstatte
serverautorisasjon. Seerr eig godkjenningsreglar, kvotar og endeleg aksept av førespurnader.
Spole gjettar ikkje identitet mellom uavhengige Emby- og Jellyfin-installasjonar.

Tenarkontraktar: [Seerr-rettar](https://github.com/seerr-team/seerr/blob/develop/server/lib/permissions.ts), [Seerr-førespurnader](https://github.com/seerr-team/seerr/blob/develop/server/routes/request.ts), [Jellyfin-økter](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/SessionController.cs).
