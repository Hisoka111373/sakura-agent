# Sakura 0.8.0

- Saludar: inclinación juguetona y frase local con voz bajo demanda. Saltito: pequeños saltos y frase local; voz opcional si Voz auto está activada.
- Frases de pensando/lista/pausa/error no son respuestas del modelo ni resultados verificados. No usan tokens ni red. Auto voz de frases limitada a una cada 8 segundos; solo primer plano y voz española local. La respuesta del modelo sigue leyendo con Voz auto.
- Animaciones pausadas fuera de primer plano o si desactivas Movimiento. No Live2D, articulación ni sincronización de labios.
- Bajo: salida local máxima 160 tokens, hasta 2 mensajes anteriores recortados a 1800 caracteres cada uno. Alto: 384 tokens, hasta 6 mensajes/4096 caracteres. El borrador nuevo no se recorta silenciosamente. El contexto nativo continúa siendo 2048 tokens; puede requerir acortar mensajes/notas.
- JNI actualiza progreso cada 8 tokens en vez de 4 (también el primer token), reduciendo llamadas y recomposiciones. No cambia la velocidad matemática del modelo ni garantiza tiempo.
- Continuar: disponible tras una respuesta y con borrador vacío. Pulsa para pedir otra parte; no repite automáticamente ni elimina límites. Cada petición nube requiere revisión del payload y puede costar dinero. Contexto anterior acotado: puede perder detalles.
- Sin modelo descargado automáticamente, herramientas autónomas ni cambios al Jardín/93 skills/arte.

## Prueba en teléfono pendiente
Android 13+ ARM64: saluda, pulsa Saltito, cambia de página, desactiva movimiento y envía Sakura al fondo durante un gesto. Verifica pausa/sin repetición. Activa voz local con confirmación y verifica que no habla en segundo plano. Prueba Bajo/Alto con el mismo mensaje y anota tiempo; no hay benchmark físico todavía. Genera una respuesta, pulsa Continuar, prueba con borrador escrito (no debe ofrecerse) y revisa confirmación nube. No desinstales para cambiar firma sin preservar datos.
