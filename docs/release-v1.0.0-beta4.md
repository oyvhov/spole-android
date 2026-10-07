# Spole 1.0.0-beta4

Spole finn no Jellyfin- og Emby-tenarane på heimenettet sjølv, og innlogginga er laga for fjernkontrollen.

- Første skjerm viser tenarane på nettverket som store kort. På TV får første tenar fokus med ein gong.
- Ein vald Jellyfin-tenar opnar «Godkjenn på mobilen» utan at noko må skrivast. Ein vald Emby-tenar går rett til innlogging, med brukarane tenaren viser som val.
- Ei skriven adresse som `192.168.1.20` blir prøvd med Jellyfin- og Emby-portane før innlogging. Ei fullstendig adresse som ikkje svarar på prøva, går vidare til vanleg innlogging som før.
- Seerr på same maskin blir fylt inn automatisk på lokalnettet. Seerr blir aldri gjetta for offentlege namn.
- På TV er startknappane og «Logg inn» aldri grå, og startfokus hamnar aldri i eit tekstfelt. Brytarar og «Endre» har synleg fokusring.
- Velkomstskjermen på TV viser ein roleg animasjon utan tekst. Han spelar tre gonger og står stille når rørsle er skrudd av.
- Ei avspeling som har kome seg etter eit nettverksbrot og spelt vidare eitt minutt, tel ikkje lenger mot neste brot. Ein lang episode over tunnel skal ikkje stoppe på tredje korte brot.

Leitinga bruker same UDP-spørjing som Jellyfin- og Emby-appane (port 7359) og hoppar over mobilnett og VPN. Finst ingen tenar, kan du søkje på nytt eller skrive inn adressa.

Installer over førre versjon. Testutgåver må vere på i oppdateringsinnstillingane.
