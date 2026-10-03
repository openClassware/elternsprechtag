#!/usr/bin/env node
/**
 * Einmal-Skript für Issue #111: die zweite Eingabe „E-Mail-Adresse wiederholen" der Eltern-Ansicht
 * bei 375, 1024 und 1440 px — leer, mit Buchstabendreher nach dem Verlassen des Felds und
 * übereinstimmend. Prüft je Aufnahme Feldfehler, Footer-Text und Buchen-Button sowie, dass die
 * Seite nicht seitlich überläuft.
 *
 * Erwartet die Demo-Daten (Profil `demo`, Elternlink `demo-aktiv`). Schickt nichts ab, schreibt
 * also nichts in die Datenbank. Die Route ist öffentlich, es wird kein Passwort gebraucht.
 *
 * Aufruf (App muss laufen), aus dem Projekt-Root:
 *   node tools/screenshots/email-wiederholung.mjs
 */

import { chromium } from "playwright-core";
import { mkdir } from "node:fs/promises";
import path from "node:path";

const OUT_DIR = path.resolve(import.meta.dirname, "../../target/screenshots");
const baseUrl = (process.env.BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");
const ELTERNLINK = `${baseUrl}/elternsprechtag/demo-aktiv`;
const WIDTHS = [375, 1024, 1440];

const fehler = [];
const browser = await chromium.launch({ channel: "chrome" });
try {
  await mkdir(OUT_DIR, { recursive: true });
  for (const width of WIDTHS) {
    const context = await browser.newContext({
      viewport: { width, height: width === 375 ? 667 : 900 },
      deviceScaleFactor: 2,
    });
    const page = await context.newPage();
    await page.goto(ELTERNLINK, { waitUntil: "networkidle" });
    await settle(page);

    const email = page.locator("vaadin-email-field").nth(0);
    const wiederholung = page.locator("vaadin-email-field").nth(1);

    // Angaben und ein Termin — erst mit einer Auswahl nennt der Footer seine Blocker.
    await page.locator("vaadin-text-field input").nth(0).fill("Anna Müller");
    await page.locator("vaadin-text-field input").nth(1).fill("Lukas Müller");
    await email.locator("input").fill("anna.mueller@example.com");
    await page.locator("vaadin-select").click();
    await page.waitForTimeout(400);
    await page.locator("vaadin-select-item").first().click();
    await settle(page);
    await page.locator(".elternsprechtag-view__lehrkraft").first().click();
    await settle(page);
    await page
      .locator(
        ".elternsprechtag-view__slot:not(.elternsprechtag-view__slot--belegt):not(.elternsprechtag-view__slot--konflikt)",
      )
      .first()
      .click();
    await settle(page);

    await pruefe(page, width, "leer", { feldfehler: false, buttonAktiv: false });

    // Buchstabendreher: während des Tippens schweigt das Feld, nach dem Verlassen meldet es.
    await wiederholung.locator("input").fill("anna.meuller@example.com");
    await settle(page);
    await pruefe(page, width, "tippt", { feldfehler: false, buttonAktiv: false });
    await wiederholung.locator("input").blur();
    await settle(page);
    await pruefe(page, width, "abweichend", { feldfehler: true, buttonAktiv: false });

    // Korrigiert, mit anderer Schreibung und Randleerzeichen: gilt als gleich.
    await wiederholung.locator("input").fill(" Anna.Mueller@example.com ");
    await settle(page);
    await pruefe(page, width, "gleich", { feldfehler: false, buttonAktiv: true });

    await context.close();
  }
} finally {
  await browser.close();
}

console.log(fehler.length === 0 ? "\nERGEBNIS: alles wie erwartet" : `\nERGEBNIS: ${fehler.join(", ")}`);
if (fehler.length > 0) {
  process.exit(1);
}

async function pruefe(page, width, zustand, erwartet) {
  const schritt = `${width}-${zustand}`;
  const wiederholung = page.locator("vaadin-email-field").nth(1);
  const feldfehler = await wiederholung.evaluate((el) => el.hasAttribute("invalid"));
  const buttonAktiv = !(await page
    .locator(".elternsprechtag-view__footer vaadin-button")
    .first()
    .isDisabled());
  const footer = (await page.locator(".elternsprechtag-view__footer-status").textContent()).trim();
  const breite = await page.evaluate(() => ({
    scroll: document.documentElement.scrollWidth,
    client: document.documentElement.clientWidth,
  }));

  const ok =
    feldfehler === erwartet.feldfehler &&
    buttonAktiv === erwartet.buttonAktiv &&
    breite.scroll <= breite.client;
  console.log(
    `${ok ? "OK  " : "FAIL"} ${schritt}: Feldfehler=${feldfehler} Button=${buttonAktiv} ` +
      `Überlauf=${breite.scroll > breite.client} Footer="${footer}"`,
  );
  if (!ok) {
    fehler.push(schritt);
  }
  // Ausschnitt um das Formular — die ganze Seite wäre bei geöffnetem Terminraster zu lang.
  await page
    .locator(".elternsprechtag-view__form")
    .screenshot({ path: path.join(OUT_DIR, `email-wiederholung-${schritt}.png`) });
  await page.screenshot({ path: path.join(OUT_DIR, `email-wiederholung-${schritt}-seite.png`) });
}

/** Vaadin baut die View erst nach dem Roundtrip auf; auf definierte Custom Elements warten. */
async function settle(page) {
  await page
    .waitForFunction(() => document.querySelectorAll("vaadin-button:not(:defined)").length === 0)
    .catch(() => {});
  await page.waitForTimeout(500);
}
