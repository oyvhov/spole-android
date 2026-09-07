# Verifisering 0.13.2

- Produksjonsbygg: app.reelstack, versjon 0.13.2, kode 37, 7 139 645 byte.
- APK SHA-256: 9b34f9eff52bcdb6f842442c7e78f66ce15487d7ae4fbe71dd5a1125081c0501.
- Signering verifisert: 36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10.
- 203 JVM-testar bestod. HomeMediaRowsTest: 6/6 Android-testar bestod på emulator-5562. Heile Android-testpakken er ikkje køyrd.
- lintDebug: 0 feil, 26 åtvaringar. Release lint bestod.
- Endeleg produksjons-APK installert med -r over 0.13.1. Tidlegare førstegongsoppsett/demoval er bevarte.
- Kald oppstart filma og undersøkt rammevis: ordmerket tonar inn, ligg på same plass og tonar ut til Heim. Systemoppstart før første Compose-ramme viser berre bakgrunnen og kan ta ekstra tid på emulatoren.
- Nytt launcher-ikon kontrollert i appoversikta. Ein eldre føreslått snarveg i dock viste framleis eit tidlegare ikon.
- Testen brukte demodata, ikkje ekte kontoar. Ingen innloggingar på review-emulatoren er endra.
- Gammal debug-app og testpakke på den isolerte testemulatoren vart erstatta grunna ulik debug-signering. Produksjonsappen vart ikkje avinstallert.
