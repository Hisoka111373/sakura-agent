# Sakura 0.7 — Gemini, mascota viva y respuesta acotada

Conserva Kotlin/Compose, Jardín, 93 skills y agente supervisado de 0.6.

## Gemini comprobado sin incluir secretos en la APK

Se comprobó la clave entregada por formulario seguro contra Google Gemini:
listado de modelos HTTP200 y respuesta real **«Sakura conectada.»** con
`gemini-3.1-flash-lite`. También pasó una petición con el **TextSearchClient
real de Sakura** desde JVM (~1.35 s en esa única prueba). No es una medida de
rendimiento de tu móvil ni una garantía de disponibilidad/precio futuros.

Una prueba inicial con96 tokens y thinking LOW no dejó texto visible: el presupuesto
se consumió en pensamiento. Se corrigió a MINIMAL y mayor presupuesto; no se finge
una respuesta cuando Gemini devuelve solo pensamiento o agota la salida.

**La clave no está en fuentes, GitHub, ZIP ni APK.** En el teléfono:
1. Privacidad → credenciales de servicios → `gemini_key` → campo oculto → Guardar cifrado.
2. APIs → Gemini → `gemini-3.1-flash-lite` → Guardar selección.
3. Autoriza Internet en Privacidad.
4. APIs → Revisar prueba real de Gemini → lee JSON/coste y confirma.

Se guarda en Android Keystore/AES-GCM. La prueba móvil no incluye chat, recuerdos,
archivos ni pantalla. Cada petición posterior exige su propia revisión. No guardar
una clave en una APK pública es intencionado: cualquiera puede extraerla.

## Cambio Local / En línea y pensamiento

Selector visible en Chat, Agente y APIs. Guarda el último proveedor online separado
del modo local: cambiar de modo no borra el GGUF ni manda una petición. Local sigue
requiriendo modelo cargado; online requiere proveedor/modelo y clave válidos.

| Ajuste | Bajo | Alto |
|---|---|---|
| Chat local | Hasta192 tokens, una pareja anterior, cuatro notas | Hasta384 tokens, tres parejas, doce notas |
| Límite local cooperativo |45 s|90 s|
| Agente local |Hasta384 tokens para JSON, mismo límite temporal del nivel|Hasta384 tokens,90 s|
| Gemini 3.1 Flash-Lite |MINIMAL,512 tokens totales|HIGH,2048 tokens totales|
| Otros proveedores de texto |768 tokens|1024 tokens|

En Gemini el presupuesto incluye pensamiento y texto. Alto puede aumentar latencia
y consumo; no revela razonamiento interno. Otros modelos Gemini conservan el thinking
del proveedor: no se envía un parámetro inventado para modelos no comprobados.

## Esperas locales y progreso

- JNI informa progreso y respuesta parcial mientras genera, hasta cada cuatro tokens;
  ya no se muestra únicamente un spinner durante toda la respuesta.
- Contexto/historia y longitud menores en Bajo reducen el trabajo solicitado.
- Deadline cooperativo comprobado también en prefill y callback de abort de llama.cpp;
  al agotarlo se muestra un error útil, se restaura el borrador y no se guarda parcial.
- Cancelación sigue disponible; resultados tardíos de progreso se descartan por ID.
- Batch JNI usa RAII para liberar memoria incluso si falla un callback.

No es un benchmark ni prueba de velocidad física. El deadline no es un watchdog capaz
de matar una biblioteca nativa bloqueada; depende del abort cooperativo de llama.cpp.
Si sigue siendo lento, usa GGUF1.5B Q4, mensajes cortos y Bajo, o Gemini. Cargar pesos
grandes también puede tardar; no prometemos inferencia rápida con cualquier móvil8GB.

## Mascota y voz

Avatar original offline: flotación/respiración, giro suave y pulso cuando el TTS
real está hablando. Pausa en segundo plano; movimiento desactivable en Privacidad.
No es Live2D, nuevos frames de anime ni sincronización real de labios.

- Tono kawaii suave en respuestas, activado por defecto y desactivable. Mantiene
  precisión y tono de compañera adulta; no infantiliza ni inventa acciones.
- Saludar en Chat usa voz española local instalada y un saludo corto.
- Ritmo/pitch ajustados al tono kawaii; calidad/personaje dependen del motor Android.
- Lectura automática **opcional y confirmada**, solo voz local y primer plano.
  No activa micrófono ni manda texto a TTS en nube. Si no hay voz local, informa.
- Silencio, cambiar a segundo plano y desautorizar red detienen voz. Utterances
  tienen ID para que callbacks antiguos no animen una lectura nueva.

## Abrir apps, escribir y buscar

En Chat, **Abrir apps / escribir / buscar con Agente** lleva tu petición al objetivo
sin enviarla ni borrar el borrador. Tú inicias y revisas cada propuesta. Agente puede
listar/abrir actividades de inicio visibles de APK **ya instaladas**, consultar Brave
con clave o abrir búsqueda oficial externa con confirmación.

Pantalla permite ver controles accesibles y escribir/buscar en apps autorizadas
YouTube/Chrome/Firefox/Play Store, mediante sesión limitada y revisión individual.
No controla cualquier APK, no instala archivos APK, no elude restricciones Android,
no automatiza pagos/login/mensajes/permisos y no garantiza reconocimiento de todo control.
Las peticiones web no se ejecutan en secreto desde una respuesta normal de Chat.

## APK en GitHub

Se compila únicamente en GitHub Actions, no en el sandbox. Tras CI aprobado se publica
el archivo verificado en `apk/`, mediante commit/PR separado del código para no
recompilar únicamente por añadir una APK. Allí se indica commit fuente, SHA-256,
versión/SDK/ABI y firma comprobada. APK debug, no release de tienda.

Android13+ ARM64. Si la firma debug difiere de la app instalada, Android puede rechazar
actualización. **No desinstales sin conservar tus datos**: recuerdos, claves y modelos
privados se borran al desinstalar. El cifrado Keystore no migra entre certificados.

## Verificación

106 pruebas JVM PASS; todos Kotlin/Compose/API35 compilan estáticamente; C++17/JNI
con `-Wall -Wextra -Werror` contra revisión fijada PASS. Gemini real desde cliente JVM
PASS. CI Android del nuevo commit debe aprobar para publicar APK0.7.1.

Pendiente en teléfono: arranque, JNI y progreso real, latencia/RAM pico/temperatura,
Keystore, voces instaladas, animación/rotación, pruebas móviles Gemini, descargas,
accesibilidad, apps y servicios. No se enviaron mensajes ni se generaron imágenes/videos
desde servicios de Sakura durante estas pruebas.

Referencias de investigación:
- https://ai.google.dev/gemini-api/docs/thinking
- https://aistudio.google.com/docs/models
- https://ai.google.dev/api/generate-content

La disponibilidad se verificó con la API y la cuenta aportada, no se asumió que un
modelo de una versión antigua siguiera disponible para todos los usuarios.