# Spole 1.0.0-beta35 er ute: toppmenyen liten og berre øvst
2026-11-02, eigaren: «Lag ny versjon av Spole». Release `v1.0.0-beta35` (172, `4ad2b04`, prerelease): toppmenyen
frå `c95668f..e8925b9` (sjå under). Beta34 (171) var testbygget på Loftet, så denne heiter beta35.
Kontroll: 1 620 testar, lint 0 feil, signatur/hash/native OK, offentleg nedlasting lik bygget. Loftet oppdaterte
seg *gjennom appen* 171 → 172; første «Installer oppdatering» opna ingen dialog, det andre gjorde. Sjå
`VERIFICATION_v1.0.0-beta35.md`. Står att: Oppdag, Nedlastingar og Innstillingar har lina fast øvst, og
«Vis i sidemenyen»/«Legg i sidemenyen» seier framleis sidemeny om toppmenyen. Worktreen `C:/JellyBin/spole-beta34`
(grein `release-beta34`, lik `e8925b9`) kan ryddast.

# Toppmenyen på TV: liten, utan stripe, berre øvst (gitt ut som 1.0.0-beta35)
2026-11-02 kveld, eigaren på beta33: «Topp baren skulle berre ligg øverst!!!», «liten og nett slik som eksempelet
eg sendte deg i går» (Swiftly-videoen), «Den skal ikkje lage ein MØRK bar», «Test på Loftet ikkje emulator».
Beta32-arbeidet frå `C:/JellyBin/spole-sesong` (ucommitta der, urørt) er teke inn (`c95668f`), og oppå det
(`325680a`, `980646b`, `668117a`): små ord rett på biletet, profilsirkel til høgre, ingen kapsel/stripe/merke,
aksentstrek under sida du er på; lina rullar vekk med Heim, biblioteksida, rutenett i bibliotek og smarthyller
(`TopMenuLift`/`LendTopMenuLift`), og står heil når fjernkontrollen er på ho. Sjå `RORSLE_OG_TOPPMENY_2026-11-01.md` 4c.
Kontroll: 1 620 testar (1 hoppa over), lint 0 feil. Loftet: signert 1.0.0-beta34 (171) installert med `-r`
(mapping i `app/build/loftet-v1.0.0-beta34/`); Heim og Filmar prøvde: lina står øvst, er borte etter éin Ned og
kjem att med Opp. Står att: Oppdag, Nedlastingar og Innstillingar har framleis lina fast over sida. Ikkje pusha,
gitt ut som 1.0.0-beta35 (172) same kveld, sjå over.

# Spole 1.0.0-beta33: Oppdag for Stova
2026-11-02, eigaren: «commit og lag nye versjoner av begge» (Spole og Stova). `launcher-discover` er
spolt fram på `main` og gitt ut som 1.0.0-beta33 (170): stiane `discover…`, kalla
`discover_request`/`discover_withdraw` og `spole://discover`, som Oppdag i Stova 0.2.0 byggjer på.
Beta32 (169) var eit lokalt testbygg på Loftet; difor beta33. Sjå `VERIFICATION_v1.0.0-beta33.md`.
Android-testane på 5562/5566 er ikkje køyrde (eigaren: berre ekte einingar no).

# Beta32: toppmenyen bak biletet, fleksibel og med eigne bibliotek
2026-11-02: etter at eigaren såg beta31 på Loftet. Biletet går bak menylina (`topInset`), menyen følgjer «Sider og
meny» og viser biblioteka utan festing (`TopMenuEntries.kt`, `loadMenuLibraries`), fokus går attende til sida når ei
flis forsvinn, profilmenyen startar på profilen i bruk, «Neste» på biblioteksidene er Heim-korta, episodar i søket er
16:9. Sjå `RORSLE_OG_TOPPMENY_2026-11-01.md` (4b) og `VERIFICATION_v1.0.0-beta32.md`. Loftet står på Toppmeny.

# Beta31: rørslespråk, profilflukt, toppmeny og Swiftly-punkta 1–7
2026-11-02: Claude tok over att frå Codex (`1152599`), bygde profilflukta (`d96bc8a`) og fletta inn
fire parallelle agentgreiner: seriesida (`1d8f002`), spelaren (`932b18e`…`a81f2c6`), kosmetikken
(`175df9c`…`680256e`) og søk på TV (`4a15420`). Alt frå `RORSLE_OG_TOPPMENY_2026-11-01.md` og
punkt 1–7 med kosmetikk frå `SWIFTLY_LAN_2026-11-01.md` er med i beta31/168; sjå
`VERIFICATION_v1.0.0-beta31.md` for testbevis. Ingen installasjon på ekte kontoar.

## Tidlegare: vidareført etter beta30 av Codex
2026-11-02: Codex arbeider på `oppdag-samla` i `C:/JellyBin/spole-sesong`, vidare frå
Claude sin `fd03154`. Toppmeny med migrering og fjernkontrollnavigasjon, inngang til heimradene
og TV-hoppknappen er implementerte. Swiftly punkt 2 er delvis ferdig: søket held gamle treff
under lasting og viser eigne bibliotek før sein Seerr. Profilflukta og resten av Swiftly-planen står att.
Les [overleveringa til Claude](HANDOFF_CODEX_2026-11-02.md) før vidare arbeid.
Ingen ny release, push eller installasjon på ekte kontoar i denne runden.

# Spole 1.0.0-beta28: eigne profilar og Spole saman med Stova
2026-11-01, eigaren: «Lag en skikkelig release om dette fungerer, for Stova og Spole». Greinene `launcher-bridge` og `launcher-profiles` er flette med beta27 på `release-beta28` og gitt ut som 1.0.0-beta28 (165). Sjå `docs/VERIFICATION_v1.0.0-beta28.md`. Nytt sidan førre oppføring: `select_profile` på `LauncherProfilesProvider` (Spole følgjer personen som blir vald i Stova; aldri ut av ein barneprofil med PIN), fokushoppet og tilbodet i Profilar retta.

# Eigne profilar, `spole://profile` og profillista til Stova
2026-10-31, greina `launcher-profiles` (bygd på `launcher-bridge`; bestilling i Stova: `docs/superpowers/plans/2026-10-31-spole-profiles.md`). Ein konto i hovudprofilen (til dømes Emby-kontoen til eit barn ved sida av Jellyfin-kontoen til forelderen) kan få eigen, vaksen profil under Tenestene dine → Profilar: vanlege Spole med berre den kontoen, kopiert utan ny innlogging (`kid.<id>.adult`, `createProfileFrom`). Barnemodus er no «ein barneprofil er aktiv» (`ConnectionRepository.isKidProfile`, `SessionScope.kids`); frå barnemodus til ein eigen profil krev PIN-en. `spole://profile?kind=&user=` opnar Spole som profilen med kontoen. `LauncherProfilesProvider` (`<pakke>.launcher/profiles`) gir profilane til Stova og ingen andre, utan token. Ein konto med eigen profil namngir ikkje lenger hovudprofilen.
Kontroll: 1349 einingstestar (1 hoppa over), 0 feil; lint 0 feil; omsetjingar OK; signert assembleRelease (sertifikat 36fa94…). Loftet: release installert med -r; Stova-flis → `spole://profile` → `LauncherOpenActivity` → Heim. Funne der og retta: lenkjefilteret mangla verten `profile`.
Ikkje gjort: «Eigen profil for …» er ikkje prøvd på ei eining (Loftet har berre Jellyfin), Android-testpakken er ikkje køyrd, ingen push, tag eller release.

# Brua til Stova: godkjenn innlogging, framdrift og Tilbake frå bibliotek
2026-10-31, greina `launcher-bridge` (bestilling i Stova: `docs/superpowers/plans/2026-10-30-spole-bridge.md`). `LauncherApproveActivity` godkjenner Quick Connect-koden Stova viser, berre for `app.stova`/`app.stova.debug`, berre med Jellyfin-kontoar på same tenar (adresse eller `Id`), med kontoval og PIN når han er sett. Stova får sin eigen token. `LauncherProgress` sender stopp/bakgrunn til Stova. Siste Tilbake frå rota av eit bibliotek startaren opna, går til startaren. Kontrakten står i `docs/LAUNCHER_LINK.md`.
Kontroll: 1333 einingstestar (1 hoppa over), 0 feil; lint 0 feil; omsetjingar OK; signert assembleRelease (sertifikat 36fa94…, APK SHA-256 9c6b4338…). Android-testpakken på 5562 er ikkje køyrd.
Loftet (eigaren: «installere det du vil»): same beta26/163 med greina installert med -r. Prøvd på eininga: Stova godkjende med Spole og fekk eigen konto; bibliotekflis → Barnefilmar → eitt Tilbake → same Stova-flis. Framdrift prøvd med debug-bygget på emulator-5580 (Stova sin): ekte avspeling → Tilbake → tittelen fremst i «Hald fram» med ny framdrift.
Ikkje gjort: Shield, Emby (Quick Connect finst ikkje der), godkjenning med barnekonto og PIN. Ingen push, tag eller release.

# Retur til Stova etter eksterne detaljar
2026-10-29: Funn M1 implementert i `720979c`: siste Tilbake frå eksterne detaljar legg Spole i bakgrunnen; underdetaljar går eitt nivå først. Vanleg navigasjon og økt-/profilvern er bevarte.
73 målretta testar består (tre nye raude først); signert assembleRelease og lintRelease består, 0 lint-feil, 214 åtvaringar og 2 hint.
Same beta26/163 med lokal fiks er installert med -r på Shield og Loftet etter uttrykkeleg løyve. Faktisk Stova-kort → Spole-detaljar → BACK → same Stova-kort er stadfesta på begge; installert SHA er b2b29163…9cef39b, same eigarsignatur og bevarte første installasjonsdatoar.
Ingen avspeling, utlogging eller endring av kontoar/favorittar. Emby, barnemodus og underdetaljar er ikkje fysisk prøvde; sjå `docs/verification/2026-10-29-launcher-return.md`.
Stova-popupane blir ferdigstilte og prøvde vidare i C:/Launcher. Ingen push, tag eller release er gjort; ingen spørsmål står att i Spole.

# Lokal Spole beta22 – faktisk varm og kald Stova-kortopning bestått
2026-10-25: Faktisk Stova-korttrykk feila i beta21 sjølv om direkte varm/kald ADB-opning fungerte: rett URI vart send, men berre Heim kom fram.
Beta22/159 tek imot rå ACTION_VIEW i Main med same parser/økt-/konto-/barnevern. Mottakaren har tom affinity og handterer onNewIntent. Seks nye regresjonar feila først og er no grøne (18/18 i målretta klassar).
Full testDebugUnitTest, lintDebug og signert assembleRelease er grøne: 1293 bestått, 1 skip; lint 0 feil, 212 åtvaringar, 2 hint. APK 28 060 364 byte, SHA-256 f309682bc23f04535877fe0a54624950037d234893f1b77b86957788447351e8, same eigarsignatur og 20 uendra native bibliotek.
Hovudagenten installerte beta22 med -r; installert SHA er identisk og firstInstallTime er framleis 10.09.2026. Faktisk Stova-kort + D-pad OK opnar rette «One Battle After Another»-detaljar varmt og etter force-stop-kaldstart, med Jellyfin, Trailer og «Hald fram · 143 min att».
Back frå detaljar går til Spole Heim; Stova vart opna uttrykkeleg etterpå. Ingen avspeling, utlogging, kontoimport eller datatømming. Emby/profilbyte/barnemodus er ikkje fysisk prøvde her. Sjå `docs/VERIFICATION_v1.0.0-beta22-local.md`; artefaktcommit er 830cd4d. Berre dokumentasjon er endra etterpå; ingen push/tag/release.

## Beta21: direkte lenkjeopning før faktisk kortprøve
2026-10-25: Brukaren logga inn. Hovudagenten stadfesta ekte Spole-heim og både varm og force-stop-kald `spole://open`-opning med `action=details` for «One Battle After Another» frå Stova sitt Jellyfin-snapshot.
Begge UI-dumpar viser rett tittel, Jellyfin, samandrag og «Hald fram · 143 min att»; den varme skjermdumpen er òg kontrollert visuelt. Ingen avspeling eller konto-/tokenendring frå agentane.
Berre dokumentasjon er endra etter artefaktcommit `92ce694`; signert beta21/158 og APK SHA-256 er uendra. Ingen nye bygg eller einingskall i denne oppdateringa.
Attstår: faktisk korttrykk frå endeleg Stova-installasjon og Back-retur, som hovudagenten tek. Avspeling, profilbyte og barnemodus er ikkje prøvde fysisk. Ingen push eller offentleg release.

## Tidlegare status før brukaren logga inn
2026-10-25: Flett beta20 inn i launcher-greina er fullført i `dd46cbe`. Signert lokal beta21/158 er bygd og installert med -r på Shield; ikkje publisert.
1288 testar totalt: 1287 bestått, 0 feil, 1 eksisterande LAN-test hoppa over. Lint 0 feil (212 åtvaringar, 2 hint), release-bygg OK. Sertifikatet er den eksisterande eigarsignaturen.
APK SHA-256 `3fe9870fa82c6f37fcfa747e44c029ac60014dfe4dc63cd69368873e7ae5c4ee` er verifisert på Shield. Native bibliotek er uendra frå beta20; APK og mapping er arkiverte lokalt.
Mottakaren for spole://open blir no funnen, men både vanleg kaldstart og faktisk detaljlenkje viser Velkomen/Logg inn. Ingen clear/uninstall/utlogging/kontoendring/tokenmigrering eller avspeling er utført. Ekte detaljopning og konto-kontinuitet er ikkje stadfesta; sjå `docs/VERIFICATION_v1.0.0-beta21-local.md`.
Shield-UI er frigjeven til hovudagenten. Etter innlogging: prøv kald/varm detaljopning frå Stova og Back-retur. Versjonskode 158 er lokalt distribuert; ikkje bruk same kode for eit anna artefakt. Ingen push eller offentleg release.
