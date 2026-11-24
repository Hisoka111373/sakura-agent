# Sakura 0.10.0 — mascota viva, skills ampliadas y local más rápido

Esta versión conserva la supervisión previa: ninguna herramienta se ejecuta sin una
confirmación explícita, no hay bucles autónomos y los secretos siguen cifrados en el
dispositivo. No afirma rendimiento medido en un teléfono.

## 1. Más skills

La biblioteca pasa de 93 a **136 especialidades** (`Skills.kt`, `AdvancedSkills.kt` y el
nuevo `MegaSkills.kt`). La UI de biblioteca, favoritos, contadores y detección por
palabras clave ya son genéricos. Las nuevas familias son:

- **Agente Sakura**: abrir apps con revisión, escribir en pantalla, buscar vídeos,
  explorador, planes por pasos y explicación de resultados.
- **GitHub**: leer árbol/archivos, preparar ediciones, commits revisados, ramas y PR.
- **Rendimiento**: modo local rápido, elección de GGUF, diagnóstico de lentitud,
  timeouts y memoria.
- **Mascota**: expresiones, tono, voz local y ahorro de batería.
- **Productividad, investigación, creatividad y vida diaria**: auditoría de
  privacidad, pruebas en teléfono, logs, permisos, fuentes, modelos, guiones,
  hábitos y japonés.

Las skills siguen siendo guías de texto: no ejecutan código, no amplían permisos y
no habilitan herramientas por sí solas.

## 2. Mascota Sakura viva

`SakuraLiveMascot.kt` dibuja a Sakura con Compose Canvas (sin Live2D ni vídeo):

- **Ojos**: párpado en el ciclo de parpadeo, pupilas que miran alrededor y brillo.
- **Boca**: sonrisa en reposo y apertura rítmica cuando la voz local está hablando.
  Es una **aproximación visual**, no sincronización fonética real.
- **Manos**: brazos y manos con saludo/balanceo; salto y `squash/stretch` en eventos
  felices.
- **Estados**: `Saludo`, `Saltito`, `Pensando`, `Lista`, `Pausa` y `Error` cambian
  expresión y frase local. La lógica pura vive en `MascotExpressions.kt` y tiene
  pruebas JVM.

La animación se pausa fuera de primer plano o al desactivar Movimiento, igual que
antes. Las miniaturas históricas del chat siguen usando el WebP original.

## 3. Herramientas supervisadas añadidas al agente

| Herramienta | Campos | Qué hace |
|---|---|---|
| `github_tree` | `owner,repo,branch` | Lista hasta 300 blobs; solo lectura. |
| `github_open` | `owner,repo,branch,path` | Lee el SHA actual y abre el archivo en el panel GitHub para editarlo tú. |
| `video_search` | `query` | Abre YouTube oficial con la consulta codificada. |
| `browser_search` | `query` | Abre Brave Search en el navegador elegido por Android. |

Siguen sin exponerse escrituras de GitHub como herramientas del modelo: commits,
ramas, archivos y PR se revisan en la pestaña GitHub con token cifrado. Tampoco se
extraen streams, se omiten DRM ni se leen resultados automáticamente.

## 4. Motor local más rápido (sin benchmark)

En `sakura_llm.cpp`:

- `n_batch`/`n_ubatch` de 256 a **512** para reducir llamadas de prefill.
- Hilos detectados automáticamente con clamp **2–6**, dejando un núcleo para la UI.
- **Flash Attention** activado explícitamente.
- Caché KV **Q8_0** para K y V (con Flash Attention), frente a F16 por defecto.
- `no_perf = true` para evitar sobrecarga de medición interna.
- **Warm-up** después de cargar: prefill corto y limpieza de memoria, sin lanzar
  excepciones si falla. Cubre arranque y descarga inicial.
- `nativeRuntimeInfo()` muestra `ctx`, `batch`, `ubatch`, hilos, Flash Attention,
  KV y versión de llama.cpp en el estado de carga.

CI verifica `-O3` efectivo, los tokens nativos anteriores y las pruebas JVM. La
latencia real, RAM pico y temperatura **deben medirse en el teléfono**.

## 5. Publicación de APK

El workflow `build-apk.yml` publica, tras CI verde y solo en `main`, el archivo:

- `apk/SakuraAgent-0.10.0-arm64-debug.apk`
- `apk/SHA256SUMS.txt`
- `apk/BUILD-0.10.0.md`

Usa `secrets.GITHUB_TOKEN` del propio Actions; no necesita un PAT pegado en el chat.
El commit del bot lleva `[skip ci]` y `apk/**` está en `paths-ignore`, por lo que no
genera un bucle de compilación. La versión interna es `0.10.0` / `versionCode 12`.

## Prueba física pendiente

Instalar en ARM64/Android 13+, descargar un GGUF pequeño, decir «hola», medir tiempo,
detener y reenviar, comprobar voz, parpadeo/manos/boca, Accesibilidad y una búsqueda
supervisada. Sin esas pruebas no se afirma que el rendimiento haya mejorado.
