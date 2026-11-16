# APK de Sakura Agent 0.6.0

APK debug ARM64 generada por GitHub Actions desde `main` (commit `38996b0`)
y publicada también en el release `v0.6.0`.

## Archivo

- `SakuraAgent-0.6.0-arm64-debug.apk`
- Paquete: `com.sakura.agent`
- Versión: `0.6.0` (versionCode 6)
- Tamaño: 73.250.013 bytes (~69,9 MiB)
- SHA-256: `6512ec74cae0f18d95c9ec02efbb60b0ae039be02e9160c08b449ca822e7cd96`

## Contenido

- Jardín Sakura original: `sakura_garden.webp`, `sakura_avatar.webp` y
  `sakura_blossoms.webp` empaquetados, más `GardenPage.kt`.
- 93 skills clasificadas con buscador, categorías y favoritos cifrados.
- Agente supervisado 0.6 con propuestas JSON revisables y una acción por confirmación.
- Chat, voz/TTS, memoria cifrada, GitHub, descargas, mensajes y APIs de texto.
- Motor llama.cpp para ARM64. No incluye pesos GGUF ni claves.

## Cómo instalar

1. Descarga el archivo `.apk` desde esta carpeta con el botón de descarga.
2. En Android, permite instalar apps de origen desconocido para el navegador o
   gestor de archivos que uses.
3. Abre el APK e instala. Requiere Android 13 o posterior y CPU ARM64.
4. Importa o descarga un modelo GGUF desde la app; no viene incluido.

## Notas

- Es una APK debug, no apta para Google Play.
- Si ya hay instalada otra build con firma debug distinta, Android puede rechazar
  la actualización. Exporta tus datos antes de desinstalar; desinstalar borra
  recuerdos, claves, favoritos y modelos privados.
- Verificación de CI: run `43757718044`, 86 tests JVM y `assembleDebug` en verde.
