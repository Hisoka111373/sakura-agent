# Mejoras añadidas (rama `feature/revision-2026-11-25`)

Fecha: 2026-11-25. No se borró ningún APK ni se reescribió el historial.

## Velocidad y reproducibilidad

- **Gradle Wrapper 8.9** (`gradlew`, `gradle/wrapper/`). Antes hacía falta instalar Gradle a mano.
- `scripts/check-policy.sh` ahora incluye `Sakura012Test`, mascotas, autopilot, visión y catálogo multimedia; antes omitía `Sakura012Test`.
- Los números de tests fijos en documentación se sustituyeron por «los muestra GitHub Actions», porque cambiaban con cada versión.
- La CI publica la APK como **GitHub Release** en lugar de hacer `git push` a `main`. Se elimina el riesgo de bucle y el job ya no necesita escribir en la rama.

## Mascota (offline, sin dependencias nuevas)

Nuevos gestos deterministas y comprobables en JVM: **Corazón** (pulso), **Dormir** (ojos cerrados + “ZZZ”), **Celebrar** (lluvia de pétalos) y **Girar** (giro suave), además del parpadeo, mirada y saludo existentes. Se activan desde la tarjeta de Sakura o desde Opciones del chat. Se pueden desactivar con el interruptor de movimiento.

## Piloto automático supervisado

`AutopilotPolicy.kt` clasifica cada acción como LOW / MEDIUM / HIGH. Solo se ejecuta sin diálogo una acción **LOW conocida** (listar modelos, listar/leer archivos, leer GitHub) y solo si el interruptor «Piloto automático» está activo en Privacidad. Red, descargas, escrituras, mensajes, instalaciones y generación con coste siguen exigiendo confirmación. Las acciones desconocidas se tratan como HIGH.

## PDF y visión

- `PdfExporter.kt` crea PDF de texto con `android.graphics.pdf.PdfDocument` (sin librerías nuevas). Botón «Exportar chat a PDF» en Privacidad: Android SAF elige el destino; Sakura no sobrescribe archivos.
- `VisionClient.kt` + `VisionPrompt.kt`: descripción de una imagen adjunta con Gemini usando la clave `gemini_key` guardada cifrada. La imagen viaja **solo tras confirmación**; el texto de la imagen se marca explícitamente como DATOS no confiables, no instrucciones. Límite 8 MiB, sin reintentos ni redirecciones.

## Modelos de imagen y vídeo

`MediaModels.kt` documenta con honestidad lo que existe hoy:

- **En línea con clave del usuario:** FLUX schnell (imagen) y LTX Video 0.9.1 (vídeo) en fal.ai; Gemini 2.0 Flash para entender imágenes.
- **Local:** el puente JNI actual es llama.cpp (texto). El catálogo local verificado por SHA-256 incluye Qwen2.5 0.5B Q4_K_M como opción rápida. **No se publican hashes inventados** de modelos de difusión/vídeo: añadirlos exigiría un motor nativo nuevo y una verificación real.

> No existe un «modelo propio» ni una clave API de Sakura. Cualquier proveedor en línea usa la clave que tú guardas cifrada en el teléfono. No se han encontrado secretos en el repositorio.

## Correcciones pequeñas

- `apk/README.md`: 0.11.0 ya no figura como «pendiente».
- Documentación de Tests coherente con el script.
- Notas de compilación actualizadas para Wrapper.

## Limitaciones conocidas

- La compilación local en este entorno no era posible (JDK 11, sin SDK/NDK). La validación real la hace GitHub Actions.
- No se ha probado en un dispositivo físico; no se promete rapidez sin medir.
- No se han borrado los APK versionados: solo se cambió la CI para futuras versiones.
