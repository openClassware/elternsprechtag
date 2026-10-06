// Liest die Mail-Ablage der App. Läuft die Doku-Instanz mit ELTERNSPRECHTAG_MAIL_ABLAGE, legt sie
// jede Mail als Datei ab, statt sie zu versenden (`DateiBenachrichtigungSender`): `An:`, `Betreff:`,
// eine Leerzeile, dann der Text. Der Teil ohne Dateisystem steht hier und ist per plain
// `node --test` geprüft; das Warten auf eine Mail steht in `werkzeug.mjs`.

/** Zerlegt eine abgelegte Mail. Ohne Kopf ist es keine Mail der App, sondern ein Fehler. */
export function leseMail(inhalt) {
	const zeilen = inhalt.replaceAll('\r\n', '\n').split('\n');
	if (!zeilen[0]?.startsWith('An: ') || !zeilen[1]?.startsWith('Betreff: ') || zeilen[2] !== '') {
		throw new Error(`Keine Mail der App — erwartet sind die Zeilen „An:“ und „Betreff:“, dann eine Leerzeile:\n${inhalt.slice(0, 200)}`);
	}
	return {
		an: zeilen[0].slice('An: '.length),
		betreff: zeilen[1].slice('Betreff: '.length),
		text: zeilen.slice(3).join('\n'),
	};
}

/**
 * Was die Seite *Welche E-Mails Eltern bekommen* zeigt: Betreff und Text. Die Adresse fehlt — sie
 * ist aus dem Lauf erfunden und sagt dem Leser nichts.
 */
export function alsTextblock(mail) {
	return `Betreff: ${mail.betreff}\n\n${mail.text}\n`;
}

/**
 * Die Mails, die seit `vorher` hinzugekommen sind, in Versandreihenfolge — die App benennt sie so,
 * dass der Name danach ordnet. Nur fertige `.txt`, keine, die gerade geschrieben wird.
 */
export function neueDateien(vorher, jetzt) {
	return jetzt.filter((datei) => datei.endsWith('.txt') && !vorher.includes(datei)).sort();
}
