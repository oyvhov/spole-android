## Spole 0.11.1 · Emby-innlogging

- Rettar brukarnamn/passord-innlogging på Emby.
- Spole brukar no Emby sitt profilendepunkt `/Users/{Id}` med brukar-ID-en frå det vellykka innloggingssvaret. Førre versjon brukte Jellyfin-stien `/Users/Me`; Emby svarte med tenarfeil sjølv om passordet alt var godkjent.
- Innlogginga blir berre lagra når Emby stadfestar nøyaktig same profil-ID.
- Feil ved innlogging skil no mellom feil brukarnamn/passord, nettverksproblem, ugyldig adresse og Emby-tenarfeil.
- Appen identifiserer seg som Spole i nettverkskalla.

Installer over Spole 0.11.0. Same app-ID og signering er bevarte, så dei andre innloggingane dine blir med vidare. Dette er ein debug-signert APK for direkte installasjon.

Rett Emby-endepunkt er stadfesta både i tenaren si eiga API-skildring og i [Emby-dokumentasjonen](https://dev.emby.media/reference/RestAPI/UserService/postUsersAuthenticatebyname.html). Ingen passord eller tilgangsteikn er logga.

### Verifisert

- 149 einingstestar og 62 Android-testar utan feil
- Android lint: 0 feil
- Oppgradering over 0.11.0 bevarer lokale data og innloggingar
- SHA-256: `57e895d1f72d0ef4212d96bed2974a030fa2e546de9c77374eb7e6995e3e88db`
