# Spole — arbeidsbolk alpha03

9. september 2026 · 0.16.0-alpha03 / bygg 46 · lokal førehandsversjon, ikkje publisert.

## Levert

- Brei sidemeny med ikon/tekst, tydeleg vald rad og eigen tastatur-/D-pad-fokusmarkering. Smale vindauge held den kompakte rada.
- På breitt oppsett ligg Spole-namn/logo berre i sidemenyen. Profilkontrollen held fram i høgre topphjørne. Manuell bildekontroll fanga feil arva tekstfarge i den nye menyen og utsjånadsoverskrifta; begge brukar no eksplisitt onSurface.
- Innstillingar i kategori/innhald-oppsett når den tilgjengelege breidda er minst 900 dp. Kvar kategori bevarer sin rulleposisjon; smale vindauge viser ei samanhengande side.
- Spelaren skjuler både status- og navigasjonslinje, med systemgest for å vise dei mellombels. Nytt val mellom å bevare heile videoen og å fylle flata med mogleg kantbeskjering.

- Mediaradene går til høgre skjermkant på breie vindauge; profil og kontrollar har framleis innrykk. Lesesider beheld avgrensa linjelengd.
- Personlege einingsval: fire aksentfargar, tre biletstorleikar, førehandsvising og nullstilling av utsjånad. Same storleik i kunst og lasteskjelett; Oppdag tilpassar kolonnetalet.
- Jellyfin startar som standard direkte frå lagra framdrift. Val om å spørje først ligg under Avspeling. Val blir lagra lokalt utan å endre kontoar eller rettar.

- Søkesnarvegen er fjerna frå Heim i vindauge frå 640 dp, både ståande og liggjande nettbrett. Søket i Oppdag er uendra. Eit smalt delt vindauge får mobiloppsettet med søkesnarvegen tilbake.
- Breie episodedetaljar legg thumbnail og tittel/episode ved sida av kvarandre. Omtalen kjem høgare opp. Biletgeometrien blir vald før innlasting; dekoding endrar ikkje storleik eller oppsett.
- Sesong-/førespurnadspanelet får større plakat på breie vindauge. Val, identitet, varsel og sendeknapp har same rekkefølgje og tilgangsreglar som på mobil.
- 86 tekstressursar og tre fleirtalsressursar på nynorsk/engelsk for kontooversikt, sesongval, følgjevarsling, sending, tilbaketrekking og førespurnadsframdrift.
- App-genererte sesongnamn, neste episode og premieredato i førespurnadspanelet følgjer grensesnittspråket. Originaltitlar, omtalar og andre servernamn blir bevarte.
- Statusane «Requested», «Downloading», «Adding to library» og «In your library» kjem frå dei eksisterande enum-/Seerr-verdiane, ikkje omsette tekstar som styringslogikk. Delvis tilgjenge blir ikkje ferdig, og godkjenning blir ikkje nedlasting.
- Popupar brukar ikkje strekk-effekt ved scrollkanten. Vanleg rulling og inn-/uttoning blir bevarte; hovudsidene får ikkje endra scrollåtferd.

## Designmønster og avgrensingar

Breie episodedetaljar: innhaldsbreidd minst 600 dp, 304 dp brei 16:9-kunst (eller 142 dp portrett), 24 dp mellomrom og eksisterande headline-/body-typografi. Under denne breidda blir bilete og tekst stabla. Tittelen kan vekse ved stor skrift; ingen fast teksthøgd. Kunsten er dekorativ for skjermlesaren; episode og tittel blir lesne som tekst. Knappar i den faste verktøylinja er uendra.

Sesongval: 132 × 198 dp plakat på breitt panel, elles 82 × 123 dp. Ingen ny todelt navigasjon, automatisk innsending, avspeling eller ekstra tenarkall. Ukjend status og ukjend premieredato får eksplisitte tekstar. Ingen sesong blir vald automatisk.

Scrollkanten blir avgrensa i den delte popup-verten med `LocalOverscrollFactory provides null`, som er Android sin dokumenterte mekanisme for dette. [Android: overscroll](https://developer.android.com/reference/kotlin/androidx/compose/foundation/OverscrollConfiguration). Dette er ei avgrensa endring i lesepanel, ikkje ei global avslåing av animasjonar.

Engelsk er framleis førehandsvising. Innloggingsskjema, nettverks-/repository-feil, nokre metadata, kalender og bakgrunnsvarsel har attståande arbeid. Denne bolken legg ikkje til ein backend, TV-startpunkt eller nye førespurnadsrettar.

## Biletbuffer — vurdering, ikkje ny implementering

Appen brukar Coil 3.4.0 med standard delt ImageLoader, som har minne- og diskbuffer. Profilbilete har i tillegg ein avgrensa minnebuffer med opptil 12 oppføringar. Det finst ikkje ein eksplisitt samla strategi for utløp/fornying av same biletadresse i appen. Coil 3 ignorerer Cache-Control som standard utan eiga cache-strategi. Neste eigna arbeid er avgrensa diskstorleik, konto-/tenarskilte nøklar, serverens bilettag/ETag eller tidsstyrt fornying, og ei trygg tømming i Innstillingar. Gamle bilete kan visast medan ei oppdatering pågår; innloggingsfeil skal ikkje skjulast som vellukka nettoppdatering. Dette er vurdert, ikkje levert som ny buffer i alpha03.

Kjelder: [Coil-buffrar](https://coil-kt.github.io/coil/image_loaders/#caching), [Cache-Control](https://coil-kt.github.io/coil/network/#cache-control-support).

Fullskjerm følgjer [Android si immersive-rettleiing](https://developer.android.com/develop/ui/views/layout/immersive). Fysisk Galaxy Tab S7+ er meld av brukaren, ikkje tilgjengeleg for lokal verifisering. DeX-/fleirvindaugsrammer blir styrte av systemet; 16:9-video kan framleis ha svarte felt på andre skjermformat i «Heile biletet».

## Testar

- [x] 279/279 einingstestar: vindaugsreglar, tekstressursar, framhaldsval, ukjende lagra val og kontrast i alle fire palettar.
- [x] Debug-app, test-app, lint og signert release-APK bygde frå siste kjelde, inkludert den siste visuelle rettinga. Logg: `app/build/flow-alpha03/review-build.txt`.
- [x] 57/57 Android-testar på nettbrett etter den visuelle rettinga, 135,573 sekund. Logg: `review-tablet.txt`. Dekkjer toppfelt, meny/innstillingar, alle 16 reelle videofiksturtestar, personalisering, dialogar, breie sider/episodar, engelsk førespurnadsflyt, popup-rulling og spelarkontrollar. Video inkluderer immersive-vising, automatisk framhald, start på nytt, spoling, kontobyte, rotasjon og begge Tilbake-kontrollane. Førre bygg bestod 52/52 fordelte på `final-regressions.txt` og `final-tablet-layout.txt`.
- [x] Full mobilrunde på siste bygg, etter den visuelle rettinga: 159/159 Android-testar, 283,855 sekund. Logg: `review-full.txt`. Den ekstra testen sikrar éin logo-eigar og bevart profilkontroll. Førre fullrunde bestod òg (158/158, `final-full.txt`).
- [x] Lint: 0 feil / 28 eksisterande åtvaringar. Pakke `app.reelstack`, bygg 46, signert med det eksisterande Spole-sertifikatet.
- [x] APK: 3 646 057 byte. SHA-256 `AD7995856126CC012E2E077FABF7AF8EE8E328948BC04C129A561C07045B0874`. Lokal kopi: `app/build/flow-alpha03/Spole-0.16.0-alpha03.apk`, med tilhøyrande R8-mapping i same mappe.
- [x] Endeleg release-APK installert med `-r` på innlogga review-emulator 5560, 1920 × 1200 / 240 dpi. Seerr-profilbilete og ekte Jellyfin-/Emby-rader viste data. Mediaradene gjekk til høgre kant, med profilkontrollen innrykt. Profilbiletet kom fram igjen etter ned-/opprulling. Ein Jellyfin-plakat viste den rolege reservegrafikken; ikkje erstatta med demobilete.
- [x] Hav + stor biletstorleik vart valde i den ekte appen, prosessen vart stoppa og opna igjen, og begge vala var bevarte. Automatisk framhald stod på. Til slutt tilbakeført til nynorsk, lime og standard storleik. Ingen innlogging eller serverhandling vart endra.
- [ ] Fysisk nettbrett/Galaxy Tab S7+, fysisk ytingsmåling, full engelsk kjerneflyt og Android TV er ikkje godkjende av denne bolken.

Automatiske testar brukar berre isolert 5562. Ekte kontoar på 5560 blir ikkje brukte til testinnsending, utlogging eller sletting. Lokale skjermbilete med ekte konto-/mediedata blir ikkje lagde i Git eller releasevedlegg.

### Lokale skjermbilete

- [Heim og ny sidemeny](../app/build/flow-alpha03/home-final.png)
- [Separate bibliotekrader heilt til høgre kant](../app/build/flow-alpha03/library-final.png)
- [Innstillingar, Hav-palett og stor kunst etter omstart](../app/build/flow-alpha03/settings-ocean-final.png)
- [Profilbilete etter ned-/opprulling](../app/build/flow-alpha03/home-return-final.png)

Desse lenkjene gjeld berre den lokale arbeidsmappa. Bileta er faktiske emulatoropptak, ikkje mockups. Live video-/kalenderfunksjon mot brukarens tenarar er ikkje verifisert av denne visuelle kontrollen; avspelingsregresjonane ovanfor brukar isolerte videofiksturar.

### Avvik som vart fanga under arbeidet

Ein mellomrunde hadde 152/153 bestått: tastatur-/lukkekontrollen fekk eit tidsavbrot på 1000 ms. Begge AdaptiveDialog-testane bestod deretter uendra separat, og bestod òg i den endelege samlerunden med 158/158.

Første breie menytest starta i berøringsmodus. Testen ber no eksplisitt om tastaturmodus før D-pad. Ei reell tilstandsfeil vart òg fanga: utsjånadspanelet lukka seg ved kategori-byte. Utvidingstilstanden er no eigd av Innstillingar utanfor den utskiftbare innhaldskolonna. Kravet om at valet blir bevart er ikkje fjerna frå testen.

Ved første immersive-avspeling viste Android si systemrettleiing «Viewing full screen» over spelaren, noko som blokkerte automatiske trykk. Ho vart observert og kvittert ut på den isolerte testemulatoren, ikkje undertrykt i produksjonskode. Videodekodaren kan òg bli klar før vindauget har fokus og før eit tidlegare tastatur er borte; maskinvare-Tilbake-testen ventar no på desse faktiske føresetnadene før eitt trykk. Krava om lukking og rapportert stopp står uendra. Alle 16 videofiksturtestane bestod etterpå.

Dei første engelske testane måtte bevare aktivitetens løyveregister i den omsette ressurskonteksten. Vindaugssøk blir styrt frå målte appvindaugsreglar, ikkje eit etterslepande Configuration-felt. Dei ti eksisterande popup-testane er ikkje svekte; halden-peikar-testen fullfører etter at overscroll-strekk vart fjerna.
