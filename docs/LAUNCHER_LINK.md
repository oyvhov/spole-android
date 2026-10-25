# Stova-lenkjer til Spole

Denne låste kontrakten lèt Stova eller ein annan Android-startar opne ein tittel i Spole med ei app-til-app-lenkje. Lenkja gir ingen nye tilgangsrettar. Sjå [verifisering og attståande prøver](VERIFICATION_LAUNCHER_LINK.md).

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

Begge ID-ane skal etter dekoding passe heile uttrykket `[A-Za-z0-9_-]{1,128}`. Alle fire parametrane er påkravde og skal finnast éin gong kvar. Rekkjefølgja er fri. Ukjende parametrar blir ignorerte, men må framleis ha gyldig URI-/prosentkoding. Namn og verdiar blir prosentdekoda éin gong som UTF-8; ugyldig prosentkoding eller UTF-8 blir avvist, også i ukjende felt. Dobbelkoda ID-ar gir ikkje ein ny dekodingsrunde.

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

I barnemodus blir detaljane viste i barnet sitt eige skal, med avspeling av eigen tittel eller episodeval og lukking. Vaksenverten for detaljar blir ikkje opna. Ei sesonglenkje brukar foreldreserien til episodeoversikta og vel den etterspurde sesongen.

`play` går gjennom den eksisterande spelaren og den eksisterande logikken for å halde fram. Spelaren har standard aktivitetsmodus og blir opna med `CLEAR_TOP`, slik at ein eksisterande spelar blir oppretta på nytt for måltittelen utan å stable fleire spelarar. Faktisk vidareføring av posisjon, byte av tittel under avspeling og Android-stakken på TV er **ikkje prøvde på eining i denne oppgåva**.
