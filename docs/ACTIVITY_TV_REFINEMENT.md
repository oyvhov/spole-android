# Aktivitet og TV-detaljar

9.–10. september 2026. Kjeldearbeid etter alpha06, utan ny publisering.

## Designval

- Det første forsøket med større radkort vart avvist av brukaren. Aktivitet er derfor bygd om til eit plakatgalleri utan grå kortboksar: normalt to kolonner på mobil og fleire på breie vindauge. Ved stor skrift blir kolonnene breiare/færre. Overskrift, konto, statusfilter og daggrupper går over heile breidda.
- Plakatane bevarer 2:3-format utan beskjering. Status ligg over ei mørk nedtoning nedst på biletet, tittel og sesongar ligg utanfor. Framdrift og varsel har eigne plassar under; ingen heile-kort-skala ved fokus eller storleiksanimasjon ved bilethenting. «Trekk tilbake» ligg under «Fleire val» og krev framleis stadfesting. «Slutt å følgje» gjeld framleis berre lokale varsel.
- Menyen brukar ei original Spole-vektorfamilie, ikkje Material sine standardikon: heim med avspeling, oppdag med søkjar, aktivitet med filmruter og hake, innstillingar med fysiske skyvarar. 24-einings rutenett, 1,8-einings avrunda strekar. Same ikon på mobil, nettbrett og TV; etikettar og fokussemantikk er bevarte. TV får også eigne ikon for å utvide/minimere menyen.
- Nettbrettlogoen vekslar mellom kompakt og brei sidemeny. Same kontroll beheld fokus og tilgjengeleg namn. På TV blir den eksplisitte menyknappen bevart for å gjere handlinga lettare å finne med fjernkontroll.
- Profiloverlegget har ikkje lenger ei tjukk svart bakgrunnsflate. Berre portrettet har ei tynn lys kant; treffflata og fokusmarkeringa er uendra.
- Titteldetaljar på TV brukar heile vindauget, utan avrunda popupflate eller skyv frå botnen. Tilbake ligg øvst til venstre. Mobil/nettbrett beheld den stabile panelvisinga.
- På TV står kunsten fast til venstre medan metadata og handlingar rullar i kolonna til høgre. Filmplakaten held 2:3-formatet utan beskjering; episodar brukar breitt format. Dette erstattar eit strekt mobilpanel med mykje tomrom.

Teknisk brukar TV-visinga framleis eit separat Android-dialogvindauge for å isolere fokus og sikre system-Tilbake. Det er fullskjermpresentasjon, ikkje ein ny aktivitet eller komplett TV-navigasjonsarkitektur. Innlogging, førespurnadskomponering og stadfestingar er ikkje bygde om til fullskjerm i denne bolken.

Tilgang, henting av aktivitet og Seerr-handlingar er ikkje utvida. Vanleg brukar ser framleis berre si eiga aktivitet. Ingen ekte førespurnader blir sende eller trekte tilbake i testane.

## Bibliotek: valt retning, ikkje implementert enno

Vi vel ei eiga **Bibliotek**-side med alle titlar kontoen faktisk har tilgang til. Heim skal framleis vere eit utval, ikkje ei komplett katalogside. Jellyfin og Emby må kunne identifiserast og filtrerast kvar for seg.

Neste implementering må bruke paginerte, kontoavgrensa bibliotekspørjingar — ikkje late som dei korte Heim-listene er heile biblioteket. Start med filmar/seriar, sortering og bibliotekval. Bevar eksplisitte barnebibliotekval og skil Heim-ekskludering frå reell tilgangskontroll. Bibliotekstøtte frå Emby er ikkje det same som støtte for Emby-avspeling.

## Verifisering av siste kjeldeversjon

- Release, debug og Android-test-APK bygde; 283 JVM-testar bestod. Lint: 0 feil, 36 åtvaringar (inkludert ubrukt eldre detaljetikett etter redesignet).
- 21 målretta Android-testar bestod på den isolerte `emulator-5562`: `ActivityAdaptiveTest`, `RequestFlowUiTest`, `WideNavigationSettingsTest` og `TabletFeatureTest`. Siste køyring: 62,441 sekund. Testane omfattar mobil/brei galleriutforming, kontoavgrensa kjelder, rett detaljidentitet, stadfesting før tilbaketrekking, stabil fullskjermgeometri, statisk TV-kunst medan tekst rullar, navigasjon og eksisterande sesong-/varslingsflyt.
- Signaturen på release-APK-en vart kontrollert og APK-en installert med `install -r` på `emulator-5564`, den ekte innlogga Google TV-instansen. Ingen avinstallasjon, sletting av kontodata eller instrumentering på denne instansen.
- Visuelt kontrollert: originale menyikon på Heim/Aktivitet, profil utan svart ring, ekte Seerr-plakatar og status, redusert TV-topp, opning av Vaiana-detaljar med ukutta plakat og system-Tilbake til Aktivitet. Ingen ekte førespurnad sendt/trekt tilbake eller varslingsval endra.
- Lokale skjermbilete: `app/build/spole-activity-gallery-final.png`, `app/build/spole-gallery-check.png`, `app/build/spole-gallery-details.png`. Dei inneheld ekte konto-/bibliotekdata og blir ikkje lagde i Git.
- Automatisk smal/brei kontroll er ikkje fysisk telefon-/nettbrettgodkjenning. Engelsk er framleis førehandsvising; nokre metadata/tekstar er ikkje omsette. Bibliotekside er framleis planlagt, ikkje levert.
- Ingen ny versjonskode eller GitHub-release i denne bolken. Den lokale signerte APK-en byggjer framleis som alpha06 / 49.

## Inspirasjon

[Wholphin si funksjonsoversikt](https://github.com/damontecres/Wholphin#features) vart lesen på nytt i denne bolken. Relevante mønster er eigne bibliotek, tilpassa bilete/rutenett og ei navigasjonsrad med rask tilgang. Eksisterande referanse til Wholphin sitt detaljhovud i ROADMAP.md blir vidareført. Ingen Wholphin-kode eller bilete er kopierte inn i appen. Nettlesaren fekk ikkje opna detaljskjermbiletet i README; det blir ikkje hevda at denne bolken er ei visuell samanlikning med køyrande Wholphin.
