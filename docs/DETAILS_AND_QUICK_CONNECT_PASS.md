# Detaljar og felles innlogging – 14. september 2026

Arbeidsbygg: 0.16.0-beta10 (77). Ikkje publisert på GitHub i denne gjennomgangen.

## Endringar

- Quick Connect held på same kode ved kortvarige transportfeil under polling. Lesekall får opptil tre forsøk; utgått kode, avvising og kansellering blir ikkje prøvde om att. Seerr får lengre tid til å stadfeste godkjenninga. Kontrollen av at begge tenestene brukar same Jellyfin-tenar er bevart.
- Det enkle fellesoppsettet brukar det same kodepanelet som ordinær Quick Connect. Mobil og nettbrett har kopieringsknapp og stadfesting. Mobilen blir ikkje automatisk rulla ned til avbryt-knappen når koden kjem.
- Jellyfin/Emby-namnet er fjerna frå toppen av bibliotekdetaljane.
- TV har mindre episodebilete, handlingane ved tittelen og ei vassrett sesongrad før omtale og lydval. Episodane held nummerrekkjefølgja uavhengig av sett-status. Spesialepisodar frå andre sesongar blir filtrerte bort.
- Detaljoppslaget gjenopprettar serie- og sesonginformasjon når eit lagra heimekort manglar henne. Episodedetaljar og sesongkort brukar eige episodebilete når serveren tilbyr det.
- Bakgrunnskunst frå filmen/serien kan visast dempa bak detaljane. Valet ligg under utsjånad. Høg kontrast og lett TV-modus utelèt bakgrunnen.
- Medverkande med person-ID frå Jellyfin/Emby kan opnast. Resultata viser filmar og seriar i biblioteket til den innlogga brukaren. Seriar brukar breie kort og filmar plakatformat. Oppslaget skjer først ved trykk.
- Oppstartsskriptet opprettar no `/run/user/0` dersom mappa manglar etter WSL-omstart. Utan henne krasja emulatoren si nettverksteneste før Android kom opp. Dette endrar ikkje AVD- eller brukardata.

## Avgrensingar

Den rapporterte innloggingsfeilen frå tidlegare på dagen er ikkje reprodusert med ei ny verkeleg innlogging. Dei eksisterande kontoane på review-emulatorane er bevarte. Rettinga dekkjer ein konkret transportfeil i kodeflyten, men er ikkje dokumentasjon på kva som skjedde i den økta.

Medverkande-oppslag krev person-ID frå medietenaren. Funksjonen er ikkje ei ekstern, komplett filmografi. Bakgrunnar krev at serveren har slik kunst. Ingen nye ytelsesmålingar av kald oppstart er gjorde i denne gjennomgangen.

## Verifisering

- 485/485 einingstestar bestått. Nye testar dekkjer nettverksbrot/kansellering, sesongrekkjefølgje, spesialepisodar, kunstformat og manglande kontekst i lagra heimekort.
- Lint: 0 feil, 31 åtvaringar.
- Signert release-APK bygd: `app.reelstack`, versjon 0.16.0-beta10, kode 77, 11 650 930 byte.
- APK SHA-256: `aa78598066b3c249a3501d35006135abb63bcc39a5883272d288e83843d62303`.
- Signeringssertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Manuell review med lagra, ekte kontoar: Taskmaster og Alone Australia, film 300, nummerert sesongliste, medverkandefokus på TV og oppslag av Alex Horne sine seriar. Mobil episodeark er også kontrollert. Skjermbileta ligg berre lokalt i ignorert byggemappe.
- Android-testen for kopiering ved skriftstorleik 2.0 er oppdatert og test-APK-en er bygd. Testkøyringa er ikkje fullført: etter minnepress og omstart feilar Android sjølv med EIO ved lesing av `/data/misc/apexdata/com.android.permission/access.abx` i den isolerte testprofilen. Testprofilen er stoppa. Ingen instrumentering er køyrd på kontoemulatorane.
- WSL fekk mellombels 8 GB til sluttkontrollen. Den lagra `.wslconfig` er sett tilbake til opphavlege 4 GB; den mellombelse grensa gjeld berre fram til neste WSL-omstart.
- Endeleg lint vart køyrd separat etter at ein samla byggprosess stoppa uventa. APK og test-APK vart bygde før dette; signaturen er kontrollert etterpå.
- Endeleg signert APK er installert med `-r` på TV, og versjon 77 er stadfesta. TV-emulatoren er opna att. Etter omstart viser tenestekontrollen avvik på Jellyfin/Emby og tilkoplingsfeil på Seerr; ny endeleg visuell kontroll av episodekunsten vart derfor ikkje fullført. Tidlegare review med ekte innhald i same gjennomgang er dokumentert over. Mobilen fekk eit tidlegare arbeidsbygg, men installasjonen av det endelege bygget vart ikkje stadfesta.

## Vidare produktidear – ikkje implementerte

1. **Kva rekk eg?** Vel kor mykje tid du har, og få filmar eller neste episodar som passar. Bruk attståande speletid på påbegynt innhald.
2. **Tilbake i serien.** Ei kompakt vising av sist sette episode, dato og neste steg når ein kjem tilbake etter lang tid, utan å røpe komande handling.
3. **Filmkveld.** Ei lita kortliste som fleire kan stemme på, med tydeleg skilje mellom klart til avspeling og innhald som må førespørjast.
4. **Utforsk samanhengar.** Gå frå ein skodespelar til eigne filmar og vidare til sjangrar og samlingar, med få og gode val på TV.
