# Bild-Assets

Statische Bilder, ausgeliefert unter `/images/...`.

## login-bg.webp

Hintergrundbild der Login-Seite (`LoginView`, `styles/login-view.css`).

| | |
|---|---|
| Motiv | Leeres Klassenzimmer mit Holztischen und Fenstern |
| Fotograf | 2y.kang ([@2ykang](https://unsplash.com/@2ykang)) |
| Quelle | https://unsplash.com/photos/dFohf_GUZJ0 |
| Lizenz | [Unsplash-Lizenz](https://unsplash.com/license) |
| Abgerufen am | 2026-09-04 |
| Bezogen als | `images.unsplash.com/photo-1635424239131-32dc44986b56?w=1200&h=1800&fit=crop&crop=center&fm=webp&q=55` |
| Maße / Größe | 1200 × 1800, WebP, 233 KB |

### Lizenzhinweis

**Dieses Bild steht nicht unter der Apache-2.0-Lizenz des übrigen Repositorys.**
Es wird unter der Unsplash-Lizenz genutzt: kostenlos, kommerziell verwendbar, keine
Namensnennung erforderlich. Nicht erlaubt ist, Unsplash-Fotos zu einem konkurrierenden
Bilddienst zusammenzustellen. Wer dieses Repository forkt, erwirbt am Bild keine
Apache-2.0-Rechte, sondern nutzt es ebenfalls unter der Unsplash-Lizenz.

Der Nachweis hier ist Sorgfaltsdokumentation, keine Lizenzauflage — deshalb steht er
neben der Datei und nicht in `NOTICE`. Ein `NOTICE`-Eintrag würde Nachnutzern eine
Weitergabepflicht auferlegen, die die Unsplash-Lizenz gar nicht verlangt.

### Wenn das Bild ausgetauscht wird

`styles/login-view.css` setzt Eigenschaften dieses Motivs voraus:

- **Hochformat.** Das Bild füllt auf dem Desktop die linke 60-%-Spalte (Verhältnis ~1,15)
  und unter 1024 px die volle Fläche (auf 375 × 667 px Verhältnis ~0,56). Ein
  Querformat-Motiv verliert in beiden Fällen zu viel.
- **Dunkler unterer Bildbereich.** Der Markentext unten links ist hell gesetzt und
  bekommt nur einen schwachen Verlauf. Bei einem unten hellen Motiv wird er unlesbar —
  dann muss der Verlauf in `.login-view__media::after` kräftiger werden oder der Text
  auf dunkle Schrift wechseln.
- **Ruhiger unterer Bildbereich.** Feine Muster (Text, Raster, Handschrift) unter dem
  Markentext machen ihn unruhig, auch bei ausreichendem Kontrast.

EXIF-Daten bitte vor dem Committen entfernen — das Repository ist öffentlich, und Fotos
tragen oft GPS-Koordinaten. Die hier abgelegte Datei enthält keine (vom Unsplash-CDN
bereits bereinigt: kein `EXIF`- und kein `XMP`-Chunk).
