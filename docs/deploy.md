# Deploy — Demo-Umgebung

Die Demo läuft unter <https://demo.openclassware.de> als Docker-Compose-Stack auf einem
kleinen VPS. Jeder Push auf `main` (und jeder manuelle `workflow_dispatch`) baut ein Image,
schiebt es in die GitHub Container Registry und rollt es per SSH aus.

Dieses Dokument beschreibt, was **außerhalb** der Automatisierung von Hand eingerichtet
werden muss: Secrets, DNS und die einmalige Server-Vorbereitung. Die Automatisierung selbst
steht in [`.github/workflows/deploy-demo.yml`](../.github/workflows/deploy-demo.yml), der
Server-Stack in [`deploy/`](../deploy).

## Überblick

```
Push auf main ─► Test-Gate (mvn verify) ─► Image bauen ─► GHCR ─► SSH auf VPS
                                                                    │
                                        docker compose pull && up -d
                                                                    │
                          ┌─────────────────────────────────────────┴──────┐
                          │  caddy (80/443, TLS via Let's Encrypt)         │
                          │     └─► app (8080, intern)                     │
                          │            └─► database (Postgres 17, intern)  │
                          └────────────────────────────────────────────────┘
```

Ein rotes Test-Gate bricht die Kette ab, bevor irgendetwas gepusht oder ausgerollt wird.
Der teure Vaadin-Production-Build läuft auf dem GitHub-Runner; der VPS zieht nur das
fertige Image.

## GitHub Secrets

Alle Geheimnisse liegen als **Repository Secrets** unter *Settings → Secrets and variables →
Actions*. Der Deploy-Job schreibt daraus die `.env` auf dem Server.

| Secret               | Bedeutung                                                                                   |
|----------------------|---------------------------------------------------------------------------------------------|
| `SSH_HOST`           | Hostname oder IP des VPS. Wird zusätzlich für `ssh-keyscan` benutzt.                          |
| `SSH_USER`           | Login-Benutzer auf dem VPS, der den Stack besitzt und `docker` ohne `sudo` ausführen darf.    |
| `SSH_PRIVATE_KEY`    | Privater Schlüssel des Deploy-Keys (kompletter PEM-Block, ohne Passphrase).                   |
| `POSTGRES_PASSWORD`  | Passwort der internen Demo-Datenbank. Frei wählbar — die DB ist von außen nicht erreichbar.   |
| `ORGANIZER_USERNAME` | Benutzername des Organizer-Logins der Demo.                                                   |
| `ORGANIZER_PASSWORD` | **Nackter bcrypt-Hash** des Demo-Organizer-Passworts, ohne `{bcrypt}`-Präfix (siehe unten).    |
| `ACME_EMAIL`         | Kontaktadresse für Let's Encrypt (Ablauf-Benachrichtigungen).                                  |

`GITHUB_TOKEN` setzt GitHub selbst — es dient dem Push nach GHCR und dem `docker login` auf
dem Server. Es ist kein manuell zu pflegendes Secret.

Nicht geheime Werte (`DEMO_DOMAIN`, `POSTGRES_DB`, `POSTGRES_USER`, Image-Name, Zielpfad)
stehen als `env:` direkt im Workflow und gehören nicht in die Secrets.

### Organizer-Passwort-Hash erzeugen

`ORGANIZER_PASSWORD` enthält den bcrypt-Hash, **nicht** das Klartext-Passwort:

```bash
docker run --rm httpd:alpine htpasswd -nbBC 10 "" 'GeheimesDemoPasswort' | cut -d: -f2
```

Der Wert wird **unescaped** ins GitHub Secret gelegt — der Workflow verdoppelt die `$` beim
Schreiben der `.env` selbst. Nur wenn man die `.env` von Hand pflegt (siehe
[`deploy/.env.example`](../deploy/.env.example)), muss man die `$` selbst als `$$` escapen.

Das `{bcrypt}`-Präfix setzt `application.properties`; es darf im Secret nicht enthalten sein.

## DNS

Ein A-Record ist Voraussetzung und wird beim Domain-Anbieter von Hand gesetzt:

```
demo.openclassware.de.   A   <VPS-IP>
```

Er muss aufgelöst werden, **bevor** der erste Deploy läuft: Caddy holt das Zertifikat über
ACME HTTP-01 auf Port 80 und braucht dafür die zeigende Domain. Bei IPv6 zusätzlich einen
passenden AAAA-Record setzen.

## Einmalige Server-Vorbereitung

Manuell durch den Betreiber, außerhalb der Automatisierung:

1. **VPS anlegen** (Hetzner-Klasse genügt), aktuelles Linux mit `docker` und dem
   `compose`-Plugin (`docker compose version` muss laufen).
2. **Deploy-User anlegen** und in die `docker`-Gruppe aufnehmen — der Workflow ruft `docker`
   ohne `sudo` auf:
   ```bash
   adduser --disabled-password deploy
   usermod -aG docker deploy
   ```
3. **SSH-Key hinterlegen**: den öffentlichen Teil des Deploy-Keys in
   `~deploy/.ssh/authorized_keys` eintragen; den privaten Teil als `SSH_PRIVATE_KEY`
   speichern. Passwort-Login und Root-Login abschalten.
4. **Zielverzeichnis** `/opt/elternsprechtag` anlegen und dem Deploy-User geben. Der
   Workflow legt es zwar per `mkdir -p` an, aber nur, wenn der User dort schreiben darf:
   ```bash
   install -d -o deploy -g deploy /opt/elternsprechtag
   ```
5. **Firewall**: eingehend nur 22/tcp, 80/tcp, 443/tcp und 443/udp (HTTP/3) öffnen. Die
   Postgres hat bewusst kein Port-Mapping und ist nur im internen Docker-Netz erreichbar.

Die Stack-Dateien selbst müssen **nicht** von Hand abgelegt werden: der Deploy kopiert
`deploy/compose.yaml`, `deploy/Caddyfile` und die erzeugte `.env` bei jedem Lauf nach
`/opt/elternsprechtag/`. Die `.env` wird dort auf `chmod 600` gesetzt und liegt nie im
Repository.

## Zugang zur Demo

- **Organizer**: eigenes, wegwerfbares Demo-Passwort aus `ORGANIZER_PASSWORD` — bewusst
  **nicht** der im Repo liegende Default-Hash aus `application.properties`. Zugangsdaten und
  Demo-Link werden privat verteilt; in der Oberfläche erscheint **kein** Credentials-Hinweis.
  Wird der Zugang bekannt, genügt ein neuer Hash im Secret plus ein Redeploy.
- **Eltern**: unverändert anonym über den Access-Token-Link des Sprechtags — kein Login,
  keine Accounts.

## Demo-Verhalten

Der App-Container läuft mit `SPRING_PROFILES_ACTIVE=demo`
([`application-demo.properties`](../src/main/resources/application-demo.properties)):

- `ddl-auto=create-drop` — bei jedem Deploy entsteht ein frisches Schema, `data-demo.sql`
  seedet neu. Änderungen fremder Besucher verschwinden mit dem nächsten Deploy.
- **Kein echter Mailversand**: `spring.mail.host` bleibt ungesetzt, deshalb wählt
  `BenachrichtigungConfig` die Log-Attrappe. Beliebige Eltern-Adressen sind in der Demo
  gefahrlos eintragbar.
- H2-Console deaktiviert, kein Browser-Autostart.

## Wiederherstellung & Betrieb

Ein kompletter Neuaufbau ist ein Redeploy: Server nach obiger Liste vorbereiten, dann den
Workflow manuell über *Actions → Deploy Demo → Run workflow* starten. Es gibt bewusst keine
Backups — die Demo-Daten sind reproduzierbar.

Nützlich auf dem Server:

```bash
cd /opt/elternsprechtag
docker compose ps
docker compose logs -f app
docker compose logs -f caddy   # bei TLS-/ACME-Problemen
```
