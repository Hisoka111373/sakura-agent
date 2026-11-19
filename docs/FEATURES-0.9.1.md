# Sakura 0.9.1 — reparación local y chat limpio

## Error reportado
«Tiempo de contexto agotado» al escribir hola: timeout durante prefill, no prueba de que el modelo esté razonando mucho. Cambios: -O3 en todos los targets nativos (incluidos llama/ggml) con verificación de flags efectivos en CI; prompt local compacto (saludo <240 caracteres, sin historial ni notas para saludos exactos), instrucciones de skill acotadas y contexto local pequeño. Bajo: 96 tokens de salida, saludo32; Alto256. Sin respuestas simuladas ni fallback nube.

Cada bloque de prefill reporta porcentaje; generación recibe su propio deadline después de prefill. Ambos deadlines cooperativos siguen activos: hasta45s por fase Bajo/90s Alto, más overhead nativo; no promesa de latencia ni interrupción instantánea. No se resuelve simplemente quitando el timeout. Si persiste prueba Qwen0.5B y registra modelo/RAM/tiempo.

Dos GGUF añadidos, hashes/revisiones de metadata Hugging Face verificados: Qwen2.5 0.5B Instruct oficial y Coder0.5B QuantFactory, ~491MB. Descarga manual confirmada con Internet autorizado. No se borran/cambian modelos anteriores. Imagen/video permanecen en catálogo EXTERNO: no hay runtime diffusion/video local en Sakura y no se pueden cargar como GGUF de chat.

Chat: Local/En línea, mascota pequeña, conversación, dictado/escribir/enviar, Detener y Continuar. Opciones abre esfuerzo/voz/skills, nuevo chat, aviso completo y demás pantallas. Sin tarjetas grandes de descarga ni banners. Mantiene biblioteca de93skills; skills de texto se añaden al prompt compacto, herramientas requieren revisión y confirmación.

Mascota: salto con squash/stretch y pequeño movimiento lateral; arte intacto, no Live2D/lip-sync. Solo avatar activo animado en chat; miniaturas históricas estáticas. Pausa fuera de primer plano y al desactivar movimiento.

Prueba física pendiente: no afirmar que está solucionado en el móvil hasta repetir hola, explica variables y una skill de traducción con un GGUF cargado, registrar tiempo/porcentaje, cancelar y volver a enviar. Verificar conversación visible con teclado y mensaje de error accesible desde Opciones.

Integrada sobre 0.9.0: conserva controles supervisados de teléfono, carpeta autorizada, catálogo Q3 y sus pruebas. No restaura la APK 8.0 retirada.
