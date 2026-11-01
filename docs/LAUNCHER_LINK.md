# Stova-lenkjer til Spole

Denne låste kontrakten lèt Stova eller ein annan Android-startar opne ein tittel i Spole med ei app-til-app-lenkje. Lenkja gir ingen nye tilgangsrettar. Sjå [verifisering og attståande prøver](VERIFICATION_LAUNCHER_LINK.md).

## Opne eit bibliotek

Frå 1.0.0-beta26 kan startaren opne eitt bestemt bibliotek med:

```text
spole://library?kind=jellyfin|emby&user=<id>&library=<id>
```

Vel éi teneste. `user` er brukar-ID-en i tenesta, og `library` er ID-en til biblioteket på den same tenaren. Begge følgjer ID-krava nedanfor. Spole brukar den lagra kontoen, kontrollerer at biblioteket er tilgjengeleg for kontoen og opnar det i vaksen- eller barneskalet. Dei same konto- og barnemodusgrensene som for tittellenkjer gjeld. Lenkja er eit Android app-til-app-kall utan `BROWSABLE`, og skal ikkje innehalde tilgangsteikn, passord eller tenesteadresser.

Når startaren opna biblioteket, går siste Tilbake frå rota av det tilbake til startaren, slik ho gjer
frå tittel-detaljar (`LibraryCoordinator.atLauncherLibraryRoot`). Inne i ei mappe går Tilbake først opp
i biblioteket.

## Opne Spole som ein profil

```text
spole://profile?kind=jellyfin|emby&user=<id>
```

Opnar Spole på Heim som profilen som har kontoen, for ei profilflis i startaren. Lenkja har eige
filter for verten `profile` i manifestet, som `open` og `library`. Ein eigen profil
(vaksen eller barn) med kontoen vinn over hovudprofilen; finst kontoen berre i hovudprofilen, blir det
hovudprofilen. Spole byter profil først når det trengst. Frå barnemodus byter ei lenkje aldri profil.
`user` følgjer ID-krava nedanfor.

Ein eigen, vaksen profil blir laga under Tenestene dine → Profilar → «Eigen profil for …», frå ein
Jellyfin- eller Emby-konto i hovudprofilen. Kontoen blir kopiert, så hovudprofilen held fram med alt,
og profilen er vanlege Spole med berre den kontoen (`kid.<id>.adult`). Barnemodus er «ein
barneprofil er aktiv», ikkje «ein annan profil enn hovudprofilen er aktiv».

## Profillista til Stova

`content://<pakke>.launcher/profiles` (`LauncherProfilesProvider`, `app.reelstack.launcher` eller
`app.reelstack.debug.launcher`) gir éi rad per profil med ein innlogga Jellyfin- eller Emby-konto:
`id`, `name`, `kind` (`jellyfin`/`emby`), `user`, `avatar` og `kid` (0/1). Hovudprofilen gir ein konto
som ikkje har eigen profil, Jellyfin før Emby. `avatar` er adressa til brukarbiletet på tenaren og har
ingen token. Berre `app.stova` og `app.stova.debug` får rader; andre får ingen.

Når nokon byter profil i Stova, kallar Stova `call("select_profile", null, {kind, user})` på same
leverandør. Spole byter til profilen med den kontoen (eigen profil eller barneprofil før
hovudprofilen) og svarar `result` = `to`, `already`, `blocked` eller `unknown`. Spole går aldri ut av
ein barneprofil på denne måten når Spole har PIN (`blocked`); det gjer ein vaksen i Spole. Andre enn
Stova får `null`, og ingenting anna blir skrive.

## Radene til ein person

`query(content://<pakke>.launcher/rows?kind=<jellyfin|emby>&user=<id>)` gir radene til personen med
den kontoen, funnen som i `select_profile`: «Hald fram» (`row` = `continue`, halvsett og så neste
episode), nye filmar (`latest_movies`) og nye episodar (`latest_series`). Kvar Jellyfin- og Emby-konto i
profilen gir sine rader. Kolonnar: `row`, `kind`, `user`, `server` (namnet på tenesta), `id` (tittel-ID-en
til `spole://open`), `type`, `title`, `series`, `season`, `episode`, `year`, `runtime`, `progress` (0–1),
`overview`, `wide`, `poster`, `backdrop`, `logo` (bildeadresser utan token), `community` (0–10), `critic`
(prosent) og `official` (aldersgrensa slik tenaren gir ho, til dømes «15» eller «PG-13»).

Spole hentar radene frå tenaren med si eiga innlogging når Stova spør, på tråden kallet kjem på. Stova
kallar difor utanfor hovudtråden. Spole sine eigne lagra rader, som berre finst for den aktive profilen,
blir ikkje rørte. Ein Stova-profil som følgjer personen, treng då ingen eigen konto: Eilev får Emby-radene
sine i Stova utan Emby-passord der. Andre enn Stova får ingen rader.

## Handlingar på ein tittel

`call("item_action", null, {kind, user, item, action})` på same leverandør: `action` = `watched` (merk
som sett) eller `remove_resume` (fjern frå «Hald fram»). Spole gjer handlinga på tenaren med kontoen Stova
nemner, og svarar `result` = `ok`, `failed` eller `unknown` (Spole har ikkje kontoen). Når kontoen høyrer
til profilen Spole har open, gløymer Spole tittelen i sin eigen avspelingslogg òg, så Heim i Spole er
samd neste gong han blir vist. Andre enn Stova får `null`.

## Oppdag for Stova

Stova kan vise Oppdag (Seerr) for personen med Seerr-innlogginga til Spole, så Stova treng ingen
Seerr-konto sjølv. Alle stiane ligg på same leverandør, krev at kallaren er Stova (`launcherMayList`),
og `kind`+`user` peikar på Spole-profilen, funnen som i `select_profile`. Spole spør Seerr-kontoen til
den profilen på tråden kallet kjem på, så Stova kallar utanfor hovudtråden. Bilete er fulle
TMDB-adresser (`https://image.tmdb.org/t/p/w500…` plakat, `w1280` bakgrunn), som Stova lastar direkte;
ingen token eller Seerr-adresse går over.

| Kall | Svar |
|---|---|
| `discover?kind=&user=` | Rader `trending`, `upcoming_movies`, `upcoming_series` med kolonnane `row, type, tmdb, title, year, overview, poster, backdrop, status, community, genres`. Extras: `seerr` = `none`/`browse`/`personal`. |
| `discover/requests?kind=&user=[&refresh=1]` | `type, tmdb, title, poster, backdrop, stage, percent, seasons, request_id, is4k` for brukaren sine følgde førespurnader. `refresh=1` ber Spole hente status frå Seerr først. |
| `discover/title?kind=&user=&type=&tmdb=` | Éi rad: `type, tmdb, title, year, tagline, overview, poster, backdrop, runtime, genres, community, critic, status, can_request, request_id, request_status, trailer`. |
| `discover/seasons?…&type=tv&tmdb=` | `number, name, episodes, status, air_date`. |
| `discover/related?…&type=&tmdb=` | Rader `similar` og `recommended` med radkolonnane. |
| `discover/search?kind=&user=&q=&page=` | Rad `search` med radkolonnane. Extras: `page`, `has_more`. |
| `call("discover_request", {kind,user,type,tmdb,seasons})` | `result` = `ok`, `already`, `not_personal`, `forbidden`, `failed`, `unknown`. |
| `call("discover_withdraw", {kind,user,request_id})` | `result` = `ok`, `failed`, `unknown`. |
| `spole://discover?kind=&user=&type=&tmdb=` | Opnar tittelen i Spole sitt Oppdag. |

- Kvart svar frå `discover…` til Stova har extra `seerr`: `none` (ukjend person, barneprofil eller
  ingen Seerr som er sett opp; då kjem ingen rader), `browse` (API-nøkkel: sjå, men ikkje be om) eller
  `personal` (ei personleg Seerr-økt: alt). Oppdag er berre for vaksne profilar, og aldersgrensa til
  profilen gjeld ikkje her: Seerr gir ingen aldersgrense på radene.
- `type` er `movie` eller `tv`, og `tmdb` er eit tal over null; `discover/seasons` svarer berre for
  `tv`. Ein tittel utan type eller TMDB-ID blir ikkje send.
- `discover` hentar dei tre radene samstundes, og `discover/related` gir `similar` før `recommended`.
  Ei rad som feilar, blir borte utan å ta dei andre med seg.
- `discover/title`: `can_request` er 1 berre for ei personleg økt og ein tittel Seerr lèt nokon be
  om. `request_id` og `request_status` er førespurnaden personen følgjer for tittelen. `community` er
  0–10, `critic` prosent (Rotten Tomatoes), og `runtime` blir ikkje sendt enno.
- `discover/requests` les førespurnadene Spole følgjer for Seerr-kontoen. `refresh=1` spør Seerr berre
  med ei personleg økt; feilar det, kjem det Spole hadde frå før.
- `discover/search` krev `q` med minst to teikn, og `page` byrjar på 1. Kortare `q` gir ingen rader,
  `page` = 1 og `has_more` = false.
- Andre enn Stova får ein tom markør med kolonnane og utan extras.

`status` er Seerr sin mediestatus (1 ukjend, 2 ventar, 3 på veg, 4 delvis, 5 i biblioteket,
6 sperra). `stage` er Spole sitt steg (`requested`, `downloading`, `importing`, `available`,
`declined`, `failed`, `unknown`, `watching`). Ein eldre Spole utan desse stiane svarer med den tomme
profilmarkøren utan `seerr`-extra; Stova les det som «ikkje svart» og viser ingenting.

## Godkjenn innlogging for Stova

Stova viser ein Jellyfin Quick Connect-kode og kan be Spole godkjenne han som ein av kontoane Spole er
logga inn med på den same tenaren. Stova får då si eiga innlogging frå tenaren: ingen tilgangsteikn,
passord eller adresseliste går frå Spole.

- Aktiviteten `app.reelstack.launcher.LauncherApproveActivity` har handlinga
  `app.reelstack.action.APPROVE_SIGN_IN` og må startast med `startActivityForResult`, slik at Spole ser
  kva app som spør. Berre `app.stova` og `app.stova.debug` får svar.
- Ekstra: `app.reelstack.extra.SERVER_URL`, `app.reelstack.extra.SERVER_ID` (Jellyfin-`Id` frå
  `System/Info/Public`, valfri), `app.reelstack.extra.CODE` (4–10 siffer) og
  `app.reelstack.extra.FOR_CHILD` (barnekontoar først).
- Spole tilbyr berre Jellyfin-kontoar på same tenar: same adresse (også den andre ruta), eller same
  `Id` når adressene er ulike. Brukaren vel kontoen, og PIN-en til Spole må skrivast når han er sett.
- Svar: `RESULT_OK` når koden er godkjend. Elles `RESULT_CANCELED` med `app.reelstack.extra.ERROR`:
  `not_signed_in`, `declined` eller `failed`.

## Framdrift tilbake til Stova

Når spelaren stoppar eller går i bakgrunnen, sender Spole ein eksplisitt kringkasting til
`app.stova` og `app.stova.debug` (`LauncherProgress`) med handlinga
`app.reelstack.action.PLAYBACK_PROGRESS` og felta `kind` (`jellyfin`/`emby`), `user`, `item`,
`position_ms`, `duration_ms` og `ended`. Meldinga er ikkje hemmeleg og gir ingen rettar; Stova rettar
radene sine med ho til tenaren har fått stopprapporten.

## Låst format

```text
spole://open?kind=jellyfin|emby&user=<id>&item=<id>&action=details|play
```

`|` viser alternativa i kontrakten; vel éin verdi i kvar faktisk lenkje.

| Parameter | Krav |
| --- | --- |
| `kind` | Nøyaktig `jellyfin` eller `emby`. |
| `user` | Brukar-ID-en i den valde medietenesta. |
| `item` | Den eksterne tittel-ID-en i same teneste. |
| `action` | Nøyaktig `details` eller `play`. |
| `start` | Valfri, berre `0` og berre med `play`: spel frå starten utan spørsmålet om å halde fram. |

Begge ID-ane skal etter dekoding passe heile uttrykket `[A-Za-z0-9_-]{1,128}`. Dei fire første parametrane er påkravde og skal finnast éin gong kvar. Rekkjefølgja er fri. Ukjende parametrar blir ignorerte, men må framleis ha gyldig URI-/prosentkoding. Namn og verdiar blir prosentdekoda éin gong som UTF-8; ugyldig prosentkoding eller UTF-8 blir avvist, også i ukjende felt. Dobbelkoda ID-ar gir ikkje ein ny dekodingsrunde.

Ruta er nøyaktig `spole://open`, utan sti, avsluttande skråstrek, port, brukarinfo eller fragment (heller ikkje eit tomt `#`). Andre skjema, vertar, store bokstavar i dei faste verdiane, manglande/tomme felt, dupliserte påkravde parameter og ugyldige ID-ar blir avviste. Duplikat blir avviste også når verdiane er like eller parameternamnet er prosentkoda. Ved ugyldig input opnar mottakaren verken detaljar eller avspeling og avsluttar.

Lenkja skal aldri innehalde tilgangsteikn, passord eller tenesteadresser. Ho brukar berre kontoar som alt er lagra i Spole. Dette er ei anna rute enn [oppsettslenkjer](APP_GUIDE.md), som brukar `spole://setup`.

## Syntetiske døme

Alle ID-ar nedanfor er oppdikta og viser berre formatet.

```text
spole://open?kind=jellyfin&user=synthetic_adult_01&item=synthetic_movie_01&action=details
spole://open?kind=jellyfin&user=synthetic_adult_01&item=synthetic_movie_01&action=play
spole://open?kind=emby&user=synthetic_child_02&item=synthetic_series_02&action=details
spole://open?kind=emby&user=synthetic_child_02&item=synthetic_episode_03&action=play
```

## Android-kall frå Stova

Mottakaren `app.reelstack.launcher.LauncherOpenActivity` er eksportert, utan eige grensesnitt (`Theme.NoDisplay`), utan historikk og skjult frå nylege appar. Intent-filteret har `VIEW`, `DEFAULT` og `spole`/`open`. Det har **ikkje `BROWSABLE`**: integrasjonen er eit Android app-til-app-kall, ikkje ei nettlesarlenkje.

Frå lokal beta22 har mottakaren tom `taskAffinity`, slik at han ikkje deler oppgåvetilknyting med
hovudskjermen. Begge aktivitetsinngangane handterer ei ny lenkjeintensjon. Dersom Android
gjenbruker ein eldre oppgåvestakk og leverer den rå `ACTION_VIEW`-lenkja til `MainActivity`, går
ho gjennom same parser, kontooppløysing og øktbundne konsumering som den vanlege mottakaren.
Rå lenkjer kan ikkje bruke interne ekstra for å overstyre profil eller omgå ei ugyldig URI.

Send `ACTION_VIEW` med pakken `app.reelstack`. Dersom produksjonsappen ikkje kan opnast, kan utviklarintegrasjonen prøve `app.reelstack.debug` som reserve. Til dømes:

```kotlin
val uri = Uri.parse(
    "spole://open?kind=jellyfin&user=synthetic_adult_01&item=synthetic_movie_01&action=details"
)
fun openIn(packageName: String) = context.startActivity(
    Intent(Intent.ACTION_VIEW, uri)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .setPackage(packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
)
try {
    openIn("app.reelstack")
} catch (_: ActivityNotFoundException) {
    try {
        openIn("app.reelstack.debug")
    } catch (_: ActivityNotFoundException) {
        // Vis at Spole må installerast i startaren sitt eige grensesnitt.
    }
}
```

Dømet brukar Android-typane `android.net.Uri`, `android.content.Intent` og `android.content.ActivityNotFoundException`, og ein gyldig Android-`Context`. Det er integrasjonskode, ikkje dokumentasjon på ei utført einingsprøve.

## Konto og barnemodus

Spole krev først ei lagra hovudtilkopling for den valde tenesta med eit ikkje-tomt tilgangsteikn. Dette er ein lokal innloggingskontroll, ikkje ei stadfesting frå tenaren på at teiknet framleis er gyldig. Tenaren handhevar innhaldsrettane.

| Aktiv modus og mål | Resultat |
| --- | --- |
| Vaksenmodus, hovudkontoen sin eigen brukar-ID | Opnar med hovudkontoen. |
| Vaksenmodus, ein kjend barnekonto | Byter til barnet sin lokale profil, validerer på nytt og opnar med barnet si eiga tenestetilkopling. |
| Barnemodus, den aktive barnekontoen sin eigen brukar-ID | Opnar berre med barnet si eiga tilkopling og eige tilgangsteikn. |
| Barnemodus, hovudkontoen eller eit anna kjent barn | Blir stoppa; lenkja kan ikkje gå ut av barnemodus eller byte barn. |
| Ukjend brukar eller manglande nødvendig innlogging | Blir stoppa som ikkje innlogga. |

`user` er den faktiske Jellyfin-/Emby-brukar-ID-en; han kan vere ulik ID-en til den lokale Spole-barneprofilen. Ein lokal profil-ID er ikkje i seg sjølv ei innlogging. Ingen vaksentilkopling blir brukt som reserve for eit barn. Sjå også [barnemodusplanen](BARNEMODUS_PLAN.md).

Dei nynorske meldingane i `values-b+nn/strings.xml` er:

- `launcher_not_signed_in`: «Denne kontoen er ikkje logga inn i Spole.»
- `launcher_kid_mode`: «Spole er i barnemodus. Gå ut av barnemodus i Spole først.»

Ei ventande opning er knytt til økta som tok imot henne, blir konsumert høgst éin gong og blir validert på nytt før opning. Eit profilbyte frå ei slik opning krev ny kontroll etter bytet. Gamle førespurnader og seine detaljsvar skal ikkje opne innhald i ei ny økt.

## Detaljar og avspeling

`details` fører til Heim og nullstiller tidlegare detaljnavigasjon. Eit lokalt treff må samsvare med både kjelde og ekstern `remoteId`. Utan treff blir tittelen henta direkte frå tenesta, også før framsideradene er lasta. Detaljarket viser skeleton under lasting, deretter dei rike metadataa tenesta tilbyr; manglande felt fell bort. Ved feil blir den vanlege detaljfeilen vist utan oppdikta innhald.

Tilbake frå eksternt opna detaljar går først gjennom detaljhistorikken (til dømes episode → serie).
Når det yttarste detaljarket blir lukka, legg Main Spole-oppgåva i bakgrunnen, slik at launcheren
som opna tittelen kjem fram att. Returen blir konsumert éin gong og er bunden til den same økta;
profilbyte, eksplisitt Heim og vanleg fanenavigasjon fjernar han. Vanlege detaljar inne i Spole
har ingen slik retur. Sjå [prøva på Shield og Loftet](verification/2026-10-29-launcher-return.md).

I barnemodus blir detaljane viste i barnet sitt eige skal, med avspeling av eigen tittel eller episodeval og lukking. Vaksenverten for detaljar blir ikkje opna. Ei sesonglenkje brukar foreldreserien til episodeoversikta og vel den etterspurde sesongen.

`play&start=0` («Spel frå starten» i Stova) startar tittelen på null utan å spørje om å halde fram;
neste episode etterpå følgjer vanleg logikk igjen. `start` med andre verdiar, med `details` eller to
gonger blir avvist.

`play` går gjennom den eksisterande spelaren og den eksisterande logikken for å halde fram. Spelaren har standard aktivitetsmodus og blir opna med `CLEAR_TOP`, slik at ein eksisterande spelar blir oppretta på nytt for måltittelen utan å stable fleire spelarar. Faktisk vidareføring av posisjon, byte av tittel under avspeling og Android-stakken på TV er **ikkje prøvde på eining i denne oppgåva**.
