# Spole 0.16.0-alpha09 — TV, bibliotek og personleg tilpassing

Arbeidet tek utgangspunkt i dei fire bileta og dei 13 punkta frå 10. september 2026. APK-en er ei lokal, signert oppdatering av `app.reelstack`, bygg 52. Eksisterande kontoar og bibliotekval skal følgje med ved oppdatering.

## Kva som er endra

| Punkt | Resultat |
| --- | --- |
| 1. Bibliotekval i innstillingar | «Vel bibliotek» ligg i Innstillingar, under Heim på breie skjermar. Biblioteksida er ei rein innhaldsside. |
| 2. Bibliotek | Eige galleri med bibliotekbilete, lyse titlar, posterar, episodebilete og framdrift. Film- og seriebibliotek hentar faktiske Movie-/Series-element rekursivt innanfor det valde biblioteket, slik at filmapper med filnamn ikkje blir hovudvisinga. Sesongar, samlingar og andre mapper kan framleis opnast. Paginering og tilbakeveg er bevarte. |
| 3. Ukjende statusar | Importen bruker statusen frå Seerr-lista med ein gong. Mislykka metadataoppslag bevarer sist stadfesta framdrift. Førespurnader utan ekte tittel blir samla i ei utvidbar liste. Følgjar og varselval blir ikkje sletta. Ein samla tekst opplyser om mislykka oppdatering. |
| 4. TV-detaljar | Den overflødige Tilbake-/Detaljar-linja er fjerna. Play/Hald fram får fokus. Framdrift, tid att, stjernevurdering og tilgjengeleg video-/lydkvalitet står saman med omtalen. Tomme sjanger-/slagordfelt tek ikkje opp store mellomrom på TV. |
| 5. Tilbake under avspeling | TV bruker den fysiske knappen. Telefon og nettbrett viser skjermknappen saman med avspelingskontrollane; han forsvinn når kontrollane blir skjulte. |
| 6. Andre ulogiske overgangar | Heim og opne Jellyfin-detaljar blir oppdaterte etter retur frå spelaren. Null prosent blir ikkje presentert som ein gjenopptakingsstatus på ei usett neste episode. Menyfokus som kjem automatisk ved innlasting skal ikkje opne TV-menyen. |
| 7. TV-meny | Kollapsa ved oppstart og etter val av side. Venstretrykk inn i menyen opnar etikettane. Breidde/falming blir animert over innhaldet, utan at posterane blir ommålte gjennom animasjonen. |
| 8. Tilpassing | Rekkefølgje på hovudmenyen; skjul Bibliotek, Oppdag eller Aktivitet; legg enkeltbibliotek i sidemenyen; vis/skjul Neste episode; kombiner sjårader; vis/skjul stort tittelbilete, vurderingar og kvalitet; vel roleg/rask oppstart. Heim og Innstillingar er alltid tilgjengelege. Tidlegare farge-, biletstorleik-, heimrad- og avspelingsval er bevarte. |
| 9. Neste episode | Jellyfin si personlege Next Up-liste, avgrensa til valde seriebibliotek før innlasting. Eiga rad eller samla med Hald fram å sjå, utan dupliserte element i den samla rada. Mellomlageret er knytt til konto og bibliotekval. |
| 10. Oppdag og Aktivitet | Sidene er haldne separate: Oppdag hjelper med å finne innhald, Aktivitet med å følgje førespurnader. Menyvalet gjer det mogleg å flytte eller skjule dei etter behov. |
| 11. Fleire plattformer | Fjernkontrollfokus og fysisk Tilbake på TV; trykk, skjermknappar og botnmeny på telefon; sidemeny og breie panel på nettbrett. |
| 12. Oppstart | Logoen formar seg rolegare, medan nettverksarbeidet går i bakgrunnen. Ventinga på data er avgrensa: sein tenar kan ikkje halde appen fast i oppstartsskjermen. Raskare animasjon kan veljast. |
| 13. TV-innlogging | Den viktigaste innloggingsknappen er festa nedst på TV. «Start Quick Connect» er synleg sjølv om hjelp, innloggingsfelt eller stor skrift krev rulling. |

## Kvifor dei tomme aktivitetskorta oppstod

Tidlegare vart inntil 100 eksisterande personlege førespurnader importerte med generisk «Film»/«Serie» og `UNKNOWN`, medan berre 20 metadataoppslag vart utførte per oppdatering. Resten gav store, tomme kort. Ein feil i eit metadataoppslag sette dessutan ein tidlegare kjend status tilbake til ukjend.

Den avgrensa, roterande innlastinga er bevara for å unngå mange hundre samtidige kall. Listestatus blir brukt før detaljane kjem. Ved feil står kjend status med ein oppdateringsmerknad; ufullstendige titlar kan opnast frå den samla lista. Dette gøymer ikkje eller slettar nokon førespurnad på tenaren.

## Data og avgrensingar

- Bibliotekvalet gjeld nye filmar, episodar, Hald fram å sjå, Neste episode, bibliotekssøk og dei relevante utgjevingsradene. Eit uttrykkeleg tomt val lastar ikkje bibliotekinnhaldet.
- Snarvegar til bibliotek er lagra per tenar og profil. Ein snarveg til eit bortvalt bibliotek blir ikkje vist.
- Vurdering, omtale og kvalitet kjem frå tenaren. Manglande informasjon blir ikkje dikta opp. Next Up krev Jellyfin; eksisterande Emby-funksjonar er bevarte.
- Dette gjer ikkje Spole til ein full musikk-, bok- eller fotoklient. Biblioteknettlesaren kan opne mapper for desse typane; den integrerte spelaren er laga for video.
- Fysisk TV, nettbrett og langvarig avspeling må framleis prøvast på dei aktuelle einingane. Emulatortestane er ikkje ei godkjenning av heile TV-milepålen.

## Kontroll

**312 einingstestar**, **221 ulike utførte Android-testar på telefon** og **15 målretta TV-testar** bestod, inkludert dokumenterte atterkøyringar etter rettingar i teststyringa. To TV-dialogtestar bestod òg med 200 % systemskrift. Lint har 38 åtvaringar og ingen feil. [Verifikasjonsrapporten](VERIFICATION_v0.16.0-alpha09.md) viser bygg, avvik, loggar, APK-signatur og kontrollane med ekte data.

Dei to private emulatorane er oppdaterte med den signerte produksjons-APK-en og har behalde ekte kontoar. Instrumentering brukte separate testemulatorar og syntetiske kontoar.

Next Up-kallet følgjer Jellyfin sine eigne parameter for profil, bibliotek og gjenopptakbare episodar: [Jellyfin ShowApiGetNextUpRequest](https://typescript-sdk.jellyfin.org/interfaces/generated-client.ShowApiGetNextUpRequest.html).
