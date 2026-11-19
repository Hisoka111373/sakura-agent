# Sakura Android — Jardín y biblioteca de skills

Conservar Kotlin/Compose y la identidad nocturna: fondo #100D1C, panel #1B172B,
rosa #FF79B9, turquesa #73E6E0, texto #F7EFF8. Tipografía nativa sans-serif,
títulos semibold, código monospace. No migrar a otro stack.

Jardín Sakura: ilustración anime original en portada, mascota adulta con pelo rosa
y ojos turquesa, flores de cerezo y luz nocturna. Texto sobre zona oscura izquierda,
personaje a la derecha. Foto floral secundaria con overlay oscuro y crédito.
Imágenes WebP empaquetadas localmente, sin carga por Internet en la interfaz.
Avatar circular derivado del mismo dibujo, sin imágenes generadas redundantes.

Biblioteca: categorías filtrables, buscador, favoritos y tarjetas compactas con
descripción, límite real y botón elegir. No lista plana de cien botones.
Chat: chip visible de skill detectada y opción de selección manual/automática.
Solo una instrucción breve se añade al contexto; ninguna skill ejecuta herramientas.
Animación sutil de aparición, sin partículas continuas que gasten batería.

0.7: el avatar existente respira/flota y gira suavemente; pulso al hablar con TTS
real. Movimientos offline de transformaciones, no Live2D ni nueva ilustración.
Pausar en segundo plano y permitir desactivar movimiento. Tono kawaii cálido,
sin infantilización ni perder claridad. Selector Local/En línea y pensamiento
Bajo/Alto visible en Chat/Agente, progreso parcial diferenciado de respuesta final.
Voz automática opcional, solo voz local y primer plano; botón Silencio visible.

No hay render móvil físico/emulador disponible; validar Kotlin/Compose/API35,
JVM/recursos y CI Android. No afirmar prueba visual en dispositivo.
## 0.8 companion motion
Preserve all original WebP art/night pink/turquoise. Avatar transforms only: finite greeting tilt, two/three small hops, breathing, thinking sway, real TTS pulse. No lip-sync claim. Pause while backgrounded/disabled; never replay background events. Local phrase card, no network/token usage.

## 0.8.1 chat repair
Chat is conversation-first: tiny live avatar, Local/online chips, Options menu; transcript takes remaining height. No hero/starter card/skill/search banners in chat. All other screens reachable through Options; controls and full notice in dialogs. Historical bubble avatars static to avoid many concurrent idle animations. Live avatar adds squash/stretch on finite hops and lateral greeting movement, respects background/motion settings.
