import { chromium } from "playwright-core";
import path from "node:path";

const suffix = process.argv[2] ? `-${process.argv[2]}` : "";
const OUT = path.resolve("target/screenshots");

const browser = await chromium.launch({ channel: "chrome" });
const context = await browser.newContext({
  viewport: { width: 375, height: 667 },
  deviceScaleFactor: 2,
  permissions: ["clipboard-read", "clipboard-write"],
});
const page = await context.newPage();
await page.goto("http://localhost:8080/login", { waitUntil: "networkidle" });
await page.fill('input[name="username"]', "user");
await page.fill('input[name="password"]', process.env.ORGANIZER_PASSWORD);
await page.press('input[name="password"]', "Enter");
await page.waitForURL((u) => !u.pathname.endsWith("/login"));
await page.waitForTimeout(1500);

for (const width of [375, 640, 1024]) {
  await page.setViewportSize({ width, height: width === 375 ? 667 : 900 });
  for (const [name, pattern] of [["share", /link|teilen/i], ["cancel", /absagen/i]]) {
    await page.goto("http://localhost:8080/sprechtage", { waitUntil: "networkidle" });
    await page.waitForTimeout(1200);
    await openMenuItem(page, pattern);
    const file = path.join(OUT, `dialog-${name}-${width}${suffix}.png`);
    await page.screenshot({ path: file });
    console.log(`\n=== ${name} @ ${width}px -> ${file}`);
    console.log(JSON.stringify(await page.evaluate(mess), null, 2));

    if (name === "share" && width === 375) {
      await page.getByRole("button", { name: /kopieren/i }).click();
      await page.waitForTimeout(600);
      const inhalt = await page.evaluate(() => navigator.clipboard.readText());
      console.log("Zwischenablage nach Klick:", JSON.stringify(inhalt));
    }
  }
}
await browser.close();

function mess() {
  const box = (el) => {
    const r = el.getBoundingClientRect();
    return { l: Math.round(r.left), r: Math.round(r.right), t: Math.round(r.top), b: Math.round(r.bottom), w: Math.round(r.width), h: Math.round(r.height) };
  };
  const dlg = document.querySelector("vaadin-dialog[opened]");
  if (!dlg) return "kein Dialog offen";
  const alle = [...dlg.querySelectorAll("*")];
  const sichtbar = alle.filter((e) => e.getBoundingClientRect().width > 0);
  const linkBox = dlg.querySelector(".share-link-dialog__link");
  return {
    viewport: `${innerWidth}x${innerHeight}`,
    link: linkBox ? { ...box(linkBox), scrollWidth: linkBox.scrollWidth, clientWidth: linkBox.clientWidth, text: linkBox.textContent } : null,
    buttons: alle.filter((e) => e.tagName.toLowerCase() === "vaadin-button").map((b) => ({ t: b.textContent.trim(), ...box(b) })),
    ueberlauf: sichtbar
      .filter((e) => { const r = e.getBoundingClientRect(); return r.right > innerWidth + 1 || r.bottom > innerHeight + 1 || r.left < -1 || r.top < -1; })
      .slice(0, 10)
      .map((e) => `${e.tagName.toLowerCase()}.${e.className || "-"} ${JSON.stringify(box(e))}`),
    dokumentUeberlauf: document.documentElement.scrollWidth - document.documentElement.clientWidth,
  };
}

async function openMenuItem(page, pattern) {
  const menus = page.locator(".sprechtag-table__menu");
  const count = await menus.count();
  for (let i = 0; i < count; i++) {
    await menus.nth(i).click();
    await page.waitForTimeout(400);
    const item = page.locator("vaadin-context-menu-item").filter({ hasText: pattern });
    if ((await item.count()) > 0) {
      await item.first().click();
      await page.waitForTimeout(1000);
      return;
    }
    await page.keyboard.press("Escape");
    await page.waitForTimeout(300);
  }
  throw new Error(`Kein Menüeintrag für ${pattern}`);
}
