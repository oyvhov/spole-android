# Visuelt menysystem på TV og mobil

Vidareføring av [dei visuelle innstillingane](SETTINGS_VISUALS_2026-09-29.md).
Målet er at vegen inn i ei innstilling og dei neste vala kjennest som same app.
Sidebaren og hovudnavigasjonen på mobil er haldne uendra etter brukaren si avgrensing.

## Felles mønster

| Flate | Utforming |
| --- | --- |
| Valdialogar | 24 dp hjørne, tydeleg tittel med kategorisymbol, fast lukkeknapp og rullbare val. |
| Filter- og handlingsmenyar | Opne rader utan bakgrunnsboksar, 56 dp minimumshøgd, 6 dp mellomrom og frittståande ikon. Val og fokus får ein svak bakgrunnstone. |
| Profil- og tenestemenyar | Same fokus- og trykkrespons som dei andre menyane. |
| Redigering av sider | Opne grupper utan ytre kortbakgrunn og synleg fokus på dei indre vala. |

`SpoleDropdownMenu` eig utsjånaden til dei forankra menyane. Material eig framleis
plassering, rulling og lukking ved Tilbake eller trykk utanfor. `SpoleDropdownMenuItem`
bevarer heile rada som eitt fokusmål; valfrie `selected` gir radioval og vald flate.
Deaktiverte handlingar får dempa tekst og kan ikkje aktiverast.

Etter tilbakemeldinga om for boksete menyar er kvilebakgrunnen på innstillingsrader,
kategoriar og menyval fjerna. Symbola står fritt utan eigne ikonboksar. Redigering av
sider og menyrekkjefølgje har heller ikkje ytre kortbakgrunn. Førehandsvisinga av
tema har fått fjerna ramma, medan dei små visuelle prøvene er bevarte. Dialogen er
framleis éi samlande flate; berre aktivt val, trykk og fokus tonar ei enkelt rad.
Hovudsidebaren er uendra.

`SpoleChoiceDialog` og `SpoleChoiceRow` blir brukte til språk, bibliotekikon, sjanger,
år, alfabet, undertekstutsjånad, spor og andre val. Radioindikatoren er dekorativ;
semantikken og handlinga ligg på rada. Titlar kan vekse med skriftstorleiken.

## Dekning

- Bibliotek: sortering, sett-status, oppløysing, filter, vising, tenesteval,
  sjanger, år, alfabet og bibliotekikon.
- Heim: handlingar ved langt trykk på eit omslag, med eigne ikon for spel,
  detaljar, favoritt, sett-status og fjerning frå hald fram.
- Oppdag og Aktivitet: status-/kjeldemenyar og handlingar på førespurnader.
- Innstillingar: språk, undertekststil, profil-/tenesterader, oppdateringsrader
  og redigering av heim- og biblioteksider.
- Spelar og titteldetaljar: dei delte menyane for lyd, tekst, kvalitet og kapittel.

Bibliotekformat og rutenett/liste har små teikna førehandsvisingar. Desse er ikkje
eigne fokusmål. Dialogar for stadfesting og tekstinntasting beheld dei eksisterande
handlingane og temaflatene. Kontorettar, lagring, PIN og mediehandlingar er uendra.

## Kontroll

Før korrigeringa av sidebaren vart tenestevalet i biblioteket vist på TV med ekte
bibliotekinnhald. Dei to tenestene hadde eigne symbol, og den valde tenesta var
markert. Utsjånadssida vart òg vist på mobil med skriftstorleik 2.0. Mobilprofilen
stod frå før på engelsk og demodata. Android sin tilbakevendande Bluetooth-dialog
avbraut gjennomgangen av valdialogen; full gjennomgang av alle undermenyane er
ikkje utført. Skriftstorleiken vart sett tilbake til den opphavlege verdien 1.0.

Sidebaren og hovudnavigasjonen på mobil vart deretter tilbakeførte. Ingen nye
automatiske testar er lagde til eller køyrde for denne utvidinga. Dei 15 testane
i førre rapport gjeld den førre innstillingsendringa.

Etter tilbakeføringa: `assembleRelease` fullførte på 5 minutt og 22 sekund.
Signert APK vart installert med `-r` på både `emulator-5564` og `emulator-5560`.
Sidebarkomponentane har ingen kodeendringar att i denne oppgåva.
Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
APK SHA-256: `4ec712b4cdf74aafc32b1bb919a28e40836852860f9c34666794a8130fa284ac`.

### Oppfølging: opnare menyar

Bygget med opne rader fullførte `assembleRelease` på 2 minutt og 26 sekund.
Signaturen er den same som over. APK SHA-256:
`5fb1bf00f5564ffca1e246bd1d70d37d2fa73f217bc93b7e78a2fe1d3484ff77`.
Ingen nye automatiske testar vart køyrde for denne utsjånadsjusteringa.
Installert med `-r` på TV (5564) og mobil (5560), og opna på begge. Eit lokalt
skjermbilete av utsjånadssida på TV viser opne rader og frittståande kategorisymbol.
