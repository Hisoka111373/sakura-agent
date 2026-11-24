# Sakura 0.12.0 — APK publicada por CI

Archivo: `SakuraAgent-0.12.0-arm64-debug.apk`; copia estable: `sakura.apk`.

- Tamano: 82465970 bytes
- SHA-256: `ad75838d56a25adc1ae4773c9cd3118bd153c18baa79b9e103796e5bb2746579`
- Datos completos: `BUILD-0.12.0.md` y `SHA256SUMS.txt`.

---

# Sakura 0.11.0 — pendiente de publicación por CI

La versión `0.11.0` (`versionCode 13`) está preparada en el código. Tras la CI en
`main`, GitHub Actions debe publicar aquí:

- `SakuraAgent-0.11.0-arm64-debug.apk`
- `SHA256SUMS.txt`
- `BUILD-0.11.0.md` con commit, ejecución, tamaño y SHA-256 reales

No hay hash hasta que CI termine: no copies ni inventes datos de una APK que no se ha
compilado. La versión se lee de `versionName` en `app/build.gradle.kts`.

---

# Sakura 0.10.0 — pendiente de publicación por CI

La versión `0.10.0` (`versionCode 12`) está preparada en el código. GitHub Actions
debe compilarla y publicar aquí, tras pruebas y verificación nativa en verde:

- `SakuraAgent-0.10.0-arm64-debug.apk`
- `SHA256SUMS.txt`
- `BUILD-0.10.0.md` con commit, ejecución, tamaño y SHA-256 reales

El workflow usa `secrets.GITHUB_TOKEN`; el commit del bot lleva `[skip ci]` y
`apk/**` está excluido de los disparadores de CI para evitar bucles. Todavía no hay
hash: no copies ni inventes datos de una APK que no se ha compilado.

---

# Sakura 0.9.1 — APK verificada

Archivo: `SakuraAgent-0.9.1-arm64-debug.apk`. Versión Android **0.9.1**, versionCode **11**, paquete `com.sakura.agent`. Android 13+ ARM64; APK debug, no versión de tienda.

- Fuente: `03783eb9cddaeb022940f6f245b4fbb12b468d7d` (PR #13).
- CI: https://github.com/Hisoka111373/sakura-agent/actions/runs/44457756920 — éxito: pruebas Gradle, build ARM64 y verificación de -O3 efectivo en llama/ggml/JNI.
- 143 pruebas JVM portátiles aprobadas; compilación Kotlin/API35 y sintaxis C++ verificadas también.
- Tamaño: **65369281 bytes**.
- SHA-256: `3354adf104353337ce449ee334184230fa2a32450ba6381effdae825eb3b9560`.
- ZIP/CRC, manifiesto 0.9.1/code11, bibliotecas ELF ARM64, arte original y ausencia de las claves configuradas: verificados.
- Firma APK v2 verificada. Certificado debug SHA-256: `22b8a7c64d91dc5d6c73fb3300c52f69ec14674e8c0c029f8e45d15102a2bada`.

Repara la ruta local: prompt compacto, saludos sin arrastrar historial/notas, salida corta en Bajo, motor CPU optimizado y porcentaje de preparación del contexto. La respuesta dispone de su propio plazo limitado después del contexto; no elimina los timeouts ni simula respuestas. Prueba Qwen2.5 0.5B Instruct (~491 MB), modo Bajo y «hola».

Chat despejado: Local/En línea, conversación, dictado, enviar/detener y Continuar. El resto está en Opciones, incluidos Archivos, skills y ajustes. Mascota con salto squash/stretch y movimiento lateral; conserva arte original, Jardín y 93 skills. Conserva controles de teléfono supervisados de 0.9.0 y modelos Q3; añade Coder0.5B. Imagen/video son descargas externas: no hay motor local para generarlos en Sakura.

**Rendimiento y funcionamiento físico pendientes de probar en tu teléfono.** La firma difiere de 0.9.0: Android podría rechazar una actualización directa. No desinstales sin preservar tus datos; recuerdos, claves y modelos privados pueden perderse, y Keystore no se restaura simplemente copiando archivos.

Se conserva 0.9.0 y las demás APK existentes. No se restaura la APK 8.0 retirada; su descripción inferior es solo histórica.

---

## Documentación histórica (no describe 0.9.1)

# Sakura 0.9.0 — APK nueva

Archivo: `SakuraAgent-0.9.0-arm64-debug.apk` (**0.9.0**, versionCode `10`, paquete `com.sakura.agent`). Android 13+ ARM64, build debug, no release de tienda.

- Fuente: `63e4b58` (rama `feature/genki-phone-controls`).
- CI: https://github.com/Hisoka111373/sakura-agent/actions/runs/44228599518 — éxito (`testDebugUnitTest` y `assembleDebug` en verde).
- 132 tests JVM portátiles pasados localmente antes del push; Gradle Unit tests y build ARM64: aprobados en CI.
- Tamaño: `73600089` bytes.
- SHA-256: `fa6d3a72a18760dabe02d5198ff1659e1124b7de132393a0e099d3890fb17c59`.
- Verificación ZIP: `AndroidManifest.xml`, `classes.dex` y `lib/arm64-v8a/libsakura_llm.so` presentes, 152 entradas. La firma es APK v2 (no aparece como JAR v1 con `keytool`).

Incluye el arreglo del modo local que se quedaba pensando: presupuesto determinista del prompt, mensaje nuevo preservado, limpieza del estado `Pensando` en éxito/error/timeout/cancelación y deadline nativo antes de tokenizar. Mantiene selector Local/En línea y `Continuar` con borrador vacío.

Añade GGUF pequeños verificados (0.5B Q4_K_M y 1.5B Q3_K_M); imagen y vídeo se muestran como descarga externa porque Sakura no tiene motor local para ellos.

Añade controles del teléfono **siempre con revisión previa**: Google en navegador, lectura de pantalla vía Accesibilidad (sin texto sensible), escritura confirmada en campos seguros, archivos de carpeta SAF y apertura/instalación de APK con diálogo y permiso `REQUEST_INSTALL_PACKAGES`. No hay bucles autónomos ni envío de pantalla a la nube.

**No probado físicamente**: arranque en móvil, rendimiento del GGUF, JNI, voz, Accesibilidad, SAF con proveedores de terceros y pantalla de instalación de Android. La firma debug puede diferir de versiones anteriores; conserva tus datos antes de actualizar.

Las APK anteriores se conservan intactas.

---

## Documentación histórica (no describe la build nueva)

# Sakura 8.0 — APK nueva

Archivo: `SakuraAgent-8.0-arm64-debug.apk` (nombre solicitado: 8.0). **Versión interna Android: 0.8.0, versionCode 9**, paquete `com.sakura.agent`. Android 13+ ARM64, build debug, no release de tienda.

- Fuente: `2b523f592ae1100f2bf64480cff735a36e7ef978`, PR #11.
- CI: https://github.com/Hisoka111373/sakura-agent/actions/runs/44219846378 — éxito.
- 116 pruebas JVM, compilación Android, CRC, manifiesto/ABI, firma APK v2 y escaneo de claves: aprobados.
- Tamaño: 73517525 bytes.
- SHA256: `2a183165b7d52d2e794a14a741b5cd7b8c1f1b3cabcc68b6b9c939533e76b638`.
- Certificado debug SHA256: `567c1335588ad29d5052a9411e4a9a6f38a83a054bb7dec44497b6c589b8c8f6`.

Saludar, saltitos y reacciones al pensar/hablar; frases locales sin tokens con voz local opcional solo en primer plano. Botón Continuar pide otra parte por solicitud explícita (la nube sigue requiriendo revisión y puede consumir cuota/dinero). Bajo reduce salida local a 160 tokens y acota contexto anterior; no elimina límites ni garantiza más velocidad en tu móvil. Conserva Jardín, arte y 93 skills. No probado físicamente: inicio, rendimiento, JNI/TTS/gestos/accesibilidad.

No incluye claves ni pesos. Configura tu clave cifrada en APIs y confirma la conexión Gemini desde el móvil. Descarga de GGUF manual confirmada; ninguna herramienta se ejecuta automáticamente.

**Firma diferente de 0.7.1:** Android puede rechazar la actualización. No desinstales sin preservar tus datos; recuerdos/claves/modelos privados pueden perderse y Keystore no se restaura simplemente copiando archivos.

Las APK anteriores se conservan intactas.

---

## Documentación histórica (no describe la build nueva)

# Sakura Agent 0.7.1 — APK verificada

- Archivo: `SakuraAgent-0.7.1-arm64-debug.apk`
- Android 13+ (minSdk 33), ARM64; paquete `com.sakura.agent`.
- Versión 0.7.1, versionCode 8; APK debug, no release de tienda.
- Fuente: `578be9d5499aaf98a544ea1f2e9ed16b1a56bbc6` (PR #9).
- GitHub Actions: https://github.com/Hisoka111373/sakura-agent/actions/runs/44217716451
- Tamaño: 73501141 bytes.
- SHA-256: `93a7d15923dcb17139e7b2cd8c5d3b2666a769b2f1bdc5b6e6cfbf4d929abfea`.
- ZIP/CRC, manifiesto/ABI y firma APK v2 comprobados independientemente.

Incluye Gemini comprobado desde el cliente JVM, selector Local/online, Bajo/Alto,
progreso JNI y deadline cooperativo, mascota animada y voz local opcional confirmada.
Conserva Jardín, 93 skills, centro de modelos y búsqueda desde chat revisada.
No incluye claves ni pesos. Configura `gemini_key` oculta/cifrada en APIs o Privacidad,
elige `gemini-3.1-flash-lite`, autoriza Internet y confirma la prueba real desde el móvil.
Qwen2.5 1.5B se descarga MANUALMENTE desde Modelos con confirmación (~1.1 GB).

No probado físicamente: arranque, rendimiento, JNI, TTS y accesibilidad en teléfono.
Abrir apps ya instaladas y escribir en allowlist requiere revisión por acción;
no controla cualquier APK ni instala automáticamente.

La firma debug puede diferir de APK anteriores: Android podría rechazar la actualización.
No desinstales sin preservar datos: recuerdos, claves y modelos privados se borran;
el cifrado Keystore no se restaura simplemente copiando archivos.
Las APK 0.6.0 y 0.7.0 anteriores se conservan sin cambios.

---

## Documentación histórica de 0.7.0 (no describe la nueva 0.7.1)

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
