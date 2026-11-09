# Plan de integración de modelos locales para Sakura Agent

## Estado real

Este documento guarda el alcance y el plan de trabajo. La integración nativa y el modelo todavía NO están implementados en esta rama. Un intento anterior de subir código fue rechazado por falta de permiso de escritura del agente; crear la rama sí funcionó. No debe interpretarse este plan como una APK que ya contenga IA.

## Objetivo acordado

- Requerir Android 13 o posterior (API 33+) para esta primera versión con llama.cpp.
- Orientar la compilación inicial a teléfonos ARM64; 8 GB de RAM es una recomendación, no garantía de buen rendimiento en cualquier teléfono.
- Ejecutar GGUF localmente con llama.cpp y conservar el chat, dictado y lectura en voz alta existentes cuando se integren correctamente.
- Incluir Qwen2.5-1.5B-Instruct Q4_K_M como modelo inicial. El modelo ocupa aproximadamente 1 GB y su licencia indicada es Apache-2.0. El modelo es pequeño para estándares actuales: puede responder en español y ayudar con tareas generales sencillas, pero se equivocará y no será experto en todo.
- Evitar subir un modelo binario de ~1 GB al historial Git. El workflow de CI debe descargar una versión/revisión fijada del modelo y empaquetarla en el APK de prueba, de modo que la APK pueda usarlo sin descargar pesos en el primer inicio.
- Crear una sección Modelos que muestre el modelo incluido y alternativas compatibles, permita descargar, ver progreso, elegir, eliminar modelos opcionales y cargar uno a la vez. Aceptar solo GGUF compatibles con el backend y validar tamaño/descarga antes de cargar.
- Mantener acciones externas limitadas y sujetas a confirmación del usuario; no permitir ejecución arbitraria de órdenes.

## Alcance técnico por etapas

1. Fijar una revisión estable de llama.cpp y verificar su ejemplo Android, dependencias, licencia, CMake/NDK y API real antes de portar código.
2. Configurar Android Gradle Plugin, Kotlin, SDK 33+ y NDK/CMake compatibles; añadir `arm64-v8a` como ABI inicial. Actualizar CI para descargar llama.cpp en una ruta fija y compilar JNI.
3. Integrar wrapper JNI y gestionar con cuidado el ciclo de vida del modelo, contexto, concurrencia, cancelación, errores y memoria. No ejecutar inferencia en el hilo de UI.
4. Actualizar el chat para usar inferencia local, streaming si está soportado, contexto limitado y manejo de errores; preservar dictado y TextToSpeech sin afirmar que el dictado siempre es offline.
5. Añadir el panel Modelos con modelo empaquetado y catálogo de descargas HTTPS, porcentajes/reintentos, cancelación, selección y borrado. Guardar archivos en almacenamiento privado, no pedir permisos amplios de archivos.
6. Actualizar workflow para descargar el GGUF desde un origen/revisión verificados, validar su tamaño/checksum, incluirlo en los assets sin compresión y publicar el APK como artefacto. El tamaño final puede superar 1 GB; el primer build tardará más y los límites de artefactos deben revisarse.
7. Compilar y probar en Actions. Si compila, instalar la APK en un teléfono Android 13+ ARM64 con espacio libre suficiente. Medir carga, RAM, temperatura, velocidad y uso sin conexión. La compilación exitosa por sí sola no demuestra funcionamiento en todos los dispositivos.

## Seguridad y privacidad

- La inferencia del GGUF debe permanecer en el dispositivo; las descargas de modelos sí necesitan Internet.
- El proveedor de reconocimiento de voz de Android puede enviar audio a un servicio remoto; informarlo y dejar el dictado como acción iniciada por el usuario.
- Mostrar fuente, nombre, licencia, formato y tamaño de cada modelo; no descargar ni ejecutar archivos arbitrarios proporcionados por terceros sin validación.
- Abrir aplicaciones solo mediante intents permitidos y solicitud/confirmación humana.

## Fuentes de referencia consultadas

- Ejemplo Android de llama.cpp: https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android
- Guía Android de llama.cpp: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- Modelo Qwen GGUF: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF

## Próximo paso

Integrar primero la compilación nativa fijando los detalles que requiera la revisión escogida de llama.cpp. Después ejecutar CI y corregir los errores antes de añadir o declarar lista la descarga del modelo. No asumir que una integración nativa aproximada compila solo porque el ejemplo upstream compila en su propio proyecto.
