# Spole 0.17.0-beta20

Rettingar etter problema med pause ved oppstart, buffering og treg retur i beta19.

- Kortvarig fokusbyte set ikkje synleg video permanent på pause. Å gå ut av appen pausar framleis.
- På TV betyr Play framleis Play medan videoen bufrar; det blir ikkje omgjort til Pause.
- Ingen tvungne HDMI-skjermmodusbyte ved start og slutt. Media3 sine sømlause frekvenshint er framleis på.
- Nettverksreserve bevarer eksisterande serverøkt og tidslinje i staden for å starte ny avspeling.
- Feil i undertekstuthenting gir ei eiga melding og lèt videoen halde fram utan det feilande tekstsporet.
- Tilbake avbryt hengande undertekstkall. Diagnostikk gjer ikkje lenger tunge dekodarsøk på UI-tråden.
- Stats skil buffering frå pause og viser feilkoden også når siste reserveforsøk feilar.

Same signatur og pakkenamn; universal-APK for telefon og TV. Versjonskode **99**.
Oppdater med **Appoppdateringar → Sjekk no** og testutgåver på.

Dei konkrete filene «Kiki's Delivery Service», «The Batman», «Affeksjonsverdi» og
«The Housemaid» er ikkje reproduserte på fysisk Pixel/Shield. Denne utgåva rettar
kodefeila over, men er ikkje ei stadfesting av at alle desse filene no fungerer.
Lokal FFmpeg-lydreserve, maskinvarevideo og støtta passthrough frå beta19 er bevarte.
Den tidlegare kjende PiP-returfeilen er ikkje erklært løyst.

FFmpeg-lisensar ligg i APK-en. Tilsvarande uendra kjelde-/relenkingsarkiv ligg ved releasen.
