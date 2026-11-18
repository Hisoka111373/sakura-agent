package com.sakura.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SakuraPink = Color(0xFFFF79B9)
private val SakuraAqua = Color(0xFF73E6E0)
private val SakuraInk = Color(0xFFF7EFF8)

@Composable
internal fun SakuraHero(title: String, subtitle: String, compact: Boolean = false) {
    val drift = if(mascotMotionActive()) {
        val motion=rememberInfiniteTransition(label="hero")
        val value by motion.animateFloat(0f,1f,infiniteRepeatable(tween(16000,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="drift")
        value
    } else 0f
    Box(Modifier.fillMaxWidth().height(if (compact) 152.dp else 248.dp).clip(RoundedCornerShape(26.dp))) {
        Image(
            painterResource(R.drawable.sakura_garden),
            "Sakura, mascota original, en un jardín de cerezos nocturno",
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.02f + drift * 0.05f
                scaleY = 1.02f + drift * 0.05f
                translationX = drift * 14f
                translationY = -drift * 8f
            },
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF100D1C).copy(alpha = .94f), Color(0xFF100D1C).copy(alpha = .6f), Color.Transparent))))
        Petals(Modifier.matchParentSize())
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.63f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SAKURA / TU ESPACIO", color = SakuraAqua, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(title, color = SakuraInk, fontSize = if (compact) 23.sp else 31.sp, fontWeight = FontWeight.Bold, lineHeight = if (compact) 26.sp else 34.sp)
            Text(subtitle, color = SakuraInk.copy(alpha = .85f), fontSize = 12.sp, lineHeight = 17.sp)
        }
        SakuraMascot(size = if (compact) 58.dp else 76.dp, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp))
    }
}

@Composable
private fun Petals(modifier: Modifier = Modifier) {
    if(!mascotMotionActive()) return
    val motion = rememberInfiniteTransition(label = "petals")
    val progress by motion.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "fall"
    )
    Canvas(modifier) {
        val count = 14
        for (index in 0 until count) {
            val seed = index * 0.173f
            val speed = 0.22f + (index % 5) * 0.07f
            val x = ((seed * 7.3f + progress * speed) % 1f) * size.width
            val y = ((seed * 3.1f + progress * (0.3f + (index % 4) * 0.09f)) % 1f) * size.height
            val width = 6f + (index % 3) * 3f
            rotate(progress * 360f + index * 31f, Offset(x, y)) {
                drawOval(
                    color = Color(0xFFFFB7D5).copy(alpha = 0.28f + (index % 4) * 0.08f),
                    topLeft = Offset(x - width / 2f, y - width / 4f),
                    size = Size(width, width / 2f)
                )
            }
        }
    }
}

@Composable
internal fun SakuraMascot(modifier: Modifier = Modifier, size: Dp = 72.dp, speaking: Boolean = false) {
    Box(modifier) { AnimatedMascot(size,LocalMascotMotion.current,speaking,LocalMascotSpeaking.current) }

}

@Composable
internal fun GardenPage(vm: SakuraViewModel, confirm: (String, () -> Unit) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AnimatedVisibility(visible, enter = fadeIn()) { SakuraHero("Ideas que\nflorecen.", "Tu compañera para crear, aprender y programar. A tu ritmo.") } }
        if (!vm.starterReady || vm.loadedModel == null) item { StarterModelCard(vm, confirm) }
        item {
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SakuraMascot(size = 86.dp, speaking = vm.starterDownloading)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Sakura te escucha", color = SakuraPink, fontWeight = FontWeight.Bold)
                        Text(if (vm.autoSpeak) "La voz automática está activada. También puedes pedirle un saludo." else "Puedes pedirle un saludo o activar la lectura automática en el chat.", fontSize = 12.sp)
                        Row {
                            TextButton(onClick = vm::speakGreeting) { Text("Escuchar saludo") }
                            TextButton(onClick = { if(vm.autoSpeak) vm.updateAutoSpeak(false) else confirm("Leer respuestas en voz alta con voz local, solo en primer plano. Personas cercanas pueden oírlas.") { vm.updateAutoSpeak(true) } }) { Text(if (vm.autoSpeak) "Voz auto: sí" else "Voz auto: no") }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.page = "Chat" }, modifier = Modifier.weight(1f)) { Text("Conversar") }
                OutlinedButton(onClick = { vm.page = "Skills" }, modifier = Modifier.weight(1f)) { Text("Explorar skills") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GardenStat("${Skills.entries.size}", "skills disponibles", Modifier.weight(1f))
                GardenStat(vm.skills.config.provider, "motor elegido", Modifier.weight(1f))
            }
        }
        item { Text("Un pequeño taller para grandes ideas", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Código, sin prisa", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Python, Kotlin, Java, TypeScript, SQL y más. Depuración, pruebas y arquitectura con explicaciones claras.")
                    TextButton(enabled = !vm.busy, onClick = { vm.skills.use(Skills.entries.first { it.name == "Kotlin / Android" }) }) { Text("Trabajar en Kotlin →") }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().heightIn(min = 178.dp).clip(RoundedCornerShape(22.dp))) {
                Image(painterResource(R.drawable.sakura_blossoms), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                Box(Modifier.matchParentSize().background(Color(0xFF100D1C).copy(alpha = .77f)))
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Una skill para cada idea", color = SakuraAqua, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("Sakura selecciona una especialidad de texto por reglas locales y te muestra cuál. Puedes cambiarla; las acciones externas siguen bajo tu control.")
                    TextButton(onClick = { vm.page = "Skills" }) { Text("Abrir biblioteca →") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tu espacio, tus permisos", fontWeight = FontWeight.Bold)
                    Text("Ilustraciones incluidas en la app: no necesitan Internet. Chat local con GGUF; APIs opcionales con revisión de cada petición. Ninguna skill de texto ejecuta código o herramientas.")
                    TextButton(onClick = { vm.page = "Privacidad" }) { Text("Revisar privacidad →") }
                }
            }
        }
        item { Text("Arte original generado para Sakura. Las tres imágenes reutilizan la misma ilustración; no son personajes de una franquicia. No hay generación visual local en el teléfono.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)) }
    }
}

@Composable
private fun GardenStat(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(15.dp)) {
            Text(value, color = SakuraAqua, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp)
        }
    }
}
