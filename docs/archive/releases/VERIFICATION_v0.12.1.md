# Verifisering · popup-retting 0.12.1

Dato: 6. september 2026. Pakke `app.reelstack`, versionCode 31.

## Reprodusert før rettinga

- På den ekte Jellyfin-filmen som låg først på Heim, viste produksjonsversjon 0.12.0 X langt inne i arket. Topplinja brukte både ein tittel med `weight(fill = false)` og ein separat vekta Spacer. Ubrukt tittelplass vart ikkje gitt til mellomrommet; knappen enda før høgre kant.
- Eit nedoverdrag frå innhaldet ved toppen flytta heile popupen langt ned. Dette vart fanga medan fingeren framleis var nede, ikkje berre etter at animasjonen var over.
- Innlogging la både overskrift og X i sin vertikale rullekolonne. Kalender/førespurnad/detaljar hadde ulike topplinjer. Avspeling og innlogging hadde i tillegg innhaldsavhengige høgder.

## Rettar sjølve interaksjonen

- Felles `SheetToolbar`, éin lukkeknapp ved høgre kant utanfor rulleinnhaldet. 48 dp treffflate, også med stor skrift.
- Alle popup-ruter har same 82 % ramme. Kroppen får resten av plassen og eig si eiga rulling.
- Deaktivert modal dra-/nested-scroll-åtferd og fjerna drahandtaket. X, scrim og Android Tilbake er lukkemåtane.
- X køyrer lukkeanimasjonen ferdig før innhaldet blir fjerna.
- Tastaturmarg blir handtert éin gong. Ingen tittelbyte ved ein vilkårleg pikselterskel under rulling.
- Ny test avdekte at Android Tilbake kunne passere det gamle sending-vernet. Dialogen sperrar no òg denne vegen medan ei personleg førespurnad blir send.

## Testdekning

- `SheetInteractionTest`: held fingeren nede ved øvre rullegrense; rask fling og sein metadataoppdatering; identisk X-plassering i detaljar, kalender, innlogging, avspeling og førespurnad; tekstfeltfokus og feil; skriftstorleik 2.0; Android Tilbake ved lesing og under sending.
- Eksisterande kalender-test ventar no på fullført lukkeanimasjon i staden for å krevje synkron fjerning same augneblink som klikket.
- Endeleg full køyring på den isolerte `Spole_Instrumentation`, port 5562: **76/76 Android-testar bestod**, `OK (76 tests)`, 132,967 sekund. Mellomkøyringa som fann sending-vernet vart følgd opp før denne køyringa.
- Den neste køyringa bestod alle dei sju nye popup-testane, men avdekte ein race i den eksisterande Emby-testen: testen las profilen etter at skjemaet var lagra, men før den separate profiloppdateringa var ferdig. Testen ventar no avgrensa på profil eller profilfeil og krev framleis same verifiserte namn/ID/token. Ingen innloggingskode er endra for å få testen til å passere.
- 168/168 JVM-testar bestod på den endelege kjelda. Release-bygg og lint bestod (0 feil, 20 åtvaringar).

## Produksjonsfil

- `Spole-v0.12.1.apk`, 7 140 068 byte, pakke `app.reelstack`, versionCode 31, minimum Android 8 / API 26.
- SHA-256: `42775f5b580bfd73db38cd52c2e2dc564a74ae56e64b00ce8197483c5c4e2f1a`.
- Produksjonssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`, same nøkkel som dei førre produksjonsutgåvene.

## Ekte kontoar og avgrensingar

- Review på `Spole_Review`, port 5560: oppdatert produksjonsapp med `install -r`, ingen sletting av data eller kontoar. Ekte Jellyfin-detaljar med omtale, metadata, medverkande og bibliotekstatus vart opna, utvida og rulla begge vegar. X held seg ved høgre kant. Same nedoverdrag som flytta det gamle arket, lèt det nye arket stå.
- Emby-innloggingsarket vart opna og kontrollert utan å sende innloggingsdata, byte konto eller logge ut. Den felles topplinja blir ståande ved feltfokus og rulling.
- Emulatoren brukte flytande inntastingspanel. Fullt dokka skjermtastatur er ikkje manuelt stadfesta i denne runden. Mellombelse emulatorval for tastaturvising vart sette tilbake til opphavlege verdiar.
- Skjermbilete med ekte kontoar er berre lokale, ikkje del av Git eller utgåva.
- Dette verifiserer geometri og gesthandtering, ikkje garantert 60/120 fps på ein fysisk telefon. Ei diagnostisk rulleprøve i programvare-renderaren SwiftShader, med den andre emulatoren i testkøyring, viste 51/79 forseinka rammer (median 61 ms). Det er ikkje ein kontrollert før/etter-benchmark og kan ikkje brukast som bevis for fysisk biletrate. Ingen fysisk telefon var tilkopla.
