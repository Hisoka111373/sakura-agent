# Sakura Agent 🌸

Prototipo Android de chat con estética cyberpunk suave.

## Funciones actuales

- Chat de demostración con respuestas predefinidas; **no incluye un modelo de IA**.
- Dictado opcional mediante el servicio de reconocimiento de voz disponible en Android. Ese servicio puede procesar audio fuera del dispositivo y podría requerir Internet.
- Lectura en voz alta mediante el motor de texto a voz configurado en Android.
- El dictado solo rellena el cuadro de texto: la persona revisa y pulsa Enviar. La app no ejecuta órdenes ni controla otras aplicaciones.

## APK de prueba

El workflow **Build Sakura Agent APK** compila una APK de depuración al subir cambios a `main`. En GitHub Actions, descarga el artefacto `sakura-agent-debug-apk`, descomprímelo y busca `app-debug.apk`. Es una compilación de prueba, no una versión de tienda.

## Modelo local

Esta versión no descarga ni ejecuta modelos locales todavía. Un modelo requiere integrar un motor de inferencia compatible y elegir un modelo cuyo tamaño y requisitos sean adecuados para el teléfono. No se debe presentar el chat de demostración como IA real.
