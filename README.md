# Sakura Agent 🌸

Agente Android nativo en Kotlin/Jetpack Compose. Conserva el diseño nocturno rosa/turquesa.

**La APK no está probada en un teléfono.** La versión 0.2 compiló en GitHub Actions
y se integró en `main` mediante el [PR #3](https://github.com/Hisoka111373/sakura-agent/pull/3).
La 0.3 también pasó CI (run 43551453495) y se integró mediante PR #4. La 0.4 tiene su
propio PR/CI. La 0.5 está en la rama `sakura-v0.5-agent`; su build Android sigue
pendiente de GitHub Actions y las pruebas en dispositivo siguen sin hacerse.
El chat no utiliza respuestas predefinidas: requiere GGUF real o API configurada.

## Ampliación 0.5 — agente con herramientas

- **Bucle acotado de 10 pasos.** El modelo solo propone un objeto JSON por paso
  (`{"tool":"...","args":{...}}` o `{"final":"..."}`). Kotlin valida el esquema, los
  argumentos y la lista cerrada de herramientas; el modelo nunca recibe callbacks
  ejecutables. Cada paso y su observación quedan visibles en la página Agente.
- **Herramientas:** búsqueda web Brave, lectura de páginas HTTPS públicas, búsqueda y
  streams de YouTube mediante instancias Piped, descarga directa, descarga de GGUF del
  catálogo, GGUF por URL personalizada, generación de imagen con Pollinations, listar y
  abrir apps, sesión de pantalla, lectura de GitHub, modelos locales y `finish`.
- **Consentimiento en dos niveles:** al iniciar se muestra una vista previa de la sesión
  (objetivo, herramientas de red, límites y consumo). Descargas, imágenes, apertura de
  apps y sesiones de pantalla piden **otra confirmación por acción**. El botón Detener
  cancela el bucle, las llamadas activas y cualquier confirmación pendiente.
- **Muchos proveedores de texto:** OpenAI, OpenRouter, Groq, DeepSeek, Mistral, xAI,
  Together, Fireworks, Gemini, Anthropic y endpoint compatible. Las claves se guardan
  cifradas y nunca se incluyen en prompts, argumentos ni registros.
- **Pantalla general por app:** además del modo de búsqueda restringido, puedes elegir
  una app instalada y revisar sus controles visibles. Se excluyen contraseñas, pagos,
  permisos, instalación y paquetes sensibles; cada pulsación/escritura se confirma y la
  sesión dura 3 minutos. No hay capturas, lectura en reposo ni segundo plano.
- **Descarga de GGUF por URL:** HTTPS directa terminada en `.gguf`, hasta 2.5 GB, con
  cabecera GGUF, tamaño y espacio verificados. **Sin SHA-256 fijado**: origen, licencia
  y calidad no están verificados. Úsala solo con pesos que tengas derecho a descargar.

YouTube/Piped y Pollinations son servicios externos gratuitos: pueden fallar, limitar
peticiones o requerir PO token. No se garantiza disponibilidad ni calidad. Un stream de
video sin audio necesita muxing externo; Sakura no descarga YouTube ni elude DRM.

Configuración, alcances reales y pruebas: [docs/FEATURES-0.4.md](docs/FEATURES-0.4.md)
(la 0.5 se documenta aquí y en la propia UI).

## Ampliación 0.4 — skills dentro de Sakura

- **21 skills** de texto y accesos a herramientas, sin ejecución autónoma del modelo.
- **Buscar**: prepara peticiones como «busca una canción de Maná» y revisa abrir
  YouTube/navegador/Google Play; búsqueda interna opcional con Brave Search API.
- **APIs de texto**: OpenAI, endpoint Chat Completions compatible HTTPS configurable,
  Gemini y Anthropic. Claves cifradas en Privacidad; proveedor/modelo en APIs.
  Cada petición muestra JSON completo sin clave, contexto exacto y posible coste.
  No envía recuerdos guardados, archivos ni pantalla. Local sigue siendo el defecto.
- **Pantalla**: servicio de Accesibilidad opcional y activado manualmente. Sesiones
  de búsqueda de hasta 3 minutos en YouTube/Chrome/Firefox/Play Store, DETENER fijo,
  lectura puntual y confirmación por pulsación/escritura. No cualquier APK ni UI;
  sin mensajes, pagos, login, permisos, instalación, screenshots ni gestos arbitrarios.
- **Audio autorizado**: MP3/WAV/FLAC/OGG por enlace público directo, además de
  imagen/video. No extrae canciones/videos de YouTube ni evade DRM.

Configuración, alcances reales y pruebas: [docs/FEATURES-0.4.md](docs/FEATURES-0.4.md).

## Ampliación 0.3 — herramientas con revisión humana

- **Plan:** GGUF propone JSON limitado a 6 pasos. Se valida una lista cerrada de
  herramientas; cada acción tiene vista previa y confirmación individual. No hay
  ejecución encadenada, comandos de shell ni reintentos automáticos. Si editas el
  JSON debes validarlo otra vez. Se muestra evidencia recibida, no resultados inventados.
- **Descargas:** catálogo de chat/imagen/video con revisiones y SHA-256 fijados.
  Qwen2.5 3B Q4_K_M oficial es el candidato inicial para un teléfono de 8 GB:
  **2,104,932,768 bytes (~2.10 GB / 1.96 GiB)**. Prueba 1.5B si resulta lento.
  Tamaño de archivo ≠ RAM necesaria; no hay garantía de rapidez ni compatibilidad.
- **Archivos multimedia:** URL HTTPS directa pública autorizada; imagen/video hasta
  512 MiB, 5 redirecciones máximo y 45 minutos por descarga. Se validan IPs públicas
  en el DNS usado por la conexión, sin proxies, cookies ni tokens de servicios.
  Sin extraer YouTube/HLS, entrar en cuentas ni eludir DRM. Guardado privado,
  cancelación, borrado confirmado y exportación por selector Android.
- **Mensajes:** compartir borrador con WhatsApp/Telegram para que tú elijas contacto
  y pulses Enviar, o enviar por APIs oficiales tras confirmar destinatario y texto.
  Telegram requiere bot y Chat ID permitido; **no actúa como tu cuenta personal**.
  WhatsApp requiere Business/Cloud API, token Meta y Phone Number ID: esta versión
  solo envía texto en ventana de servicio de 24 h, sin plantillas ni webhooks.
  Aceptación de la API no garantiza entrega o lectura.
- **Crear:** imagen (`fal-ai/flux/schnell`) o video (`fal-ai/ltx-video`) en fal.ai
  usando tu clave, Internet autorizado y confirmación del prompt y posible gasto.
  No envía chat ni recuerdos. No impone tope monetario; revisa tarifa/licencia y cuota
  en fal.ai. Las URLs de salida pueden ser públicas y expirar. Consultar/cancelar
  trabajo no reenvía generación; el último ID se guarda cifrado y puede recuperarse.
- **GitHub ampliado:** consultar ramas/commits/PR, crear ramas desde un SHA revisado,
  crear texto nuevo sin sobrescritura implícita, editar o borrar texto con SHA leído,
  y crear pull requests. No incluye toda la API: no borra repositorios, administra
  secretos/permisos ni mezcla PR automáticamente. Requiere permisos mínimos por acción.
- **Medición:** RAM total/disponible y PSS actual de Sakura; tiempo real de carga
  y respuesta. Son muestras, **no RAM pico ni benchmark certificado**.

**Generación visual local no instalada:** los checkpoints SD1.5/LTX del catálogo
son descargas para evaluación externa. No se cargan con llama.cpp ni convierten
Sakura en un motor de difusión. Necesitan otro runtime, componentes compatibles
y pruebas físicas; no prometemos video local rápido con 8 GB. Consulta
[docs/FEATURES-0.3.md](docs/FEATURES-0.3.md) para configuración, límites y pruebas.

## Integración en GitHub

0.2 y 0.3 ya integradas por PR #3/#4. La ampliación 0.5 se sube a
`sakura-v0.5-agent`, base `bae912f758e4f3e14d54dbb0f6a245db086c4bb7`, mediante pull request, conservando el proyecto Android original.
Consulta su PR/Actions para conocer el estado actual de integración y compilación; las
verificaciones estáticas indicadas al final no sustituyen un build Android ni pruebas físicas.

## Funciones implementadas en el código

| Área | Alcance real |
|---|---|
| Personalidad | Español, amabilidad, humor opcional y tono de compañera definidos en el prompt. El cumplimiento depende del modelo. |
| Memoria | Notas explícitas de preferencia/proyecto/avance, cifradas con AES-GCM + Android Keystore; revisión y borrado individual o total. Hasta 100 notas; últimas 12 en contexto. |
| Aprendizaje | Nivel principiante/intermedio/avanzado persistente. Adapta instrucciones al modelo; no reentrena pesos. |
| Agente | Hasta 10 pasos. El modelo propone JSON; Kotlin valida y ejecuta una herramienta por paso. Búsqueda web, lectura HTTPS, YouTube/Piped, GitHub, apps/modelos y `finish` bajo el consentimiento inicial. Descargas, imágenes, apps y pantalla piden confirmación aparte. Las observaciones se envían al proveedor de texto; las notas privadas nunca. Detener cancela llamadas y confirmaciones. |
| IA local | Puente JNI a llama.cpp fijado a un commit; importar, cargar, cambiar, liberar y borrar GGUF. CPU, 2048 tokens de contexto, hasta 384 de salida. El agente puede usar el GGUF cargado o una API. |
| Programación | Importar texto UTF-8 de hasta 64 KiB y pedir explicaciones/sugerencias para Python, Java, Kotlin, HTML y JavaScript. No ejecuta ni compila código. |
| GitHub | Leer/listar, ramas/commits/PR, crear ramas y archivos, editar/borrar con SHA y crear PR; escrituras confirmadas. El agente solo lee archivos de texto. |
| Android | Listar/abrir apps visibles con confirmación. Pantalla: modo de búsqueda en apps permitidas y modo general por app instalada elegida; lectura solo al pulsar, confirmación por acción, 3 minutos, sin capturas ni segundo plano. |
| Descargas | Imagen/video/audio por URL directa hasta 512 MiB; GGUF del catálogo con SHA-256; GGUF personalizado por URL `.gguf` hasta 2.5 GB con cabecera y espacio verificados, sin SHA. Guardado privado; no se ejecuta. |
| YouTube | Búsqueda y listado de streams para reproducción vía Piped. Los enlaces pueden expirar o requerir PO token; no se descarga YouTube/HLS/DRM (las URLs de YouTube/Piped se rechazan en Descargas) y el video sin audio requiere muxing. |
| Imágenes | Pollinations gratuito para generar una imagen 1024×1024. El prompt se envía a un servicio externo y se confirma antes; licencia y disponibilidad no garantizadas. |
| APK | Revisar la APK seleccionada: paquete, versión, SDK, permisos, hashes de certificado y archivo, bibliotecas. Sin instalar, ejecutar ni extraer ZIP. No es antivirus ni verificación completa de firma. |
| Voz | Dictado on-device si Android lo ofrece; de lo contrario, requiere autorización de red. Rellena el borrador, nunca envía automáticamente. TTS con voz española instalada, priorizando local. |
| Privacidad | Red desautorizada al iniciar. Chat en RAM; memoria/credenciales cifradas. fal.ai/Pollinations reciben el prompt confirmado; las APIs de texto reciben el objetivo y las observaciones del agente, sin notas guardadas ni archivos. No hay analítica añadida ni envío de pantalla. FLAG_SECURE. |

## Requisitos y compatibilidad

- Android **13 o posterior** (API 33+), sistema y CPU **ARM64**. No se incluye ABI de 32 bits.
- Objetivo de prueba: teléfono con 8 GB de RAM. La RAM total no garantiza RAM disponible,
  velocidad ni compatibilidad. No se puede prometer funcionamiento en cualquier celular.
- Modelo instruct/chat GGUF v2/v3 de arquitectura y plantilla admitidas por la revisión
  fijada de llama.cpp. La cabecera GGUF solo valida el formato; la carga verifica lo demás.
- Importación limitada a 2.5 GB por archivo y al espacio libre disponible. Se copia al
  almacenamiento privado; la descarga original ocupa espacio adicional.
- Contexto pequeño: solo las últimas tres parejas del chat y doce notas se incluyen.
  Si el prompt supera el límite de tokens, se muestra error; acorta el fragmento o abre nuevo chat.
- Usa modelos de fuentes confiables. Archivos GGUF/APK no confiables pueden tener riesgos
  de parser; esta app no ofrece aislamiento de seguridad contra vulnerabilidades nativas.

### Modelo inicial sugerido (no incluido)

Como primer candidato de prueba para 8 GB, usa el catálogo para descargar
[Qwen2.5-3B-Instruct-GGUF oficial](https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF)
en Q4_K_M. Su licencia específica debe revisarse antes de usarlo. La alternativa menor es
[Qwen2.5-1.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF).
Para enfocarte en código puedes evaluar
[Qwen2.5-Coder-1.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF).
Revisa su licencia y tarjeta antes de descargar. No se redistribuyen pesos, no se añade
descarga automática y ninguno de estos candidatos se ha ejecutado en esta entrega.

## Compilar desde tu computadora

1. Abre el proyecto en Android Studio con soporte para AGP 8.7.3.
2. Instala Java 17, Gradle 8.9, plataforma Android 35, Build Tools 35.0.0,
   NDK `27.2.12479018` y CMake `3.31.6`.
3. Desde la raíz, descarga la dependencia nativa fijada:

   ```bash
   bash scripts/fetch-llama.sh
   ```

   En Windows usa Git Bash para ese script. `llama.version` es la única fuente del commit.
   Se necesita Internet en esta etapa. El script no sobrescribe un checkout distinto ni
   cambios locales. `third_party/` está ignorado y no se incluye en el ZIP.

4. Configura la ruta del SDK en Android Studio o `local.properties` y ejecuta:

   ```bash
   gradle --no-daemon testDebugUnitTest
   gradle --no-daemon assembleDebug
   ```

   No hay Gradle Wrapper en el repositorio original ni en esta entrega; necesitas Gradle
   8.9 instalado o la configuración local equivalente de Android Studio.

5. Instala `app/build/outputs/apk/debug/app-debug.apk` en un teléfono ARM64 de prueba.
   Es una APK de depuración, no una versión firmada para tienda.

## GitHub Actions (alternativa)

El workflow `.github/workflows/build-apk.yml`
descarga llama.cpp al commit fijado, prepara SDK/NDK/CMake, ejecuta pruebas y solicita
compilar la APK ARM64. También puede iniciarse desde Actions → **Build Sakura Agent APK**
→ **Run workflow**. Descarga el artefacto `sakura-agent-debug-apk` solo si termina en verde.
Consulta Actions para comprobar su resultado; puede requerir ajustes de CI.
Se eliminó la copia redundante en `github/workflows/`, que GitHub no ejecuta.

## Primer uso

1. Modelos → Importar GGUF → Cargar y confirmar. Espera la carga antes de chatear.
2. Privacidad → selecciona nivel y humor. Mantén la red desactivada para empezar.
3. Memoria → guarda notas concretas. No se extraen recuerdos del chat automáticamente.
4. Chat → escribe un mensaje corto. Micrófono pide permiso; revisa el dictado antes de enviar.
5. Herramientas → selecciona archivos o apps. No se pide acceso general al almacenamiento,
   instalación de paquetes, root ni QUERY_ALL_PACKAGES. Accesibilidad opcional solo
   si tú la activas manualmente para Pantalla; consulta la divulgación antes.

### GitHub y autorización

- Activa red en Privacidad únicamente si quieres usar funciones con Internet.
- La lectura pública puede funcionar sin token; GitHub aplica límites de consultas.
- Para un repositorio privado crea un **fine-grained personal access token** limitado a
  ese repositorio con `Contents: read`. Para commits necesita `Contents: write`.
- Introduce el token en el campo oculto de Privacidad, **nunca en el chat**. Se cifra con
  Keystore y se envía solo a `api.github.com` por HTTPS; no se siguen redirecciones.
- Leer archivo → llevar al borrador → pedir sugerencias locales → editar el contenido en
  GitHub → Revisar antes de escribir → inspeccionar ANTES/DESPUÉS → Confirmar commit.
- Crear ramas/PR requiere tus botones y confirmación. Usa una rama de trabajo.
  La confirmación de archivos indica repositorio, rama, ruta, SHA (si existe), contenido
  y mensaje. Crear PR requiere `Pull requests: write`; cambiar workflows puede requerir
  `Workflows: write` además de `Contents: write`.
- Un error o timeout de escritura obliga a releer. Comprueba GitHub antes de repetir:
  el servidor podría haber aplicado el cambio aunque no llegara la respuesta.
- Borrar el token local no lo revoca en GitHub; revócalo también en tu cuenta.

## Límites de privacidad

El interruptor de red controla acciones iniciadas por Sakura; **no es un firewall** para
servicios de Android ni apps abiertas. Un servicio local de voz es el que Android declara
on-device; si no puede reconocer español, falla sin activar una alternativa en red a
escondidas. Desautorizar red detiene la voz activa. La voz también se detiene al pasar
la app a segundo plano. La inferencia no recibe herramientas ni credenciales.

`allowBackup=false` desactiva el backup estándar. El cifrado no protege contra un teléfono
rooteado o comprometido ni garantiza borrado forense de memoria flash. Los archivos GGUF
importados están privados pero no cifrados; los originales del selector no se modifican.

## Verificación de esta entrega

- 63 pruebas JVM de reglas/URLs/catálogo/planes/skills/protocolos/pantalla/agente: aprobadas con Kotlin 2.0.21 y JUnit 4.13.2.
- Todos los archivos Kotlin, incluidas las pantallas Compose: compilación estática con
  API 36, dependencias AndroidX y plugin Compose 2.0.21 aprobada. No es un build Android
  de Gradle; la APK debe compilarse en Actions o Android Studio.
- C++: `g++ -fsyntax-only -Wall -Wextra -Werror` contra cabeceras del commit fijado y JNI:
  aprobado. No enlaza la biblioteca ni prueba instrucciones ARM64.
- Scripts: `bash -n` y comprobación de parche: aprobados.
- Build Android/ARM64 de 0.2/0.3 aprobado en Actions. Consulta CI del nuevo commit 0.5.
- **Pendiente en dispositivo:** inferencia real, RAM pico/rendimiento, Keystore, voz,
  APK de prueba, apps, exportaciones, downloads completos y operaciones de GitHub,
  mensajería, fal.ai, Brave, APIs de texto, Piped/YouTube, Pollinations y Accesibilidad
  general. No se enviaron mensajes ni se hicieron peticiones pagadas para probar.

Consulta [docs/TESTING.md](docs/TESTING.md) antes de usarla con datos o repositorios importantes.
