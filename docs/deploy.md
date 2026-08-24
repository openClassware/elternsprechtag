# Deploy — Demo-Umgebung

Die Demo läuft unter <https://demo.openclassware.de> als Docker-Compose-Stack auf einem
kleinen VPS. Jeder Push auf `main` (und jeder manuelle `workflow_dispatch`) baut ein Image,
schiebt es in die GitHub Container Registry und rollt es per SSH aus. Derselbe Workflow läuft
zusätzlich nach Zeitplan und setzt die Demo täglich zurück (siehe
[Täglicher Reset](#täglicher-reset)).

Dieses Dokument beschreibt, was **außerhalb** der Automatisierung von Hand eingerichtet
werden muss: Secrets, DNS und die einmalige Server-Vorbereitung. Die Automatisierung selbst
steht in [`.github/workflows/deploy-demo.yml`](../.github/workflows/deploy-demo.yml), der
Server-Stack in [`deploy/`](../deploy).

Die vollständige Liste aller Konfigurationswerte samt Umgebungsvariablen, Defaults und Profilen
— unabhängig von dieser Demo-Umgebung — steht in [`konfiguration.md`](konfiguration.md). Was
auf dem Weg **nach** `main` passiert (PR-Workflow, Branch Protection), steht in
[`ci.md`](ci.md).

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
  **nicht** der im Repo liegende Default-Hash aus `application.properties`. Die Zugangsdaten
  gelten als **öffentlich**: sie dürfen zusammen mit dem Demo-Link frei weitergegeben und im
  Repository dokumentiert werden, damit eine interessierte Schule die Demo ohne
  Kontaktaufnahme ausprobieren kann. Vertretbar ist das, weil der Deploy die Datenbank vor
  jedem Start zurücksetzt, der Mailversand dort die Log-Attrappe ist (kein Spam-Vektor) und
  alle Daten erfunden sind. Soll der Zugang trotzdem wechseln, genügt ein neuer Hash im Secret
  plus ein Redeploy. (Die frühere Festlegung, sie privat zu verteilen, gilt nicht mehr.)
- **Eltern**: unverändert anonym über den Access-Token-Link des Sprechtags — kein Login,
  keine Accounts.

## Demo-Verhalten

Der App-Container läuft mit `SPRING_PROFILES_ACTIVE=demo`
([`application-demo.properties`](../src/main/resources/application-demo.properties)):

- **Dieselbe Flyway-Migrationskette wie eine Schulinstanz.** Die Demo baut ihr Schema nicht
  mehr selbst auf (früher: `ddl-auto=create-drop`). Das ist Absicht: Weil der Deploy die
  Datenbank vorher leert, läuft die Kette hier täglich auf eine leere Datenbank — genau der
  Weg, den eine Schule bei der Erstinstallation nimmt. Bliebe die Demo bei `create-drop`,
  würde die Kette nirgends automatisch ausgeführt, bevor eine Schule sie ausführt.
- **Wegwerfbar bleibt sie trotzdem**: Der Deploy setzt die Datenbank vor jedem Start zurück
  (siehe unten), anschließend seedet `data-demo.sql` neu. Änderungen fremder Besucher
  verschwinden also mit dem nächsten Deploy, spätestens aber mit dem nächtlichen Reset.
- **Kein echter Mailversand**: `spring.mail.host` bleibt ungesetzt, deshalb wählt
  `BenachrichtigungConfig` die Log-Attrappe. Beliebige Eltern-Adressen sind in der Demo
  gefahrlos eintragbar.
- H2-Console deaktiviert, kein Browser-Autostart.

## Täglicher Reset

Der Deploy-Workflow hat neben `push: main` und `workflow_dispatch` einen dritten Trigger:

```yaml
schedule:
  - cron: '0 2 * * *'
```

Um 02:00 UTC läuft dieselbe Kette wie bei einem Deploy — Test-Gate, Image, Ausrollen. Damit
findet ein Besucher die Demo auch nach Wochen ohne Push im Ausgangszustand vor, und dort
eingegebene Namen und Adressen sind spätestens am nächsten Tag verschwunden.

Zurückgesetzt wird im Ausrollschritt selbst, und zwar bei **jedem** Lauf: Die App wird
angehalten, das Schema der Demo-Datenbank verworfen und neu angelegt, danach fährt der Stack
wieder hoch. Beim Hochfahren spielt Flyway die Migrationskette auf die leere Datenbank ein und
`data-demo.sql` seedet sie.

```
docker compose up -d --wait database   # Datenbank muss laufen und Verbindungen annehmen
docker compose stop app         # sonst schreibt die App während des Drops weiter
psql ... -c 'DROP SCHEMA public CASCADE; CREATE SCHEMA public;'
docker compose up -d            # startet die App wieder
```

Der Reset gehört in den Ausrollschritt und nicht in einen eigenen: `docker compose up -d`
allein setzt **nichts** zurück. Bei einem Lauf ohne neuen Commit zeigt `APP_IMAGE` auf denselben
Commit-SHA wie beim letzten Deploy, Compose sieht keine Änderung und ließe den App-Container in
Ruhe — der Container liefe weiter, und die Datenbank behielte alles, was Besucher eingegeben
haben. Das `docker compose stop app` davor erzwingt den Neustart auch in diesem Fall.

`DROP SCHEMA public` statt „Volume verwerfen“: Es trifft genau die Daten dieser Datenbank und
lässt den Postgres-Container samt Benutzer stehen. Das Passwort kommt dabei aus der Umgebung
des Datenbank-Containers und steht nie auf einer Kommandozeile.

Weil der Reset am gewöhnlichen Deploy hängt, gilt die `concurrency`-Gruppe `deploy-demo` für
beide: ein nächtlicher Reset und ein Deploy aus `main` können sich nicht überholen, der
spätere Lauf wartet.

Manuell auslösen — und damit exakt den Reset-Pfad testen — geht über *Actions → Deploy Demo →
Run workflow*.

Zwei Betriebshinweise:

- GitHub **deaktiviert zeitgesteuerte Workflows nach 60 Tagen ohne Repository-Aktivität**.
  Steht die Demo trotz Zeitplan still, ist das die erste Stelle zum Nachsehen; ein Klick auf
  *Enable workflow* schaltet ihn wieder scharf.
- `cron` in GitHub Actions ist **UTC** und nicht minutengenau — bei Last verschiebt sich der
  Start um mehrere Minuten. Für einen Demo-Reset ist das ohne Belang.

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
