package com.sakura.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
internal fun AgentPage(vm: SakuraViewModel) {
    val agent = vm.agent
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { SakuraHero("Agente por pasos","Sakura propone. Tú revisas y decides.",true) }
        item { Info("Hasta 10 propuestas por sesión. Cada herramienta y cada petición de nube se revisa y confirma por separado. Nunca continúa sola. No ejecuta shell, instala APK, mezcla PR ni automatiza pagos. Datos y registro solo en RAM; no hay historial persistente del agente.") }
        item { Text("Proveedor: ${vm.skills.config.provider} · propuesta ${agent.steps}/${AgentTools.MAX_STEPS}") }
        item { Column { EngineSelector(vm) } }
        item { Field("Objetivo (sin claves ni secretos)",agent.goal,{agent.goal=it.take(1500)},!vm.busy && !agent.active && !agent.awaiting,false) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(enabled=!vm.busy && !agent.active && !agent.awaiting && agent.goal.isNotBlank(),onClick=agent::start) { Text("Iniciar / nueva sesión") }
            OutlinedButton(enabled=agent.active || agent.awaiting,onClick=agent::stop) { Text("Detener") }
        } }
        item { OutlinedButton(enabled=!vm.busy && agent.canNext,onClick=agent::next) { Text("Proponer siguiente paso…") } }
        if(agent.awaiting) item { Info(if(vm.busy) "Operación confirmada en curso. Detener solicita cancelación, no garantiza reembolso ni deshacer una escritura." else "Hay una solicitud pendiente: lee la vista previa completa y confirma o cancela. No se ha ejecutado todavía.") }
        if(agent.proposal.isNotEmpty()) item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Info("Propuesta del modelo, no resultado. Puedes corregir sus argumentos antes de preparar la confirmación. No aceptes hashes ni destinatarios inventados.")
            Field("JSON de una herramienta",agent.proposal,{agent.proposal=it.take(24000)},!vm.busy && !agent.awaiting,false)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(enabled=!vm.busy && !agent.awaiting,onClick=agent::prepare) { Text("Validar y revisar acción…") }
                TextButton(enabled=!vm.busy && !agent.awaiting,onClick=agent::skip) { Text("Rechazar paso") }
            }
        } }
        if(agent.final.isNotEmpty()) item { Card { SelectionContainer { Text(agent.final,Modifier.padding(14.dp)) } } }
        item { Text("Registro y resultados (fragmentos)",style=MaterialTheme.typography.titleMedium) }
        items(agent.events) { event -> Card { SelectionContainer { Text(event,Modifier.padding(12.dp),fontFamily=FontFamily.Monospace) } } }
        if(!agent.active && agent.steps >= AgentTools.MAX_STEPS) item { Info("Límite alcanzado. Revisa resultados antes de iniciar una nueva sesión. No significa que el objetivo esté completado.") }
        item { TextButton(enabled=!vm.busy,onClick={vm.page="APIs"}) { Text("Elegir proveedor / GGUF en Modelos") } }
    }
}