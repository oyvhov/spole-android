# Detaljar og episodeval – utkast til visuell godkjenning

Brukaren godkjende dei varsame endringane og bad om å halde fram etter å ha fått
presisert kva som faktisk var endra på TV.

Første runde etter dei opnare innstillingsmenyane. Hovudsidebaren er uendra.

- Fakta, aldersgrense og kjelde står ope saman og kan bryte ved stor skrift.
- Vurderingane kan flyte over fleire linjer i staden for å krevje sidelengs rulling.
- Mobil viser metadata før handlingar og episodar; dobbelt sett med fakta i
  plakatoverskrifta er fjerna.
- Sesongval er tekstfaner med ein kort strek under det valde alternativet.
- TV har breiare episodebilete. Mobil har breie bilete med tittel og omtale under.
- Framdrift er ei smal linje under episodebiletet. Tilgjenge, sett-status,
  avspelingshandlingar og kontorettar brukar same data og reglar som før.

Endringane er eit lokalt utkast. Skjermbilete frå emulatorane skal visast til
brukaren for godkjenning før eventuell publisering. Ingen release er publisert.

## Lokal gjennomgang

`assembleRelease` fullførte på 5 minutt og 40 sekund. APK installert med `-r` på
TV 5564 og mobil 5560. Den eksisterande signaturen vart stadfesta.
APK SHA-256: `4a986c093774346078be87b9a0ca3b4c58ecb1bf149493a1fd80ecce6737f29d`.

Skjermbilete ligg lokalt under den ignorerte `app/build`-mappa:

- `approval-movie-tv.png`: Toy Story 5 frå det ekte TV-biblioteket.
- `approval-episodes-tv.png`: Kongen befaler, sesongfaner og episodeval.
- `approval-movie-phone.png`: filmvising frå den eksisterande demoprofilen.
- `approval-episode-phone.png`: episodedetaljar frå den eksisterande demoprofilen.

Mobilprofilen har ikkje ekte episodeval til denne gjennomgangen. Bileta stadfestar
derfor mobilens detaljutforming, ikkje den nye episodelista med reelle data.
Android sine Bluetooth-/System UI-dialogar måtte lukkast før skjermbileta.
Ingen nye automatiske testar eller full gjennomgang med stor skrift er køyrde.
Søk, generelle filter, avspelarmenyar og tomtilstandar ventar til etter denne
første godkjenninga.
