# Sakura Agent 🌸

Agente Android nativo en Kotlin/Jetpack Compose. Conserva el diseño nocturno rosa/turquesa.

**La APK no está probada en un teléfono.** La versión 0.8 añade Gemini 2.5,
selector Local/Online, pensamiento bajo/alto, acciones desde el chat y una mascota
más animada; su compilación se valida en GitHub Actions. La 0.2 compiló en Actions
y se integró en `main` mediante el [PR #3](https://github.com/Hisoka111373/sakura-agent/pull/3).
La 0.3 también pasó CI (run 43551453495) y se integró mediante PR #4. La 0.4 pasó
CI run43553143121 y se integró por PR #5. La 0.5 pasó CI43638461195 y se integró
por PR #7. Para 0.6 consulta su propio PR/CI; las pruebas en dispositivo siguen pendientes.
El chat no utiliza respuestas predefinidas: requiere GGUF real o API configurada.

## Ampliación 0.8 — Gemini, Local/Online y pensamiento

- **Gemini corregido:** modelos 2.5 (`flash`, `flash-lite`, `pro`), IDs con o sin
  `models/`, `thinkingBudget` 0/1024 solo para 2.5 Flash y errores HTTP con el
  motivo del proveedor sin exponer la clave.
- **Probar sin guardar:** APIs prueba el formulario y la clave visibles; «Guardar
  y usar en el chat» guarda, activa Online y vuelve a Chat. Guardar no conecta.
- **Local u Online:** chips en Chat. Online exige proveedor y clave; Local usa el
  GGUF. La elección se conserva.
- **Pensamiento bajo/alto:** local 160/320 tokens, 4/6 mensajes y temperatura
  0.55/0.8; nube 512/1024 tokens. Alto tarda más; no garantiza mejores respuestas.
- **Acciones desde el chat:** «abre YouTube», «abre https://…», «escribe hola en
  Chrome» o «abre YouTube y busca lofi». Se interpretan localmente, muestran una
  vista previa y requieren confirmación. Pantalla sigue limitada a YouTube,
  Chrome, Firefox y Play Store.
- **Mascota y voz kawaii:** rebote, balanceo, parpadeo, chispas, nota musical,
  20 pétalos y frases rotativas. Voz automática activada por defecto, pitch 1.14;
  se puede desactivar. TTS local primero, sin subir audio.
- **Rendimiento local:** hilos automáticos (2–8), batch 512/ubatch 256, contexto
  2048, sampler por temperatura. Sigue siendo CPU ARM64, sin GPU.
- **Límites:** no probado en teléfono; Gemini depende de Google y de tu cuenta;
  las acciones no dan autonomía al modelo. Ver [docs/FEATURES-0.8.md](docs/FEATURES-0.8.md).

## Ampliación 0.7 — una APK ligera que descarga su modelo

- **Sin pesos dentro de la APK.** La APK sigue siendo pequeña. En el primer
  inicio Sakura descarga **Qwen2.5 1.5B Instruct Q4_K_M** (1.117.320.736 bytes,
  Apache 2.0) desde Hugging Face, comprueba tamaño y SHA-256, lo copia al
  almacenamiento privado y lo carga automáticamente. También puedes cancelar,
  reintentar o desactivar la descarga automática en Privacidad.
- **Voz de Sakura:** lectura automática opcional de respuestas, botón de saludo
  de voz en Jardín y lectura bajo demanda en cada mensaje. Sigue usando una voz
  española instalada en Android; no hay voz clonada ni subida de audio.
- **Mascota animada:** respiración, brillo, pétalos y parallax en Jardín y en el
  chat. No son GIFs ni video: es animación Compose en el dispositivo.
- **Modelos visibles:** la pestaña Modelos aparece junto a Chat e incluye el
  modelo inicial, modelos instalados, catálogo filtrable, enlaces de licencia y
  descargas con confirmación. El GGUF personalizado sigue en Descargas.
- **Claves API claras:** campo directo en APIs con mostrar/ocultar, modelos
  sugeridos editables, guardado cifrado, borrado y **Probar conexión** con una
  petición mínima revisada. Las demás credenciales siguen en Privacidad con un
  selector y el mismo campo oculto.
- **Buscar desde el chat:** escribe «busca …», «búscame …», «encuentra …» o
  «search …». Si hay clave Brave, pide confirmación y añade hasta 5 resultados
  al chat. Sin clave Brave, pide confirmación y abre YouTube/navegador/Play Store.
  Los resultados son contenido no confiable y no se abren ni descargan solos.
- **Se conserva:** Jardín, mascota original, 93 skills, favoritos, memoria
  cifrada, agente, planes, GitHub, mensajes, pantalla, descargas y privacidad.
- **Requisitos:** Android 13+, ARM64, idealmente 8 GB de RAM y Wi-Fi para la
  primera descarga. El modelo ocupa ~1,1 GB; el APK + copia privada puede añadir
  ~2,2 GB. Tamaño ≠ RAM necesaria. Aún **no se ha probado en un teléfono**.
- La APK 0.7 se publica solo en `apk/`, no como release. La descarga del modelo
  se hace desde Hugging Face; no se envían archivos, chat, recuerdos ni claves.

## Ampliación 0.6 — integración del agente pendiente

- Nueva pestaña **Agente**: hasta 10 propuestas por sesión con GGUF o API de texto.
  Cada petición de modelo y cada herramienta requiere revisión independiente;
  no hay bucle autónomo, espera activa ni continuación automática tras confirmar.
- JSON estricto validado: web Brave, lectura HTTPS pública, GitHub lectura, catálogo,
  descargas, mensajes, generación fal.ai, apps visibles y sesiones limitadas de pantalla.
  Reutiliza las confirmaciones existentes con registro de resultados reales en RAM.
- Conserva **Jardín, 93 skills, favoritos, chat, voz y privacidad** de main.
- Presets OpenRouter/Groq/DeepSeek/Mistral/xAI/Together/Fireworks además de los anteriores;
  claves cifradas y modelo de texto exacto configurable. No garantiza cuota gratis.
- GGUF personalizado con SHA-256 esperado obligatorio; bloquea YouTube/Googlevideo/Piped
  al iniciar descarga y en cada redirección. YouTube se abre oficialmente, no extrae streams.
- No incorpora la URL obsoleta de Pollinations ni promete imágenes gratis. Usa fal.ai
  con revisión de coste. No amplía accesibilidad a apps sensibles ni instala APK.

En modo nube, Agente puede enviar fragmentos de resultados previos (incluso GitHub privado
o inventario de apps) **solo al confirmar su JSON completo**. No comparte recuerdos,
chat ni pantalla automáticamente. Ver [docs/FEATURES-0.6.md](docs/FEATURES-0.6.md).

## Ampliación 0.5 — Jardín Sakura y 93 skills

- **Jardín**: portada con mascota anime original y flores de cerezo, avatar y
  detalles florales en la interfaz. Tres WebP empaquetados (175 KiB aprox.), sin red.
- **93 skills**: programación e ingeniería, aprendizaje, productividad,
  investigación, datos, creatividad y vida diaria. Biblioteca con buscador,
  categorías, descripciones y favoritos cifrados.
- **Selección automática de texto** por reglas locales, visible en Chat;
  se puede cambiar manualmente. Una instrucción breve guía al modelo GGUF/API.
  Elegir skill no envía ni borra el borrador; ninguna skill ejecuta herramientas.
- **Cloud con revisión**: JSON incluye la guía elegida; sigue requiriendo tu
  confirmación exacta y no incluye notas guardadas ni pantalla.

Las skills no ejecutan código/tests/shell ni garantizan especialización perfecta.
Ver [docs/FEATURES-0.5.md](docs/FEATURES-0.5.md) y [arte](docs/ARTWORK.md).

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

0.2, 0.3, 0.4 y 0.5 ya integradas por PR #3/#4/#5/#7. 0.6 se integró desde
`sakura-v0.6-integration`, 0.7 desde `feature/sakura-0.7-auto-model` y 0.8 desde
`feature/sakura-0.8-gemini-chat` en `main`, conservando el proyecto Android
original. Las APK viven en `apk/`; no hay releases. Consulta el CI de `main` para
el estado actual; las verificaciones estáticas no sustituyen pruebas en un teléfono.

## Funciones implementadas en el código

| Área | Alcance real |
|---|---|
| Personalidad | Español, amabilidad, humor opcional y tono de compañera definidos en el prompt. El cumplimiento depende del modelo. |
| Memoria | Notas explícitas de preferencia/proyecto/avance, cifradas con AES-GCM + Android Keystore; revisión y borrado individual o total. Hasta 100 notas; últimas 12 en contexto. |
| Aprendizaje | Nivel principiante/intermedio/avanzado persistente. Adapta instrucciones al modelo; no reentrena pesos. |
| IA local | Puente JNI a llama.cpp fijado a un commit; importar, cargar, cambiar, liberar y borrar GGUF. CPU, 2048 de contexto, 160/320 tokens de salida según pensamiento, hilos automáticos y batch 512/256. |
| Programación | Importar texto UTF-8 de hasta 64 KiB y pedir explicaciones/sugerencias para Python, Java, Kotlin, HTML y JavaScript. No ejecuta ni compila código. |
| GitHub | Leer/listar, ramas/commits/PR, crear ramas y archivos, editar/borrar con SHA y crear PR; escrituras confirmadas. |
| Android | Listar/abrir apps visibles con confirmación. Pantalla: búsqueda accesible manual solo en apps permitidas, no control general. |
| APK | Revisar la APK seleccionada: paquete, versión, SDK, permisos, hashes de certificado y archivo, bibliotecas. Sin instalar, ejecutar ni extraer ZIP. No es antivirus ni verificación completa de firma. |
| Voz | Dictado on-device si Android lo ofrece; de lo contrario, requiere autorización de red. Rellena el borrador, nunca envía automáticamente. TTS con voz española instalada, priorizando local; lectura automática activada por defecto (desactivable), pitch kawaii, frases rotativas y botón de saludo. |
| Privacidad | Red desautorizada al iniciar. Chat en RAM; memoria/credenciales cifradas. fal.ai recibe prompt visual confirmado; APIs de texto reciben contexto confirmado sin notas guardadas. No hay analítica añadida ni envío de pantalla. FLAG_SECURE. |

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

### Modelo inicial (descarga automática opcional)

El modelo inicial de 0.7 es
[Qwen2.5-1.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF),
archivo `qwen2.5-1.5b-instruct-q4_k_m.gguf`, Apache 2.0. Sakura lo descarga en el
primer inicio si no hay otro modelo, comprueba tamaño/SHA-256 y lo carga. No se
redistribuyen pesos dentro del repositorio ni de la APK. Para 8 GB también puedes
probar [Qwen2.5-3B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF)
desde el catálogo; revisa su licencia. La descarga automática se puede desactivar
en Privacidad y el modelo se puede borrar desde Modelos.

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

1. Instala la APK 0.7 y abre Sakura. En el primer inicio descarga el modelo inicial
   (~1,1 GB), lo verifica y lo carga. Usa Wi-Fi; puedes cancelar desde Jardín, Chat
   o Modelos y reintentar después. No hace falta importar nada para empezar.
2. Privacidad → selecciona nivel, humor, voz automática y si quieres descarga/carga
   automática del modelo. El interruptor de Internet sigue desactivado al iniciar.
3. Memoria → guarda notas concretas. No se extraen recuerdos del chat automáticamente.
4. Chat → escribe un mensaje corto, dicta o pide «busca …». Micrófono pide permiso;
   revisa el dictado antes de enviar. La búsqueda siempre pide confirmación.
5. APIs → elige proveedor, modelo sugerido y guarda la clave cifrada; usa «Probar
   conexión» solo si aceptas la petición mínima. Local no necesita clave.
6. Herramientas → selecciona archivos o apps. No se pide acceso general al almacenamiento,
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

- 86 pruebas JVM de reglas/URLs/catálogo/planes/skills/routing/protocolos/pantalla y agente: aprobadas con Kotlin 2.0.21 y JUnit 4.13.2.
- Todos los archivos Kotlin: compilación estática con API 35, dependencias AndroidX y
  plugin Compose 2.0.21 aprobada. No es un build Android de Gradle.
- C++: `g++ -fsyntax-only -Wall -Wextra -Werror` contra cabeceras del commit fijado y JNI:
  aprobado. No enlaza la biblioteca ni prueba instrucciones ARM64.
- Scripts: `bash -n` y comprobación de parche: aprobados.
- Build Android/ARM64 de 0.2/0.3/0.4/0.5 aprobado en Actions. Consulta CI del nuevo commit 0.6.
- **Pendiente en dispositivo:** inferencia real, RAM pico/rendimiento, Keystore, voz,
  APK de prueba, apps, exportaciones, downloads completos y operaciones de GitHub,
  mensajería, fal.ai, Brave, APIs de texto y Accesibilidad. No se enviaron mensajes
  ni se hicieron peticiones pagadas para probar sus servicios. Solo se generó el
  dibujo decorativo solicitado para la interfaz, fuera del runtime Android.

Consulta [docs/TESTING.md](docs/TESTING.md) antes de usarla con datos o repositorios importantes.
