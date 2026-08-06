#!/usr/bin/env node
/**
 * Einmal-Skript für Issue 19/20: Formular und die beiden Dialoge bei 375 px aufnehmen.
 * Passwort ausschließlich aus ORGANIZER_PASSWORD, wird nie ausgegeben.
 *
 * Aufruf (App muss laufen), aus dem Projekt-Root:
 *   node <dieses-skript> [suffix]
 */

import { chromium } from "playwright-core";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

const OUT_DIR = path.resolve("target/screenshots");
const baseUrl = (process.env.BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");
const username = process.env.ORGANIZER_USERNAME ?? "user";
const password = process.env.ORGANIZER_PASSWORD;
const suffix = process.argv[2] ? `-${process.argv[2]}` : "";

if (!password) {
  console.error("ORGANIZER_PASSWORD ist nicht gesetzt.");
  process.exit(1);
}

const browser = await chromium.launch({ channel: "chrome" });
try {
  const context = await browser.newContext({
    viewport: { width: 375, height: 900 },
    deviceScaleFactor: 2,
    permissions: ["clipboard-read", "clipboard-write"],
  });
  const page = await context.newPage();

  await page.goto(`${baseUrl}/login`, { waitUntil: "networkidle" });
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await page.press('input[name="password"]', "Enter");
  await page.waitForURL((url) => !url.pathname.endsWith("/login"), { timeout: 15_000 });
  await settle(page);

  await mkdir(OUT_DIR, { recursive: true });

  // --- Issue 19: Formular ---
  await page.goto(`${baseUrl}/sprechtag`, { waitUntil: "networkidle" });
  await settle(page);
  await shot(page, `formular${suffix}`, true);
  console.log("Überlauf Formular:", await overflow(page));
  await writeFile(
    path.join(OUT_DIR, `formular-dom${suffix}.html`),
    await page.evaluate(() => document.querySelector(".edit-sprechtag-view")?.outerHTML ?? ""),
  );

  // Validierungsfehler erzwingen: leer speichern
  await page.getByRole("button", { name: /anlegen|speichern/i }).first().click();
  await page.waitForTimeout(600);
  await shot(page, `formular-fehler${suffix}`, true);
  console.log("Überlauf Formular (Fehler):", await overflow(page));

  // Datepicker öffnen
  await page.goto(`${baseUrl}/sprechtag`, { waitUntil: "networkidle" });
  await settle(page);
  await page.locator("vaadin-date-picker").first().click();
  await page.waitForTimeout(800);
  await shot(page, `formular-datepicker${suffix}`, false);

  // --- Issue 20: Dialoge ---
  await page.goto(`${baseUrl}/sprechtage`, { waitUntil: "networkidle" });
  await settle(page);
  await openMenuItem(page, /link|teilen/i);
  await shot(page, `dialog-share${suffix}`, false);
  console.log("Überlauf Share-Dialog:", await dialogOverflow(page));
  await writeFile(
    path.join(OUT_DIR, `dialog-share-dom${suffix}.html`),
    await page.evaluate(() => document.querySelector("vaadin-dialog-overlay")?.outerHTML ?? ""),
  );

  await page.goto(`${baseUrl}/sprechtage`, { waitUntil: "networkidle" });
  await settle(page);
  await openMenuItem(page, /absagen/i);
  await shot(page, `dialog-cancel${suffix}`, false);
  console.log("Überlauf Absage-Dialog:", await dialogOverflow(page));
} finally {
  await browser.close();
}

/** Öffnet das Zeilenmenü der ersten Zeile, die den passenden Eintrag anbietet. */
async function openMenuItem(page, pattern) {
  const menus = page.locator(".sprechtag-table__menu");
  const count = await menus.count();
  for (let i = 0; i < count; i++) {
    await menus.nth(i).click();
    await page.waitForTimeout(400);
    const item = page.locator("vaadin-context-menu-item").filter({ hasText: pattern });
    if ((await item.count()) > 0) {
      await item.first().click();
      await page.waitForTimeout(800);
      return;
    }
    await page.keyboard.press("Escape");
    await page.waitForTimeout(300);
  }
  throw new Error(`Kein Menüeintrag für ${pattern} gefunden`);
}

async function shot(page, name, fullPage) {
  const file = path.join(OUT_DIR, `${name}.png`);
  await page.screenshot({ path: file, fullPage });
  console.log(`  -> ${file}`);
}

/** Waagerechter Überlauf des Dokuments. */
function overflow(page) {
  return page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
    culprits: [...document.querySelectorAll("*")]
      .filter((el) => el.getBoundingClientRect().right > window.innerWidth + 1)
      .slice(0, 12)
      .map((el) => `${el.tagName.toLowerCase()}.${el.className || "-"} right=${Math.round(el.getBoundingClientRect().right)}`),
  }));
}

/** Maße des offenen Overlays gegen den Viewport. */
function dialogOverflow(page) {
  return page.evaluate(() => {
    const overlay = document.querySelector("vaadin-dialog-overlay");
    if (!overlay) return "kein Overlay gefunden";
    const r = overlay.getBoundingClientRect();
    const inner = overlay.shadowRoot?.querySelector('[part="overlay"]');
    const ir = inner?.getBoundingClientRect();
    return {
      viewport: `${window.innerWidth}x${window.innerHeight}`,
      overlay: `left=${Math.round(r.left)} right=${Math.round(r.right)} top=${Math.round(r.top)} bottom=${Math.round(r.bottom)}`,
      inner: ir ? `left=${Math.round(ir.left)} right=${Math.round(ir.right)} top=${Math.round(ir.top)} bottom=${Math.round(ir.bottom)}` : "-",
      contentScrollWidth: inner ? inner.scrollWidth : null,
    };
  });
}

async function settle(page) {
  await page
    .waitForFunction(() => document.querySelectorAll("vaadin-button:not(:defined)").length === 0)
    .catch(() => {});
  await page.waitForTimeout(500);
}
