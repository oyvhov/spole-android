# Spole alpha12 — oppdateringar og bibliotek på TV

Versjon 0.16.0-alpha12, bygg 55. GitHub-repoet vart gjort offentleg 10. september 2026 etter uttrykkeleg instruks frå eigaren. Denne endringa krev inga GitHub-innlogging i appen.

## Appoppdateringar

Innstillingar → Oppdateringar har automatisk sjekk, val av stabile/testutgåver og manuell sjekk. Førehandsutgåver er standard for eksisterande alpha-/beta-installasjonar; stabile installasjonar vel stabile utgåver. Automatisk sjekk skjer ved opning/retur og er avgrensa til kvar tolvte time, også etter feil. Nettverksarbeidet blokkerer ikkje oppstart.

Ei ny utgåve gir eit diskret panel på Heim. Brukaren opnar notata, startar nedlasting og ser framdrift. Seinare skjuler varselet for denne utgåva; neste utgåve kan varsle igjen. Nedlasting kan avbrytast og startast på nytt. Ferdig APK blir kontrollert på nytt etter prosessomstart og før installasjon. Ei avbroten, ufullstendig nedlasting blir ikkje installert; dette er ikkje ein bakgrunnsteneste med bytevis gjenopptaking.

GitHub-klienten har ingen medietenesteteikn eller cookies. Berre HTTPS til den faste offisielle repo-API-en og kjende GitHub asset-vertar er tillate. Redirects blir kontrollerte. Nedlastinga har storleiksgrense og blir ikkje eksponert før SHA-256, pakkenamn, stigande Android-versjonskode, tag/versjon, minimum Android-versjon og identisk signeringssertifikat er stadfesta. Debug-APK, kladdar og tvitydige fleir-APK-utgåver blir avviste. Utgåva må ha éin signert universal-APK med GitHub sin SHA-256-digest. Appen verken nedgraderer eller installerer stille.

Android eig installasjonsgodkjenninga. Dersom «installer ukjende appar» manglar, opnar Spole innstillinga og forklarer at brukaren må returnere og velje Installer. Ingen nye nøklar blir oppretta; noverande Spole-signatur må bevarast.

## Bibliotek og fokus

Bibliotekvalet har ei eiga, avgrensa flate med tydeleg inkludering, menyval og ein fast Lagre-knapp. TV brukar to kolonnar; telefon og stor skrift får éin. Det finst ti standardikon for snarvegar. Val er lokale og avgrensa til tenar/konto, og blir ikkje lagra før Lagre. Inkludering styrer framleis både bibliotek, søk og heimrader.

Biblioteket har søk, seks sorteringar og retning, sett/ikkje sett/påbyrja, favorittar, videooppløysing, sjanger og år. Jellyfin utfører filter og paginering innanfor det opna biblioteket; Spole filtrerer ikkje berre dei fyrste 60 resultata. Sjanger- og årsval kjem frå Jellyfin sitt filterendepunkt. Ved manglande filtermetadata kan dei skrivast inn. Nullstill fjernar alle filter. Mapper og sesongar bevarer den eksisterande hierarkiske navigeringa.

Mediekort i Bibliotek, Aktivitet, førespurnader og Oppdag har fokusramme rundt kunstflata. Tittel og metadata under biletet ligg utanfor. Avrundinga kjem frå same form som klipper biletet; standard, markert og aksentfokus er bevarte. Vanlege knappar og innstillingsrader bevarer si fulle fokusflate.

## TV-detaljar og oppstart

Førespurnads-/sesongknappen ligg rett under tittelen på TV og får første fokus når metadata er klare. Lang omtale og medverkande ligg etter handlinga. Den endelege send-knappen er framleis ei separat stadfesting, fast nedst i førespurnadsskjemaet. Ingen førespurnad blir send ved opning eller fokus.

Oppstarten brukar den originale Spole-logoen med filmstriper og ein projektorboge som samlar seg rundt merket. Normal forming tek 1,65 sekund (kort val: 0,8 sekund), med avgrensa venting på data og vanleg overgang til lastetilstand om tenaren er treg. Framsida blir komponert straks slik at også kunstlastinga kan gå under animasjonen. Skjulte kontrollar er sperra for input og skjermlesar. Android sitt animasjonsval blir respektert.

## Kjeldegrunnlag

- [GitHub release-API](https://docs.github.com/en/rest/releases/releases).
- [GitHub asset-digest og nedlasting](https://docs.github.com/en/rest/releases/assets).
- [Android PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager).
- [Jellyfin filterendepunkt](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/FilterController.cs).

Sjå [verifikasjonsrapporten](VERIFICATION_v0.16.0-alpha12.md) for bestått produksjonsbygg, 340 JVM-testar, TV-/telefontestar, kontroll med ekte data og APK-signatur. APK-en er levert lokalt. Det er ikkje publisert ein alpha12 GitHub Release; full installasjon av ei framtidig nyare GitHub-utgåve er derfor ikkje prøvd ende til ende.
