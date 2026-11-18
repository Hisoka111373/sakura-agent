package com.sakura.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun StarterModelCard(vm: SakuraViewModel, confirm: (String, () -> Unit) -> Unit, compact: Boolean = false) {
    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Modelo inicial de Sakura", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Qwen2.5 1.5B Instruct · Q4_K_M · 1.1 GB · Apache 2.0", fontSize = 12.sp)
            Text("GGUF pequeños (0.5B Q4_K_M y 1.5B Q3_K_M) en Centro de modelos; descarga confirmada con SHA-256.", fontSize = 12.sp)
            Text(
                "No viaja dentro de la APK. Se descarga una vez, se verifica con SHA-256 y se copia al almacenamiento privado. Después puede conversar sin Internet.",
                fontSize = 12.sp
            )
            when {
                vm.starterDownloading -> {
                    LinearProgressIndicator(progress = { vm.starterFraction }, modifier = Modifier.fillMaxWidth())
                    Text(vm.starterProgress, fontSize = 12.sp)
                    OutlinedButton(onClick = vm::cancelStarter) { Text("Cancelar descarga") }
                }
                vm.starterReady && vm.loadedModel == null -> {
                    Text("Descargado y verificado. Aún no está en RAM.", fontSize = 12.sp)
                    Button(enabled = !vm.busy, onClick = {
                        confirm("Cargar ${StarterModel.entry.title}. Se liberará el modelo anterior y se borrará el chat abierto al completar la carga.", vm::downloadOrLoadStarter)
                    }) { Text("Cargar modelo") }
                }
                vm.starterError.isNotBlank() -> {
                    Text("Error: ${vm.starterError}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Button(enabled = !vm.busy, onClick = { confirm("Descargar Qwen2.5 1.5B: 1.1 GB desde Hugging Face, verificar SHA-256 y cargar en CPU. Requiere Internet autorizado; puede consumir datos. Puedes cancelar.",vm::downloadStarter) }) { Text("Reintentar descarga") }
                }
                !vm.starterReady -> {
                    Button(enabled = !vm.busy, onClick = { confirm("Descargar Qwen2.5 1.5B: 1.1 GB desde Hugging Face, verificar SHA-256 y cargar en CPU. Requiere Internet autorizado; puede consumir datos. Puedes cancelar.",vm::downloadStarter) }) { Text("Descargar modelo ahora · 1.1 GB") }
                    Text("Usa Wi-Fi si puedes. La descarga se puede cancelar y no se incluye en la APK.", fontSize = 11.sp)
                }
                else -> Text("Modelo cargado: ${vm.loadedModel?.name ?: "sí"}", fontSize = 12.sp)
            }
            if (!compact) Row {
                TextButton(onClick = { vm.page = "Modelos" }) { Text("Centro de modelos") }
                TextButton(onClick = { vm.page = "Privacidad" }) { Text("Ajustes de descarga") }
            }
        }
    }
}
