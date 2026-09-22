// Belege für die Sammelaktion „Lehrkraft fällt aus" (#159): Abschnittskopf der Auswertung und
// der Dialog selbst, an den beiden Schwellen und an der Untergrenze 375 px.
//
//   node tools/screenshots/ausfall.mjs <sprechtagId> [suffix]
//
// ORGANIZER_PASSWORD muss in der Umgebung stehen (siehe README.md im selben Verzeichnis).
import { chromium } from "playwright-core";
import path from "node:path";

const sprechtagId = process.argv[2];
if (!sprechtagId) {
  console.error("Aufruf: node tools/screenshots/ausfall.mjs <sprechtagId> [suffix]");
  process.exit(1);
}
if (!process.env.ORGANIZER_PASSWORD) {
  console.error("ORGANIZER_PASSWORD ist nicht gesetzt.");
  process.exit(1);
}
const suffix = process.argv[3] ? `-${process.argv[3]}` : "";
const OUT = path.resolve("target/screenshots");

const browser = await chromium.launch({ channel: "chrome" });
const context = await browser.newContext({ viewport: { width: 375, height: 667 }, deviceScaleFactor: 2 });
const page = await context.newPage();
await page.goto("http://localhost:8080/login", { waitUntil: "networkidle" });
await page.fill('input[name="username"]', "user");
await page.fill('input[name="password"]', process.env.ORGANIZER_PASSWORD);
await page.press('input[name="password"]', "Enter");
await page.waitForURL((u) => !u.pathname.endsWith("/login"));
await page.waitForTimeout(1500);

for (const width of [375, 640, 1024]) {
  await page.setViewportSize({ width, height: width === 375 ? 667 : 900 });
  await page.goto(`http://localhost:8080/auswertung/${sprechtagId}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(1200);
  await page.screenshot({ path: path.join(OUT, `auswertung-ausfall-kopf-${width}${suffix}.png`), fullPage: true });

  await page.getByRole("button", { name: /fällt aus/i }).first().click();
  await page.waitForTimeout(800);
  await page.screenshot({ path: path.join(OUT, `dialog-ausfall-${width}${suffix}.png`) });
  console.log(`\n=== Dialog @ ${width}px`);
  console.log(JSON.stringify(await page.evaluate(mess), null, 2));

  // Und derselbe Dialog mit „alle auswählen": Dann steht die Vorschau auf ihren größten Zahlen.
  await page.getByRole("button", { name: /alle auswählen/i }).click();
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, `dialog-ausfall-alle-${width}${suffix}.png`) });
  console.log(JSON.stringify(await page.evaluate(mess), null, 2));
}
await browser.close();

function mess() {
  const box = (el) => {
    const r = el.getBoundingClientRect();
    return { l: Math.round(r.left), r: Math.round(r.right), w: Math.round(r.width), h: Math.round(r.height) };
  };
  const dlg = document.querySelector("vaadin-dialog-overlay[opened], vaadin-dialog[opened]");
  if (!dlg) return "kein Dialog offen";
  const alle = [...dlg.querySelectorAll("*")];
  const sichtbar = alle.filter((e) => e.getBoundingClientRect().width > 0);
  const vorschau = dlg.querySelector(".ausfall-dialog__vorschau");
  return {
    viewport: `${innerWidth}x${innerHeight}`,
    vorschau: vorschau ? { ...box(vorschau), text: vorschau.textContent } : null,
    zeilen: [...dlg.querySelectorAll(".ausfall-dialog__zeile")].slice(0, 4).map((z) => z.textContent.trim()),
    buttons: alle.filter((e) => e.tagName.toLowerCase() === "vaadin-button").map((b) => ({ t: b.textContent.trim(), ...box(b) })),
    ueberlauf: sichtbar
      .filter((e) => { const r = e.getBoundingClientRect(); return r.right > innerWidth + 1 || r.left < -1; })
      .slice(0, 10)
      .map((e) => `${e.tagName.toLowerCase()}.${e.className || "-"} ${JSON.stringify(box(e))}`),
    dokumentUeberlauf: document.documentElement.scrollWidth - document.documentElement.clientWidth,
  };
}
