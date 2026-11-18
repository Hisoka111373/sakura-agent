# Sakura 0.9.0

## Arreglo del modo local

- El prompt local tiene ahora un presupuesto determinista: 3400 caracteres en Bajo y
  3000 en Alto, repartidos entre sistema, notas e historial. El mensaje nuevo (por
  ejemplo «hola») se conserva siempre íntegro y el historial antiguo se descarta
  o recorta primero. Antes, un «hola» podía arrastrar hasta 6 mensajes de 4096
  caracteres, 12 notas y guías largas: el decode del contexto agotaba el deadline
  y la interfaz se quedaba en «Procesando contexto local…».
- El estado de generación (`busy`, `generating`, respuesta parcial y evento de la
  mascota) se limpia en todos los caminos: éxito, excepción Java, excepción JNI,
  timeout y cancelación. Cancelar ya no muestra un falso error.
- El deadline nativo se arma antes de formatear y tokenizar, y se comprueba también
  después. `nativeGenerate` resetea cancelación y deadline al entrar; se reservan
  64 tokens para plantilla/salida.
- `Continuar` solo se ofrece con borrador vacío y tras una respuesta.
- La UI de chat mantiene el selector **Local / En línea** y el botón **Continuar**.

## Modelos

- GGUF pequeños verificados con la API de Hugging Face (tamaño y SHA-256):
  Qwen2.5 0.5B Q4_K_M (~0.49 GB) y Qwen2.5 1.5B Q3_K_M (~0.92 GB). No probados
  en dispositivo; la calidad y la RAM real pueden diferir.
- Los checkpoints de imagen y vídeo aparecen como **descarga externa**: Sakura no
  incluye motor local de difusión y no los carga con llama.cpp.

## Funciones del teléfono (siempre con revisión previa)

- `google_search`: abre `google.com/search?q=` en el navegador externo. No extrae
  ni envía resultados al modelo.
- `screen_read`: lee texto visible de la app autorizada durante una sesión de
  Accesibilidad; excluye contraseñas, campos editables y etiquetas sensibles. El
  texto se muestra localmente y no entra en el contexto del modelo.
- `screen_type`: prepara un texto corto y exige elegir un campo editable seguro y
  pulsar un segundo botón de confirmación en el panel. Sin Enter automático nuevo.
- `file_list` / `file_read`: carpeta elegida con el selector de documentos (SAF),
  hasta 100 entradas y lectura de texto UTF-8 de hasta 64 KiB. Sin permiso amplio
  de almacenamiento.
- `apk_open_selected`: abre la APK analizada previamente con `ACTION_VIEW`. Hay
  botones separados y confirmados para abrir e instalar; Android muestra su propia
  confirmación y Sakura no verifica firma ni seguridad.
- Nueva pestaña **Archivos** y permiso `REQUEST_INSTALL_PACKAGES`. Ninguna acción
  se ejecuta sin el diálogo de vista previa; no hay bucles autónomos ni envío de
  pantalla a la nube.

## Verificación

- 132 tests JVM de reglas, catálogo, prompt local y nuevas herramientas: OK con
  Kotlin 2.0.21 y JUnit 4.13.2.
- Build Android ARM64 y pruebas del módulo: GitHub Actions.
- Pendiente en dispositivo: rendimiento real del GGUF, JNI, voz, Accesibilidad, SAF
  con proveedores de terceros y pantalla de instalación de Android.
