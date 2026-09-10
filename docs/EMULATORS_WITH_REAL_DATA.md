# Start Spole på TV og mobil med ekte data

Denne rettleiinga er for AI-agentar og utviklarar på den eksisterande Windows/WSL-arbeidsstasjonen. Skriptet brukar dei lagra emulatorprofilane med Spole og kontoane som alt er sette opp. Ein ny klone av GitHub-repoet inneheld ikkje desse profilane eller kontoane.

## Éin kommando

Køyr i PowerShell 7, frå kva mappe som helst:

```powershell
pwsh -NoProfile -File C:\JellyBin\reelstack-android\scripts\Start-SpoleEmulators.ps1
```

Dette startar begge emulatorane om nødvendig, ventar på Android og nettverk, vekkjer skjermen, opnar produksjonsappen `app.reelstack` og viser vindauga **Spole - Android TV** og **Spole - Mobil**. Køyrer dei frå før, blir dei brukte vidare. Eksisterande speglingsvindauge blir ikkje dupliserte. Eit minimert vindauge kan hentast fram frå oppgåvelinja.

Ved kald oppstart kan det ta nokre minutt. Skriptet skriv framdrift og har tidsgrenser. Ein AI med kort verktøytidsgrense skal halde fram med å vente på den eksisterande prosessen, ikkje starte nye eksemplar av skriptet.

```powershell
# Berre TV eller berre mobil
pwsh -NoProfile -File C:\JellyBin\reelstack-android\scripts\Start-SpoleEmulators.ps1 -Device TV
pwsh -NoProfile -File C:\JellyBin\reelstack-android\scripts\Start-SpoleEmulators.ps1 -Device Phone

# Utan nye synlege speglingsvindauge (eksisterande vindauge blir ståande)
pwsh -NoProfile -File C:\JellyBin\reelstack-android\scripts\Start-SpoleEmulators.ps1 -NoWindows

# Opne appen på nytt etter til dømes nettverksproblem; bevarer data
pwsh -NoProfile -File C:\JellyBin\reelstack-android\scripts\Start-SpoleEmulators.ps1 -Device Phone -RestartApp
```

`-RestartApp` stoppar berre Spole-prosessen og kan avbryte avspeling i denne emulatoren. Standardkommandoen tvingar ikkje appen til å starte på nytt. Ingen av variantane installerer ein APK, aktiverer demo, køyrer testar, loggar ut eller slettar data.

## Rette profilar

| Bruk | AVD | Varig datamappe | ADB-serienummer |
| --- | --- | --- | --- |
| **TV med ekte data** | `Spole_GoogleTV_Test` | `C:\JellyBin\.spole-tv-avds` | `emulator-5564` |
| **Mobil med ekte data** | `Spole_Review` | `C:\JellyBin\.spole-review-avds` | `emulator-5560` |
| Isolert TV-test | `Spole_TV_Instrumentation` | `C:\JellyBin\.spole-tv-test-avds` | `emulator-5566` |
| Isolert mobiltest | `Spole_Instrumentation` | `C:\JellyBin\.spole-test-avds` | `emulator-5562` |

Trass namnet er **Spole_GoogleTV_Test på 5564 den innlogga TV-profilen**. Skriptet startar berre dei to første radene. Instrumentering skal bruke dei isolerte testprofilane; ho må ikkje køyrast mot ekte kontoar.

Kontoar og appval ligg i AVD-brukardata utanfor repoet og `app/build`. `-no-snapshot` betyr vanleg oppstart utan snapshot, **ikkje** sletting av brukardata. Skriptet krev at den eksisterande profilen finst og lagar ikkje ein tom erstatning.

Ikkje bruk `-wipe-data`, `pm clear`, avinstallering eller sletting av AVD-mappa for å få oppstart til å fungere. Ikkje kopier konto-/tokenfiler, AVD-ar, skjermbilete eller innloggingsdata til GitHub.

## Kva skriptet handterer

1. Kontrollerer PowerShell 7, Windows-adb, Linux SDK, scrcpy og lagra AVD-filer.
2. Finn **gjeldande** IPv4-adresse i Ubuntu/WSL. Ikkje hardkod den tidlegare adressa i nye kommandoar.
3. Brukar den delte WSL-adb-tenaren på port 5037, eller startar han om han ikkje svarar. Det endrar ingen brannmurreglar og stoppar ikkje andre adb-tenarar.
4. Kontrollerer AVD-namn og portar før oppstart. Ved konflikt stoppar skriptet med forklaring, utan å ta over ein annan emulator.
5. Startar TV med `swiftshader`; mobil med `swangle` og Vulkan avslått. Begge får 1536 MB RAM. Hjelpeprosessane køyrer utan ekstra konsollvindauge.
6. Ventar på `sys.boot_completed=1` og deretter eit validert Android-nettverk. Ved nettverkstidsgrense opnar det appen med ei åtvaring; kontoane er framleis lagra.
7. Vekkjer skjermen og fjernar emulator-låseskjermen før appstart. Dette unngår at appen køyrer bak ein mørk/låst skjerm.
8. Startar scrcpy gjennom den same WSL-adb-tenaren. Eigne tunnelportar: TV 27190, mobil 27191.

Standardar: WSL-distribusjon `Ubuntu`, Linux SDK `/home/oyvhov/Android/Sdk`, Windows SDK `%LOCALAPPDATA%\Android\Sdk`. scrcpy blir funne i PATH eller WinGet-installasjonen. Avvik kan givast med `-Distro`, `-LinuxSdk`, `-WindowsSdk`, `-ScrcpyPath` og `-DataRoot`. Flytting av AVD-ar krev òg at deira `.ini`-registrering peikar på rett Linux-sti; `-DataRoot` flyttar ingen data.

## Kontroll for AI-agenten

Etter oppstart ligg ei maskinlesbar oppsummering i:

```text
C:\JellyBin\reelstack-android\app\build\emulators\last-start.json
```

Ho inneheld serienummer, gjeldande adb-vert, nettverksstatus og loggmappe, men ingen mediekontoar eller token. `NetworkValidated=true` stadfestar Android-nettverket, **ikkje** at kvar Jellyfin-/Seerr-tenar eller innlogging fungerer.

Bruk riktig serienummer og den returnerte verten ved vidare kontroll:

```powershell
$sessions = Get-Content C:\JellyBin\reelstack-android\app\build\emulators\last-start.json -Raw | ConvertFrom-Json
$tv = @($sessions) | Where-Object Device -eq TV
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -H $tv.AdbHost -P 5037 -s $tv.Serial shell uiautomator dump /sdcard/spole-check.xml
& $adb -H $tv.AdbHost -P 5037 -s $tv.Serial pull /sdcard/spole-check.xml C:\JellyBin\reelstack-android\app\build\spole-tv-check.xml
```

Sjå etter faktisk bibliotekinnhald på framsida. Ikkje konkluder med at ekte data fungerer berre fordi appstart returnerte OK. UI-dump og skjermbilete kan innehalde personopplysningar; hald dei lokalt under den ignorerte byggmappa. Ikkje køyr `uiautomator dump` samstundes med instrumentering på same emulator.

## Feilsøking

- **Tomme rader etter kaldstart:** vent til nettverket er klart. Køyr så éin gong med `-Device Phone -RestartApp` eller tilsvarande TV. Dette løyste den observerte nettverksovergangen utan ny innlogging. Er innhaldet framleis tomt, undersøk tenestetilkoplinga i appen; ikkje slå på demo eller nullstill profilen.
- **Manglande profil eller produksjonsapp:** skriptet stoppar. Finn den eksisterande profilen eller ein godkjend signert APK. Ikkje bygg ein blank AVD og kall han «ekte data».
- **Svart vindauge / `null root node`:** skriptet vekkjer og låser opp emulatoren. Køyr det på nytt før du tolkar dette som appkrasj.
- **Portkonflikt / adb utilgjengeleg:** les den konkrete feilen og loggane. Ikkje køyr ein generell `adb kill-server`, slett låsfiler eller stopp vilkårlege emulatorar.
- **scrcpy manglar:** bruk `-ScrcpyPath 'full sti til scrcpy.exe'`, eller `-NoWindows` for berre appstart. UI kan ikkje stadfestast visuelt utan eit vindauge eller skjermbilete.
- **Treg maskin:** unngå tunge Gradle-bygg og fleire testemulatorar samstundes. Ikkje stopp eit pågåande bygg for å frigjere minne utan å avklare arbeidsflyten.

Loggar for kvar køyring ligg i `app/build/emulators/<tidspunkt>/`. Oppstartsskriptet avsluttar med feil ved manglande føresetnader eller tidsgrense; det nullstiller aldri profilen som reserveplan.

## Verifikasjon

10. september 2026: skriptet vart køyrt mot begge eksisterande emulatorane. Det fann WSL-adressa, brukte riktige AVD-ar vidare, stadfesta validert nettverk og opna produksjonsappen. TV-speglingsvindauget vart lukka og opna att av skriptet; neste køyring brukte det vidare utan duplikat. Mobil åleine med `-NoWindows` vart også kontrollert. Ein medvite manglande datasti gav ei tydeleg feilmelding før emulatorstart, utan å opprette ein ny profil. PowerShell-syntaksen er kontrollert.

Kaldoppstartskommandoane og GPU-vala er dei same som vart brukte til å starte begge profilane med ekte data tidlegare i økta. Ei ny kald omstart vart ikkje tvinga fram berre for å teste skriptet.
