# Spole stabil 1.0

Spole skal bli ein stabil offentleg Android-app med dagens mørke design og ferdig kjerne i
barnemodus. Beta5 rettar minnebruk, bilettoken og profilgrenser før større
funksjonsutvidingar. Kontrollen legg vekt på bygg, relevante regresjonstestar og vanleg bruk på mobil/TV.

## Endringar i utviklingsbygget

| Område | Implementasjon | Kontroll |
| --- | --- | --- |
| Katalog og kalender | Fire arbeidarar, kø på 16, projisert metadata i 8 MiB minnecache og kontoavgrensa Room-indeks. Diskindeksen er avgrensa til 32 MiB med seks timars levetid. Filmfrø held ikkje heile bibliotekdetaljar. | Test med 10 000 element og cachebudsjett består. Kort ekte TV-prøve ved 192 MiB utan nytt minnekrasj. |
| Bilettoken | Emby-adresser utan token, autentisering i header etter opphav/base-path-kontroll, Coil utan automatiske omdirigeringar. | Nettverks- og migreringstestar. |
| Oppgradering | Room 4 til 5 tømmer dashboard-cache og opprettar metadataindeksen. Gammalt Coil-namnerom blir fjerna dersom tilgjengeleg; nytt namnerom blir brukt. | Signert oppgradering på eksisterande TV: kontoar, nynorsk og ekte Jellyfin-/Emby-rader bevarte. Oppgradering med ferdige offlinefiler står att. |
| Profil og konto | Felles session med profil, fingeravtrykk og generasjon. Gamle jobbar og spelarar blir tilbakekalla; linked login skriv mot fanga profil. | Byte bort og tilbake, gamle aktivitetar og sein innlogging. |
| PIN | Vanleg deaktivering krev rett PIN; barneprofil kan ikkje deaktivere vernet eller erstatte ein eksisterande PIN gjennom oppsett. | Rett/feil PIN og byte under gjenoppretting. |
| Tidleg heimlasting | Personlege rader ventar på avklart Seerr-identitet. Verifisert vanleg mediekonto kan vise ordinære bibliotekmetadata medan kontrollen ventar. | Forseinka identitet, mismatch og separat Seerr-svikt. |
| Offline | Aktiv nettransport blir kontrollert. Cachen bevarer ferdige filmar, men nye data må halde seg under 32 GiB/80 prosent av volumet og etterlate minst 512 MiB ledig. | Nettbyte, plassmangel, pause og gjenopptaking. |
| Design og språk | Kompakt hero utan kunst, konsekvent arkverktøylinje, språkressursar for foreldreval og verdstema. | Mobil/TV, tre språk og skriftstorleik 2.0. |
| CI | Einingstestar, lint og bygg på push/PR. Korte smoketestar på éin telefon ved PR; utvalde regresjonstestar for telefon/TV ved manuell køyring før release. Jev er valfri rådgjeving berre på manuelt aktiverte køyringar. | Lokal kontroll og første CI-køyring. |

## Testresultat

Kontrollen før endringane gav 881 beståtte einingstestar, éin hoppa over og null feil.
Lint gav null feil, 188 åtvaringar og eitt tips. Beta4 krasja på TV med minnemangel ved 192 MiB heap.
Sluttbygget 7. oktober 2026 køyrde 903 einingstestar: 902 beståtte, éin hoppa over og null feil.
Lint: null feil, 185 åtvaringar og eitt tips. Debug, test-APK og signert release vart bygde.
Android: 42 ulike mobiltestar og 16 ulike TV-testar er beståtte i dei utvalde pakkene.
Resultatet er samla frå fleire køyringar og målretta omprøver; det er ikkje ei køyring av heile
Android-testpakken. To ekstra oppstartstestar består òg.

| Flate | Testar | Resultat |
| --- | --- | --- |
| Mobil | `ReelstackSmokeTest`, `SheetInteractionTest`, `LoginExperienceTest`, `LinkedLoginTest`, `ProfileSessionBoundaryTest`, `RequestHistoryFlowTest`, `OfflineDownloadsUiTest`, `OfflinePlaybackIntegrationTest` | 42/42 beståtte etter omprøver |
| TV | `LoginExperienceTest`, `ProfileSessionBoundaryTest`, `WatchNextTest`, `TvNavigationIntegrationTest`, dei to spelarprøvene for kontobyte/byte bort og tilbake | 16/16 beståtte etter omprøver |
| Oppstart på mobil | Tilgjenge etter opning og maksimal ventetid ved tregt nett | 2/2 beståtte |

Feila vart undersøkte før omprøving. TV mangla faktisk ein innloggingsknapp ved endring av ei
lagra tilkopling; knappen og fokusflyten er retta. Mobil-AVD-en hadde ein System UI-heng og gamle
skjermoverstyringar. Smoke-fixturen set no sine eigne syntetiske konto-/radval før aktiviteten
startar, rullar til innhald på telefon og kontrollerer den personlege kalenderen frå beta3.
Spelarprøva kontrollerer både tilbakekalling, tømd media-kø og fjerna tittel-ID; standardtittelen
etter tømming er «Spole».

Den siste samla Gradle-køyringa gav `BUILD SUCCESSFUL` for:

```powershell
./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home --no-daemon --max-workers=2 testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

Test-APK-en vart bygd på nytt etter retting av testfixturane. Instrumentering brukte
`app.reelstack.debug.test/app.reelstack.SpoleTestRunner` berre på 5562 og 5566, med klassane
i tabellen og målretta omprøver. Språksjekken godkjenner 1539 standardnøklar og éin tillaten
standardnøkkel utan omsetjing. Versjonssjekk, YAML-parsing og `git diff --check` består.

### Signert lokal kandidat før beta5

Kandidaten er bygd frå arbeidsmappa etter `a9885bd`, med lokale endringar. Versjonen er framleis
`1.0.0-beta4` / kode `141`; dette er eit lokalt utviklingsbygg, ikkje ein ny publisert beta4.

| Felt | Verdi |
| --- | --- |
| Pakke | `app.reelstack` |
| APK-storleik | 11 564 130 byte |
| SHA-256 | `5ecedcafce23d8fe6754ec1a618b469594511c8c13e94f1549b1b6c17719725f` |
| Signatur SHA-256 | `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` |
| Android | minimum 26, target 36 |
| ABI | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Lokal arkivering | `app/build/stable-verification/candidate/Spole-local-candidate.apk` og tilhøyrande `mapping.txt` |

APK-en vart installert med `-r` på eksisterande review-TV, utan ny innlogging eller sletting av
appdata. Ekte kunst, profilbilete og separate Jellyfin-/Emby-rader lasta inn. Ved 192 MiB heap
gjekk total PSS frå om lag 144 til 87 MiB gjennom den korte oppdaterings-/navigasjonsprøva.
Siste Dalvik-allokering var 13,3 MiB. Ingen ny `AndroidRuntime`-krasj vart funnen for den nye
prosessen. Dette er ikkje ein langvarig belastningsprøve. Mobilen med lagra review-data er
halden på førre APK.

Native-biblioteka er uendra. Dei fire ABI-ane har kontrollert `LOAD`-justering på 16 KiB, og
arkivet med FFmpeg-kjelder/relink-materiale finst. R8-mapping frå førre produksjonsbygg vart
teken vare på før bygging.

### Beta5

`1.0.0-beta5` / kode `142` er bygd og signert med den eksisterande nøkkelen. APK, mapping og
FFmpeg-kjelder er arkiverte saman. Signert oppgradering på review-TV bevarte kontoar og val.
Kort ekte Jellyfin-/Emby-avspeling viste 1080p AVC og EAC3 5.1 gjennom FFmpeg utan registrerte
lydavbrot eller droppa bilete i dei observerte prøvene. Offline-integrasjonsprøva består med
pause/gjenopptaking, avspeling utan tenar og profil-/kontobyte. Offentleg oppdatering er under
kontroll; endelege resultat ligg i
[beta5-verifiseringa](VERIFICATION_v1.0.0-beta5.md).

### Prov for tryggleiksrettingane

| Eigenskap | Endra kode og kontroll |
| --- | --- |
| Bilettoken skal ikkje liggje i URL eller gå vidare ved omdirigering | `ServiceClients`, `ArtworkPolicy`, `MediaAuthHeaders` og image-loaderen. `ArtworkPolicyTest` brukar ekte lokale HTTP-tenarar og kontrollerer både same opphav utanfor base-path og framandt opphav. Migreringstesten tømmer gamle dashboard-rader og bevarer uvedkommande kontodata. |
| Socket-token skal berre sendast i header | `JellyfinSessionSocket`. `SessionSocketTest` kontrollerer handshake-header, URL utan token og tilbakefall til polling etter avvising. |
| Sein innlogging, følgjeval og gamle skjermar skal ikkje skrive/vise ein annan profil | `SessionScope`, `ConnectionRepository`, ViewModel, heimkoordinator og spelarar. `SessionScopeTest` dekkjer byte bort/tilbake, seine linked-login-svar og ei sperra kalenderlagring under profilbyte. `ProfileSessionBoundaryTest` og spelarprøvene dekkjer tilbakekalte skjermar på mobil/TV. |
| PIN kan berre fjernast etter rett kode eller stadfesta vaksenpassord | `ProfileViewModel` og PIN-flyten i hovud-ViewModel. Einingstestar dekkjer feil/rett kode, barneprofil og vern mot å erstatte eksisterande PIN. |

Ein fersk, uavhengig kodegjennomgang fann to regresjonar i kandidaten: utlogging kansellerte
arbeids-scopet før oppryddinga kunne starte, og kalenderlagring las aktiv profil for seint.
Begge er retta. Utlogging er prøvd med eit forseinka historiesvar i `RequestHistoryFlowTest`;
kalenderlagring er prøvd med profilbyte mellom kontroll og lagring. Ingen konkret attståande
regresjon vart funnen i den avgrensa gjennomgangen. Dette er ikkje ei påstand om at heile appen
er fri for tryggleiksfeil.

Instrumentering skal berre køyrast på dei eksisterande isolerte testprofilane. Kontoane og
brukardata på review-einingane 5560 og 5564 skal bevarast.

## Krav før stabil publisering

- Ingen kjende kritiske feil i dei endra flytane. Einingstestar, lint og utvalde mobil-/TV-regresjonstestar skal bestå, utan svekte testforventningar.
- Katalogtesten med 10 000 element skal halde minne-/arbeidarbudsjettet. Prøv oppdatering og navigasjon på TV med ekte bibliotek; dokumenter minnebruk og eventuelle krasj.
- Signert oppgradering frå beta4 skal bevare kontoar, profilar, innstillingar og offlinefiler.
- Kort manuell mobil-/TV-prøve: innlogging, profilbyte, navigasjon og avspeling. Kontroller fokus, tekststorleik 2.0 og tilgjenge der skjermar er endra.
- Prøv avspeling og undertekstar mot dei tilgjengelege Jellyfin-/Emby-tenarane. Dokumenter kva tenarversjonar som er prøvde; fixtures åleine stadfestar ikkje ei ekte tenarprøve.
- Kontroller signatur, APK-digest og mapping for kvart publisert bygg. Kontroller native-kjelder, ABI-ar og 16 KiB-sider når native-avhengigheiter blir endra.
- Publisering skjer først når brukaren ber om ein release. Nokre dagars vanleg bruk av kandidaten er tilrådd, utan ei fast ventetid som release-port.

## Arbeid etter 1.0-kjernen

Daglege skjermtidskvotar, casting, SyncPlay og nye medietypar kjem seinare. Leggetid er ei lokal
familierutine, og barnet sin tenarkonto eig bibliotek- og innhaldsrettane. PIN er ikkje kioskvern.
Vidare oppdeling av skjerm- og ViewModel-ansvar skal skje med regresjonstestar for kvar flyt.

Før stabil 1.0 står kort fysisk mobil-/TV-prøve og lengre prøving av undertekstar, nettbyte og
offlinebruk att. Passordgjenoppretting er gjennomgått i koden; ei prøve med eit ekte vaksenpassord
står att. CI er kontrollert lokalt; den endra GitHub-workflowen er enno ikkje køyrd på GitHub.
Beta5 har høgare versjonskode og si eiga verifikasjonsfil, og er ei testutgåve.
