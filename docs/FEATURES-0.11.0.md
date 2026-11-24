# Sakura 0.11.0 — edición de GitHub desde el agente, apps por nombre y mascota más viva

Versión `0.11.0`, `versionCode 13`. Conserva la regla central del proyecto: **ninguna
herramienta se ejecuta sin confirmación explícita**, no hay bucles autónomos y los
secretos siguen cifrados con Android Keystore. No se afirma rendimiento medido en un
teléfono: la latencia real, la RAM pico y la temperatura siguen pendientes de medir.

## 1. Editar el propio repositorio desde el agente

Nueva herramienta supervisada `github_edit` (`owner,repo,branch,path,find,replacement,message`):

1. El modelo propone **un fragmento exacto** (`find`) que debe aparecer **una sola vez**
   en el archivo y su `replacement` (ambos ≤4096 caracteres, límite del protocolo JSON).
2. Sakura lee el archivo y su SHA actual con el token cifrado; no escribe nada todavía.
3. Se abre el panel de GitHub con **ANTES/DESPUÉS** y el mensaje de commit propuesto.
4. La persona revisa el diff y confirma; solo entonces se envía el `PUT` a GitHub.

Sigue habiendo una confirmación adicional para cada paso. Un timeout nunca se reintenta
automáticamente: el código obliga a releer el archivo antes de volver a intentarlo.
También se añade `github_issues` (solo lectura de incidencias abiertas) y el botón
`Listar Issues` en la pestaña GitHub.

Esto permite que, en el futuro, pidas al agente que modifique este mismo repositorio
desde el teléfono; el commit siempre lo autorizas tú y conviene trabajar en una rama.

## 2. Abrir aplicaciones y buscar vídeos por nombre

- `open_app_named(name)` resuelve alias en español (`youtube`, `chrome`, `navegador`,
  `explorador`, `web`, `buscador`, `firefox`, `play store`, `tienda`, `google play`) a
  paquetes de una allowlist. El modelo no tiene que adivinar nombres de paquete.
- La apertura usa solo actividades de inicio exportadas y visibles, sin
  `QUERY_ALL_PACKAGES`. Tras confirmar, se pueden encadenar `video_search` o
  `browser_search` (también con confirmación) para «abre explorador y busca videos».
- `screen_start`, `screen_read` y `screen_type` se conservan: sesión de 3 minutos,
  paquetes permitidos, campos seguros y una segunda confirmación para escribir texto.

No se extraen resultados ni streams, no se omiten DRM y no se automatizan pagos,
mensajes, contraseñas ni instalaciones.

## 3. Modelo local: descarga preparada y modo rápido

- El GGUF es **autocontenido** (incluye vocabulario) y el motor llama.cpp ya viaja
  dentro de la APK: no hay dependencias descargables adicionales para inferir.
- Nueva opción **«Descargar también modelo rápido 0.5B (+491 MB)»** en la tarjeta del
  modelo inicial. Si está activa, al descargar el modelo principal se descarga y
  verifica por SHA-256 un Qwen2.5 0.5B Q4_K_M; así se puede cambiar a respuestas
  rápidas sin Internet. Por defecto está desactivada para no consumir datos sin aviso.
- El flujo de primera descarga sigue verificando tamaño/cabecera/SHA, copiando a
  almacenamiento privado, cargando en CPU y haciendo **warm-up** antes de conversar.
- Ajustes nativos heredados de 0.10: batch/ubatch 512, hilos automáticos 2–6,
  Flash Attention, caché KV Q8_0, `no_perf`, `-O3` y precalentamiento. En 0.11 el
  estado de carga etiqueta esos valores como **solicitados** (no medidos) y el
  precalentamiento ya no borra una cancelación solicitada entre carga y warm-up.

## 4. Mascota más expresiva

Además de parpadeo, mirada, boca y manos de 0.10:

- **Parpadeo doble** ocasional en el ciclo de expresiones.
- **Mechones que se balancean** con más suavitud cuando Sakura piensa.
- **Rubor** en eventos felices (`Saludo`, `Saltito`, `Lista`).
- **Tres partículas de polen** orbitando alrededor de la silueta.
- El jardín ya propaga el estado `pensando` a la mascota en vez de forzarlo a `false`.

Sigue siendo animación vectorial en Compose Canvas, **no Live2D ni lip-sync real**, y
se pausa fuera de primer plano o al desactivar Movimiento.

## 5. Biblioteca y pruebas

- 140 skills (4 nuevas: edición GitHub desde el agente, issues, apps por nombre y
  modelo de respaldo). Correcciones de texto: «usar Buscar» y «Trivia educativa».
- `Sakura010Test` amplía cobertura de los nuevos campos/validaciones, alias de apps y
  las funciones puras de animación. CI ejecuta `testDebugUnitTest` y `assembleDebug`.

## 6. APK

El workflow `build-apk.yml` obtiene la versión directamente de `versionName` en
`app/build.gradle.kts` y publica, solo tras CI verde en `main`:

- `apk/SakuraAgent-0.11.0-arm64-debug.apk`
- `apk/SHA256SUMS.txt`
- `apk/BUILD-0.11.0.md` con commit, ejecución, tamaño y SHA-256 reales.

El commit del bot usa `[skip ci]` y `apk/**` está excluido de los disparadores para
evitar bucles. El SHA real solo existe después de que CI publique la APK: no se inventa.

## Prueba física pendiente

Instalar en ARM64/Android 13+, descargar un GGUF, decir «hola», medir tiempo, probar
`open_app_named` + `video_search`, escribir en un campo con Accesibilidad, revisar un
`github_edit` en una rama, y comprobar parpadeo/manos/boca/voz. Sin eso no se afirma
que la app sea más rápida ni que cada acción externa haya terminado.
