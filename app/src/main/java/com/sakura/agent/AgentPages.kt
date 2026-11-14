package com.sakura.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AgentPage(vm: SakuraViewModel) {
    val agent = vm.agent
    var objective by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Info("Agente con hasta ${AgentTools.MAX_STEPS} pasos. El modelo solo propone JSON; Kotlin valida y ejecuta una herramienta por paso. Puede buscar en Brave, leer páginas HTTPS, consultar YouTube/Piped y leer GitHub. Descargas, imágenes, abrir apps y pantalla piden confirmación aparte. No ejecuta comandos, no instala APK, no paga ni envía mensajes. Puedes detenerlo cuando quieras.")
        Info("El objetivo y las observaciones se envían al proveedor de texto configurado en APIs. Con GGUF local se quedan en el teléfono. No incluyas tokens, contraseñas ni datos privados innecesarios.")
        Field("Objetivo del agente (máximo 2000 caracteres)", objective, { objective = it.take(2000) },
            !vm.busy && !agent.running, singleLine = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !vm.busy && !agent.running && objective.isNotBlank() && vm.internetAllowed,
                onClick = { agent.start(objective) }) { Text("Revisar e iniciar agente…") }
            OutlinedButton(enabled = agent.running || vm.busy, onClick = agent::stop) { Text("Detener") }
        }
        if (!vm.internetAllowed) Text("Activa Internet en Privacidad: el agente lo necesita para buscar y leer.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        if (agent.running) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Ejecutando… paso ${agent.steps.size}/${AgentTools.MAX_STEPS}", fontSize = 12.sp)
        }
        if (agent.finalAnswer.isNotBlank()) {
            Card { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Respuesta final", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                SelectionContainer { Text(agent.finalAnswer) }
            } }
        }
        if (agent.steps.isNotEmpty()) Text("Registro de pasos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        agent.steps.forEach { step ->
            Card { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${step.index}. ${step.tool}${if (step.failed) " · fallo" else ""}",
                    color = if (step.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
                if (step.detail.isNotBlank()) SelectionContainer { Text(step.detail, fontSize = 12.sp) }
                SelectionContainer { Text(step.observation, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            } }
        }
        if (agent.lastRaw.isNotBlank()) {
            Text("Última respuesta cruda del modelo", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            SelectionContainer { Text(agent.lastRaw.take(4000), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        OutlinedButton(onClick = { vm.page = "APIs" }) { Text("Configurar proveedor de texto") }
    }
}
