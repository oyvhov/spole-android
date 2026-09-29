# Visuelle innstillingar på TV og mobil

Innstillingskategoriane og vala under dei har eit felles visuelt språk. Dette byggjer vidare
på organiseringa i [Sider og meny](SETTINGS_PAGES_2026-09-29.md).

## Kategoriar

- Mobil: kvar kategori har eit større ikon i ei mjuk, tona flate, tittel, forklaring og pil.
- TV og nettbrett: same ikon og farge følgjer kategorien i sidelista og i toppen av innhaldspanelet.
- Kategorifargane kjem frå dei eksisterande palettane. Dei er dempa; tekst bruker framleis
  temafargane for lesbarheit. Ikona og tekstane gjer at ein ikkje er avhengig av farge.
- Innhaldet ligg i éin rullbar kolonne. Høgda på kvar rad veks med teksten.

## Rader og valdialogar

`settingsSurface` er den felles flata for handlingar, brytarar, val og kategorikort.
Ho har 16 dp hjørne, litt lysare flate ved fokus/val og eksisterande fokusramme.
Trykk gir ei kort skalering til 98,8 prosent. Fokus endrar ikkje storleiken på raden.
«Rolege overgangar», lett TV-modus og systemvalet for animasjonar slår av rørsla.

`SettingsOptionPreview` viser det valet faktisk endrar:

| Val | Førehandsvising |
| --- | --- |
| Bakgrunn | Ei lita skjermflate med paletten sine eigne bakgrunns- og flatefargar |
| Aksentfarge | Handlingsfargen og dei tilhøyrande tonane |
| Biletstorleik | Tre omslag med storleiken til det aktuelle valet |
| Hjørne | Omslag med den valde hjørnerundinga |
| Fokus | Normal, aksentfarga eller tjukk fokusramme |
| Sesong | Sesongfargen med ei diskret markering |

Førehandsvisingane står både ved gjeldande verdi og ved alternativa i dialogen. Dei er
ikkje eigne fokusmål. Skjermlesaren les etiketten og valtilstanden på raden.
Radiovalssemantikk, brytarsemantikk, fokus ved dialogopning og eksisterande lagring er bevarte.
Tilpass framsida, Tilpass biblioteksida og Appnamn har eigne, meiningsfulle ikon.

## Kontroll

`SettingsVisualsTest` kontrollerer kategoriopning og tilbakeveg, samt val og lukking av
dialogar med visuelle døme, ved skriftstorleik 2,0. `SettingsPagesTest` dekkjer den eksisterande
organiseringa og redigeringa av sidene.

- Einingskontroll: 15 av 15 bestått (`SettingsVisualsTest`: 2, `SettingsPagesTest`: 13).
- `git diff --check`: ingen whitespace-feil.
- Produksjonsbygg og `lintVitalRelease`: bestått. Pakke `app.reelstack`, versjon 129 / 0.18.0-beta9.
- Signatur stadfesta mot den faste produksjonsnøkkelen. APK SHA-256:
  `30195a901e7bf80d8e9bb0a4323b764424ae2e0beee14feed11af43d68a22764`.
- Same APK installert med `install -r` på 5564 og 5560. Ingen appdata vart sletta.
- TV: ekte innhald synleg etter oppdatering. Kategorioversikt, Utsjånad, fokusflytting med
  fjernkontroll, bakgrunnsvaldialog og tilbakeveg kontrollerte visuelt.
- Mobil: kategorioversikt, Utsjånad og bakgrunnsvaldialog kontrollerte visuelt. Den eksisterande
  profilen viste engelske tekstar og dømeinnhald; dette verifiserer menyane, ikkje ekte tenestekoplingar.
- Android System UI i mobilemulatoren krasja før gjennomgangen med manglande
  `BLUETOOTH_CONNECT` og `READ_CONTACTS`. Desse vart gjevne tilbake til `com.android.systemui`;
  etter omstart av Spole kom skjermvisinga tilbake. Ein Bluetooth-krasjdialog vart lukka.
  Ingen instrumentering vart køyrd på emulatorane med lagra brukardata.
- Lokale skjermbilete ligg under den Git-ignorerte `app/build/`-mappa:
  `settings-tv-after.png`, `settings-tv-choice.png`, `settings-phone-after.png`,
  `settings-phone-appearance.png` og `settings-phone-choice.png`.
