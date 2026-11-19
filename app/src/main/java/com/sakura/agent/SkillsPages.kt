package com.sakura.agent

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SkillsPage(vm: SakuraViewModel) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todas") }
    var onlyFavorites by remember { mutableStateOf(false) }
    val categories=listOf("Todas")+Skills.entries.map { it.category }.distinct()
    val filtered=Skills.entries.filter { skill -> (category=="Todas" || skill.category==category) &&
        (!onlyFavorites || skill.name in vm.skills.favorites) &&
        (query.isBlank() || Skills.normalized(skill.name+" "+skill.instruction+" "+skill.category).contains(Skills.normalized(query))) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { SakuraHero("Tu biblioteca", "${Skills.entries.size} skills. Una idea, muchas posibilidades.",true) }
        item { Field("Buscar skill, lenguaje o tema",query,{query=it.take(100)},true) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            categories.forEach { name -> FilterChip(category==name,{category=name},label={Text(name)}) }
        } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            FilterChip(onlyFavorites,{onlyFavorites=!onlyFavorites},label={Text("Favoritos (${vm.skills.favorites.size})")})
            TextButton(enabled=!vm.busy,onClick={vm.skills.automatic();vm.page="Chat"}) { Text("Selección automática") }
        } }
        item { Text("${filtered.size} resultados · texto guiado, no ejecución de código ni permisos automáticos") }
        items(filtered,key={it.name}) { skill ->
            Card { Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(skill.category,color=MaterialTheme.colorScheme.secondary)
                Text(skill.name,style=MaterialTheme.typography.titleMedium)
                Text(skill.instruction.ifBlank { "Abre ${skill.page}. Usa los botones, revisiones y permisos de esa herramienta." },style=MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(enabled=!vm.busy,onClick={vm.skills.favorite(skill)}) { Text(if(skill.name in vm.skills.favorites) "Quitar favorito" else "Guardar favorito") }
                    Button(enabled=!vm.busy,onClick={vm.skills.use(skill)}) { Text(if(skill.page=="Chat") "Elegir skill" else "Abrir") }
                }
            } }
        }
        if(filtered.isEmpty()) item { Info("No hay coincidencias con estos filtros. Cambia categoría o búsqueda.") }
    }
}

@Composable
internal fun TextApiPage(vm: SakuraViewModel) {
    val current = vm.skills.config
    var provider by remember(current) { mutableStateOf(current.provider) }
    var model by remember(current) { mutableStateOf(current.model) }
    var endpoint by remember(current) { mutableStateOf(current.endpoint) }
    var key by remember(provider) { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val config = TextConfig(provider, model.trim(), endpoint.trim(),current.effort)
    val keyConfigured = config.key in vm.features.configured
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Info("Local por defecto y sin claves. API opcional: cada respuesta exige una vista previa completa del texto enviado y posible coste. Chat no añade notas privadas, archivos ni pantalla. La clave se guarda cifrada con Android Keystore y se envía solo al proveedor elegido. No se prueba una conexión al guardar; usa «Probar conexión» para una petición mínima.")
        Text("Proveedor", color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextProtocol.providers.forEach { option ->
                FilterChip(provider == option, {
                    provider = option
                    model = TextProtocol.firstModel(option)
                    endpoint = "https://api.openai.com/v1/chat/completions"
                }, label = { Text(option) }, modifier = Modifier.padding(end = 5.dp))
            }
        }
        if (provider != "Local") {
            Text("Modelos sugeridos", color = MaterialTheme.colorScheme.secondary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextProtocol.suggestions[provider].orEmpty().forEach { suggestion ->
                    AssistChip(onClick = { model = suggestion }, label = { Text(suggestion) }, modifier = Modifier.padding(end = 5.dp))
                }
            }
            Field("ID exacto del modelo (editable)", model, { model = it.take(150) }, !vm.busy)
            if (provider == "Compatible") {
                Field("Endpoint HTTPS completo /v1/chat/completions", endpoint, { endpoint = it.take(4096) }, !vm.busy)
                Info("Compatible significa protocolo Chat Completions con max_tokens y respuesta choices[].message.content. Solo dominio público HTTPS, sin query/credenciales/redirects. Tu clave se envía al host elegido: verifica que confías en él.")
            }
            Text("Clave API de $provider", color = MaterialTheme.colorScheme.secondary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.take(512) },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Clave API (se guarda cifrada)") },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "Ocultar clave" else "Mostrar clave")
                    }
                },
                supportingText = { Text(if (keyConfigured) "Hay una clave guardada para $provider. No se muestra." else "Sin clave guardada para $provider.", fontSize = 11.sp) }
            )
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Button(enabled = !vm.busy && key.isNotBlank(), onClick = {
                    vm.features.saveCredential(config.key, key.trim())
                    key = ""
                }) { Text("Guardar clave") }
                TextButton(enabled = !vm.busy && keyConfigured, onClick = { vm.features.saveCredential(config.key, ""); key = "" }) { Text("Borrar clave") }
                OutlinedButton(enabled = !vm.busy && keyConfigured, onClick = { if(current.provider=="Gemini") vm.skills.testGemini() else vm.skills.prepareProbe() }) { Text("Probar conexión") }
            }
            Text("Guardar no conecta ni valida la clave. «Probar conexión» envía una petición mínima y puede consumir cuota.", fontSize = 11.sp)
        } else {
            Info("Modo local: no necesitas clave API. Descarga Qwen2.5 1.5B desde Modelos; se verifica, se copia al almacenamiento privado y se carga en CPU.")
            Button(enabled = !vm.busy, onClick = { vm.page = "Modelos" }) { Text("Abrir centro de modelos") }
        }
        EngineSelector(vm)
        Info("Bajo: respuesta local de hasta192 tokens/45 s, una pareja de chat y cuatro notas; prioriza brevedad. Alto: hasta384 tokens/90 s, tres parejas y doce notas. No cambia pesos ni expone pensamiento interno. Gemini 3.1 Flash-Lite usa MINIMAL/HIGH con512/2048 tokens totales; en otros modelos no se inventa un control de pensamiento. Puede aumentar consumo y latencia.")
        Button(enabled=!vm.busy,onClick={vm.skills.save(TextConfig(provider,model.trim(),endpoint.trim(),vm.skills.config.effort))}) { Text("Guardar selección sin conectar") }
        Text("Selección guardada: ${current.provider} · ${current.model}")
        if(current.provider=="Gemini") {
            Button(enabled=!vm.busy && vm.internetAllowed,onClick=vm.skills::testGemini) { Text("Revisar prueba real de Gemini…") }
            Text(vm.skills.connectionStatus)
            Text("La APK no incluye tu clave. Guarda gemini_key en Privacidad y confirma esta prueba desde tu móvil.",style=MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick={vm.page="Privacidad"}) { Text("Configurar claves cifradas") }
        OutlinedButton(onClick={vm.page="Chat"}) { Text("Volver al chat") }
        if(vm.busy) TextButton(onClick=vm.skills::cancel) { Text("Cancelar petición de texto / búsqueda") }
    }
}

@Composable
internal fun SearchPage(vm: SakuraViewModel) {
    val controller=vm.skills
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Info("Escribe, por ejemplo, ‘busca una canción de Maná’. Preparar interpreta el destino sin enviar nada; tú revisas y abres YouTube, Google, navegador o Play Store. Google abre el navegador externo sin extraer resultados. Para resultados dentro de Sakura usa Brave con tu clave. Buscar un video no permite extraerlo ni descargarlo de YouTube.") }
        item { Field("Petición o consulta",controller.query,{controller.query=it.take(500)},!vm.busy) }
        item { OutlinedButton(enabled=!vm.busy,onClick={controller.route(controller.query)}) { Text("Preparar destino de la petición") } }
        item { Row { listOf("Web","YouTube","Google","Juegos").forEach { kind ->
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
    var typeText by remember { mutableStateOf("") }
    var screenReport by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Info("Control de pantalla opcional mediante Accesibilidad. Puede ver controles accesibles de la app autorizada y escribir/buscar. No hace capturas ni transmite pantalla; no lee en reposo. Sesión máxima 3 minutos, botón DETENER visible y confirmación por acción. Sin mensajes, pagos, contraseñas, permisos, instalaciones ni gestos arbitrarios. Solo apps de la lista; otras APK se pueden abrir desde Herramientas, no automatizar.")
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
        Button(enabled=!vm.busy,onClick={
            vm.features.prepare("LEER TEXTO VISIBLE DE LA PANTALLA AUTORIZADA\nRequiere sesión activa en la app autorizada. Hasta 200 nodos, profundidad 12 y 4000 caracteres; excluye contraseñas, campos sensibles y etiquetas bloqueadas. NUNCA se envía al modelo ni a la red automáticamente; se muestra en el panel y aquí.",false) {
                val service=SakuraAccessibilityService.connected ?: error("Activa el servicio y una sesión manualmente.")
                val visible=service.readVisibleText()
                screenReport=visible.ifBlank { "No se encontró texto visible permitido." }
                "Texto visible leído y mostrado solo localmente (${visible.length} caracteres)."
            }
        }) { Text("Leer texto visible de esta pantalla") }
        Field("Texto corto para escribir (máximo 500 caracteres)",typeText,{typeText=it.take(500)},!vm.busy)
        Button(enabled=!vm.busy && typeText.isNotBlank(),onClick={
            val value=typeText
            vm.features.prepare("ESCRIBIR TEXTO EN LA PANTALLA AUTORIZADA\nTexto exacto:\n$value\nAl confirmar solo se prepara en el panel: debes elegir un campo editable y pulsar «Confirmar escribir texto preparado». Sin Enter/IME automático. Requiere sesión activa en la app autorizada.",false) {
                val service=SakuraAccessibilityService.connected ?: error("Activa el servicio y una sesión manualmente.")
                service.prepareTypeText(value)
            }
        }) { Text("Revisar escribir texto en pantalla…") }
        if(screenReport.isNotBlank()) SelectionContainer { Text(screenReport,fontFamily=FontFamily.Monospace,fontSize=12.sp) }
        OutlinedButton(onClick={SakuraAccessibilityService.connected?.stopSession(); vm.notice("Sesión detenida; el permiso de Accesibilidad se desactiva en Ajustes.")}) { Text("Detener cualquier sesión") }
        TextButton(onClick={vm.page="Buscar"}) { Text("Alternativa: buscar por enlace sin Accesibilidad") }
    }
}

@Composable
internal fun FilesPage(vm: SakuraViewModel, folder: String?, chooseFolder: () -> Unit) {
    val context=LocalContext.current
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Info("Carpeta elegida con el selector de documentos de Android (SAF). Sakura no pide permiso de almacenamiento amplio y solo ve esa carpeta. Cada listado o lectura exige revisar una vista previa y confirmar; el contenido se muestra aquí y no se envía a ningún servicio.")
        Button(enabled=!vm.busy,onClick=chooseFolder) { Text(if(folder==null) "Elegir carpeta" else "Cambiar carpeta") }
        Text("Carpeta: ${folder ?: "sin elegir"}",color=MaterialTheme.colorScheme.secondary,fontSize=12.sp)
        Button(enabled=!vm.busy && folder!=null,onClick={
            folder?.let { raw ->
                vm.features.prepare("LISTAR ARCHIVOS DE LA CARPETA ELEGIDA\nCarpeta SAF: $raw\nHasta 100 entradas (nombre y tamaño) de esa carpeta. Sin permisos amplios ni acceso a otras ubicaciones. No se envía nada a la red ni al modelo; revisa la siguiente petición antes de compartir algo.",false) {
                    withContext(Dispatchers.IO) { PhoneFiles.list(context,Uri.parse(raw)) }
                        .joinToString("\n") { "${it.name} · ${it.size} bytes" }.ifBlank { "La carpeta no muestra archivos." }
                }
            }
        }) { Text("Listar archivos de la carpeta") }
        Field("Nombre exacto del archivo a leer",name,{name=it.take(200)},!vm.busy)
        Button(enabled=!vm.busy && folder!=null && PhoneFiles.validName(name),onClick={
            folder?.let { raw ->
                val target=name
                vm.features.prepare("LEER ARCHIVO DE LA CARPETA ELEGIDA\nCarpeta SAF: $raw\nNombre exacto: $target\nHasta 64 KiB, UTF-8 estricto; se rechazan binarios y bytes nulos. Solo se muestra localmente; no se envía a la red ni al modelo sin que revises la siguiente petición.",false) {
                    withContext(Dispatchers.IO) { PhoneFiles.readText(context,Uri.parse(raw),target) }
                }
            }
        }) { Text("Revisar lectura de archivo…") }
        if(vm.features.report.isNotBlank()) SelectionContainer { Text(vm.features.report,fontFamily=FontFamily.Monospace,fontSize=12.sp) }
    }
}