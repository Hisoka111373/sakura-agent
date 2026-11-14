package com.sakura.agent

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SkillsPage(vm: SakuraViewModel) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        item { Info("Skills dentro de Sakura: instrucciones de texto y accesos a herramientas reales. El modelo no ejecuta acciones. Las habilidades de texto requieren GGUF o API configurada; no incluyen ejecución de código ni generación visual local.") }
        items(Skills.entries) { skill ->
            OutlinedButton(enabled=!vm.busy,onClick={vm.skills.use(skill)},modifier=Modifier.fillMaxWidth()) { Text("${skill.name} → ${skill.page}") }
        }
    }
}

@Composable
internal fun TextApiPage(vm: SakuraViewModel) {
    val current=vm.skills.config
    var provider by remember(current) { mutableStateOf(current.provider) }
    var model by remember(current) { mutableStateOf(current.model) }
    var endpoint by remember(current) { mutableStateOf(current.endpoint) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Info("Local por defecto. API opcional: cada respuesta exige una vista previa completa del texto enviado y posible coste. No se envían notas privadas, archivos ni pantalla. Configura claves cifradas en Privacidad. No se prueba una conexión al guardar. Disponibilidad/precios y retención dependen del proveedor.")
        Row(Modifier.horizontalScroll(rememberScrollState())) { TextProtocol.providers.forEach { option ->
            FilterChip(provider==option,{provider=option;model=""},label={Text(option)},modifier=Modifier.padding(end=5.dp))
        } }
        if(provider!="Local") {
            Field("ID exacto del modelo (de tu proveedor)",model,{model=it.take(150)},!vm.busy)
            if(provider=="Compatible") {
                Field("Endpoint HTTPS completo /v1/chat/completions",endpoint,{endpoint=it.take(4096)},!vm.busy)
                Info("Compatible significa protocolo Chat Completions con max_tokens y respuesta choices[].message.content. No cualquier API. Solo dominio público HTTPS, sin query/credenciales/redirects. Tu clave compatible se envía al host elegido: verifica que confías en él.")
            }
            Text("Clave requerida: ${TextConfig(provider,model,endpoint).key} · Privacidad")
        }
        Button(enabled=!vm.busy,onClick={vm.skills.save(TextConfig(provider,model.trim(),endpoint.trim()))}) { Text("Guardar selección sin conectar") }
        Text("Selección guardada: ${current.provider} · ${current.model}")
        OutlinedButton(onClick={vm.page="Privacidad"}) { Text("Configurar claves cifradas") }
        OutlinedButton(onClick={vm.page="Chat"}) { Text("Volver al chat") }
        if(vm.busy) TextButton(onClick=vm.skills::cancel) { Text("Cancelar petición de texto / búsqueda") }
    }
}

@Composable
internal fun SearchPage(vm: SakuraViewModel) {
    val controller=vm.skills
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Info("Escribe, por ejemplo, ‘busca una canción de Maná’. Preparar interpreta el destino sin enviar nada; tú revisas y abres YouTube, navegador o Play Store. Para resultados dentro de Sakura usa Brave con tu clave. Buscar un video no permite extraerlo ni descargarlo de YouTube.") }
        item { Field("Petición o consulta",controller.query,{controller.query=it.take(500)},!vm.busy) }
        item { OutlinedButton(enabled=!vm.busy,onClick={controller.route(controller.query)}) { Text("Preparar destino de la petición") } }
        item { Row { listOf("Web","YouTube","Juegos").forEach { kind ->
            FilterChip(controller.kind==kind,{controller.kind=kind},label={Text(kind)},modifier=Modifier.padding(end=5.dp))
        } } }
        item { Button(enabled=!vm.busy && controller.query.isNotBlank(),onClick=controller::prepareExternalSearch) { Text("Revisar búsqueda externa…") } }
        item { Button(enabled=!vm.busy && vm.internetAllowed && controller.query.isNotBlank(),onClick=controller::prepareSearch) { Text("Revisar búsqueda Brave API…") } }
        if(vm.busy) item { TextButton(onClick=controller::cancel) { Text("Cancelar petición Brave / texto") } }
        items(controller.results,key={it.url}) { result ->
            Card { Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(result.title); Text(result.description); Text(result.url)
                TextButton(enabled=!vm.busy,onClick={controller.prepareExternal(result.url)}) { Text("Revisar abrir resultado…") }
                Row {
                    TextButton(enabled=!vm.busy && vm.internetAllowed,onClick={vm.features.prepareDownload(result.url,"Video")}) { Text("Si es archivo de video…") }
                    TextButton(enabled=!vm.busy && vm.internetAllowed,onClick={vm.features.prepareDownload(result.url,"Imagen")}) { Text("Si es imagen…") }
                }
                Text("Descarga solo si es archivo directo y tienes permiso. Las páginas web se rechazan por cabecera; no se extrae su contenido.")
            } }
        }
    }
}

@Composable
internal fun ScreenPage(vm: SakuraViewModel) {
    val context=LocalContext.current
    var app by remember { mutableStateOf("YouTube") }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Info("Control de pantalla opcional mediante Accesibilidad. Puede ver controles accesibles de la app autorizada y escribir/buscar. No hace capturas ni transmite pantalla; no lee en reposo. Sesión máxima 3 minutos, botón DETENER visible y confirmación por acción. Sin mensajes, pagos, contraseñas, permisos, instalaciones ni gestos arbitrarios. Aquí solo apps de la lista; el Agente puede iniciar una sesión general en otra app instalada, siempre con confirmación y sin paquetes sensibles.")
        Info("Android debe permitirte activar el servicio manualmente. Una instalación externa puede requerir que tú habilites ajustes restringidos según tu versión; Sakura no los cambia. Desactiva el servicio en Ajustes al terminar. Si no reconoce un control, usa la app manualmente; no garantiza compatibilidad con todas las versiones.")
        OutlinedButton(enabled=!vm.busy,onClick={
            vm.features.prepare("ABRIR AJUSTES DE ACCESIBILIDAD\nLee los permisos de Android. Activar Sakura es opcional y manual: autoriza capacidad de leer controles visibles y realizar acciones dentro de sesiones confirmadas. No se activa ni concede permiso automáticamente.",false) {
                withContext(Dispatchers.Main) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                "Ajustes abiertos; activación NO verificada. Vuelve a Sakura tras decidir."
            }
        }) { Text("Revisar abrir Ajustes…") }
        Row(Modifier.horizontalScroll(rememberScrollState())) { ScreenPolicy.packages.keys.forEach { name ->
            FilterChip(app==name,{app=name},label={Text(name)},modifier=Modifier.padding(end=5.dp))
        } }
        Field("Consulta a escribir (p. ej. Maná Rayando el sol)",query,{query=it.take(500)},!vm.busy)
        Button(enabled=!vm.busy && query.isNotBlank(),onClick={
            val chosen=ScreenPolicy.packages.getValue(app); val text=query
            vm.features.prepare("INICIAR SESIÓN Y ABRIR $app\nPaquete: $chosen\nConsulta preparada: $text\nMáximo 3 minutos. Ver controles lee solo controles de búsqueda de esa app; cada pulsación/escritura requiere confirmación en el panel visible. La app usa su cuenta y su propia red. No ejecuta ni continúa automáticamente. Mantén libre el botón DETENER.",false) {
                withContext(Dispatchers.Main) {
                    val service=SakuraAccessibilityService.connected ?: error("Activa el servicio manualmente en Ajustes y vuelve.")
                    val intent=context.packageManager.getLaunchIntentForPackage(chosen) ?: error("App oficial no instalada o no visible.")
                    service.startSession(chosen,text)
                    try { context.startActivity(intent) } catch(e:Exception) { service.stopSession(); throw e }
                }
                "Sesión iniciada. Usa el panel de Sakura: Ver controles → elegir → revisar → confirmar. No se ha escrito ni buscado todavía."
            }
        }) { Text("Revisar iniciar sesión de pantalla…") }
        OutlinedButton(onClick={SakuraAccessibilityService.connected?.stopSession(); vm.notice("Sesión detenida; el permiso de Accesibilidad se desactiva en Ajustes.")}) { Text("Detener cualquier sesión") }
        TextButton(onClick={vm.page="Buscar"}) { Text("Alternativa: buscar por enlace sin Accesibilidad") }
    }
}