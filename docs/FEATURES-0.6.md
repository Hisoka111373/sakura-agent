# Sakura 0.6 — agente integrado, supervisado y acotado

Integra las capacidades útiles de la rama `sakura-v0.5-agent` (PR #6) sobre
`main` 0.5 sin reemplazar Jardín, sus imágenes, biblioteca de 93 skills ni favoritos.
No es un merge directo del PR conflictivo: su bucle y adaptadores se sustituyen
por un controlador por eventos con revisión individual y sin automatización encadenada.

## Primer uso

1. Instala la APK debug en Android 13+ ARM64. Los pesos y claves no están incluidos.
2. Para local: Modelos → importar/cargar un GGUF confiable. Empieza por un modelo
   pequeño; contexto 2048, máximo 384 tokens de salida. Un prompt largo puede fallar;
   8 GB no garantiza velocidad ni memoria suficiente.
3. Para nube: APIs → proveedor/modelo exacto de texto Chat Completions; Privacidad →
   guardar su clave oculta/cifrada y autorizar Internet. Guardar no prueba la conexión.
4. Agente → objetivo corto, sin secretos → Iniciar. En local solicita una propuesta;
   en nube muestra primero el JSON completo y coste potencial para confirmar.
5. Revisa el JSON de herramienta; corrige datos erróneos → Validar y revisar acción.
   Lee la vista previa y confirma o cancela. Cancelar no ejecuta esa acción.
6. Tras el resultado, pulsa **Proponer siguiente paso** si quieres continuar.
   En nube revisa otra vez el JSON: puede contener fragmentos privados de resultados.
   Nunca continúa por sí solo. Límite 10 propuestas incluidas las inválidas/finales.
7. Detener invalida propuestas pendientes y solicita cancelación de operaciones
   de lectura/descarga/inferencia propias. No garantiza deshacer mensajes, trabajos
   fal.ai o peticiones ya enviados, ni reembolso. Comprueba el servicio antes de repetir.

El chat normal mantiene sus 93 skills y no ejecuta el protocolo del agente.
Plan sigue disponible con JSON revisable y confirmaciones por paso.

## Herramientas del agente

| Nombre | Alcance |
|---|---|
| web_search | Brave API, consulta exacta confirmada, 8 resultados; requiere clave/cuota. |
| fetch_text | Una GET HTTPS pública sin cookies/auth/redirects/JS; DNS validado en conexión, 256 KiB y 30 s, hasta 12000 caracteres visibles. |
| github_read | Archivo UTF-8 limitado, token cifrado opcional, SHA devuelto; no escribe. |
| model_list / download_model | Catálogo empaquetado; descarga exige archivo exacto y valida tamaño/hash/licencia anunciada. |
| download_url | Imagen/audio/video autorizado directo; no extrae páginas, YouTube/Piped/HLS/DRM. |
| download_gguf | URL `.gguf` y SHA-256 esperado obligatorio, hasta 2.5 GB/espacio libre; importa pero no carga. |
| image_generate / video_generate | Reutiliza fal.ai con prompt completo y aviso de posible gasto. Devuelve ID, no archivo generado. Consulta resultado en Crear sin reenviar. |
| telegram / whatsapp | APIs oficiales, envío real confirmado. Telegram bot, WhatsApp Business y ventana de servicio de 24 h; no lee chats. |
| list_apps / open_app | Actividades de inicio visibles/exportadas, sin QUERY_ALL_PACKAGES. Abrir no automatiza ni verifica su contenido. |
| open_url / youtube_search | Abre app/navegador oficial mediante Android y confirmación, no prueba reproducción ni descarga. |
| screen_start | Solo allowlist YouTube/Chrome/Firefox/Play Store; sesión manual 3 min, Stop visible. Cada lectura/escritura/clic se revisa en el servicio. Sin capturas/envío de pantalla. |

No shell, instalación automática, pagos, credenciales como argumentos, mezcla de PR,
ni automatización general de apps sensibles. Las funciones GitHub de escritura siguen
en la pestaña GitHub con sus propias confirmaciones/SHA; no se dan al modelo como tools.

## Integridad y consentimiento

- Parser de JSON estricto de objetos/cadenas, sin claves duplicadas, campos extra,
  números coercionados, anidación libre, múltiples objetos, Markdown ni prosa alrededor
  de una herramienta. Texto normal se trata como respuesta final, nunca acción.
- Cada herramienta valida sus campos y límites antes de preparar un callback interno.
  El modelo no recibe callbacks/credenciales ni decide permisos de red.
- Resultado de web/GitHub/app es dato no confiable. Hasta dos fragmentos de 1000
  caracteres entran en siguiente contexto, más objetivo de 1500 caracteres.
- En nube se muestra el **mismo payload** que se envía, sin clave, antes de cada petición.
  Puede incluir datos privados de GitHub/apps si tú confirmas; el proveedor tiene su
  propia retención. No se incorporan chat, recuerdos ni pantalla.
- Registro de resultados del agente en RAM, máximo 12000 caracteres por evento;
  no es una bitácora completa del servidor ni demuestra cumplimiento del objetivo.
- Hash de GGUF personalizado debe obtenerse del editor por fuente independiente.
  Hash sugerido por IA no demuestra autenticidad; cabecera/hash no prueban seguridad.
- YouTube/Googlevideo/Piped se bloquean también en cada redirección de descarga.
  La lectura web no extrae enlaces de streams y no incorpora clientes Piped.
- Sustituimos el antiguo endpoint Pollinations por fal.ai existente: no se promete
  gratuidad, generaciones sin clave, ni difusión local instalada.

## Proveedores de texto

Local por defecto. OpenAI, Gemini, Anthropic, Compatible, OpenRouter, Groq, DeepSeek,
Mistral, xAI, Together y Fireworks son selecciones con claves independientes cifradas.
Usa un modelo que admita Chat Completions y el parámetro de tokens correspondiente.
Un modelo retirado/restringido puede fallar. No activa herramientas del proveedor,
fallbacks de Sakura, reintentos ni conexión al guardar. Revisa tarifas y privacidad.
OpenRouter puede enrutar internamente según su política; Sakura no controla ese routing.

Referencias oficiales consultadas para endpoints (no pruebas con claves):
- https://openrouter.ai/docs/api/api-reference/chat/create-a-chat-completion
- https://console.groq.com/docs/openai
- https://api-docs.deepseek.com/api/create-chat-completion/
- https://docs.mistral.ai/api/endpoint/chat
- https://x.ai/api (Chat Completions documentado como legacy)
- https://docs.together.ai/reference/chat-completions (`api.together.ai`, no antiguo `.xyz`)
- https://docs.fireworks.ai/tools-sdks/openai-compatibility
- https://gen.pollinations.ai/docs (el PR usaba una ruta antigua y asumía gratuidad)

## Verificación y pruebas físicas pendientes

86 pruebas JVM aprobadas y compilación estática de todos los Kotlin/Compose/API35.
Build Android real: consulta Actions del commit de 0.6; builds anteriores no lo sustituyen.
No se ejecutaron inferencia, APIs de pago, mensajes ni automatizaciones en un teléfono.

- [ ] Arrancar las 16 pestañas, conservar Jardín/favoritos tras actualización compatible.
- [ ] Modelo devuelve JSON inválido/duplicado o tool desconocida: no ejecuta nada.
- [ ] Cancelar vista previa no hace petición y permite solicitar otro paso manualmente.
- [ ] Confirmar una acción solo ejecuta esa acción y no solicita otro modelo solo.
- [ ] Cambiar configuración antes de confirmar propuesta cloud invalida la petición.
- [ ] Rechazar compartir código privado/inventario en siguiente JSON: no sale a nube.
- [ ] Detener/desautorizar red durante headers/body y verificar archivos `.part` descartados.
- [ ] GGUF con hash erróneo/cabecera inválida no se publica en Modelos; el correcto no carga solo.
- [ ] YouTube, Googlevideo, Piped y redirect hacia estos se rechazan antes de descargar.
- [ ] Límite 10 propuestas; no navegación/lectura automática fuera de sesiones permitidas.
- [ ] Mensajes/fal con cuenta de pruebas: confirmar aceptación real, comprobar estado antes de repetir timeout.

APK debug, no release para tienda; Android puede rechazar actualización si la firma
debug de CI difiere de la instalada. No desinstales sin exportar/copiar tus datos:
desinstalar borra recuerdos, claves, favoritos y modelos privados. No hay migración
automática entre certificados ni exportación de claves cifradas de Keystore.