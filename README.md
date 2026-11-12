# Sakura Agent 🌸 — Android 0.2

Agente nativo Kotlin/Jetpack Compose con el diseño nocturno rosa/turquesa original.

## Funciones integradas en el código

- Personalidad en español, amabilidad y humor configurable.
- Memoria explícita de preferencias, proyectos y avances, cifrada con AES-GCM y Android Keystore. Revisar y borrar notas; no se guardan conversaciones automáticamente.
- Nivel de aprendizaje principiante, intermedio o avanzado; adapta explicaciones, no reentrena el modelo.
- IA local mediante llama.cpp/JNI: importar, cargar, cambiar, liberar y borrar GGUF compatibles. Sin respuestas de demostración.
- Lectura de código UTF-8 y ayuda con Python, Java, Kotlin, HTML y JavaScript mediante el modelo. No ejecuta ni compila código.
- GitHub: listar archivos, leer proyectos y proponer cambios en el chat. La edición de un archivo existente exige revisar ANTES/DESPUÉS y confirmar el commit.
- Android: abrir aplicaciones visibles con confirmación, sin controlar su interfaz.
- APK: análisis estático del archivo seleccionado, sin instalarlo: paquete, versión, SDK, permisos, hashes y bibliotecas. No es antivirus ni certificación de seguridad.
- Dictado revisable y respuestas habladas. Se prioriza voz local; una alternativa que necesite red exige autorización.
- Privacidad: red desautorizada al iniciar la sesión, token cifrado y chat solo en RAM; no hay proveedor de IA remoto.

## Estado de integración

El código de esta ampliación se encuentra en la rama `sakura-v0.2-integration`. El cambio al workflow de compilación sigue pendiente de autorización de GitHub para modificar `.github/workflows/build-apk.yml`; no se presenta una APK como generada.

## Requisitos

Android 13+ (API 33), sistema/CPU ARM64. Objetivo de prueba: 8 GB de RAM; no se garantiza rendimiento en todos los dispositivos. Inferencia en CPU, contexto de 2048 tokens, hasta 384 tokens de salida.

El motor está integrado, pero los pesos GGUF no están incluidos en el repositorio ni en la APK. Se importa un modelo instruct/chat compatible desde el selector de Android. Candidato de prueba: [Qwen2.5-1.5B-Instruct-GGUF oficial](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF), variante `qwen2.5-1.5b-instruct-q4_k_m.gguf`. Revisa su licencia y usa fuentes confiables.

## Compilar en tu computadora

Android Studio, Java 17, Gradle 8.9, Android SDK 35, Build Tools 35.0.0, NDK `27.2.12479018` y CMake `3.31.6`. No se incluye Gradle Wrapper. Desde la raíz:

```bash
bash scripts/fetch-llama.sh
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
```

Configura la ruta de tu SDK en Android Studio o `local.properties`. En Windows usa Git Bash para el script. `llama.version` fija el commit nativo; la descarga requiere Internet.

## Uso y permisos

Modelos → Importar GGUF → Cargar. Memoria → guardar notas. Privacidad → nivel, humor y autorización opcional de red. El micrófono pide permiso y solo rellena el borrador.

Para GitHub privado usa un fine-grained token limitado al repositorio: `Contents: read`; para commits necesita `Contents: write`. Guárdalo en el campo oculto de Privacidad, nunca en el chat. Se envía solo a `api.github.com` por HTTPS. Si hay conflicto o timeout, vuelve a leer y comprueba el historial antes de repetir. Borrar el token local no lo revoca en GitHub.

El interruptor de red no es un firewall para Android ni las apps abiertas. El cifrado no protege un teléfono comprometido ni garantiza borrado forense. Los archivos originales seleccionados no se modifican.

## Verificación

Las ocho pruebas JVM de reglas aprobaron. Todo el Kotlin pasó una compilación estática con API 35 y Compose 2.0.21; C++ pasó comprobación de sintaxis contra la revisión fijada de llama.cpp. Esto no sustituye el build Android completo, el enlazado ARM64 ni las pruebas en un teléfono.
