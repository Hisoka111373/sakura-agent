# Sakura 0.3 — configuración y alcance verificable

## Decisiones acordadas

Conservar Kotlin/Compose y diseño nocturno rosa/turquesa. Teléfono declarado: 8 GB
de RAM; modelo, SoC y versión de Android aún no especificados. No puede prometerse
rapidez ni generación visual local viable basándose solo en RAM total.

No se envían mensajes, suben archivos, descargan pesos ni gastan créditos al instalar.
Internet vuelve a desautorizarse en cada sesión. Toda escritura/envío/generación
requiere una vista previa y confirmación de esa acción; la planificación no concede
permisos. No hay shell, control por accesibilidad, root ni ejecución remota de código.

## Chat y catálogo de modelos

Qwen2.5-3B-Instruct Q4_K_M oficial: 2,104,932,768 bytes. La estimación informal
de 1.9 GB no es el tamaño decimal de este archivo; son ~1.96 GiB. Se fija revisión
de Hugging Face y hash LFS. Qwen 3B tiene licencia específica, no asumir Apache 2.0.
El catálogo también incluye Qwen 1.5B, SD1.5 y LTX 2B FP8 con tamaños/hashes fijados.

Descargas de chat se guardan en Modelos y todavía necesitan pulsar Cargar. Las
visuales se guardan como archivos privados; **no son un motor instalado**. Importar
chat conserva el límite 2.5 GB. Checkpoints visuales del catálogo: límite 5 GB.
No se incluye ninguna descarga de miles de millones de parámetros en pruebas CI.

La tarjeta/licencia puede abrirse explícitamente en el navegador, que tiene sus
propios permisos y conexiones. Descargar exige autorizar Internet en Sakura.
El downloader no envía credenciales, no usa cookies/proxies, acepta HTTPS y valida
IPs públicas en el DNS usado para conectar. Cada redirección se valida; máximo 5.
Se comprueba tamaño/hash antes de publicar el archivo y se descarta el parcial en
fallos. Máximo 45 minutos; sin reanudación/servicio background. Si Android mata el
proceso, los `.part` se limpian al iniciar de nuevo. No es aislamiento de parsers
nativos ni una garantía de que un archivo sea seguro.

## WhatsApp y Telegram

**Abrir app:** ACTION_SEND dirigido a la app oficial instalada. El usuario elige
contacto y pulsa Enviar allí. Sakura solo verifica haber solicitado la apertura;
no puede afirmar que el mensaje se envió o leyó. No necesita tokens de mensajería.

**Telegram API:** crea un bot con BotFather y guarda su token en el campo oculto
de Privacidad. Introduce un Chat ID o @canal donde el bot pueda enviar. Los bots
no pueden iniciar una conversación privada con un usuario que no los haya iniciado.
No equivale a conectar la cuenta personal ni leer su historial. Texto hasta 4096
caracteres. Se muestra `message_id` devuelto, sin afirmar lectura del destinatario.

**WhatsApp API:** configura WhatsApp Business Platform/Cloud API en Meta; registra
el número y configura token con permisos de mensajería y **Phone Number ID** (no el
número de teléfono). Usa destinatario internacional sin `+` ni separadores. Graph
API v23.0, texto hasta 4096 caracteres, solo en ventana de servicio de 24 horas.
Fuera de ella hacen falta plantillas aprobadas, no incluidas en este adaptador.
Verifica consentimiento y políticas de Meta. La respuesta `wamid` indica aceptación,
no entrega o lectura: eso necesita webhooks e infraestructura no incluida.

No hay reintentos automáticos. Un timeout puede ocurrir después de enviar; consulta
Telegram/Meta antes de repetir. No se realizan campañas ni envíos masivos.

## Imagen/video en nube (opcional)

Proveedor implementado: fal.ai. Guarda una clave limitada/cuota en Privacidad y
revisa la tarifa en el proveedor antes de confirmar. Esta app no calcula ni impone
un tope monetario. No recibe claves al descargar ni en el prompt del LLM local.

- Imagen: `fal-ai/flux/schnell`, una imagen, tamaño `square`, 4 pasos, safety checker.
- Video: `fal-ai/ltx-video` (0.9.1), parámetros por defecto del proveedor y safety checker;
  revisar licencia de investigación indicada en su ficha. No garantiza duración/calidad.
- Una confirmación hace una sola petición a Queue API. Guarda último `request_id`
  cifrado y ofrece recuperación por ID. Consultar no repite generación; no polling
  continuo en background. Cancelar puede llegar tarde y no garantiza reembolso.
- Consultar verifica estado y recibe URLs. Descargar cada resultado requiere otra
  confirmación. Las URLs de fal pueden ser públicas y expirar: no subir material
  confidencial sin entender los términos del proveedor.

No se enviaron prompts a fal.ai ni mensajes a Meta/Telegram durante desarrollo.
Los campos de configuración no son pruebas de conectividad; sin claves válidas,
cuota y permisos las APIs darán error. Nunca pegues credenciales en Chat o Plan.

## GitHub

Lecturas: árbol (máximo 300 rutas), texto UTF-8 ≤64 KiB, primeras páginas de ramas,
commits y PR abiertos. No afirmar que representan todo un repo grande.

Escrituras: editar/borrar archivo leído con SHA, crear archivo sin SHA (rechaza si
existe), crear rama desde SHA de base mostrado y crear PR entre ramas existentes.
Se muestra contenido completo antes/después para editar, contenido eliminado para
borrar, repo/rama/ruta y mensaje. No se administran permisos/secretos ni se borran
repositorios; no se mezcla automáticamente un PR. Para publicar código grande o
multifichero complejo sigue usando Git/PR y CI en tu entorno de desarrollo.

Permisos mínimos: Contents read para lectura privada; write para ramas/commits;
Pull requests write para PR; Workflows write solo si cambias workflows. El token
solo viaja a api.github.com, sin redirecciones. El token usado para integrar este
repositorio NO se incrusta en la APK ni configura tu Sakura automáticamente.

## Planificador controlado

El modelo local propone JSON de 1 a 6 pasos, limitado a REVIEW, DOWNLOAD,
GITHUB_READ, TELEGRAM, WHATSAPP y CLOUD. No inventar URLs/contactos: pedir datos
si faltan. Salida inválida/JSON truncado no ejecuta nada. El límite de 384 tokens
del motor puede requerir pedir planes de 1–2 pasos.

Se puede editar y revalidar el JSON. Cada paso prepara una acción con datos exactos
y requiere confirmar; el siguiente paso no se ejecuta solo. REVIEW únicamente
coloca texto en el borrador. Los resultados de API/hash/SHA se muestran como evidencia;
aceptación de un trabajo no equivale a tener un archivo generado. No hay registro
persistente del plan/chat ni evaluación automática de éxito por otro LLM.

## Evaluación de generación visual local

llama.cpp es el motor de chat, no de difusión. `stable-diffusion.cpp` documenta
Android mediante Termux/Local Diffusion y distintos backends, pero eso no demuestra
compatibilidad ni rendimiento de JNI dentro de esta app. SD1.5 necesita runtime
compatible y memoria de trabajo. LTX checkpoint FP8 puede requerir encoder/VAE,
conversiones y más memoria que el archivo por sí solo. No basta con descargarlo.

Antes de integrar un segundo motor: conocer SoC/Android, elegir modelo/runtime y
licencia, fijar revisión, comprobar ARM64/Vulkan si procede, descargar solo con
consentimiento y medir RAM pico/temperatura/latencia en el teléfono. Liberar Qwen
antes de difusión para evitar coexistencia innecesaria. Video local en 8 GB no está
aprobado ni implementado aquí. No ofrecemos botones de generación local simulada.

## Pruebas manuales pendientes

- Arranque, las 10 pestañas, teclado, rotación y proceso terminado durante descarga.
- Qwen 3B y alternativa 1.5B: tiempo de carga/respuesta, RAM pico, temperatura.
- Descarga pequeña legítima y redirecciones; cancelar; hash incorrecto; límite de
  tamaño/espacio; dominio privado bloqueado; exportar copia con selector Android.
- Mensajería: cancelar no envía; compartir borrador no envía automáticamente;
  bot/cuenta Business de prueba, sin datos sensibles; rechazos y timeout ambiguo.
- fal.ai: con consentimiento de gasto y cuenta de prueba, submit/ID/consulta/cancelación,
  recuperación tras reinicio y descarga autorizada. No generar lotes de prueba.
- GitHub en repo/rama de prueba: SHA conflictivo, archivo ya existente, crear rama
  y PR con permisos mínimos; cancelar todas las confirmaciones no produce commits.
- JSON de plan malicioso/truncado; cambiarlo exige revalidar; confirmar un paso no
  dispara los demás. Borrar claves locales no revoca las del proveedor.

## Fuentes técnicas consultadas (12 noviembre 2026)

- https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF
- https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF
- https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-v1-5
- https://huggingface.co/Lightricks/LTX-Video
- https://core.telegram.org/bots/api#sendmessage
- https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started
- https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/send-messages
- https://developers.facebook.com/docs/graph-api/changelog/
- https://fal.ai/docs/documentation/model-apis/inference/queue
- https://fal.ai/models/fal-ai/flux/schnell/api
- https://fal.ai/models/fal-ai/ltx-video/api
- https://github.com/leejet/stable-diffusion.cpp

Catálogo/endpoints/licencias pueden cambiar. Los hashes fijados protegen integridad
de la revisión elegida, no la disponibilidad futura del servidor ni la seguridad del modelo.