# Sakura Agent 0.12.0 — chat primero, supervisión siempre

Esta versión amplía lo que puedes pedir **desde el chat**, sin convertir a Sakura en un
agente autónomo. No se ha añadido otra mascota: sigue siendo la misma Sakura vectorial.

## 1. Búsqueda real desde el chat

- Sin clave API: **Wikipedia en español** primero y **DuckDuckGo Lite** como respaldo.
- Con clave Brave ya configurada, se mantiene Brave.
- Los resultados se insertan como **texto no confiable**. Sakura no abre enlaces por su
  cuenta ni convierte páginas en instrucciones.
- Cualquier apertura de enlace, descarga o envío sigue pasando por el panel de revisión.

## 2. Enrutamiento determinista y skills automáticas

`ChatRouter` detecta la intención y propone **una acción exacta**:

- búsqueda («busca documentación de Kotlin»);
- lectura GitHub («lee README.md de Hisoka111373/sakura-agent»);
- reemplazo GitHub («cambia Sakura por Sakura Agent en README.md de …»);
- descarga del catálogo («descarga whisper tiny», «descarga el modelo sd turbo»);
- recuerdo local («recuerda que …»);
- apertura permitida («abre YouTube»).

La detección **propone**, nunca ejecuta. Puedes desactivarla con el interruptor
**«Detectar herramientas desde el chat»** (`chatAutoTools`). Las acciones siguen
mostrando vista previa y requieren tu confirmación.

## 3. Multimedia adjunta, leída en el teléfono

- Botón de clip en el chat, hasta **4 archivos por mensaje**.
- Sakura extrae localmente metadata/EXIF, OCR de imágenes y de hasta 3 fotogramas de
  vídeo, texto de PDF/DOCX y metadatos de audio.
- El audio **no se transcribe**: no hay motor Whisper incluido en esta APK.
- No se sube nada por defecto. Si el chat está en modo nube, el texto extraído se
  muestra antes de enviarse y requiere confirmación.

## 4. Más modelos verificados

- Checkpoints de imagen: Openjourney, SSD-1B y SD Turbo.
- Vídeo: LTX 0.9.6, LTX 0.9.8 y upscalers espacial/temporal.
- Audio: Whisper Tiny, Tiny English Q5 y Base (ggml).
- Tamaños, revisiones y SHA-256 tomados de metadatos públicos de Hugging Face.
- Límite visual ampliado a **16 GB**; archivos sueltos siguen en **512 MiB** y GGUF
  personalizados en 2,5 GB.

Los modelos visuales y Whisper **solo se descargan y verifican**. Sakura no incluye
motores de difusión ni de transcripción para ejecutarlos.

## 5. Programación GitHub supervisada

Desde el chat o desde el agente se pueden proponer lectura de archivo, árbol, issues,
reemplazo exacto, reemplazo de archivo completo, creación, borrado, rama y pull request.
`github_edit`/`github_write` muestran ANTES/DESPUÉS; el resto muestra la acción exacta.
Se recomienda trabajar en rama. Ninguna escritura se envía sin confirmación humana.

## 6. Límites y verificaciones

- APK **debug ARM64**, `minSdk 33` (Android 13+), versionCode 14.
- CI ejecuta pruebas JVM, compila la APK y verifica la optimización nativa antes de
  publicarla en `apk/`.
- No hay prueba física: no se afirma instalación, rendimiento, precisión de OCR ni
  velocidad real en un dispositivo concreto.
- Las respuestas en nube pueden tener coste; los tokens mostrados no son un límite
  monetario.

## Descarga

- Archivo estable: https://github.com/Hisoka111373/sakura-agent/raw/main/apk/sakura.apk
- Release con asset: https://github.com/Hisoka111373/sakura-agent/releases/download/v0.12.0/SakuraAgent-0.12.0-arm64-debug.apk
- Datos reales de build: `apk/BUILD-0.12.0.md` y `apk/SHA256SUMS.txt`.
