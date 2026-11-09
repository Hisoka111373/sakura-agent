package com.sakura.agent

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Night = Color(0xFF100D1C)
private val Panel = Color(0xFF1B172B)
private val Pink = Color(0xFFFF79B9)
private val Aqua = Color(0xFF73E6E0)
private val Ink = Color(0xFFF7EFF8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SakuraTheme { SakuraScreen() } }
    }
}

@Composable
private fun SakuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Pink,
            secondary = Aqua,
            background = Night,
            surface = Panel,
            onPrimary = Night,
            onBackground = Ink,
            onSurface = Ink
        ),
        content = content
    )
}

private data class Message(val text: String, val fromSakura: Boolean)

@Composable
private fun SakuraScreen() {
    val context = LocalContext.current
    val messages = remember {
        mutableStateListOf(
            Message("¡Hola! Soy Sakura 🌸 Puedo conversar con respuestas básicas y leerlas en voz alta. ¿Qué hacemos hoy?", true)
        )
    }
    var draft by remember { mutableStateOf("") }
    var voiceStatus by remember { mutableStateOf("") }
    var speechEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speechEngine?.language = Locale("es", "ES")
            }
        }
        speechEngine = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            speechEngine = null
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val recognized = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        if (!recognized.isNullOrBlank()) {
            draft = recognized
            voiceStatus = "Voz reconocida; revisa el texto y pulsa enviar."
        } else {
            voiceStatus = "No se reconoció voz. Puedes intentarlo otra vez o escribir."
        }
    }

    fun sendMessage() {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            messages.add(Message(text, false))
            messages.add(Message(reply(text), true))
            draft = ""
            voiceStatus = ""
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Pink.copy(alpha = 0.18f)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Pink, modifier = Modifier.padding(13.dp))
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("SAKURA AGENT", color = Pink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Compañera digital · prototipo", color = Aqua, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Text(
                    "Modo actual: respuestas de demostración, sin modelo de IA local. El micrófono usa el reconocedor de voz disponible en Android y podría requerir Internet. No ejecuto órdenes ni controlo otras apps. Tú revisas y envías cada mensaje.",
                    modifier = Modifier.padding(14.dp),
                    color = Ink.copy(alpha = 0.86f),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = true
            ) {
                items(messages.asReversed()) { message ->
                    MessageBubble(message = message, onSpeak = {
                        if (message.fromSakura) speechEngine?.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, "sakura_reply")
                    })
                }
            }

            if (voiceStatus.isNotBlank()) {
                Text(voiceStatus, modifier = Modifier.padding(vertical = 6.dp), color = Aqua, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con Sakura")
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        voiceStatus = "No hay un servicio de reconocimiento de voz instalado."
                    }
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "Dictar mensaje", tint = Aqua)
                }

                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("Escribe o dicta a Sakura…", color = Ink.copy(alpha = 0.45f))
                        inner()
                    }
                )

                Button(onClick = { sendMessage() }, colors = ButtonDefaults.buttonColors(containerColor = Pink)) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar mensaje", tint = Night)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Pulsa el micrófono para dictar; revisa antes de enviar.", modifier = Modifier.align(Alignment.CenterHorizontally), color = Ink.copy(alpha = 0.58f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun MessageBubble(message: Message, onSpeak: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromSakura) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            colors = CardDefaults.cardColors(containerColor = if (message.fromSakura) Panel else Pink.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 13.dp, end = 5.dp, top = 7.dp, bottom = 7.dp)) {
                Text(message.text, modifier = Modifier.weight(1f).padding(vertical = 6.dp), color = if (message.fromSakura) Ink else Pink, fontSize = 15.sp)
                if (message.fromSakura) {
                    IconButton(onClick = onSpeak) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Leer respuesta en voz alta", tint = Aqua)
                    }
                }
            }
        }
    }
}

private fun reply(text: String): String = when {
    text.contains("hola", true) || text.contains("buenas", true) ->
        "¡Hola! 🌸 Cuéntame qué tienes en mente y lo pensamos paso a paso."
    text.contains("ayuda", true) ->
        "Puedo conversar con respuestas básicas y leer mis mensajes en voz alta. Todavía no tengo un modelo de IA conectado."
    text.contains("qué puedes hacer", true) || text.contains("que puedes hacer", true) ->
        "Puedes escribirme o dictarme un mensaje y leer mis respuestas con el botón de voz. Esta versión aún no usa un modelo inteligente local."
    text.contains("gracias", true) ->
        "¡De nada! Me alegra acompañarte 💖"
    else ->
        "Te leo 🌸 Esta respuesta es una demostración: Sakura todavía no tiene un modelo de IA conectado."
}
