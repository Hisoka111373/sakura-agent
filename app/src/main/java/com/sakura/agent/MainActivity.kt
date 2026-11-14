package com.sakura.agent

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private val Night = Color(0xFF100D1C)
private val Panel = Color(0xFF1B172B)
private val Pink = Color(0xFFFF79B9)
private val Aqua = Color(0xFF73E6E0)
private val Ink = Color(0xFFF7EFF8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Pink, secondary = Aqua,
                background = Night, surface = Panel, onPrimary = Night, onBackground = Ink, onSurface = Ink)) {
                SakuraScreen()
            }
        }
    }
}

@Composable
private fun SakuraScreen(vm: SakuraViewModel = viewModel()) {
    val context = LocalContext.current
    val voice = remember(context) { SakuraVoice(context, { if (!vm.busy) vm.draft = it }, vm::notice) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(voice, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); voice.close() }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) voice.dictate(vm.internetAllowed) else vm.notice("Sin permiso de micrófono. Puedes escribir.")
    }
    var confirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val confirm: (String, () -> Unit) -> Unit = { text, action -> confirmation = text to action }
    val chooseModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importModel) }
    val chooseCode = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importSource) }
    val chooseApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::inspectApk) }
    var exportFile by remember { mutableStateOf<DownloadedFile?>(null) }
    val exportDownload = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val selected = exportFile
        exportFile = null
        if (uri != null && selected != null) vm.features.export(selected,uri)
    }

    Surface(Modifier.fillMaxSize(), color = Night) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(44.dp), shape = CircleShape, color = Pink.copy(alpha = 0.18f)) {
                    Icon(Icons.Default.Favorite, null, tint = Pink, modifier = Modifier.padding(12.dp))
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("SAKURA AGENT", color = Pink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(if (vm.loadedModel != null) "IA local · ${vm.level}" else "Sin modelo cargado", color = Aqua, fontSize = 12.sp)
                }
                TextButton(onClick = { voice.stop(); vm.notice("Voz detenida.") }) { Text("Silencio") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Chat", "Agente", "Skills", "Buscar", "Pantalla", "APIs", "Plan", "Memoria", "Modelos", "Descargas", "Crear", "Mensajes", "Herramientas", "GitHub", "Privacidad").forEach { page ->
                    FilterChip(selected = vm.page == page, onClick = { vm.page = page }, label = { Text(page) })
                }
            }
            if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(vm.status, color = Aqua, fontSize = 12.sp, modifier = Modifier.padding(vertical = 7.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (vm.page) {
                    "Agente" -> AgentPage(vm)
                    "Chat" -> ChatPage(vm, { message -> voice.speak(message, vm.internetAllowed) }, {
                        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                            voice.dictate(vm.internetAllowed)
                        else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }, { voice.stop(); vm.send() }, confirm)
                    "Memoria" -> MemoryPage(vm, confirm)
                    "Modelos" -> ModelsPage(vm, { chooseModel.launch(arrayOf("*/*")) }, confirm)
                    "Herramientas" -> ToolsPage(vm, { chooseCode.launch(arrayOf("*/*")) }, { chooseApk.launch(arrayOf("*/*")) }, confirm)
                    "GitHub" -> GithubPage(vm)
                    "Skills" -> SkillsPage(vm)
                    "Buscar" -> SearchPage(vm)
                    "Pantalla" -> ScreenPage(vm)
                    "APIs" -> TextApiPage(vm)
                    "Plan" -> PlanPage(vm)
                    "Descargas" -> DownloadsPage(vm, { file ->
                        confirm("Exportar una copia de ${file.name} a la ubicación que elijas. Fuera del almacenamiento privado, otras apps pueden acceder a ella.") {
                            exportFile = file; exportDownload.launch(file.name)
                        }
                    })
                    "Crear" -> CreateMediaPage(vm,confirm)
                    "Mensajes" -> MessagesPage(vm,confirm)
                    "Privacidad" -> PrivacyPage(vm) { voice.stop(); vm.setInternet(it) }
                }
            }
        }
    }

    confirmation?.let { (text, action) ->
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text("Confirmar acción") },
            text = { Text(text) }, dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancelar") } },
            confirmButton = { Button(enabled = !vm.busy, onClick = { confirmation = null; action() }) { Text("Confirmar") } })
    }
    vm.pendingEdit?.let { edit ->
        AlertDialog(onDismissRequest = vm::dismissEdit, title = { Text("Escribir en GitHub") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text("Se creará un commit REAL en ${edit.source.owner}/${edit.source.repo}, rama ${edit.source.branch}. No es un pull request.", color = Pink)
                    Text("Archivo: ${edit.source.path}\nSHA leído: ${edit.source.sha}\nCommit: ${edit.message}")
                    Text("ANTES", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    SelectionContainer { Text(edit.source.text, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    Text("DESPUÉS", fontWeight = FontWeight.Bold, color = Aqua, modifier = Modifier.padding(top = 12.dp))
                    SelectionContainer { Text(edit.replacement, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    Text("Solo confirma si revisaste todo el cambio. Se enviará el contenido a GitHub por Internet.")
                }
            }, dismissButton = { TextButton(onClick = vm::dismissEdit) { Text("Cancelar") } },
            confirmButton = { Button(enabled = !vm.busy && vm.internetAllowed && vm.hasToken, onClick = vm::confirmEdit) { Text("Confirmar commit") } })
    }
    vm.features.pending?.let { action ->
        AlertDialog(onDismissRequest = vm.features::dismiss, title = { Text("Revisar acción completa") },
            text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                SelectionContainer { Text(action.preview, fontSize = 13.sp) }
                Text("Solo esta acción. No continúa automáticamente con el siguiente paso.",color = Aqua)
            } },
            dismissButton = { TextButton(onClick = vm.features::dismiss) { Text("Cancelar") } },
            confirmButton = { Button(enabled = !vm.busy && (!action.network || vm.internetAllowed), onClick = vm.features::confirm) { Text("Confirmar esta acción") } })
    }
}

@Composable
private fun ChatPage(vm: SakuraViewModel, speak: (String) -> Unit, dictate: () -> Unit, send: () -> Unit,
    confirm: (String, () -> Unit) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tu compañera, a tu ritmo", color = Ink, modifier = Modifier.weight(1f))
            TextButton(onClick={vm.page="APIs"}) { Text(vm.skills.config.provider) }
            TextButton(enabled = !vm.busy && vm.messages.isNotEmpty(), onClick = {
                confirm("Borrar el chat abierto y su borrador. Las notas de memoria se conservan.", vm::clearChat)
            }) { Text("Nuevo chat") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.messages.asReversed()) { message -> MessageBubble(message) { speak(message.text) } }
            if (vm.messages.isEmpty()) item {
                Info("¡Hola! Soy Sakura 🌸 Importa un modelo GGUF en Modelos y lo pensamos juntas, paso a paso. Las herramientas solo se activan cuando tú las pulsas.")
            }
        }
        if (vm.generating) TextButton(onClick = vm::cancelGeneration) { Text("Detener generación") }
        if (vm.busy && vm.skills.config.provider != "Local") TextButton(onClick=vm.skills::cancel) { Text("Cancelar petición nube") }
        if (vm.draft.isNotBlank()) TextButton(enabled=!vm.busy,onClick={vm.skills.route(vm.draft)}) { Text("Preparar como búsqueda (sin enviarla al modelo)") }
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !vm.busy, onClick = dictate) { Icon(Icons.Default.Mic, "Dictar o terminar dictado", tint = Aqua) }
            OutlinedTextField(value = vm.draft, onValueChange = { vm.draft = it }, enabled = !vm.busy,
                modifier = Modifier.weight(1f), placeholder = { Text("Escribe o dicta…") }, maxLines = 5)
            IconButton(enabled = !vm.busy && vm.draft.isNotBlank(), onClick = send) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar local o revisar petición API", tint = Pink) }
        }
        Text("El dictado no envía mensajes. El chat se mantiene solo durante esta sesión.", fontSize = 11.sp, color = Ink.copy(alpha = 0.6f), modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onSpeak: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromSakura) Arrangement.Start else Arrangement.End) {
        Card(Modifier.fillMaxWidth(0.92f), colors = CardDefaults.cardColors(containerColor = if (message.fromSakura) Panel else Pink.copy(alpha = 0.18f)), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SelectionContainer(Modifier.weight(1f)) { Text(message.text, color = if (message.fromSakura) Ink else Pink, fontSize = 15.sp) }
                if (message.fromSakura) IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Leer respuesta", tint = Aqua) }
            }
        }
    }
}

@Composable
private fun MemoryPage(vm: SakuraViewModel, confirm: (String, () -> Unit) -> Unit) {
    var kind by remember { mutableStateOf("Preferencia") }
    var note by remember { mutableStateOf("") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Info("Memoria explícita y cifrada en este teléfono. No se guardan recuerdos automáticamente. Sakura recibe como contexto las últimas 12 notas; puedes revisar y borrar las 100 permitidas.") }
        item {
            Column {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Preferencia", "Proyecto", "Avance").forEach {
                        FilterChip(selected = kind == it, onClick = { kind = it }, label = { Text(it) }, modifier = Modifier.padding(end = 5.dp))
                    }
                }
                Field("Nota (máximo 500 caracteres)", note, { note = it.take(500) }, !vm.busy, singleLine = false)
                Button(enabled = !vm.busy && note.isNotBlank(), onClick = { vm.addMemory(kind, note) { note = "" } }) { Text("Guardar recuerdo") }
            }
        }
        items(vm.memories, key = { it.id }) { memory ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(memory.kind, color = Aqua, fontWeight = FontWeight.Bold)
                    SelectionContainer { Text(memory.text) }
                    TextButton(enabled = !vm.busy, onClick = { confirm("Borrar esta nota: ${memory.text}", { vm.deleteMemory(memory.id) }) }) { Text("Borrar nota") }
                }
            }
        }
        item { TextButton(enabled = !vm.busy && vm.memories.isNotEmpty(), onClick = { confirm("Borrar todos los recuerdos guardados. No se puede deshacer.", vm::clearMemories) }) { Text("Borrar toda la memoria") } }
    }
}

@Composable
private fun ModelsPage(vm: SakuraViewModel, choose: () -> Unit, confirm: (String, () -> Unit) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Info("Motor llama.cpp en CPU · Android 13+ ARM64. Qwen2.5 3B Q4_K_M es el candidato inicial para tu teléfono de 8 GB; prueba 1.5B si resulta lento o consume demasiada RAM. Archivo ≠ memoria necesaria. En Descargas tienes el catálogo con licencia, tamaño y hash. No se promete rapidez. Importación: 2.5 GB; contexto: 2048 tokens.") }
        item { Row { TextButton(onClick = { vm.page = "Descargas" }) { Text("Catálogo y descargas") }; TextButton(enabled = !vm.busy,onClick = vm::memoryStats) { Text("Medir RAM actual") } } }
        item { Button(enabled = !vm.busy, onClick = choose) { Text("Importar GGUF seleccionado · local") } }
        items(vm.models, key = { it.id }) { model ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(model.name, color = Pink, fontWeight = FontWeight.Bold)
                    Text("${model.bytes / (1024 * 1024)} MiB · ${if (vm.loadedModel?.id == model.id) "cargado" else "en almacenamiento privado"}")
                    Row {
                        TextButton(enabled = !vm.busy, onClick = { confirm("Cargar ${model.name}. Se liberará el modelo anterior y se borrará el chat abierto al completar la carga.", { vm.loadModel(model) }) }) { Text("Cargar") }
                        TextButton(enabled = !vm.busy, onClick = { confirm("Borrar ${model.name} de Sakura y liberarlo si está cargado. Tu archivo original no cambia.", { vm.deleteModel(model) }) }) { Text("Borrar archivo") }
                    }
                }
            }
        }
        item { TextButton(enabled = !vm.busy && vm.loadedModel != null, onClick = vm::unloadModel) { Text("Liberar modelo de RAM") } }
    }
}

private data class LaunchableApp(val label: String, val packageName: String, val activity: String)

@Composable
private fun ToolsPage(vm: SakuraViewModel, chooseCode: () -> Unit, chooseApk: () -> Unit,
    confirm: (String, () -> Unit) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Info("Estas herramientas se ejecutan solo con tus botones. Sakura no puede activar acciones desde una respuesta del modelo. No se ejecuta código ni se instalan APK.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !vm.busy, onClick = chooseCode) { Text("Seleccionar código · local") }
                Text("Python, Java, Kotlin, HTML y JavaScript: lectura de texto y ayuda mediante el modelo. No es un compilador ni un depurador ejecutable.", fontSize = 13.sp)
                Button(enabled = !vm.busy, onClick = chooseApk) { Text("Analizar APK seleccionada · local") }
                Button(enabled = !vm.busy, onClick = {
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    apps = context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
                        .filter { it.activityInfo.exported }.map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName, it.activityInfo.name) }
                        .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }.take(150)
                    vm.notice("Aplicaciones con actividad de inicio visibles para Sakura (máximo 150).")
                }) { Text("Mostrar aplicaciones que puedo abrir · local") }
            }
        }
        items(apps, key = { it.packageName }) { app ->
            OutlinedButton(onClick = { confirm("Abrir ${app.label} (${app.packageName}). Esa app tiene sus propias conexiones y permisos.", {
                try {
                    context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(ComponentName(app.packageName, app.activity)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    vm.notice("Se solicitó a Android abrir ${app.label}; no se controla su contenido.")
                } catch (_: Exception) { vm.notice("Android no permitió abrir esta aplicación.") }
            }) }) { Text(app.label) }
        }
        if (vm.toolReport.isNotBlank()) item { SelectionContainer { Text(vm.toolReport, fontFamily = FontFamily.Monospace, fontSize = 12.sp) } }
    }
}

@Composable
private fun GithubPage(vm: SakuraViewModel) {
    var owner by remember { mutableStateOf("Hisoka111373") }
    var repo by remember { mutableStateOf("sakura-agent") }
    var branch by remember { mutableStateOf("main") }
    var path by remember { mutableStateOf("README.md") }
    val source = vm.repoFile
    var replacement by remember(source) { mutableStateOf(source?.text ?: "") }
    var commit by remember { mutableStateOf("Sakura: cambio revisado") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Info("GitHub requiere Internet. Leer no escribe. Para repositorios privados y commits, configura un token limitado en Privacidad. Los cambios se envían solo tras revisar y confirmar el contenido completo; usa una rama de trabajo, no main, si quieres aislarlos.")
        Field("Propietario", owner, { owner = it }, !vm.busy)
        Field("Repositorio", repo, { repo = it }, !vm.busy)
        Field("Rama existente", branch, { branch = it }, !vm.busy)
        GithubExtras(vm,owner,repo,branch,path)
        Field("Ruta de archivo", path, { path = it }, !vm.busy)
        Button(enabled = !vm.busy && vm.internetAllowed, onClick = { vm.readTree(owner, repo, branch) }) { Text("Listar archivos · Internet") }
        Button(enabled = !vm.busy && vm.internetAllowed, onClick = { vm.readRepo(owner, repo, branch, path) }) { Text("Leer archivo · Internet") }
        if (vm.githubReport.isNotBlank()) SelectionContainer { Text(vm.githubReport, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        if (source != null) {
            Text("Leído: ${source.owner}/${source.repo} @ ${source.branch}\n${source.path}", color = Aqua)
            TextButton(enabled = !vm.busy, onClick = vm::useRepoInChat) { Text("Llevar al borrador para pedir sugerencias locales") }
            OutlinedTextField(value = replacement, onValueChange = { replacement = it }, enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 400.dp), label = { Text("Contenido propuesto (editable)") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            Field("Mensaje del commit", commit, { commit = it.take(200) }, !vm.busy)
            Button(enabled = !vm.busy && vm.internetAllowed && vm.hasToken && replacement != source.text,
                onClick = { vm.prepareEdit(replacement, commit) }) { Text("Revisar antes de escribir…") }
            OutlinedButton(enabled = !vm.busy && vm.internetAllowed && vm.hasToken,onClick = { vm.features.prepareDeleteFile(source,commit) }) { Text("Revisar borrado de este archivo…") }
        }
    }
}

@Composable
private fun PrivacyPage(vm: SakuraViewModel, onInternet: (Boolean) -> Unit) {
    var token by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Info("LOCAL: chat y planificación GGUF, recuerdos cifrados, lectura de código y análisis de APK. RED: GitHub, descargas, APIs de mensajes, fal.ai y voz si la requiere. Crear en nube envía únicamente el prompt que revisas, no el chat ni los recuerdos. Compartir con otras apps usa sus propios permisos y conexiones.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Autorizar funciones con Internet", Modifier.weight(1f))
            Switch(checked = vm.internetAllowed, enabled = !vm.busy || vm.internetAllowed, onCheckedChange = onInternet)
        }
        Text("Desactivado al iniciar cada sesión. Este interruptor controla las operaciones de Sakura, no es un firewall para Android ni para las apps abiertas.", fontSize = 12.sp)
        Text("Aprendizaje y personalidad", color = Pink, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            AgentPolicy.levels.forEach { level ->
                FilterChip(selected = vm.level == level, enabled = !vm.busy, onClick = { vm.preferences(level, vm.humor) }, label = { Text(level) }, modifier = Modifier.padding(end = 5.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Humor suave", Modifier.weight(1f))
            Switch(checked = vm.humor, enabled = !vm.busy, onCheckedChange = { vm.preferences(vm.level, it) })
        }
        Text("Adaptar explicaciones no reentrena el modelo. La voz usa un motor español instalado en Android, con ritmo y tono de Sakura; no incluye una voz clonada.", fontSize = 12.sp)
        Text("Credencial de GitHub", color = Pink, fontWeight = FontWeight.Bold)
        Text("${if (vm.hasToken) "Hay un token guardado y cifrado." else "No hay token guardado."} Usa un fine-grained token limitado al repositorio: Contents: read para leer o write para commits. No pegues tokens en el chat.", fontSize = 12.sp)
        OutlinedTextField(value = token, onValueChange = { token = it.take(512) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth(),
            label = { Text("Token (oculto)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Row {
            Button(enabled = !vm.busy && token.isNotBlank(), onClick = { vm.saveToken(token); token = "" }) { Text("Guardar cifrado") }
            TextButton(enabled = !vm.busy && vm.hasToken, onClick = { vm.saveToken(""); token = "" }) { Text("Borrar token") }
        }
        Text("El cifrado protege el almacenamiento, no un teléfono comprometido. Desinstalar borra datos privados; los archivos originales importados no se borran. Revocar acceso requiere revocar el token también en GitHub.", fontSize = 12.sp)
        ServiceCredentials(vm)
    }
}

@Composable
internal fun Info(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Text(text, Modifier.fillMaxWidth().padding(14.dp), color = Ink.copy(alpha = 0.86f), fontSize = 13.sp)
    }
}

@Composable
internal fun Field(label: String, value: String, change: (String) -> Unit, enabled: Boolean, singleLine: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = change, enabled = enabled, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = singleLine, maxLines = if (singleLine) 1 else 5)
}