# Konfiguration & gespeicherte Daten

Diese Seite richtet sich an **Schul-IT**, die eine eigene Instanz einrichtet. Sie listet jeden
Wert, den die Anwendung aus der Konfiguration liest — mit Umgebungsvariable, Default und
Bedeutung — und benennt am Ende, welche Anwendungsdaten dabei entstehen.

Quelle der Angaben sind [`application.properties`](../src/main/resources/application.properties),
[`application-demo.properties`](../src/main/resources/application-demo.properties) und die
auswertenden Klassen im Code.

**Was hier _nicht_ steht:** Betrieb, Härtung, Reverse Proxy, TLS und Datensicherung. Wie die
öffentliche Demo betrieben wird, steht in [`deploy.md`](deploy.md); eine allgemeine
Betriebsanleitung für Schulen ist eine eigene Spec und noch nicht geschrieben.

## Wie Werte gesetzt werden

Die Anwendung ist eine Spring-Boot-Anwendung. Jeder Wert unten wird als **Umgebungsvariable**
gesetzt — im Container über `environment:` bzw. eine `.env`, beim Start per JAR über die Umgebung
des Prozesses. Eine eigene `application.properties` neben dem JAR funktioniert ebenfalls, ist aber
nicht der vorgesehene Weg.

Zwei Mechanismen greifen dabei nebeneinander:

- **Explizite Platzhalter.** Steht in `application.properties` `${SPRING_MAIL_PORT:587}`, liest
  Spring genau diese Variable und fällt sonst auf `587` zurück.
- **Relaxed Binding.** Zusätzlich übersetzt Spring jede Umgebungsvariable in Großbuchstaben mit
  Unterstrichen automatisch in den gleichnamigen Property-Namen: `SPRING_MAIL_HOST` →
  `spring.mail.host`, `ELTERNSPRECHTAG_SCHOOLNAME` → `elternsprechtag.schoolname`. So sind auch
  Werte überschreibbar, für die unten keine eigene Variable in der Tabelle steht.

Die Defaults in `application.properties` sind **Entwicklungswerte**. Ohne gesetzte Variablen
startet die Anwendung lokal unverändert — für eine echte Instanz müssen mindestens
Datenbankverbindung und Organizer-Zugang gesetzt werden.

## Datenbank

Die Anwendung erwartet eine **PostgreSQL**-Datenbank. Das Schema legt **Flyway** an: Die Skripte
liegen im Repository unter [`src/main/resources/db/migration`](../src/main/resources/db/migration)
und laufen beim Start der Anwendung automatisch — beim ersten Start auf einer leeren Datenbank
ebenso wie bei jedem Update. Sie sind vor dem Update lesbar; Flyway führt jedes Skript genau einmal
aus und schreibt das in die Tabelle `flyway_schema_history` derselben Datenbank.

Der Datenbank-Benutzer braucht daher Rechte zum Anlegen und Ändern von Tabellen, nicht nur zum
Lesen und Schreiben von Daten.

| Umgebungsvariable            | Default                                              | Bedeutung                                                     |
|------------------------------|------------------------------------------------------|---------------------------------------------------------------|
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5432/elternsprechtag`   | JDBC-URL der Datenbank.                                       |
| `SPRING_DATASOURCE_USERNAME` | `myuser`                                             | Datenbank-Benutzer.                                           |
| `SPRING_DATASOURCE_PASSWORD` | `mysecret`                                           | Passwort des Datenbank-Benutzers.                             |

Die JPA-Einstellungen selbst (`spring.jpa.hibernate.ddl-auto=validate`, `show-sql=false`,
`open-in-view=false`) sind bewusst fest verdrahtet und keine Konfigurationspunkte für Betreiber.
Über Relaxed Binding ließen sie sich überschreiben — davon ist abzuraten: `validate` heißt, dass
Hibernate das migrierte Schema nur noch prüft und nichts daran ändert. Jeder andere `ddl-auto`-Wert
lässt Hibernate am Schema arbeiten, das Flyway besitzt; `create-drop` verwirft dabei alle Daten.

## Organizer-Zugang

Es gibt genau **eine** Anmeldung: die des Organizers. Sie liegt nicht in der Datenbank, sondern
als In-Memory-Benutzer aus der Konfiguration (siehe
[`SecurityConfiguration`](../src/main/java/de/openclassware/elternsprechtag/config/SecurityConfiguration.java)).
Lehrkräfte und Eltern haben **kein** Login; Eltern greifen anonym über den Access-Token-Link eines
Sprechtags zu.

| Umgebungsvariable         | Default                     | Bedeutung                                                      |
|---------------------------|-----------------------------|-----------------------------------------------------------------|
| `ORGANIZER_USERNAME`      | `user`                      | Benutzername des Organizer-Logins.                              |
| `ORGANIZER_PASSWORD_HASH` | im Repo hinterlegter Hash   | **Nackter bcrypt-Hash** des Organizer-Passworts, ohne Präfix.   |

### Fallstrick 1: der Hash wird ohne `{bcrypt}`-Präfix gesetzt

Spring Security erwartet Passwörter in der Form `{bcrypt}$2a$10$…`. Das Präfix `{bcrypt}` steht
bereits **fest in `application.properties`**, direkt vor dem Platzhalter:

```properties
elternsprechtag.security.organizer.password={bcrypt}${ORGANIZER_PASSWORD_HASH:…}
```

Grund ist Springs Platzhalter-Parser: Der Default-Wert eines `${VAR:default}` darf selbst kein
`}` enthalten — stünde `{bcrypt}` im Platzhalter, endete der Ausdruck vorzeitig. Die
Umgebungsvariable liefert deshalb **nur den Hash**. Wer das Präfix mitgibt, erzeugt
`{bcrypt}{bcrypt}$2a$…` und kann sich nicht anmelden.

Einen Hash erzeugen:

```bash
docker run --rm httpd:alpine htpasswd -nbBC 10 "" 'GeheimesPasswort' | cut -d: -f2
```

Der Hash enthält `$`-Zeichen. Trägt man ihn in eine Docker-Compose-`.env` ein, muss jedes `$` als
`$$` verdoppelt werden, sonst interpretiert Compose die Teile als leere Variablen.

### Fallstrick 2: die Variablen heißen bewusst nicht wie die Properties

Die Variablen heißen `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD_HASH` und **nicht**
`ELTERNSPRECHTAG_SECURITY_ORGANIZER_USERNAME` / `…_PASSWORD`. Setzt man die langen Namen, greift
Relaxed Binding: der Wert landet direkt auf der Property und verdrängt die Zeile mit dem
`{bcrypt}`-Präfix. Die Anmeldung scheitert dann daran, dass kein Default-Passwort-Encoder
konfiguriert ist („no default password encoder configured"). Nur die kurzen Namen sind richtig.

### Fallstrick 3: der Default-Zugang ist öffentlich bekannt

Ohne gesetzte Variablen gilt `user` mit dem im Repository hinterlegten Hash. Dieser Zugang ist für
lokale Entwicklung gedacht und für jeden nachlesbar. Eine erreichbare Instanz muss beide Werte
setzen.

## Mail / SMTP

Die Anwendung verschickt zwei Arten von Mails an Eltern: die **Buchungsbestätigung** nach der
Buchung und die **Absage-Benachrichtigung**, wenn ein Sprechtag abgesagt wird.

| Umgebungsvariable             | Default                        | Bedeutung                                                              |
|-------------------------------|--------------------------------|-------------------------------------------------------------------------|
| `SPRING_MAIL_HOST`            | *(nicht gesetzt)*              | SMTP-Server. **Schaltet den echten Versand überhaupt erst ein.**         |
| `SPRING_MAIL_PORT`            | `587`                          | SMTP-Port.                                                              |
| `SPRING_MAIL_USERNAME`        | *(leer)*                       | SMTP-Benutzer.                                                          |
| `SPRING_MAIL_PASSWORD`        | *(leer)*                       | SMTP-Passwort.                                                          |
| `SPRING_MAIL_SMTP_AUTH`       | `true`                         | SMTP-Authentifizierung verwenden.                                       |
| `SPRING_MAIL_STARTTLS_ENABLE` | `true`                         | Verbindung per STARTTLS verschlüsseln.                                  |
| `ELTERNSPRECHTAG_MAIL_ABSENDER` | `elternsprechtag@example.com` | Absenderadresse der Mails.                                              |

`spring.mail.default-encoding` ist fest auf `UTF-8` gesetzt, damit Umlaute und `ß` unabhängig vom
Plattform-Charset korrekt ankommen.

### Fallstrick: ohne Mailhost verschickt die Anwendung nichts

`spring.mail.host` hat **absichtlich keinen Default**. Davon hängt ab, welche Implementierung
[`BenachrichtigungConfig`](../src/main/java/de/openclassware/elternsprechtag/services/BenachrichtigungConfig.java)
auswählt:

- **`SPRING_MAIL_HOST` gesetzt** → echter Versand über JavaMail.
- **nicht gesetzt** → **Log-Attrappe**: Empfänger und Betreff landen nur im Anwendungslog, es geht
  keine Mail hinaus. Die Oberfläche verhält sich unverändert, Eltern bekommen aber nichts.

Ein *leer* gesetztes `SPRING_MAIL_HOST=` ist der schlimmste Fall: Spring wertet die Property dann
als gesetzt, aktiviert den echten Sender und der Versand scheitert zur Laufzeit. Die Variable
entweder mit echtem Host setzen oder ganz weglassen.

Für den produktiven Einsatz gilt sie faktisch als Pflicht: Die Buchungsbestätigung ist der einzige
Beleg, den Eltern über ihre Termine erhalten.

## Schule

| Umgebungsvariable           | Default                     | Bedeutung                                                                   |
|-----------------------------|-----------------------------|-------------------------------------------------------------------------------|
| `ELTERNSPRECHTAG_SCHOOLNAME` | `Gesamtschule Lindenhof`   | Name der Schule; erscheint in der Oberfläche und als Grußformel in den Mails. |

Der Wert hat in `application.properties` keinen Platzhalter — er wird über Relaxed Binding
gesetzt und ist der einzige Wert, den eine Schule zwingend an sich anpassen will.

## Weitere Werte

| Umgebungsvariable                  | Default | Bedeutung                                                                                                                  |
|------------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`           | *(keins)* | Aktive Profile, kommasepariert (siehe unten).                                                                              |
| `SERVER_FORWARD_HEADERS_STRATEGY`  | *(keins)* | Auf `framework` setzen, wenn ein Reverse Proxy TLS terminiert — sonst baut die Anwendung Weiterleitungen auf `http://` und den internen Port. |
| `VAADIN_LAUNCH_BROWSER`            | `true`  | Öffnet beim Start einen Browser. Im Container auf `false` setzen (das `demo`-Profil tut das bereits).                        |
| `SERVER_PORT`                      | `8080`  | Port, auf dem die Anwendung lauscht (Spring-Boot-Standard, nicht im Repo gesetzt).                                          |

## Profile

Profile werden über `SPRING_PROFILES_ACTIVE` aktiviert, mehrere kommasepariert
(`SPRING_PROFILES_ACTIVE=local,demo`).

| Profil     | Datei                                                                              | Zweck                                                                                                                                                       |
|------------|------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| *(keins)*  | [`application.properties`](../src/main/resources/application.properties)           | Standard. Gilt immer und ist die Basis für alle Profile. Für eine eigene Instanz ist **kein** Profil nötig — die Werte kommen aus der Umgebung.                |
| `local`    | `application-local.properties` (nicht im Repository)                               | Persönliche Entwicklereinstellungen, u. a. echter SMTP-Zugang zum Testen. Die Datei ist per `.gitignore` ausgeschlossen und muss lokal selbst angelegt werden. |
| `demo`     | [`application-demo.properties`](../src/main/resources/application-demo.properties) | Öffentliche Vorführinstanz: Demo-Stammdaten aus `db/demo/R__demo_stammdaten.sql`, kein Browser-Autostart.                                                                  |

Das `demo`-Profil ist **nicht** für den Echtbetrieb geeignet: Es nimmt ein zusätzliches
Migrationsverzeichnis in die Flyway-Suchpfade auf und füllt die Datenbank damit mit erfundenen
Stammdaten. Das Schema baut es nicht mehr selbst neu auf — es läuft durch
dieselbe Migrationskette wie eine Schulinstanz. Zurückgesetzt wird die Demo stattdessen beim
Deploy (siehe [Deployment](deploy.md)).

## Welche Daten die Anwendung speichert

Der Abschnitt dient der Einschätzung, ob und in welchem Umfang eine Datenschutzprüfung nötig ist.
Er ist **keine Muster-Datenschutzerklärung** — siehe unten.

**Stammdaten**, vom Organizer gepflegt:

- **Lehrkräfte**: Vorname, Nachname, Kürzel.
- **Klassen**: Bezeichnung (`10a`).
- **Fächer**: Name und Kürzel.
- **Lehraufträge**: die Zuordnung Lehrkraft ↔ Klasse ↔ Fach.
- **Sprechtage**: Titel, Datum, Zeitfenster, Slot-Dauer, Ort, Hinweistext, teilnehmende Klassen,
  Status und ein **Access-Token** (der Link, über den Eltern ohne Anmeldung Zugriff haben — wer ihn
  kennt, sieht den Sprechtag).
- **Termine**: Zeitfenster je Lehrkraft und Sprechtag.

**Von Eltern eingegebene Daten** — pro Buchung:

- **Name des Kindes**,
- **Name der Eltern**,
- **E-Mail-Adresse der Eltern** (Pflichtfeld; siehe
  [ADR 0001](adr/0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md) und
  [ADR 0002](adr/0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md)),
- eine **freiwillige Notiz** an die Lehrkraft,
- Zeitpunkt der Buchung, gebuchter Termin und Lehrauftrag.

Die Notiz ist ein Freitextfeld: Was Eltern hineinschreiben, bestimmt die Sensibilität des
Datenbestands mit. Ein Hinweis an die Eltern, dort keine Gesundheits- oder anderen besonderen
Daten einzutragen, ist zu empfehlen.

**Weitere Verarbeitung:**

- **Kein Eltern-Konto.** Es gibt keine Eltern-Entity; Name und E-Mail hängen an der einzelnen
  Buchung. Auch für Lehrkräfte existieren keine Benutzerkonten oder Passwörter.
- **Mailversand.** Bestätigungs- und Absagemails enthalten Sprechtag, Ort, Name des Kindes und
  Klasse, die gebuchten Zeiten mit Lehrkraft und Fach sowie die Notiz. Sie gehen an den
  konfigurierten SMTP-Server — dieser Anbieter verarbeitet die Daten mit.
- **Logs.** Ohne konfigurierten Mailhost protokolliert die Anwendung Empfängeradresse und Betreff
  der nicht versendeten Benachrichtigung im Anwendungslog.
- **Kein Tracking.** Die Anwendung bindet keine externen Dienste, keine Analyse-Werkzeuge und
  keine Ressourcen von Drittservern ein. Sitzungsdaten hält Vaadin serverseitig; ein
  Sitzungs-Cookie wird gesetzt.
- **Keine automatische Löschung.** Buchungen bleiben in der Datenbank stehen, bis sie jemand
  entfernt. Eine Aufbewahrungsfrist umzusetzen ist Sache der betreibenden Schule.

### Verantwortlichkeit

Jede Schule betreibt ihre **eigene** Instanz auf eigener Infrastruktur. Das Projekt liefert
ausschließlich Software und betreibt für Schulen nichts, empfängt keine Daten und hat keinen
Zugriff auf laufende Instanzen.

Datenschutzrechtlich **Verantwortliche im Sinne der DSGVO ist damit die betreibende Schule**
(bzw. ihr Schulträger). Sie entscheidet über Zwecke und Mittel der Verarbeitung und schuldet
Datenschutzerklärung, Verarbeitungsverzeichnis, Löschkonzept und — je nach Landesrecht — die
Beteiligung der zuständigen Stellen. Dieses Projekt stellt bewusst **keine**
Muster-Datenschutzerklärung bereit: Sie wäre ohne Kenntnis von Hosting, Mailanbieter und
schulischen Abläufen falsch.
