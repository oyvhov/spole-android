# Kontoval, språk og trygg TV-flyt — etter alpha06

## Levert i kjelda

- 27 ressursnøklar på nynorsk og engelsk: tilkoplingssamandrag, utlogging, kontoval, avansert oppsett, API-hjelp, alternativ adresse og samtykke til valfri innlogging på to tenester.
- Felles fokusmarkering og minst 48 dp treffflate på sekundære kontohandlingar. Det innlogga samandraget har òg fokusramme, utan at kortet endrar storleik.
- Utloggingsdialogen vel Avbryt ved tastatur-/fjernkontrollopning. Berøringsopning flyttar ikkje fokus automatisk. Fokus åleine utfører aldri utlogging.
- Forklaringa kan rullast i eit vindaugstilpassa tekstområde. Tittel og handlingar skal ikkje pressast bort av stor skrift.
- Profil-ID-hjelpa lovar ikkje lenger ein profil med alle bibliotek: tenarrettane avgjer tilgangen.
- Kompakt Spelar no har eksplisitt lys transportikonfarge mot den mørke kortflata.

Ingen endringar i autentiseringsprotokoll, deling av passord, serverrettar eller kva konto ein førespurnad blir send som. Nettverksfeil og andre hardkoda modelltekstar står framleis att før engelsk er komplett.

## Verifisering

Google TV API 36 på isolert emulator-5564, med syntetiske data. Nye testar dekkjer engelsk konto/avansert oppsett ved 2× skrift, eksplisitt samtykke med namngjeven motpart og trygg fjernkontrollopning/avbryting av utlogging med fokus tilbake til opphavsknappen.

Første køyring fann manglande engelsk overskrift. Skjermbiletet avklarte årsaka: det nye dialogvindauget fall tilbake til eininga sitt nynorske språk i staden for det engelske skjemaet. Tekstane blir no henta i det valde språket før dialogopning, og skriftstorleiken blir ført vidare. Forklaringsområdet er òg avgrensa og rullbart; testen vart ikkje svekt. Full fysisk TV-godkjenning og ekte kontobyte er framleis ikkje gjennomført. Produksjonskontoane på 5560 er urørte.

Ingen ny APK-publisering; alpha06 er framleis den publiserte testutgåva.

Sluttkontroll: 283 JVM-testar, 0 lint-feil / 32 åtvaringar. Dei 13 eksisterande TV-/innloggingstestane bestod; dei tre nye kontotestane bestod etter retting av testens synkronisering. Den lange forklaringa kan rullast med opp/ned når ho har fokus; ved enden går tastane vidare til vanleg fokusnavigasjon.
