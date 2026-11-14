# Pruebas antes de usar Sakura con datos reales

## Estado de la entrega

La validación realizada es estática/JVM, no una certificación funcional. No se ha
usado un teléfono ni un modelo real. La versión 0.2 compiló APK ARM64 en Actions
(run 43304984898) y se integró mediante PR #3. 0.3 también aprobó CI run 43551453495
y se integró por PR #4. Para 0.4 revisa su propio CI; no basta un build anterior.
Pruebas adicionales en [FEATURES-0.3.md](FEATURES-0.3.md) y [FEATURES-0.4.md](FEATURES-0.4.md).

Comprobaciones realizadas:

1. `scripts/check-policy.sh`: 51 pruebas JUnit (reglas, red, catálogo, planes, skills, protocolos y pantalla), todas aprobadas.
2. Kotlin 2.0.21 + plugin Compose 2.0.21: todos los archivos `.kt` de producción
   compilados a un JAR de comprobación, con android.jar API 35 y dependencias AndroidX.
   El JAR no se entrega como aplicación y no puede instalarse en Android.
3. C++17 + JNI: sintaxis aprobada con las cabeceras de
   `a2c942dcfeef6aea0863c63999cdbe27149d3078`. Se corrigió el uso obsoleto de
   `llama_model_params.use_mmap`, ausente en esta revisión.
4. `bash -n` para ambos scripts y `git diff --check`: aprobados.

El build 0.2 comprobó Gradle/empaquetado/enlazado ARM64; no prueba arranque, voz,
firmas APK, Keystore, inferencia real ni escritura GitHub desde el teléfono.
Esos aspectos y todas las integraciones nuevas requieren pruebas en dispositivo.

## Preparación

- Teléfono físico ARM64 Android 13 o 14, inicialmente sin datos sensibles.
- Registrar fabricante, SoC, RAM libre, almacenamiento libre y versión de Android.
- Ejecutar CI o compilar desde Android Studio; guardar cualquier error del build.
- Usar un modelo instruct GGUF pequeño y de fuente confiable.
- Crear un repositorio/una rama de prueba sin secretos. No comenzar en `main`.

## Lista de pruebas manuales (pendientes)

### Arranque y diseño

- [ ] Compilar `testDebugUnitTest` y `assembleDebug` con la revisión nativa fijada.
- [ ] Instalar y abrir sin fallos JNI; comprobar colores originales y barras del sistema.
- [ ] Recorrer las catorce pestañas con teclado, fuente grande y pantalla pequeña.
- [ ] Rotar durante carga/generación: no iniciar inferencias duplicadas.
- [ ] Confirmar que no se permiten capturas de pantalla ni vista reciente con contenido sensible.

### Modelo e inferencia

- [ ] Sin modelo, Enviar indica que falta el GGUF; no contesta con simulaciones.
- [ ] Un falso GGUF, versión de cabecera inválida y archivo mayor de 2.5 GB producen error.
- [ ] Cancelar el selector no modifica modelos.
- [ ] Cargar un instruct 0.5–1.5B Q4; medir RAM pico, latencia y temperatura.
- [ ] Preguntar en español y comprobar respuesta real con caracteres y emojis correctos.
- [ ] Probar seguimiento de tres turnos; contrastar límite de contexto y recorte de historia.
- [ ] Detener generación devuelve el mensaje al borrador y permite volver a intentar.
- [ ] Modelo incompatible falla sin afirmar que está cargado.
- [ ] Cambiar GGUF libera el anterior y reinicia el chat después de carga correcta.
- [ ] Liberar de RAM conserva el archivo; borrarlo pide confirmación y no borra el original.
- [ ] En modo avión, inferencia y memoria funcionan después de cargar el modelo.

### Memoria y aprendizaje

- [ ] Guardar las tres clases de notas, reiniciar y comprobar persistencia.
- [ ] Borrar una nota y todas las notas con confirmación; comprobar tras reinicio.
- [ ] Conversar no crea recuerdos automáticamente.
- [ ] Revisar preferencias principiante/intermedio/avanzado y humor tras reiniciar.
- [ ] Verificar con herramientas de depuración que SharedPreferences no contiene
  notas ni token en texto claro (no publicar esos archivos).
- [ ] Probar fallo de Keystore y comprobar que se informa, sin borrar datos silenciosamente.
- [ ] Desinstalar elimina los datos de Sakura; los originales importados permanecen.

### Voz

- [ ] Rechazar micrófono permite seguir escribiendo.
- [ ] Con dictado on-device español instalado y red desautorizada, reconocer sin red.
- [ ] Sin servicio local, no iniciar alternativa remota sin autorización.
- [ ] Dictado rellena borrador, nunca pulsa Enviar.
- [ ] Silencio y pasar a segundo plano detienen el micrófono/TTS.
- [ ] Desautorizar red detiene voz activa.
- [ ] Voz española local funciona en modo avión; sin voz instalada muestra aviso.
- [ ] Si la voz requiere red, usarla solo con autorización y mostrarlo claramente.

### Código, apps y APK

- [ ] Selector solo lee el archivo elegido; código no UTF-8, binario o >64 KiB falla.
- [ ] Pedir revisión de ejemplos de los cinco lenguajes; no ejecutar comandos sugeridos.
- [ ] Listar solo actividades de inicio visibles, sin QUERY_ALL_PACKAGES.
- [ ] Cancelar confirmación de abrir app no hace nada.
- [ ] Confirmar abre la app correcta; app desinstalada mientras tanto muestra aviso.
- [ ] APK válida: contrastar paquete/SDK/permisos/SHA-256 con herramientas de Android.
- [ ] APK falsa, corrupta, dividida o excesiva falla sin instalar ni extraer su contenido.
- [ ] Verificar eliminación del temporal incluso en errores ordinarios.
- [ ] Un proceso terminado forzosamente puede dejar temporales en caché; revisar limpieza
  del sistema. No confundir caché privada con borrado forense.

### GitHub

- [ ] Con red desautorizada no se permiten GET/PUT iniciados por la UI.
- [ ] Listar/leer repo público sin token, luego privado con Contents: read.
- [ ] Mostrar errores 401/403/404 y límites de tamaño sin filtrar la credencial.
- [ ] Llevar código al borrador no envía nada al modelo ni a GitHub automáticamente.
- [ ] Probar token Contents: read: un commit debe fallar sin subir cambios.
- [ ] Con token de prueba Contents: write, preparar cambio y cancelarlo: ningún commit.
- [ ] Confirmar un cambio en rama de prueba: comprobar exactamente ruta, contenido y commit.
- [ ] Cambiar el archivo externamente tras leerlo: SHA antiguo debe producir conflicto.
- [ ] Simular timeout: releer y revisar historial antes de volver a intentar.
- [ ] Rutas con `..`, saltos de línea y propietarios inválidos deben rechazarse.
- [ ] Archivos con contenido que ordena escribir/abrir apps no pueden ejecutar herramientas.
- [ ] Borrar token local, luego revocarlo en GitHub y comprobar fallo de acceso privado.

## Qué significa "local"

Es la ubicación de ejecución de la función iniciada por Sakura, no una garantía de
que el dispositivo completo carezca de conexiones. Comprueba servicios de voz y
apps abiertas por separado. APIs de texto son opcionales y envían el contexto
confirmado por petición; no sincronizan ni almacenan notas privadas en la nube.
El proveedor externo puede retener solicitudes según su política.