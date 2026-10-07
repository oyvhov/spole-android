# Spole 1.0.0-beta5

Denne betaen rettar minnebruk, konto-/profilgrenser og nokre mobil-/TV-feil før stabil 1.0.
Same pakkenamn og signatur som tidlegare; produksjonskode 142.

- Kalender og filmutgjevingar brukar fire arbeidarar, avgrensa kø og mindre metadata. Kontoavgrensa mellomlager har minne-/diskbudsjett og seks timars levetid.
- Emby-bilete og Jellyfin-socket sender tilgangsteikn i autentiseringsheader. Biletlastaren følgjer ikkje omdirigeringar, og oppgraderinga tømmer eldre dashboard-cache med token-URL-ar.
- Profil- og kontobyte tilbakekallar gamle jobbar og spelarar. Sein innlogging og kalenderlagring kan ikkje skrive til den neste profilen.
- PIN kan deaktiverast med rett kode eller stadfesta vaksenpassord. Barnet sin eigen Jellyfin-/Emby-konto eig framleis innhaldsrettane.
- Personlege rader ventar på avklart Seerr-identitet, medan verifisert mediekonto kan vise ordinære bibliotekrader.
- Offline-valet kontrollerer faktisk netttransport. Nye nedlastingar har lagringsbudsjett og bevarer ledig plass.
- Mindre tom bibliotekhero, jamn lukkeknapp i ark og omsetjing av barnemeny, foreldreval og demo-omtalar.
- TV viser innloggingsknappen når ein endrar ei eksisterande tilkopling.
- Nøktern CI: bygg, lint og einingstestar; korte mobilprøver på PR og utvalde mobil-/TV-regresjonstestar før release. Jev er berre valfri rådgjeving ved manuell aktivering.

Dette er ei testutgåve. Fysisk mobil-/TV-prøve og lengre prøving av nettbrot og offlinebruk står att.
Sjå [verifiseringa](https://github.com/oyvhov/spole-android/blob/main/docs/VERIFICATION_v1.0.0-beta5.md) og [status mot stabil 1.0](https://github.com/oyvhov/spole-android/blob/main/docs/STABLE_1_0_PROGRESS.md).
