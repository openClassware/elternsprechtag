/**
 * Spielt eine vollständige Buchung in der Eltern-Ansicht bei 375 x 667 px durch und prüft nach
 * jedem Schritt, dass das Dokument nicht breiter ist als der Viewport. Die Route ist öffentlich,
 * es wird kein Passwort gebraucht.
 *
 * Aufruf (App muss laufen, Sprechtag muss veröffentlicht sein):
 *   node tools/screenshots/buchung-375.mjs <access-token>
 *
 * Schreibt eine echte Buchung in die Datenbank — nur gegen eine lokale Umgebung laufen lassen.
 */
import { chromium } from "playwright-core";
import { mkdir } from "node:fs/promises";
import path from "node:path";

const token = process.argv[2];
if (!token) {
  console.error("Kein Access-Token angegeben. Beispiel: node buchung-375.mjs cfe70d1d-...");
  process.exit(1);
}

const baseUrl = (process.env.BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");
const OUT_DIR = path.resolve(import.meta.dirname, "../../target/screenshots");
await mkdir(OUT_DIR, { recursive: true });

const browser = await chromium.launch({ channel: "chrome" });
const context = await browser.newContext({
  viewport: { width: 375, height: 667 },
  deviceScaleFactor: 2,
  isMobile: true,
  hasTouch: true,
});
const page = await context.newPage();

const ueberlauf = [];

await page.goto(`${baseUrl}/elternsprechtag/${token}`, { waitUntil: "networkidle" });
await settle();
await pruefeUeberlauf("1 Buchungsansicht geladen");
await shot("1-start");

// Schritt 1: Angaben ausfüllen und Klasse wählen — erst danach kennt die View die Lehrkräfte.
await page.locator("vaadin-text-field input").first().fill("Anna Müller");
await page.locator("vaadin-text-field input").nth(1).fill("Lukas Müller");
await page.locator("vaadin-email-field input").fill("anna.mueller@example.com");
await page.locator("vaadin-select").click();
await page.waitForTimeout(400);
const klasse = page.locator("vaadin-select-item").first();
console.log("      Klasse:", (await klasse.textContent()).trim());
await klasse.click();
await settle();
await pruefeUeberlauf("2 Angaben ausgefüllt, Klasse gewählt");
await shot("2-angaben");

// Schritt 2: Lehrkraft
const lehrkraefte = page.locator(".elternsprechtag-view__lehrkraft");
console.log("      Lehrkräfte:", await lehrkraefte.count());
await lehrkraefte.first().click();
await settle();
await pruefeUeberlauf("3 Lehrkraft gewählt");
await shot("3-lehrkraft");

// Schritt 3: erster freier Termin
const slots = page.locator(
  ".elternsprechtag-view__slot:not(.elternsprechtag-view__slot--belegt):not(.elternsprechtag-view__slot--konflikt)",
);
console.log("      freie Slots:", await slots.count());
await slots.first().click();
await settle();
await pruefeUeberlauf("4 Termin gewählt");
await shot("4-termin");

// Schritt 4: Notiz, dann absenden
await page.locator("vaadin-text-area textarea").fill("Bitte über Mathe sprechen.");
await settle();
const buchen = page.locator(".elternsprechtag-view__footer vaadin-button").first();
console.log("      Buchen-Button aktiv:", !(await buchen.isDisabled()));
await buchen.click();
await page.waitForTimeout(2500);
await settle();
await pruefeUeberlauf("5 Bestätigungsseite");
await shot("5-bestaetigung");

const bestaetigt = (await page.locator(".elternsprechtag-view__confirm-success").count()) > 0;
console.log("      Bestätigungsbanner sichtbar:", bestaetigt);

await browser.close();

console.log(
  ueberlauf.length === 0
    ? "\nERGEBNIS: kein horizontaler Überlauf"
    : `\nERGEBNIS: Überlauf bei ${ueberlauf.join(", ")}`,
);
if (!bestaetigt || ueberlauf.length > 0) {
  process.exit(1);
}

/**
 * Beobachtbares Verhalten statt Umsetzung: läuft die Seite über den rechten Rand hinaus? Bei
 * einem Treffer werden die ersten Elemente genannt, die rechts aus dem Viewport ragen.
 */
async function pruefeUeberlauf(schritt) {
  const messung = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
    kandidaten: [...document.querySelectorAll("*")]
      .filter((el) => el.getBoundingClientRect().right > window.innerWidth + 1)
      .slice(0, 5)
      .map((el) => `${el.tagName.toLowerCase()}.${el.className || "-"}`.slice(0, 90)),
  }));
  const ok = messung.scrollWidth <= messung.clientWidth;
  console.log(
    `${ok ? "OK  " : "FAIL"} ${schritt}: scrollWidth=${messung.scrollWidth} clientWidth=${messung.clientWidth}`,
  );
  if (!ok) {
    console.log("      Kandidaten:", messung.kandidaten.join(" | "));
    ueberlauf.push(schritt);
  }
}

/** Vaadin baut die View erst nach dem Roundtrip auf; auf definierte Custom Elements warten. */
async function settle() {
  await page
    .waitForFunction(() => document.querySelectorAll("vaadin-button:not(:defined)").length === 0)
    .catch(() => {});
  await page.waitForTimeout(500);
}

async function shot(name) {
  await page.screenshot({ path: path.join(OUT_DIR, `buchung-375-${name}.png`), fullPage: true });
}
