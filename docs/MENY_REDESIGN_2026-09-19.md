# Trygg menytilpassing på mobil

Skjul-handlinga i den delte menyredigeraren sende den eksisterande synlegverdien
til lagringa. Resultatet var at eit trykk ikkje endra noko. Ho er erstatta av ein
brytar som sender den nye verdien, med tydeleg synleg/skjult-status.

Heim og Innstillingar er obligatoriske. Bibliotek er obligatorisk når brukaren har
valt det som startside. Same regel blir brukt ved rendering, endring og lagring;
gamle eller ugyldige lagra menyval kan ikkje skjule desse inngangane.

Telefon i liggjande vising får ei kompakt, 80 dp brei sidemeny. Ho tek ikkje med
den utvida TV-/nettbrettmenyen eller biblioteksnarvegane. Innstillingar ligg fast
nedst, medan dei andre vala kan rullast uavhengig i korte vindauge. Ståande telefon
held på botnmenyen. TV held på fjernkontrollnavigasjonen.

## Kontroll

- 669 JVM-/Robolectric-testar bestått, ingen feil.
- Nye grensesnitttestar trykkjer på den faktiske brytaren, kontrollerer at valet
  forsvinn/kjem tilbake i sidemenyen og at lagringa blir oppdatert.
- Ståande botnmeny er testa med skjult Oppdag og fungerande Innstillingar.
- Liggjande mobilvising er testa ved skriftstorleik 2,0 med Innstillingar fast
  nedst. Native skjermbilete er visuelt kontrollerte, lagra under
  `app/build/reports/navigation-ui/`.
- Gamle val som skjuler alle sider er testa; obligatoriske sider blir synlege.
- Instrumenteringstestane er kompilerte. Dei er ikkje køyrde på eining i denne runden.

Bygg, lint, Android-testar og installasjon er dokumenterte i
[verifikasjonen for beta30](archive/releases/VERIFICATION_v0.17.0-beta30.md).
