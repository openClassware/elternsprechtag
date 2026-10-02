#!/usr/bin/env node
/**
 * Einmal-Skript für Issue #121: die Namenssuche der Auswertung bei 375, 768 und 1280 px — mit
 * Treffern, ohne Treffer und mit dem Hinweis auf ausgeblendete stornierte Treffer. Prüft je
 * Aufnahme, dass die Seite nicht seitlich überläuft.
 *
 * Erwartet die Demo-Daten (Profil `demo`) und schreibt ein Storno in die Datenbank — also nur gegen
 * eine lokale Umgebung laufen lassen. Passwort ausschließlich aus ORGANIZER_PASSWORD.
 *
 * Aufruf (App muss laufen), aus dem Projekt-Root:
 *   node tools/screenshots/namenssuche.mjs
 */

import { chromium } from "playwright-core";
import { mkdir } from "node:fs/promises";
import path from "node:path";

const OUT_DIR = path.resolve(import.meta.dirname, "../../target/screenshots");
const baseUrl = (process.env.BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");
const username = process.env.ORGANIZER_USERNAME ?? "user";
const password = process.env.ORGANIZER_PASSWORD;
// Der aktive Demo-Sprechtag (db/demo/R__demo_stammdaten.sql).
const AUSWERTUNG = `${baseUrl}/auswertung/00000000-0000-0000-0005-000000000001`;
const WIDTHS = [375, 768, 1280];

if (!password) {
  console.error("ORGANIZER_PASSWORD ist nicht gesetzt.");
  process.exit(1);
}

let ueberlauf = false;
const browser = await chromium.launch({ channel: "chrome" });
try {
  const context = await browser.newContext({
    viewport: { width: WIDTHS[0], height: 900 },
    deviceScaleFactor: 2,
  });
  const page = await context.newPage();

  await page.goto(`${baseUrl}/login`, { waitUntil: "networkidle" });
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await page.press('input[name="password"]', "Enter");
  await page.waitForURL((url) => !url.pathname.endsWith("/login"), { timeout: 15_000 });
  await mkdir(OUT_DIR, { recursive: true });

  // 1. Treffer: Familie Neumann hat zwei Termine bei zwei Lehrkräften.
  for (const width of WIDTHS) {
    await oeffne(page, width);
    await suche(page, "neumann");
    await shot(page, `namenssuche-treffer-${width}`);
  }

  // 2. Kein Treffer.
  await oeffne(page, 375);
  await suche(page, "Meierhofer");
  await shot(page, "namenssuche-leer-375");

  // 3. Hinweis auf stornierte Treffer: eine Buchung von Lena Neumann stornieren, Schalter bleibt aus.
  await oeffne(page, 1280);
  await suche(page, "neumann");
  await page.locator(".auswertung__storno").first().click();
  await page.getByRole("dialog").getByRole("button", { name: "Stornieren", exact: true }).click();
  await settle(page);
  for (const width of WIDTHS) {
    await page.setViewportSize({ width, height: 900 });
    await settle(page);
    await shot(page, `namenssuche-stornierte-${width}`);
  }
  // Der Button im Hinweis schaltet die Stornierten ein.
  await page.locator(".auswertung__suchhinweis-button").click();
  await settle(page);
  await shot(page, "namenssuche-stornierte-eingeblendet-1280");
} finally {
  await browser.close();
}
process.exit(ueberlauf ? 1 : 0);

async function oeffne(page, width) {
  await page.setViewportSize({ width, height: 900 });
  await page.goto(AUSWERTUNG, { waitUntil: "networkidle" });
  await settle(page);
}

async function suche(page, begriff) {
  await page.locator(".auswertung__suche input").fill(begriff);
  // ValueChangeMode.LAZY wartet 400 ms nach dem letzten Tastendruck.
  await page.waitForTimeout(900);
  await settle(page);
}

async function shot(page, name) {
  const file = path.join(OUT_DIR, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  const breit = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
  if (breit) {
    ueberlauf = true;
  }
  console.log(`  -> ${file}${breit ? "  (ÜBERLAUF!)" : ""}`);
}

async function settle(page) {
  await page
    .waitForFunction(() => document.querySelectorAll("vaadin-button:not(:defined)").length === 0)
    .catch(() => {});
  await page.waitForTimeout(500);
}
