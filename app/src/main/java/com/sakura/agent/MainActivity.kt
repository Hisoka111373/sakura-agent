package com.sakura.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF100D1C)
private val Panel = Color(0xFF1B172B)
private val Pink = Color(0xFFFF79B9)
private val Aqua = Color(0xFF73E6E0)
private val Ink = Color(0xFFF7EFF8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SakuraTheme {
                SakuraScreen()
            }
        }
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
    val messages = remember {
        mutableStateListOf(
            Message(
                "¡Hola! Soy Sakura 🌸 Tu compañera de ideas, con energía tranquila y un toque anime. ¿Qué hacemos hoy?",
                true
            )
        )
    }
    var draft by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Night
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Pink.copy(alpha = 0.18f)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Pink,
                        modifier = Modifier.padding(13.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("SAKURA AGENT", color = Pink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Tu compañera digital · modo seguro", color = Aqua, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    "Estoy en modo conversación. No controlo otras aplicaciones ni envío datos a un servidor.",
                    modifier = Modifier.padding(14.dp),
                    color = Ink.copy(alpha = 0.82f),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = true
            ) {
                items(messages.asReversed()) { message ->
                    MessageBubble(message)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text("Escribe a Sakura…", color = Ink.copy(alpha = 0.45f))
                        }
                        inner()
                    }
                )

                Button(
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            messages.add(Message(text, false))
                            messages.add(Message(reply(text), true))
                            draft = ""
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Night)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Sin acciones autónomas · tú tienes el control",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Ink.copy(alpha = 0.58f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromSakura) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            colors = CardDefaults.cardColors(
                containerColor = if (message.fromSakura) Panel else Pink.copy(alpha = 0.18f)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(13.dp),
                color = if (message.fromSakura) Ink else Pink,
                fontSize = 15.sp
            )
        }
    }
}

private fun reply(text: String): String = when {
    text.contains("hola", true) || text.contains("buenas", true) ->
        "¡Hola de nuevo! 🌸 Cuéntame qué tienes en mente y lo pensamos paso a paso."

    text.contains("ayuda", true) ->
        "¡Claro! Puedo conversar, ayudarte a ordenar una idea o redactar un plan. ¿Por dónde empezamos? ✨"

    text.contains("gracias", true) ->
        "¡De nada! Me alegra acompañarte 💖"

    else ->
        "Te leo 🌸 Esta primera versión aún no tiene un modelo de IA conectado, pero puedo ayudarte a estructurar esa idea."
}
