#!/usr/bin/env node
/**
 * Screenshots der laufenden App bei mehreren Viewport-Breiten.
 *
 * Das Passwort kommt ausschließlich aus der Umgebung (ORGANIZER_PASSWORD) und wird nie
 * geloggt — so kann das Skript auch von jemandem aufgerufen werden, der die Zugangsdaten
 * nicht kennen oder sehen soll.
 *
 * Aufruf (App muss laufen):
 *   ORGANIZER_PASSWORD=... node screenshot.mjs /sprechtage 375 1024 1440
 *
 * Argumente in beliebiger Reihenfolge: alles mit "/" am Anfang ist eine Route, alles
 * Numerische eine Breite. Ohne Breitenangabe greifen die Projekt-Breakpoints 375/1024/1440.
 */

import { chromium } from "playwright-core";
import { mkdir } from "node:fs/promises";
import path from "node:path";

const DEFAULT_WIDTHS = [375, 1024, 1440];
const VIEWPORT_HEIGHT = 900;
const OUT_DIR = path.resolve(import.meta.dirname, "../../target/screenshots");

const baseUrl = (process.env.BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");
const username = process.env.ORGANIZER_USERNAME ?? "user";
const password = process.env.ORGANIZER_PASSWORD;

const args = process.argv.slice(2);
const routes = args.filter((arg) => arg.startsWith("/"));
const widths = args.filter((arg) => /^\d+$/.test(arg)).map(Number);
const unknown = args.filter((arg) => !arg.startsWith("/") && !/^\d+$/.test(arg));

if (unknown.length > 0) {
  fail(`Unbekannte Argumente: ${unknown.join(", ")}. Routen beginnen mit "/", Breiten sind Zahlen.`);
}
if (routes.length === 0) {
  fail('Keine Route angegeben. Beispiel: node screenshot.mjs /sprechtage 375 1024');
}

/** Login-geschützte Routen brauchen das Passwort; die öffentliche Elternseite nicht. */
const needsLogin = routes.some((route) => !route.startsWith("/elternsprechtag"));
if (needsLogin && !password) {
  fail(
    "ORGANIZER_PASSWORD ist nicht gesetzt — die Organizer-Routen sind ohne Anmeldung nicht erreichbar.\n" +
      "PowerShell:  $env:ORGANIZER_PASSWORD = '...'\n" +
      "bash:        export ORGANIZER_PASSWORD=...",
  );
}

const browser = await chromium.launch({ channel: "chrome" });
try {
  // Ein Context für alle Aufnahmen: einmal anmelden, Session bleibt über alle Breiten
  // erhalten. Der Viewport wird pro Breite umgesetzt, statt jedes Mal neu zu laden.
  const context = await browser.newContext({
    viewport: { width: widths[0] ?? DEFAULT_WIDTHS[0], height: VIEWPORT_HEIGHT },
    deviceScaleFactor: 2,
  });
  const page = await context.newPage();

  if (needsLogin) {
    await login(page);
  }

  await mkdir(OUT_DIR, { recursive: true });
  for (const route of routes) {
    for (const width of widths.length > 0 ? widths : DEFAULT_WIDTHS) {
      await page.setViewportSize({ width, height: VIEWPORT_HEIGHT });
      await page.goto(baseUrl + route, { waitUntil: "networkidle" });
      await settle(page);

      const file = path.join(OUT_DIR, `${slug(route)}-${width}px.png`);
      await page.screenshot({ path: file, fullPage: true });
      console.log(`${width}px  ${route}  ->  ${file}`);
    }
  }
} finally {
  await browser.close();
}

async function login(page) {
  await page.goto(`${baseUrl}/login`, { waitUntil: "networkidle" });
  // Vaadins <vaadin-login-form> kapselt die Felder im Shadow DOM; Playwrights
  // CSS-Selektoren durchdringen offene Shadow Roots, daher genügt input[name=...].
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await page.press('input[name="password"]', "Enter");
  await page.waitForURL((url) => !url.pathname.endsWith("/login"), { timeout: 15_000 }).catch(() => {
    fail("Anmeldung fehlgeschlagen: die App blieb auf /login. Stimmen ORGANIZER_USERNAME/ORGANIZER_PASSWORD?");
  });
  await settle(page);
}

/**
 * Vaadin baut die View erst nach dem ersten Roundtrip auf; networkidle allein greift also zu
 * früh. Warten, bis die Custom Elements definiert sind, plus ein kurzer Puffer für Layout.
 */
async function settle(page) {
  await page
    .waitForFunction(() => document.querySelectorAll("vaadin-button:not(:defined)").length === 0)
    .catch(() => {});
  await page.waitForTimeout(400);
}

function slug(route) {
  const cleaned = route.replace(/^\/+|\/+$/g, "").replace(/[^\w.-]+/g, "-");
  return cleaned === "" ? "root" : cleaned;
}

function fail(message) {
  console.error(message);
  process.exit(1);
}
