# Screenshots

Nimmt Screenshots der laufenden App bei mehreren Viewport-Breiten auf — gedacht für die
Responsive-Arbeit an den Views (Breakpoints 640 px und 1024 px, Referenzbreite 375 px).

Das Passwort kommt ausschließlich aus der Umgebungsvariable `ORGANIZER_PASSWORD` und wird
nie ausgegeben. Wer das Skript aufruft, muss die Zugangsdaten also nicht kennen.

## Einmalig einrichten

```
cd tools/screenshots
npm install
```

Lädt nur `playwright-core` (kein Browser-Download): das Skript startet das lokal
installierte Chrome über `channel: "chrome"`.

## Benutzen

App starten (`./mvnw spring-boot:run`), dann:

```powershell
$env:ORGANIZER_PASSWORD = '...'
node tools/screenshots/screenshot.mjs /sprechtage 375 1024 1440
```

Argumente in beliebiger Reihenfolge — alles mit `/` am Anfang ist eine Route, alles
Numerische eine Breite. Ohne Breiten greifen 375/1024/1440. Mehrere Routen sind erlaubt:

```
node tools/screenshots/screenshot.mjs /sprechtage /auswertung 375
```

Die Bilder landen als `<route>-<breite>px.png` in `target/screenshots/` (gitignored) und
sind fullPage mit `deviceScaleFactor: 2`.

## Umgebungsvariablen

| Variable              | Default                 | Zweck                                        |
| --------------------- | ----------------------- | -------------------------------------------- |
| `ORGANIZER_PASSWORD`  | —                       | Pflicht für Organizer-Routen                 |
| `ORGANIZER_USERNAME`  | `user`                  | wie in `application.properties`              |
| `BASE_URL`            | `http://localhost:8080` | z. B. für eine Deploy-Umgebung               |

Öffentliche Eltern-Routen (`/elternsprechtag/<token>`) brauchen kein Passwort — dort
überspringt das Skript die Anmeldung.
