# Sakura 0.8 — conexión clara y chat más vivo

## Objetivo

Corregir la configuración de proveedores en la nube (especialmente Gemini),
hacer explícita la elección Local/Online, añadir un modo de pensamiento bajo/alto
y ampliar las acciones que se pueden preparar desde el chat sin dar autonomía al
modelo. La mascota y la voz ganan animación y expresividad, manteniendo el tono
kawaii adulto.

## Gemini 2.5 y prueba de conexión

- Modelos sugeridos: `gemini-2.5-flash`, `gemini-2.5-flash-lite` y
  `gemini-2.5-pro`. Se acepta el ID con o sin prefijo `models/` sin duplicarlo.
- En `gemini-2.5-flash`, pensamiento bajo envía `thinkingBudget=0` y alto
  `thinkingBudget=1024`. En otros modelos no se envía `thinkingConfig`.
- La salida usa `maxOutputTokens`; las respuestas que solo traen partes
  `thought` no se muestran como respuesta final.
- Los errores HTTP incluyen código, host y el mensaje del proveedor, saneado y
  limitado. Nunca se muestra la clave ni el cuerpo completo de la petición.
- **Probar conexión** usa el formulario visible y la clave escrita, aunque no se
  hayan guardado. Envía únicamente «Responde únicamente: OK» con 32 tokens y
  pide confirmación. Sigue consumiendo cuota.
- **Guardar y usar en el chat** guarda la selección, activa el modo Online y
  vuelve a Chat. Guardar no conecta ni valida la clave.

## Local u Online, y pensamiento

- Chips en Chat: `Local`, `Online`, `Pensar: bajo` y `Pensar: alto`. La elección
  se guarda cifrada en preferencias. Online exige proveedor distinto de Local y
  una clave guardada; si falta, Sakura abre APIs.
- **Bajo** (local): 4 mensajes anteriores, 160 tokens de salida, temperatura
  0.55, instrucción de respuesta breve.
- **Alto** (local): 6 mensajes anteriores, 320 tokens de salida, temperatura 0.8,
  instrucción de análisis y riesgos. Tarda más; no es una garantía de calidad.
- **Bajo** (nube): 4 mensajes, 512 tokens. **Alto** (nube): 6 mensajes, 1024
  tokens. La nube sigue mostrando la solicitud completa antes de enviar.
- Cada respuesta local/nube respeta el modo elegido; las acciones de chat se
  preparan antes de llamar al modelo y siempre requieren confirmación.

## Acciones desde el chat

- «Abre YouTube» / «abre Chrome»: busca una app con actividad de inicio visible y
  pide confirmación. No abre paquetes arbitrarios ni ejecuta acciones dentro.
- «Abre https://…»: valida HTTPS público y abre el navegador tras confirmar.
- «Escribe hola en Chrome» o «abre YouTube y busca lofi»: prepara una sesión de
  Accesibilidad limitada a YouTube, Chrome, Firefox o Play Store. Primero abre la
  app y muestra los controles; la escritura se confirma aparte.
- «Busca …» conserva el flujo de 0.7: Brave con confirmación o apertura externa.
- El modelo no recibe permiso para actuar: estas órdenes se interpretan con
  reglas locales y se muestran en el diálogo de revisión.

## Rendimiento local

- Hilos de llama.cpp según CPU, entre 2 y 8 (`n_threads` y `n_threads_batch`).
- `n_batch=512`, `n_ubatch=256`, contexto 2048. Sigue siendo CPU ARM64, sin GPU.
- El sampler se reconstruye con la temperatura del modo. La salida se limita a
  160/320 tokens en Chat; otras rutas conservan sus topes.
- No se promete rapidez. Un teléfono con poca RAM libre puede fallar al cargar.

## Mascota y voz

- Nuevas animaciones Compose: respiración, balanceo, rebote, parpadeo, brillo,
  chispas orbitales y nota musical al generar o descargar.
- 20 pétalos con velocidades y giros variados en Jardín.
- Frases kawaii rotativas (saludo y conversación), sin contenido sexual ni trato
  infantil. El botón «Otra frase» pide una nueva línea.
- `autoSpeak` queda activado por defecto para nuevas instalaciones; se puede
  desactivar en Chat o Privacidad. La voz española local se prioriza; si el TTS
  aún no está listo, la última frase queda pendiente y se lee al inicializar.
- Tono: pitch 1.14 y velocidad 1.03 sobre la voz instalada en Android. No hay
  voz clonada ni audio subido a un servidor.

## Pruebas añadidas

- URL de Gemini con prefijo `models/`, `thinkingBudget` 0/1024 y ausencia de
  `thinkingConfig` en modelos distintos de 2.5 Flash.
- Acciones locales: enlace HTTPS, abrir app, escribir en Chrome, abrir YouTube y
  buscar, además de rechazo de texto normal y de apps fuera de la lista.
- Se conservan las 92 pruebas anteriores; el total se valida en CI.

## Límites

- No se ha probado en un teléfono real. La compilación y las pruebas JVM se
  ejecutan en GitHub Actions.
- La disponibilidad de Gemini, sus límites gratuitos y sus modelos dependen de
  Google y de la cuenta. Un error 400/403/429 se muestra, no se reintenta.
- «Pensamiento alto» no equivale a un modelo razonador ni garantiza mejores
  respuestas; solo cambia contexto, tokens, temperatura e instrucción.
- Las acciones de chat no son herramientas del modelo ni un agente autónomo.
  Abrir una app no permite leer su pantalla ni operar Cuenta, pagos o mensajes.
- La APK es debug ARM64 y no está firmada para tienda.
