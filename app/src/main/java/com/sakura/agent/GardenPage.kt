package com.sakura.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SakuraHero(title: String, subtitle: String, compact: Boolean = false) {
    Box(Modifier.fillMaxWidth().height(if(compact) 152.dp else 248.dp).clip(RoundedCornerShape(26.dp))) {
        Image(painterResource(R.drawable.sakura_garden),"Sakura, mascota original, en un jardín de cerezos nocturno",
            Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.CenterEnd)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF100D1C).copy(alpha=.94f),Color(0xFF100D1C).copy(alpha=.6f),Color.Transparent))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.59f).padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("SAKURA / TU ESPACIO",color=Color(0xFF73E6E0),fontSize=10.sp,letterSpacing=1.sp)
            Text(title,color=Color(0xFFF7EFF8),fontSize=if(compact) 23.sp else 31.sp,fontWeight=FontWeight.Bold,lineHeight=if(compact) 26.sp else 34.sp)
            Text(subtitle,color=Color(0xFFF7EFF8).copy(alpha=.85f),fontSize=12.sp,lineHeight=17.sp)
        }
    }
}

@Composable
internal fun GardenPage(vm: SakuraViewModel) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible=true }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { AnimatedVisibility(visible,enter=fadeIn()) { SakuraHero("Ideas que\nflorecen.","Tu compañera para crear, aprender y programar. A tu ritmo.") } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={vm.page="Chat"},modifier=Modifier.weight(1f)) { Text("Conversar") }
            OutlinedButton(onClick={vm.page="Skills"},modifier=Modifier.weight(1f)) { Text("Explorar skills") }
        } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            GardenStat("${Skills.entries.size}","skills disponibles",Modifier.weight(1f))
            GardenStat(vm.skills.config.provider,"motor elegido",Modifier.weight(1f))
        } }
        item { Text("Un pequeño taller para grandes ideas",fontWeight=FontWeight.SemiBold,fontSize=18.sp) }
        item { Card { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Código, sin prisa",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
            Text("Python, Kotlin, Java, TypeScript, SQL y más. Depuración, pruebas y arquitectura con explicaciones claras.")
            TextButton(enabled=!vm.busy,onClick={vm.skills.use(Skills.entries.first { it.name=="Kotlin / Android" })}) { Text("Trabajar en Kotlin →") }
        } } }
        item { Box(Modifier.fillMaxWidth().heightIn(min=178.dp).clip(RoundedCornerShape(22.dp))) {
            Image(painterResource(R.drawable.sakura_blossoms),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Color(0xFF100D1C).copy(alpha=.77f)))
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Una skill para cada idea",color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold,fontSize=19.sp)
                Text("Sakura selecciona una especialidad de texto por reglas locales y te muestra cuál. Puedes cambiarla; las acciones externas siguen bajo tu control.")
                TextButton(onClick={vm.page="Skills"}) { Text("Abrir biblioteca →") }
            }
        } }
        item { Card { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Tu espacio, tus permisos",fontWeight=FontWeight.Bold)
            Text("Ilustraciones incluidas en la app: no necesitan Internet. Chat local con GGUF; APIs opcionales con revisión de cada petición. Ninguna skill de texto ejecuta código o herramientas.")
            TextButton(onClick={vm.page="Privacidad"}) { Text("Revisar privacidad →") }
        } } }
        item { Text("Arte original generado para Sakura. Las tres imágenes reutilizan la misma ilustración; no son personajes de una franquicia. No hay generación visual local en el teléfono.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f)) }
    }
}

@Composable
private fun GardenStat(value: String, label: String, modifier: Modifier) {
    Surface(modifier,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(15.dp)) {
            Text(value,color=MaterialTheme.colorScheme.secondary,fontSize=23.sp,fontWeight=FontWeight.Bold)
            Text(label,fontSize=11.sp)
        }
    }
}