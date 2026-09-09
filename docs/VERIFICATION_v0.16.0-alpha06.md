# Verifikasjon — Spole 0.16.0-alpha06

9. september 2026 · bygg 49 · pakke `app.reelstack`.

## Omfang

Signert førehandsversjon med kjeldearbeidet til og med `113a105`: språk, breitt nettbrettoppsett, sidemeny, fokus/leseflyt og ny TV-startside med Quick Connect-rettleiing.

- 283/283 JVM-testar køyrde ved pakking, ingen feil.
- 13/13 målretta Android-testar på endeleg funksjonskjelde før versjonsauken: TV-oppsett og innloggingsoppleving på Google TV API 36. Ingen funksjonsendring etter desse testane.
- Førre kjeldebolk verifiserte faktisk videodekoding, fjernkontroll og to-stegs Tilbake på same TV-systembilete.
- Full Android-testpakke er ikkje køyrd på nytt i denne pakkingsrunden. Fysisk TV, reell Quick Connect-godkjenning på TV og full TV-launcher/store-pakking står att.

## Artefaktar

APK og R8-mapping blir lagra saman under `app/build/test-alpha06/` og som vedlegg på GitHub-førehandsutgåva. Eksisterande produksjonssignering skal vere bevart; ingen ny nøkkel eller pakkenamn.

- Release-bygg og lint bestod: 0 feil, 32 åtvaringar.
- Signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK: `Spole-0.16.0-alpha06.apk`, 3 693 477 byte, ikkje debuggable.
- APK SHA-256: `B22130E5464949B431AC8AE170DCD6FF721A6D700CFE6915A59F58ABFCFAC0F2`.
- Oppdatert produksjonsappen på review-emulator 5560 med `install -r`. Heim viser bevart personleg profil, ekte Jellyfin-kunst og den kompakte aktive økta. Ingen avspelingskommando vart sendt. Privat skjermbilete ligg berre lokalt.
