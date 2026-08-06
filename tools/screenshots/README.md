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

## Buchung bei 375 px durchspielen

`buchung-375.mjs` klickt die Eltern-Buchungsansicht bei 375 × 667 px komplett durch —
Angaben, Klasse, Lehrkraft, Termin, Notiz, Absenden — und prüft nach jedem Schritt, dass
`scrollWidth` nicht größer ist als `clientWidth`, dass die Seite also nicht seitlich
überläuft. Screenshots landen als `buchung-375-*.png` im selben Ordner wie oben.

```
node tools/screenshots/buchung-375.mjs <access-token>
```

Der Access-Token gehört zu einem **veröffentlichten** Sprechtag; die Route ist öffentlich,
ein Passwort braucht das Skript nicht. Es schreibt eine echte Buchung in die Datenbank —
also nur gegen eine lokale Umgebung laufen lassen. Bei Überlauf oder ausbleibender
Bestätigung endet es mit Exit-Code 1.

## Umgebungsvariablen

| Variable              | Default                 | Zweck                                        |
| --------------------- | ----------------------- | -------------------------------------------- |
| `ORGANIZER_PASSWORD`  | —                       | Pflicht für Organizer-Routen                 |
| `ORGANIZER_USERNAME`  | `user`                  | wie in `application.properties`              |
| `BASE_URL`            | `http://localhost:8080` | z. B. für eine Deploy-Umgebung               |

Öffentliche Eltern-Routen (`/elternsprechtag/<token>`) brauchen kein Passwort — dort
überspringt das Skript die Anmeldung.
