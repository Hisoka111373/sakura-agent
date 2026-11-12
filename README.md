# Sakura Agent 🌸

Agente Android nativo en Kotlin/Jetpack Compose. Conserva el diseño nocturno rosa/turquesa.

**Esta entrega es código fuente, no una APK probada.** La compilación Android completa, el
enlazado JNI y las pruebas en dispositivo siguen pendientes.
El chat ya no utiliza respuestas predefinidas: necesita importar y cargar un modelo real.

## Integración en GitHub

Los cambios se suben a `sakura-v0.2-integration` para integrarlos en `main` mediante
pull request, conservando el proyecto Android original. La base es
`5fa5c15b772f6fa1890371e27b5803fb07cfeba7`. Consulta el pull request y Actions
para conocer el estado actual de integración y compilación; las verificaciones
estáticas indicadas al final no sustituyen un build Android ni pruebas físicas.

## Funciones implementadas en el código

| Área | Alcance real |
|---|---|
| Personalidad | Español, amabilidad, humor opcional y tono de compañera definidos en el prompt. El cumplimiento depende del modelo. |
| Memoria | Notas explícitas de preferencia/proyecto/avance, cifradas con AES-GCM + Android Keystore; revisión y borrado individual o total. Hasta 100 notas; últimas 12 en contexto. |
| Aprendizaje | Nivel principiante/intermedio/avanzado persistente. Adapta instrucciones al modelo; no reentrena pesos. |
| IA local | Puente JNI a llama.cpp fijado a un commit; importar, cargar, cambiar, liberar y borrar GGUF. CPU, 2048 tokens de contexto, hasta 384 de salida. |
| Programación | Importar texto UTF-8 de hasta 64 KiB y pedir explicaciones/sugerencias para Python, Java, Kotlin, HTML y JavaScript. No ejecuta ni compila código. |
| GitHub | Listar archivos y leer texto. Editar un archivo existente con SHA de lectura, vista previa completa y confirmación antes de un commit real en la rama elegida. |
| Android | Listar actividades de inicio visibles y abrir una app elegida con confirmación. No controla su interfaz. |
| APK | Revisar la APK seleccionada: paquete, versión, SDK, permisos, hashes de certificado y archivo, bibliotecas. Sin instalar, ejecutar ni extraer ZIP. No es antivirus ni verificación completa de firma. |
| Voz | Dictado on-device si Android lo ofrece; de lo contrario, requiere autorización de red. Rellena el borrador, nunca envía automáticamente. TTS con voz española instalada, priorizando local. |
| Privacidad | Red desautorizada al iniciar cada sesión. Chat solo en RAM; memoria y token cifrados. Sin proveedor de IA remoto ni analítica añadida. Capturas de pantalla bloqueadas mediante FLAG_SECURE. |

## Requisitos y compatibilidad

- Android **13 o posterior** (API 33+), sistema y CPU **ARM64**. No se incluye ABI de 32 bits.
- Objetivo de prueba: teléfono con 8 GB de RAM. La RAM total no garantiza RAM disponible,
  velocidad ni compatibilidad. No se puede prometer funcionamiento en cualquier celular.
- Modelo instruct/chat GGUF v2/v3 de arquitectura y plantilla admitidas por la revisión
  fijada de llama.cpp. La cabecera GGUF solo valida el formato; la carga verifica lo demás.
- Importación limitada a 2.5 GB por archivo y al espacio libre disponible. Se copia al
  almacenamiento privado; la descarga original ocupa espacio adicional.
- Contexto pequeño: solo las últimas tres parejas del chat y doce notas se incluyen.
  Si el prompt supera el límite de tokens, se muestra error; acorta el fragmento o abre nuevo chat.
- Usa modelos de fuentes confiables. Archivos GGUF/APK no confiables pueden tener riesgos
  de parser; esta app no ofrece aislamiento de seguridad contra vulnerabilidades nativas.

### Modelo inicial sugerido (no incluido)

Como primer candidato de prueba, descarga manualmente
[Qwen2.5-1.5B-Instruct-GGUF oficial](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF)
y elige `qwen2.5-1.5b-instruct-q4_k_m.gguf`. Para enfocarte en código puedes evaluar
[Qwen2.5-Coder-1.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF).
Revisa su licencia y tarjeta antes de descargar. No se redistribuyen pesos, no se añade
descarga automática y ninguno de estos candidatos se ha ejecutado en esta entrega.

## Compilar desde tu computadora

1. Abre el proyecto en Android Studio con soporte para AGP 8.7.3.
2. Instala Java 17, Gradle 8.9, plataforma Android 35, Build Tools 35.0.0,
   NDK `27.2.12479018` y CMake `3.31.6`.
3. Desde la raíz, descarga la dependencia nativa fijada:

   ```bash
   bash scripts/fetch-llama.sh
   ```

   En Windows usa Git Bash para ese script. `llama.version` es la única fuente del commit.
   Se necesita Internet en esta etapa. El script no sobrescribe un checkout distinto ni
   cambios locales. `third_party/` está ignorado y no se incluye en el ZIP.

4. Configura la ruta del SDK en Android Studio o `local.properties` y ejecuta:

   ```bash
   gradle --no-daemon testDebugUnitTest
   gradle --no-daemon assembleDebug
   ```

   No hay Gradle Wrapper en el repositorio original ni en esta entrega; necesitas Gradle
   8.9 instalado o la configuración local equivalente de Android Studio.

5. Instala `app/build/outputs/apk/debug/app-debug.apk` en un teléfono ARM64 de prueba.
   Es una APK de depuración, no una versión firmada para tienda.

## GitHub Actions (alternativa)

El workflow `.github/workflows/build-apk.yml`
descarga llama.cpp al commit fijado, prepara SDK/NDK/CMake, ejecuta pruebas y solicita
compilar la APK ARM64. También puede iniciarse desde Actions → **Build Sakura Agent APK**
→ **Run workflow**. Descarga el artefacto `sakura-agent-debug-apk` solo si termina en verde.
Consulta Actions para comprobar su resultado; puede requerir ajustes de CI.
Se eliminó la copia redundante en `github/workflows/`, que GitHub no ejecuta.

## Primer uso

1. Modelos → Importar GGUF → Cargar y confirmar. Espera la carga antes de chatear.
2. Privacidad → selecciona nivel y humor. Mantén la red desactivada para empezar.
3. Memoria → guarda notas concretas. No se extraen recuerdos del chat automáticamente.
4. Chat → escribe un mensaje corto. Micrófono pide permiso; revisa el dictado antes de enviar.
5. Herramientas → selecciona archivos o apps. No se pide acceso general al almacenamiento,
   accesibilidad, instalación de paquetes, root ni QUERY_ALL_PACKAGES.

### GitHub y autorización

- Activa red en Privacidad únicamente si quieres usar funciones con Internet.
- La lectura pública puede funcionar sin token; GitHub aplica límites de consultas.
- Para un repositorio privado crea un **fine-grained personal access token** limitado a
  ese repositorio con `Contents: read`. Para commits necesita `Contents: write`.
- Introduce el token en el campo oculto de Privacidad, **nunca en el chat**. Se cifra con
  Keystore y se envía solo a `api.github.com` por HTTPS; no se siguen redirecciones.
- Leer archivo → llevar al borrador → pedir sugerencias locales → editar el contenido en
  GitHub → Revisar antes de escribir → inspeccionar ANTES/DESPUÉS → Confirmar commit.
- No se crean ramas ni pull requests automáticamente. Escribe en una rama existente de
  trabajo. La confirmación indica repositorio, rama, ruta, SHA, contenido y mensaje.
- Un error o timeout de escritura obliga a releer. Comprueba GitHub antes de repetir:
  el servidor podría haber aplicado el cambio aunque no llegara la respuesta.
- Borrar el token local no lo revoca en GitHub; revócalo también en tu cuenta.

## Límites de privacidad

El interruptor de red controla acciones iniciadas por Sakura; **no es un firewall** para
servicios de Android ni apps abiertas. Un servicio local de voz es el que Android declara
on-device; si no puede reconocer español, falla sin activar una alternativa en red a
escondidas. Desautorizar red detiene la voz activa. La voz también se detiene al pasar
la app a segundo plano. La inferencia no recibe herramientas ni credenciales.

`allowBackup=false` desactiva el backup estándar. El cifrado no protege contra un teléfono
rooteado o comprometido ni garantiza borrado forense de memoria flash. Los archivos GGUF
importados están privados pero no cifrados; los originales del selector no se modifican.

## Verificación de esta entrega

- 8 pruebas JVM de reglas: aprobadas con Kotlin 2.0.21 y JUnit 4.13.2.
- Todos los archivos Kotlin: compilación estática con API 35, dependencias AndroidX y
  plugin Compose 2.0.21 aprobada. No es un build Android de Gradle.
- C++: `g++ -fsyntax-only -Wall -Wextra -Werror` contra cabeceras del commit fijado y JNI:
  aprobado. No enlaza la biblioteca ni prueba instrucciones ARM64.
- Scripts: `bash -n` y comprobación de parche: aprobados.
- **Pendiente:** build APK completo, CI, inferencia real, rendimiento, Keystore, voz,
  APK de prueba, aplicaciones y operaciones reales de GitHub en un Android físico.

Consulta [docs/TESTING.md](docs/TESTING.md) antes de usarla con datos o repositorios importantes.
