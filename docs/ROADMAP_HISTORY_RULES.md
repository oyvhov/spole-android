# Personleg historikk og førespurnadsreglar

10. september 2026. Implementert i lokal alpha08. Delarbeid frå 0.17; milepålen er ikkje ferdig.

## Historikk

Aktivitet → Mine → Førespurnadshistorikk hentar den innlogga Seerr-brukaren sine førespurnader, nyaste først, i sider på 20. Eldre sider blir henta først ved «Last eldre førespurnader». Den tidlegare aktivitetsoppdateringa med grense på 100 er framleis ei avgrensa bakgrunnsoversikt; den nye historikken har ikkje denne totalgrensa.

Profilen blir stadfesta før kvar side, også for administratorar. Serverførespurnaden bruker requestedBy, og svaret blir kontrollert for same eigar. Ingen administratornøkkel blir brukt som personleg identitet. Utlogging og kontobyte avbryt henting og tømmer historikken. Profiloppdatering som mistar eller endrar identiteten fjernar gamle historikkort.

Offset tel serverrader før filtrering, slik at ufullstendige eller framande rader ikkje låser pagineringa. Deduplisering bruker førespurnads-ID, ikkje film-ID: fleire førespurnader om same film blir bevarte. Ei side som gjentek seg utan framdrift gir feil og retry. Ei mislukka neste side bevarer tidlegare kort og offset; mislukka oppdatering prøver frå starten igjen.

Metadata blir henta for den aktuelle sida når tittel/bilete manglar. Metadatafeil skjuler ikkje førespurnaden. Manglande medie-ID gir lesbart kort utan ei død detaljhandling. Historisk avvising/feil blir ikkje omskriven til tilgjengeleg berre fordi nokon seinare skaffar filmen. Dato bruker grensesnittspråket.

Historikklesing skriv ikkje til Seerr, lagrar ikkje nye følgjar og slår ikkje på varsel. Lokale aktive følgjar, tilgjengelegheitsfølging og aktiverte varsel er skjerma frå utrensking; berre passive avslutta postar har grense på 100. Historikksidene ligg i minnet og blir ikkje lova tilgjengelege utan nett.

Seerr tilbyr offset-paginering, ikkje eit frosen datasett. Nye/sletta førespurnader under blading kan flytte sidegrenser; oppdater frå starten for eit nytt samla bilete. Dette er ikkje ein arkiveksport med transaksjonsgaranti.

## Kvote og godkjenning

Førespurnadspanelet stadfestar eiga profil og hentar `/api/v1/user/{id}/quota`. Det viser om standardførespurnaden treng godkjenning, kor mange filmar/sesongar som står att og eventuell rullerande periode. `limit = 0` er uavgrensa; manglande/feil kvotesvar er ukjend, aldri null eller uavgrensa ved gjetting.

Seriekvote blir vurdert mot valde sesongar, filmkvote mot éin film. Stadfesta overskriding eller manglande førespurnadsrett deaktiverer Send. Brukaren kan redusere sesongvalet eller hente status på nytt. Ukjend kvote blir forklart, og Seerr avgjer ved sending. Endeleg sending brukar eksisterande klientkontroll av profil, rettar og sesongar; førehandsvisinga reserverer ingen kvote.

Automatisk godkjenning tek omsyn til ADMIN, MANAGE_REQUESTS, AUTO_APPROVE og rettane for aktuell medietype. Dette gjeld appens eksisterande standardførespurnader; ingen ny 4K-førespurnadsflyt er innført.

## Testgrunnlag

- JVM: 125 historikkrader over sju sider, sidegrenser, eigarfilter, manglande total/metadata, feilstatus, kansellering, profilavvik og bevaring av aktive følgjar.
- JVM: kvoteparser, ukjend/uavgrensa/restriksjon, sesongtal, medietypespesifikke rettar, eiga kvote og feil ved profilavvik.
- Android med ekte ViewModel, lokal HTTP-fixture og lagring: dobbeltrykk, forseinka svar, feil/retry, utlogging og endra profil før side eller under profiloppdatering.
- Compose: historikkinnsteg, detaljar/tilbake, retry frå riktig side, manglande metadata, kvote ved endra sesongval, endra rettar og nynorsk/engelsk med skrift 2.0 i breitt vindauge.
- Skjermbilete med syntetiske data blir lagra lokalt i `app/build/roadmap-review/`. Endelege testtal og APK-kontroll: [alpha08-verifisering](VERIFICATION_v0.16.0-alpha08.md).

Alle instrumenteringstestar i denne roadmap-runda går på isolert WSL-AVD 5562. Eldre bibliotektestar vart feilaktig køyrde på 5564 etter at ho hadde fått rolla som review-eining; dei blir ikkje gjentekne der. 5560 og 5564 har ekte kontoar og skal ikkje nullstillast eller brukast til instrumentering.

Gjenstår: eigargodkjende ekte førespurnader gjennom godkjenning, nedlasting, import og varsel; fysisk telefon/TV; langtest og nettbyte. Ingen ekte førespurnad er sendt av desse testane.

## API-grunnlag

- [Seerr: Get all requests](https://docs.seerr.dev/api/get-all-requests/)
- [Seerr: Get quotas for a specific user](https://docs.seerr.dev/api/get-quotas-for-a-specific-user/)
- [Seerr sine rettar](https://github.com/seerr-team/seerr/blob/develop/server/lib/permissions.ts)
- [Seerr sin kvoteberekning](https://github.com/seerr-team/seerr/blob/develop/server/entity/User.ts)
- [Seerr si endelege førespurnadsbehandling](https://github.com/seerr-team/seerr/blob/develop/server/entity/MediaRequest.ts)

Primærkjeldene vart kontrollerte under implementeringa. Utviklingsgreina kan endre seg; konkret serverversjon må inngå i den reelle tenartesten.
