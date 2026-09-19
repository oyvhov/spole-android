# Barnemodus og tenestene dine · 19. september 2026

## Mål

Ei filmatisk, personleg oppleving for barn på 5–10 år, med tydeleg fjernkontrollfokus,
store treffflater og direkte veg frå tittel til avspeling. Innhald frå barnet sin konto
skal visast påliteleg. Foreldreval og barnet sine kosmetiske val har kvar sin inngang.

## Referansar

- [Netflix: barneprofilar](https://help.netflix.com/en/node/114275): eigne profilar og
  aldersinnstillingar. Spole nyttar eigen tenarkonto som innhaldsgrense.
- [Disney+: foreldrekontroll](https://help.disneyplus.com/en-GB/article/disneyplus-en-lc-parental-controls):
  Junior Mode og verna profilbyte. Spole bevarer PIN-utgangen og ein enklare barneflate.
- [YouTube: val for familiar](https://support.google.com/youtubekids/answer/10315420?hl=en-au):
  mellom anna eiga gruppe for 9–12 år. Spole sitt design må òg passe større barn;
  Kino og Nordlys er rolegare alternativ til dei meir eventyrprega landskapa.

Dette er inspirasjon til oppdeling og sjølvstende, ikkje kopiering av grensesnitt eller
påstand om identiske kontrollar. Aldersgrenser og bibliotek blir administrerte på tenaren.

## Endringar

- Originale, skalerbare landskap i seks tema; kvar barneprofil hugsar eigne val.
- Stor startflate med eit faktisk tilgjengeleg innhaldselement og direkte handling.
- «Alt» er standard bibliotekval. Bibliotek-ID blir sett frå den faktiske førespurnaden,
  også når tenaren ikkje inkluderer denne i kvart medieobjekt.
- Kontobyte kansellerer henting og fjernar gamle barnebibliotek og episodeval.
- Nettverksfeil blir ikkje lenger omgjorde til eit tilsynelatande tomt bibliotek.
- Foreldre kan velje utsjånad, personleg tilgang til «Mi verd», redusert rørsle,
  undertekstar og autospel med pause etter 1–3 episodar.
- Desse avspelingsvala blir brukte av den ekte spelaren utan å endre vaksenvala.
- Tenestene er samla etter føremål; kvar barnerad opnar foreldrevala direkte.
- Profilmenyen sin kontosnarveg opnar tenestekategorien både på TV og mobil.

## Verifikasjon

Gjennomført 19. september 2026:

- `testDebugUnitTest`: 665 testar, ingen feil. Ti nye testar dekkjer barneval,
  profilskilje, bibliotekfeil og bibliotek-ID, foreldreval og kontosnarveg på TV/mobil.
- `assembleRelease` og `lintDebug`: fullførte. Lint har ingen feil, 107 åtvaringar
  og eitt hint; dette er ikkje ein påstand om at alle åtvaringar er rydda.
- Signert APK installert med `install -r` på dei eksisterande TV- og mobilemulatorane.
  Ingen brukardata sletta, ingen nye kontoar eller test-AVD-ar oppretta.
- Ekte TV-konto: bibliotekfilter, fjernkontrollfokus, temabyte med lagring,
  filmavspeling, episodetittel og episodebilete kontrollerte. Skriftstorleik 2,0
  kontrollert for heimeskjerm og temaveljar, deretter sett tilbake til 1,0.
- Foreldreinnstillingar og direkte kontonavigasjon kontrollerte i tre Compose-testar,
  med bilete frå native rendering. Mobil kontoskjerm òg kontrollert ved skriftstorleik 2,0.

Avgrensingar: Den eksisterande mobilemulatoren står på velkomstskjermen utan innlogga
konto; ekte mobilavspeling er derfor ikkje testa. Foreldre-PIN vart ikkje omgått;
passordgjenoppretting er ikkje prøvd med ekte vaksenpassord. Pause etter episodar er
ikkje ein dagleg skjermtidsgrense. Alders- og bibliotektilgang blir framleis styrt på
medietenaren. Denne lokale endringa er ikkje publisert som ny utgåve.

APK SHA-256: `28590FA6654C04CCA30792581061D66B1C719D4BA964BF3F3AE2B103175857A3`.

Skjermbilete med ekte innhald/kontonamn blir haldne lokalt i den ignorerte byggmappa.
