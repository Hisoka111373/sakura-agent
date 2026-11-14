# Sakura 0.5 — Jardín, mascota original y 93 skills

## Apariencia

Nueva portada **Jardín**: mascota anime original de pelo rosa/ojos turquesa,
cerezos y jardín nocturno, accesos a Chat/Skills y estado del motor. Avatar y
detalle floral en otras pantallas. Se conservan Kotlin/Compose y colores
nocturno/rosa/turquesa. No se usó una mascota de franquicia ni imagen de terceros.

Un único dibujo original generado con FLUX.2 Turbo, luego recortado para avatar
y flores. Tres recursos WebP en `res/drawable-nodpi`, **179576 bytes en total**
(aprox. 175 KiB). Están dentro de la APK: no necesitan red ni telemetría, no son
fondos descargados en runtime. No hay animaciones continuas; solo fade de entrada.
El arte es decoración, no reconocimiento de imágenes ni generación visual local.

## Catálogo

93 entradas: las 21 originales y 72 nuevas especialidades de texto. Categorías:

- Programación: Python, Kotlin/Android, Java, JS/TS, HTML/CSS, SQL, Git, REST,
  React, Bash/Linux, C/C++, C#/.NET, Rust, Go, regex, algoritmos y estructuras.
- Ingeniería: depuración, tests, refactor, arquitectura, revisión, seguridad
  defensiva, rendimiento, accesibilidad, documentación, CI/CD, DB, Docker,
  requisitos e interfaces.
- Aprendizaje: tutor, matemáticas, física, estadística, inglés, japonés,
  ejercicios, planes de estudio y analogías.
- Productividad: resumen, traducción, redacción, proyectos, agenda, reunión,
  CV, entrevistas, presupuesto educativo y checklist.
- Investigación: fuentes, comparación, investigación, afirmaciones, música/videos
  y juegos oficiales. Datos: análisis, hojas de cálculo, gráficos, limpieza y JSON/YAML.
- Creatividad: historias, ideas, guiones, prompts de imagen/video, naming y poemas.
- Vida diaria: recetas orientativas, viajes orientativos y organización personal.

Las skills son **instrucciones cortas y estructuradas para el modelo**, no motores
de ejecución ni instalaciones de paquetes. Dependen de calidad/compatibilidad de
tu GGUF o proveedor. No garantizan dominio perfecto de un lenguaje ni resultados
probados. No compilan código, ejecutan tests/shell, generan archivos/gráficos,
instalan apps, reservan viajes o crean alarmas desde el texto. Las herramientas
ya implementadas se abren por separado y siguen pidiendo botones/confirmaciones.

Biblioteca con buscador, filtros por categoría, tarjetas explicativas y favoritos
cifrados con Keystore. Elegir una skill de texto pasa a Chat **sin enviar nada ni
sobrescribir tu borrador**. Selección manual es de sesión; favoritos persisten.

## Selección automática, visible y local

Por defecto Chat clasifica tu petición con reglas deterministas de palabras/frases
sobre los primeros **8000 caracteres**. No llama a otra API ni carga un segundo
modelo. Verbos como traduce/resume/depura tienen prioridad; después se cuentan
coincidencias por especialidad. Es una heurística: puede equivocarse con peticiones
ambiguas, varios temas, otro idioma o texto citado. No es un router semántico universal.

El chat muestra **Skill prevista** mientras escribes y **Última skill** después.
Si no hay coincidencia, General (prompt habitual). Una sola skill breve entra
en el prompt de sistema; no se envía todo el catálogo ni aumenta el contexto
automáticamente. El contexto local permanece en 2048 tokens, salida 384: una
instrucción añadida deja menos espacio para historia/código. Acorta fragmentos
si el motor informa que no caben. No se prometen RAM/velocidad en 8 GB.

**Cambiar** abre catálogo y fija una skill manual. **Auto** restablece selección
automática. Solo prepara instrucciones de texto: encontrar la palabra GitHub,
YouTube, API, shell o descargar **no dispara acciones externas**.

En API de texto la instrucción de la skill elegida forma parte del **JSON completo
revisado** antes de confirmar. Endpoint, modelo, chat/borrador y elección de skill
se congelan; cambiarlos invalida esa confirmación. No se incluyen recuerdos
guardados ni pantalla. Modelo remoto no ejecuta tools de su respuesta. Una
respuesta puede tener coste aunque falle/cancele; revisar consumo antes de repetir.

## Uso

1. Abre Jardín → Explorar skills o Conversar.
2. Modelos: importa/carga GGUF para modo Local; alternativamente configura APIs y
   claves cifradas en Privacidad, como describe FEATURES-0.4.md.
3. Escribe «depura este código Kotlin» o «traduce este texto». Revisa Skill prevista.
4. Enviar usa la guía seleccionada. En nube además exige JSON/coste confirmado.
5. Usa favoritos/filtros para elegir manualmente cuando la heurística no acierte.

## Verificación y límites

- 64 pruebas JVM aprobadas: 51 anteriores y 13 de catálogo/routing/prompt.
- Producción Kotlin/Compose compilada estáticamente contra API35. Para ese chequeo
  se usó un **R temporal fuera del repo**; Gradle/CI genera el R real desde recursos.
- Tres WebP decodifican y se verificaron; scripts/whitespace revisados.
- 0.4 ya pasó Android/ARM64 CI run43553143121 y PR #5 está integrado.
- 0.5 requiere su propio CI Android; un build anterior no valida nuevos recursos/UI.
- No APK compilada en sandbox. No teléfono/emulador, inferencia ni API real probados.
- El único gasto de generación en desarrollo fue el dibujo decorativo solicitado,
  no una llamada con credenciales de Sakura ni un test de sus proveedores.

### Pruebas manuales pendientes

- [ ] Portada/avatar/flores sin Internet, rotación y tamaños de fuente/pantalla.
- [ ] 15 pestañas, filtros/buscador, favoritos persistentes, estado vacío.
- [ ] Python/Kotlin/SQL, traduce/depura/resume y fallback General muestran la skill.
- [ ] Elegir manual no borra borrador; Auto vuelve a detectar; no llama herramientas.
- [ ] Prompt real usa una sola skill en GGUF y en JSON revisado por cada proveedor.
- [ ] Cambio de skill tras preparar API rechaza confirmación antigua.
- [ ] Rendimiento/temperatura/contexto en el dispositivo del usuario; imágenes no
  se recargan por red ni añaden analítica.

Arte: prompt y procedencia en [ARTWORK.md](ARTWORK.md). No se incorporaron las
fotos/mascotas halladas en la búsqueda de referencias.