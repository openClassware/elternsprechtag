# ADR 0007: Benachrichtigung je Kind statt je Adresse

Status: akzeptiert
Datum: 2026-10-04

## Kontext

Eine Buchung trägt die Eltern-Adresse denormalisiert
([ADR 0001](0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md)). Ein Buchungsvorgang gilt
genau einem Kind und erzeugt eine Buchung je Lehrkraft — eine Familie mit vier Gesprächen hat also
vier Buchungen mit derselben Adresse. Damit sie nicht vier Absagen bekommt, hat die erste
Absage-Benachrichtigung die Empfänger **je Adresse** dedupliziert. Erinnerung
([#107](https://github.com/openClassware/elternsprechtag/issues/107)) und Ausfall haben die
Bündelung je Adresse übernommen, die Zählung im Absage- und im Ausfall-Dialog ebenso, zuletzt die
Liste der nicht erreichten Familien ([#110](https://github.com/openClassware/elternsprechtag/issues/110)).

Entschieden war das nie. Je Adresse fällt mehr zusammen als die Buchungen *eines* Vorgangs:

- **Geschwister**, deren Eltern zweimal mit derselben Adresse buchen
  ([`ABDECKUNG.md`](../arc/ABDECKUNG.md): ein Kind je Vorgang, Geschwister zurückgestellt),
- **Familien ohne eigene Adresse**, für die der Organizer die Stellvertreteradresse der Schule
  einträgt ([#104](https://github.com/openClassware/elternsprechtag/issues/104)) — hier mehrere
  *fremde* Familien in einem Postfach.

Erinnerung und Ausfall nannten in diesem Fall nur das *erste* Kind über den Terminen *aller*
Kinder der Adresse; die Absage nannte gar keines. Das Sekretariat bekam bei einer Absage eine
einzige, unpersönliche Mail und hatte keinen Weg, die betroffenen Familien zu ermitteln
([#148](https://github.com/openClassware/elternsprechtag/issues/148)).

Eine Vorgangs- oder Familien-Kennung gibt es nicht: `buchungen` trägt weder eine Vorgangs-ID noch
eine Schüler-ID, nur die eingefrorenen Namen.

## Entscheidung

**Eine Benachrichtigung gilt je Sprechtag einer Adresse und einem Kind.** Das Kind ist die
eingefrorene Kombination aus Schülername und Klasse an der Buchung.

- Absage, Erinnerung und Ausfall gruppieren nach (Sprechtag, Adresse, Schülername, Klasse); die
  Bestätigung gilt ohnehin einem Vorgang und damit einem Kind. Jede der vier Mailarten nennt damit
  genau ein Kind und dessen Termine.
- Jede Zählung, die „wie viele werden benachrichtigt" beantwortet — Absage-Dialog,
  Ausfall-Dialog, nicht erreichte Nachrichten —, zählt nach demselben Schlüssel und spricht von
  **Kindern**, nicht von Familien oder Eltern.
- Für die Stellvertreteradresse gibt es **keinen Sonderfall**: Das Sekretariat bekommt je Kind eine
  eigene Mail und kann sie einzeln abarbeiten.

Der Schlüssel ist eine **Annäherung** an „je Buchungsvorgang". Er steht im Code je Seite an genau
einer Stelle, damit ein späterer Familien- oder Vorgangsbegriff ihn dort ersetzt.

## Begründung

- **Es entspricht der Einheit, in der gebucht wird.** Ein Vorgang gilt einem Kind; die Mails
  folgen jetzt derselben Einheit wie die Bestätigung.
- **Die Absicht von ADR 0001 bleibt gewahrt.** Die Buchungen eines Kindes bei mehreren Lehrkräften
  bleiben in einer Mail.
- **Der Stellvertreterfall löst sich ohne Sonderlogik.** Der Versand muss die konfigurierte Adresse
  nicht erkennen — auch dann nicht, wenn sie später geändert wird und alte Buchungen noch die
  vorige tragen.
- **Zusammenfallen kann nur, was dieselbe Adresse hat.** Die Adresse ist Teil des Schlüssels; eine
  Mail kann nie an eine Familie gehen, deren Adresse nicht an ihren Buchungen steht.

## Konsequenzen

- **Positiv:** Jede Mail nennt das richtige Kind mit seinen Terminen. Das Sekretariat erhält für
  Familien ohne Adresse je Kind eine Absage und kann telefonisch nacharbeiten.
- **Negativ / Kosten:**
  - Geschwister an einer Adresse bekommen je Kind eine eigene Mail statt einer gemeinsamen.
  - **Bekannte Grenze:** Zwei gleichnamige Kinder derselben Klasse an derselben Adresse fallen in
    eine Mail. Praktisch tritt das nur an der Stellvertreteradresse auf; die Mail geht dann ans
    Sekretariat, nicht an eine fremde Familie.
  - Ein abweichend geschriebener Schülername bei einem späteren Nachtrag für dasselbe Kind ergibt
    eine zweite Mail.
- **Neu zu bewerten**, sobald ein Familien- oder Vorgangsbegriff eingeführt wird — etwa über die
  zurückgestellten Geschwisterkinder. Er löst die beiden Grenzen oben auf.

## Alternativen (verworfen)

- **Je Adresse bündeln, aber nach Kindern gliedern** (eine Mail, ein Block je Kind): hält an der
  nie entschiedenen Einheit fest und legt bei der Stellvertreteradresse fremde Familien in eine
  Nachricht.
- **Je Buchung eine Mail:** eine Mail je Lehrkraft — genau das, was ADR 0001 vermeiden wollte.
- **Sonderabsatz nur an die Stellvertreteradresse**, der die betroffenen Familien auflistet: braucht
  die Erkennung eines Konfigurationswerts im Versand und lässt den Fehler bei Geschwistern stehen.
- **Vorgangs- oder Schüler-ID an der Buchung:** löst auch die gleichnamigen Kinder, ist aber
  genau der Familien-/Vorgangsbegriff, der bewusst zurückgestellt ist.
