"""Capturas reales del informe de modernización y del informe de cobertura (Playwright).

Requisitos: ./mvnw verify  y  java -cp target/classes cl.valdora.modernizacion.herramientas.Rendimiento docs <n.° de pruebas>
Uso: python scripts/capturas.py   (variable opcional PW_CHANNEL=msedge)
"""
import os
import pathlib

from playwright.sync_api import sync_playwright

RAIZ = pathlib.Path(__file__).resolve().parents[1]
SALIDA = RAIZ / "docs" / "capturas"


def main():
    SALIDA.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        navegador = p.chromium.launch(channel=os.environ.get("PW_CHANNEL") or None)
        pagina = navegador.new_page(viewport={"width": 1280, "height": 900})
        pagina.goto((RAIZ / "docs" / "informe" / "index.html").as_uri())
        pagina.screenshot(path=SALIDA / "01-informe-modernizacion.png", full_page=True)
        cobertura = RAIZ / "target" / "site" / "jacoco" / "index.html"
        if cobertura.exists():
            pagina.goto(cobertura.as_uri())
            pagina.screenshot(path=SALIDA / "02-cobertura-jacoco.png", full_page=True)
        movil = navegador.new_page(viewport={"width": 390, "height": 844})
        movil.goto((RAIZ / "docs" / "informe" / "index.html").as_uri())
        movil.screenshot(path=SALIDA / "03-informe-movil.png", full_page=True)
        navegador.close()
    print("Capturas generadas en", SALIDA)


if __name__ == "__main__":
    main()
