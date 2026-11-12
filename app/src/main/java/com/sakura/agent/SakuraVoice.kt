package com.sakura.agent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Created and called on the main thread; no background microphone or auto-send. */
class SakuraVoice(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var ready = false
    private var listening = false
    private var disposed = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { code ->
            if (!disposed) {
                ready = code == TextToSpeech.SUCCESS
                tts?.setSpeechRate(1.02f)
                tts?.setPitch(1.08f)
            }
        }
    }

    fun dictate(internetAllowed: Boolean) {
        if (listening) { recognizer?.stopListening(); return }
        val local = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if (!local && !internetAllowed) {
            onStatus("Este dispositivo no ofrece dictado local. Instala un servicio compatible o autoriza red en Privacidad.")
            return
        }
        if (!local && !SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("No hay un servicio de dictado disponible."); return
        }
        recognizer?.destroy()
        recognizer = if (local) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onStatus(if (local) "Escuchando con el servicio local de Android…" else "Escuchando: el servicio de Android puede enviar audio por Internet…") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { onStatus("Procesando dictado; aún no se ha enviado ningún mensaje…") }
            override fun onError(error: Int) {
                listening = false
                onStatus("Dictado no disponible o interrumpido (Android $error). No se cambia a un servicio en red sin autorización.")
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    onText(text); onStatus("Dictado en borrador. Revisa el texto y pulsa Enviar.")
                } else onStatus("No se reconoció texto.")
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, local)
        }
        try { listening = true; recognizer?.startListening(intent) }
        catch (_: Exception) { listening = false; onStatus("No se pudo iniciar el dictado. Revisa el permiso de micrófono.") }
    }

    fun speak(text: String, internetAllowed: Boolean) {
        val engine = tts
        if (!ready || engine == null) { onStatus("El motor de voz todavía no está listo."); return }
        val voices = engine.voices.orEmpty().filter { voice ->
            voice.locale.language == "es" &&
                (internetAllowed || !voice.isNetworkConnectionRequired) &&
                !voice.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }.sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
        val voice = voices.firstOrNull()
        if (voice == null) { onStatus("No hay una voz española instalada compatible con tu ajuste de privacidad."); return }
        if (engine.setVoice(voice) == TextToSpeech.ERROR) { onStatus("No se pudo activar la voz española."); return }
        val result = engine.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "sakura_reply")
        onStatus(if (result == TextToSpeech.ERROR) "Falló la lectura en voz alta." else if (voice.isNetworkConnectionRequired)
            "Lectura con voz de Android que requiere Internet (hasta 3500 caracteres)." else "Lectura con voz local de Android (hasta 3500 caracteres).")
    }

    fun stop() { recognizer?.cancel(); listening = false; tts?.stop() }
    fun close() { disposed = true; stop(); recognizer?.destroy(); recognizer = null; tts?.shutdown(); tts = null }
}
