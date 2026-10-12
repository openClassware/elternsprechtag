# Verpackt die gebaute Doku-Site als Image `ghcr.io/openclassware/elternsprechtag-docs`.
#
# Das Image ist nur ein Transportbehälter: kein Webserver, keine Shell, nur die statischen Dateien
# unter `/site`. Wie man sie herausholt, steht in `docs/deploy.md` unter „Doku-Image“.
#
# Build-Kontext ist das fertige `site/dist`, nicht `site/` — so landet weder `node_modules` noch
# eine Quelle im Kontext:
#
#   docker build -f site/Dockerfile site/dist
FROM scratch

# Die Quelle verknüpft das GHCR-Paket mit dem Repository.
LABEL org.opencontainers.image.source="https://github.com/openClassware/elternsprechtag" \
      org.opencontainers.image.description="Statische Dateien der Anwender-Doku unter /site" \
      org.opencontainers.image.licenses="Apache-2.0"

COPY . /site/

# `docker create` verweigert ein Image ohne Kommando. Ausgeführt wird es nie — es gibt im Image
# nichts Ausführbares —, es macht nur `docker create` ohne weiteres Argument möglich.
CMD ["/site/index.html"]
