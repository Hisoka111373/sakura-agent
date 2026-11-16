# APK de Sakura Agent 0.7.0

APK debug ARM64 generada por GitHub Actions desde la rama
`feature/sakura-0.7-auto-model` (commit `2dd08fc`) y publicada solo en esta
carpeta. **No hay release 0.7.**

## Archivo

- `SakuraAgent-0.7.0-arm64-debug.apk`
- Paquete: `com.sakura.agent`
- Versión: `0.7.0` (versionCode 7)
- Tamaño: 73.430.237 bytes (~70,0 MiB)
- SHA-256: `fde0f724c81649edae1fc4a2500f09bead4af0b93cf6df6991cd0318efa88599`

La APK 0.6.0 sigue en esta carpeta sin cambios.

## Contenido

- Jardín Sakura original: `sakura_garden.webp`, `sakura_avatar.webp` y
  `sakura_blossoms.webp` empaquetados, con mascota animada y pétalos Compose.
- 93 skills clasificadas con buscador, categorías y favoritos cifrados.
- **Modelo bajo demanda:** en el primer inicio descarga Qwen2.5 1.5B Instruct
  Q4_K_M (~1,1 GB), verifica SHA-256 y lo carga. No incluye pesos GGUF.
- Voz automática opcional, saludo hablado y lectura de mensajes con TTS local.
- Pestaña Modelos visible con starter, catálogo filtrable y modelos instalados.
- APIs con modelo sugerido, clave cifrada, mostrar/ocultar y Probar conexión.
- Búsqueda desde el chat («busca …») con Brave o apertura externa, siempre con
  confirmación.
- Agente supervisado 0.6, chat, memoria cifrada, GitHub, descargas, mensajes y
  motor llama.cpp ARM64. No incluye claves.

## Cómo instalar

1. Descarga `SakuraAgent-0.7.0-arm64-debug.apk` desde esta carpeta.
2. En Android, permite instalar apps de origen desconocido para el navegador o
   gestor de archivos que uses.
3. Abre el APK e instala. Requiere Android 13 o posterior y CPU ARM64.
4. Abre Sakura con Wi-Fi. La app descargará el modelo inicial, lo verificará y
   lo cargará. Puedes cancelar o reintentar desde Jardín, Chat o Modelos.
5. Si ya tenías 0.6, actualizar conserva datos solo si la firma debug coincide.
   Antes de desinstalar, exporta recuerdos, claves y favoritos.

## Notas

- Primera descarga: ~1,1 GB. Reserva ~2,2 GB libres durante el proceso.
- Recomendado 8 GB de RAM, pero no es una garantía; el GGUF no es la RAM usada.
- Es una APK debug, no apta para Google Play y sin firma de tienda.
- Verificación de CI: run `43806166323`, 92 tests JVM y `assembleDebug` en verde.
- La descarga va a Hugging Face. No se envían chat, recuerdos, archivos ni claves.
