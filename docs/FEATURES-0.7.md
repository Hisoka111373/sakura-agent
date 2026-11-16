# Sakura 0.7 — modelo bajo demanda, voz y mascota

## Objetivo

Mantener una APK ligera (<100 MB) sin pesos GGUF dentro. En el primer inicio la
app descarga el modelo recomendado, lo verifica, lo copia a su almacenamiento
privado y lo carga. La experiencia final es la de una app “completa”, pero el
repositorio y la APK no crecen 1,1 GB.

## Modelo inicial

- Qwen2.5 1.5B Instruct · Q4_K_M · Apache 2.0.
- Archivo: `qwen2.5-1.5b-instruct-q4_k_m.gguf`.
- Tamaño: 1.117.320.736 bytes.
- SHA-256: `6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e`.
- URL: `https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/91cad51170dc346986eccefdc2dd33a9da36ead9/qwen2.5-1.5b-instruct-q4_k_m.gguf`.
- Se descarga a `noBackupFilesDir/models`, con archivo `.part`, límite de espacio,
  timeout de 90 minutos, cancelación y SHA-256 obligatorio. No se ejecuta ni se
  comparte con terceros; solo se hace la petición HTTP a Hugging Face.
- Tras verificar, se carga con llama.cpp en CPU (contexto 2048, salida máxima 384).
- Si falla, no se reintenta en bucle: queda un botón de reintento. La descarga
  automática se puede desactivar en Privacidad.

## Voz

- `autoSpeak` persistente: lee respuestas locales y de API.
- Botón “Escuchar saludo” en Jardín y “Voz auto” en Chat.
- `SakuraVoice` sigue priorizando voces españolas locales; si no hay, avisa y solo
  usa una voz de red cuando el interruptor de Internet está activo.
- No se sube audio a Sakura. El dictado usa el servicio de Android y nunca envía
  el mensaje automáticamente.

## Mascota y Jardín

- Animación Compose: zoom/parallax lento del jardín, pétalos dibujados en Canvas,
  respiración y brillo del avatar.
- Avatar animado en cabecera, Jardín, Chat y burbujas de Sakura.
- No se añaden GIFs, videos ni recursos de red; todo se dibuja en el dispositivo.

## Modelos visibles

- Pestaña Modelos movida junto a Chat.
- Tarjeta del modelo inicial con progreso, cancelar, reintentar y cargar.
- Catálogo filtrable con licencia, tamaño, advertencia, tarjeta externa y descarga
  con confirmación.
- Modelos instalados con Cargar/Borrar y medición de RAM.
- Descargas y GGUF personalizado siguen disponibles en su pestaña.

## Claves API

- En APIs: proveedor, modelos sugeridos, campo de clave con mostrar/ocultar,
  guardar/borrar cifrado y Probar conexión.
- Probar conexión envía una petición mínima revisada y puede consumir cuota.
- En Privacidad: selector de credenciales de servicios con el mismo campo oculto.

## Búsqueda desde el chat

- Detecta «busca», «búscame», «buscar», «encuentra», «search» e «investiga».
- Con clave Brave: confirmación y hasta 5 resultados en el chat.
- Sin clave Brave: confirmación y apertura de YouTube, navegador o Play Store.
- No se abren ni descargan resultados automáticamente; son contenido no confiable.
- La búsqueda no sustituye una verificación de fuentes.

## Límites

- Sigue sin probarse en un teléfono real en esta entrega.
- 8 GB de RAM es un objetivo, no una garantía. El archivo de 1,1 GB no es la RAM
  que usará el modelo.
- La descarga puede fallar por red, cuota, DNS o espacio; se muestra el error.
- La APK es debug y no está firmada para tienda.
- No se incluyen pesos ni se redistribuyen; se descargan desde el origen oficial.
