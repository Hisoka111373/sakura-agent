# Sakura 0.4 — skills, búsqueda, APIs y pantalla

Mantiene Kotlin/Compose, Android 13+, ARM64 y el diseño nocturno rosa/turquesa.
La 0.3 pasó CI (run 43551453495) y se integró en main por PR #4. Esta ampliación
se trabaja en `sakura-v0.4-skills`: comprobar su propio CI antes de instalar.
No se probó en un teléfono ni se hicieron consultas/generaciones API pagadas.

## Skills reales dentro de Sakura

La pestaña Skills contiene 21 accesos: programar, depurar, revisar seguridad,
resumir, traducir, aprender, redactar, comparar, planificar, buscar web/música/juegos,
descargar archivos, revisar código/APK, apps, GitHub, memoria, modelos,
imagen/video en nube, mensajes y proveedores de texto.

Las skills de texto preparan un borrador; no ejecutan código, no reentrenan pesos
ni se convierten en plugins del modelo. Las demás abren herramientas existentes.
El plan JSON local conserva su lista de acciones limitada: no puede ordenar
acciones de pantalla ni invocar un proveedor por su cuenta. No hay ejecución
automática/encadenada, shell, screenshots ni compra/instalación de APK.

## Ejemplo: «busca una canción de Maná»

1. Chat → escribe la petición → **Preparar como búsqueda** (no se envía al LLM),
   o abre Buscar y pulsa Preparar destino.
2. La heurística local sugiere YouTube; puedes cambiar destino/consulta.
3. Revisa búsqueda externa → confirma el enlace completo y la app destinataria.
4. Android abre resultados de YouTube si la app está instalada. No garantiza
   reproducción ni que aparezca el video deseado. Usa Web para navegador si falla.

Juegos abre resultados de la tienda oficial Google Play. Tú decides si instalas
o compras en la tienda. No se obtienen juegos/APK de repositorios desconocidos.
Las apps externas tienen cuenta, permisos de red y políticas propios: el
interruptor de Sakura no es un firewall para ellas.

### Brave dentro de Sakura

Privacidad → Clave Brave Search → guardar cifrado; autoriza Internet.
Buscar → consulta → Revisar búsqueda Brave API → confirmar. Envía solo consulta
a `https://api.search.brave.com/res/v1/web/search` con `X-Subscription-Token`,
count=8 y safesearch=moderate. Tu plan puede cobrar cada petición; revisa cuota.
No se envían recuerdos/chat ni se siguen enlaces automáticamente. Se muestran
título, descripción y enlaces HTTPS no confiables, sin interpretarlos como órdenes.

Puedes revisar abrir un resultado o descargarlo **solo si es archivo directo
público autorizado**. Una página de YouTube no es un archivo descargable:
no hay extractor, HLS, cookies privadas ni evasión de DRM. Si no dispones de
URL directa legal, busca la opción oficial de descarga o pide el archivo al autor.
Descargas admite imagen/video y audio MP3/WAV/FLAC/OGG, hasta 512 MiB.
Se verifica firma básica de formato, no integridad audiovisual ni seguridad completa.
No se extrae audio de videos ni se implementa reproducción musical en Sakura.

## Texto en nube con consentimiento

APIs → elige proveedor, introduce ID exacto de un modelo disponible en tu cuenta
y guarda selección. Claves **ocultas y cifradas con Android Keystore** en Privacidad:

| Proveedor | Clave | Protocolo |
|---|---|---|
| OpenAI | openai_key | POST api.openai.com/v1/chat/completions, Bearer, max_completion_tokens |
| Compatible | compatible_key | Endpoint HTTPS público completo elegido por ti, Bearer, max_tokens |
| Gemini | gemini_key | generateContent de generativelanguage.googleapis.com/v1beta, x-goog-api-key |
| Anthropic | anthropic_key | api.anthropic.com/v1/messages, x-api-key, anthropic-version 2023-06-01 |

Compatible requiere Chat Completions y `choices[].message.content`; no significa
cualquier API/proveedor. No admite LAN, IP literal, HTTP, puertos especiales,
userinfo, fragmento ni parámetros query en el endpoint. Verifica dominio antes
de entregar tu clave compatible. Se valida DNS público en la conexión; no hay
redirecciones, proxy, cookies, logging HTTP ni reintento automático.

Cada Enviar muestra endpoint/modelo y **JSON completo sin clave**, incluidos
mensaje de sistema, hasta tres parejas anteriores y borrador. No incluye notas
guardadas, archivos, pantalla ni token de otra integración. Revisa también el chat:
si pegaste secretos allí, forman parte del texto que podrías autorizar enviar.
Un borrador o configuración cambiado invalida la solicitud preparada.

Una petición, máximo 768 tokens de salida (puede incluir pensamiento según el
proveedor/modelo), respuesta limitada a 1 MiB, timeout 100 segundos. No es tope
monetario, garantía de respuesta ni compatibilidad con todos los modelos.
El límite pequeño puede truncar respuestas. No hay streaming ni tools remotas.
Los bloques tool_use/funciones no se ejecutan. Guarda claves no prueba conexión;
IDs/modelos, cuotas, facturación y retención son responsabilidad del proveedor.
Cancelación/timeout no garantizan que no se cobró; comprueba consumo antes de repetir.
Chat permanece en RAM, pero un proveedor puede retener solicitudes según sus términos.
Para volver a privacidad local, selecciona Local en APIs y guarda.

## Accesibilidad: búsqueda manual en apps permitidas

Pantalla ofrece YouTube, Chrome, Firefox y Play Store. Otras apps visibles se
pueden abrir desde Herramientas, **no se automatiza cualquier APK**. El servicio
declara `isAccessibilityTool=false`: es automatización, no herramienta que pretenda
asistir una discapacidad. Una futura distribución en tienda requerirá revisar
políticas, declaración y consentimiento de Accesibilidad; no se garantiza aceptación.

1. Lee la divulgación y revisa Abrir Ajustes. Activa tú el servicio de Sakura en
   Android; no se otorgan permisos automáticamente. Si Android restringe servicios
   instalados externamente, tú decides en sus ajustes oficiales; Sakura no los cambia.
2. Elige app/consulta → revisa iniciar sesión y abrir app → confirma.
3. Un panel visible con **DETENER fijo** permanece hasta 3 minutos; expira solo.
4. Ver controles hace una lectura puntual de controles de búsqueda de la app
   permitida. No se lee nada en eventos/reposo. Elegir un control no lo pulsa.
5. Revisa → confirma pulsar Buscar, o escribir la consulta completa. Escribir no
   inicia búsqueda; vuelve a Ver controles y confirma Buscar con el texto actual.
6. Solo si Android ofrece ACTION_IME_ENTER se permite ese paso. Si no, usa teclado
   o botón manual. Paquete/ventana/ruta/etiqueta/bounds/texto se comprueban de nuevo;
   un cambio rechaza la acción y requiere otra revisión.

No hace capturas, transmite pantalla, usa LLM para leerla, gestos por coordenadas,
mensajes, pagos, login, contraseñas, permisos, borrado ni instalación. Los controles
son una lista restrictiva de búsqueda/atrás/cerrar, no control general de UI.
Las heurísticas/etiquetas accesibles no son una barrera universal contra apps
maliciosas: usa solo apps oficiales confiables y revisa lo que confirmas.
La lectura recorre como máximo 200 nodos/12 niveles, muestra hasta 12 controles;
no conserva un registro. Diseños custom o versiones/idiomas distintos pueden fallar.
DETENER finaliza sesión, **no revoca el permiso Android**. Desactívalo en Ajustes
al terminar. Sesión se detiene también al interrumpir/destruir servicio/VM.

## Pruebas manuales pendientes

- [ ] 14 pestañas: teclado, rotación, fuente grande, panel y DETENER siempre visible.
- [ ] Modo Local sigue funcionando sin clave/red; skills no envían al pulsarlas.
- [ ] Petición Maná abre consulta correcta en YouTube; juegos abre Play; app ausente falla.
- [ ] Brave sin clave falla sin petición; resultados no ejecutan texto inyectado.
- [ ] Descargar audio propio por URL, cancelar, exportar; HTML/DRM/HLS rechazados.
- [ ] Probar cada API con cuenta de prueba/cuota limitada: confirmación cancelada
  no envía; petición exacta, historial/no notas, clave cifrada, errores sin secretos.
- [ ] Cancelar texto/Brave y desautorizar red: no reintento ni falsa respuesta.
- [ ] Endpoint compatible privado/redirect no recibe clave; errores no imprimen cuerpo.
- [ ] Accesibilidad inactiva no actúa; sesión expira; STOP siempre funciona.
- [ ] Cambiar app/campo/pantalla tras revisar rechaza acciones; password/login/pago/
  instalación/mensajería no disponibles; nunca actúa sin confirmar.
- [ ] YouTube: pulsar búsqueda, escribir Maná y confirmar IME si disponible.
- [ ] Revisar almacenamiento: no capturas/logs de pantalla ni claves en texto plano.

## Referencias oficiales consultadas

- OpenAI: https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create/
- Gemini: https://ai.google.dev/api/generate-content y https://aistudio.google.com/docs/api-key
- Anthropic: https://platform.claude.com/docs/en/api/messages/create
- Brave: https://api-dashboard.search.brave.com/documentation/services/web-search
- Android: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo.AccessibilityAction
- Servicio: https://developer.android.com/guide/topics/ui/accessibility/service

No se han realizado tests funcionales con credenciales de usuario ni pantalla real.