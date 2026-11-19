package com.sakura.agent

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Offline avatar transforms, not Live2D/lip sync. Paused off-screen or when disabled. */
@Composable
internal fun AnimatedMascot(size: Dp, motion: Boolean, thinking: Boolean, speaking: Boolean) {
    val event = LocalCompanionEvent.current
    val sequence = LocalCompanionSequence.current
    val gesture = remember { Animatable(0f) }
    var playedSequence by remember { mutableStateOf(sequence) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,_ -> resumed=lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(sequence, motion, resumed) {
        gesture.snapTo(0f)
        val fresh = sequence != playedSequence
        playedSequence = sequence
        if(fresh && motion && resumed && event in listOf("Saludo", "Saltito", "Lista")) {
            gesture.animateTo(1f,tween(1200,easing=LinearEasing))
            gesture.snapTo(0f)
        }
    }
    val values=if(motion && resumed) {
        val transition=rememberInfiniteTransition(label="Sakura companion")
        val bob by transition.animateFloat(-2f,2f,infiniteRepeatable(tween(2400,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="breathing")
        val tilt by transition.animateFloat(-2f,if(thinking) 4f else 2f,infiniteRepeatable(tween(if(thinking) 900 else 2800),RepeatMode.Reverse),label="sway")
        val scale by transition.animateFloat(1f,if(speaking) 1.075f else 1.025f,infiniteRepeatable(tween(if(speaking) 320 else 2400),RepeatMode.Reverse),label="voice pulse")
        Triple(bob,tilt,scale)
    } else Triple(0f,0f,1f)
    val wave = kotlin.math.sin(gesture.value * kotlin.math.PI.toFloat() * 6f)
    val hop = if(event == "Saltito" || event == "Lista") -kotlin.math.abs(wave) * 12f else 0f
    val greetingTilt = if(event == "Saludo") wave * 13f else wave * 3f
    val squash = if(event == "Saltito" || event == "Lista") kotlin.math.abs(wave) * 0.06f else 0f
    val density=LocalDensity.current.density
    Image(painterResource(R.drawable.sakura_avatar),"Sakura: ${if(speaking) "hablando" else if(thinking) "pensando" else "contigo"}",
        Modifier.size(size).graphicsLayer { translationY=(values.first+hop)*density; translationX=wave*3f*density; rotationZ=values.second+greetingTilt; scaleX=values.third-squash; scaleY=values.third+squash }.clip(CircleShape),contentScale=ContentScale.Crop)
}

@Composable
internal fun EngineSelector(vm: SakuraViewModel) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        FilterChip(vm.skills.config.provider=="Local",{vm.skills.selectEngine(true)},enabled=!vm.busy,label={Text("Local · GGUF")})
        FilterChip(vm.skills.config.provider!="Local",{vm.skills.selectEngine(false)},enabled=!vm.busy,label={Text("En línea · ${vm.skills.onlineConfig.provider}")})
        TextButton(enabled=!vm.busy,onClick={vm.page="APIs"}) { Text("Configurar") }
    }
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Bajo: ágil · Alto: detallado")
        ThinkingPolicy.levels.forEach { effort -> FilterChip(vm.skills.config.effort==effort,{vm.skills.effort(effort)},enabled=!vm.busy,label={Text(effort)}) }
    }
}

@Composable
internal fun CompanionCard(vm: SakuraViewModel, speaking: Boolean, greet: () -> Unit) {
    Card { Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        AnimatedMascot(60.dp,vm.mascotMotion,vm.generating,speaking)
        Column(Modifier.weight(1f)) {
            Text(vm.companionPhrase,style=MaterialTheme.typography.titleSmall)
            Text(if(vm.generating) "${vm.generatedTokens} tokens · puedes detener" else "Tu compañera para aprender y crear",style=MaterialTheme.typography.bodySmall)
        }
        Column {
            TextButton(enabled=!vm.busy,onClick=greet) { Text("Saludar") }
            TextButton(enabled=!vm.busy,onClick=vm::playfulCompanion) { Text("Saltito") }
        }
    } }
}
internal val LocalMascotMotion = compositionLocalOf { true }
internal val LocalMascotSpeaking = compositionLocalOf { false }
internal val LocalCompanionEvent = compositionLocalOf { "Saludo" }
internal val LocalCompanionSequence = compositionLocalOf { 0L }

@Composable
internal fun mascotMotionActive(): Boolean {
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,_ -> resumed=lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    return LocalMascotMotion.current && resumed
}
