package com.sakura.agent

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DownloadsPage(vm: SakuraViewModel, export: (DownloadedFile) -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("Imagen") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Info("Catálogo verificado por tamaño y SHA-256, sin pesos incluidos. Chat: GGUF para llama.cpp. Imagen/video: checkpoints para evaluación externa, NO generación local en Sakura. Consulta licencia antes de descargar; 8 GB de RAM no garantiza que puedan ejecutarse.") }
        item { Text("Modelos para chat, imagen y video",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold) }
        items(ModelCatalog.entries,key={it.url}) { model ->
            Card { Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(model.title,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
                Text("${model.category} · ${"%.2f".format(model.bytes/1e9)} GB · ${model.license}",fontSize=12.sp)
                Text(model.warning,fontSize=13.sp)
                SelectionContainer { Text(model.card,fontSize=12.sp) }
                // Opening the license page is an explicit user gesture, not model-triggered browsing.
                TextButton(enabled=!vm.busy,onClick={
                    try { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(model.card))); vm.notice("Se abrió la tarjeta externa; usa sus condiciones y permisos de red.") }
                    catch (_:Exception) { vm.notice("No hay navegador disponible.") }
                }) { Text("Consultar tarjeta y licencia") }
                Button(enabled=!vm.busy && vm.internetAllowed,onClick={vm.features.prepareDownload(model.url,"Modelo",model)}) { Text("Revisar descarga del modelo…") }
            } }
        }
        item { HorizontalDivider() }
        item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Descargar imagen o video por URL",fontWeight=FontWeight.Bold)
            Info("Solo enlaces HTTPS directos a archivos públicos que tengas permiso de descargar. Sin buscar automáticamente, entrar a cuentas, extraer de YouTube, eludir DRM ni descargar listas HLS. Máximo 512 MiB por archivo; datos privados en Sakura hasta que exportes.")
            Row { listOf("Imagen","Video").forEach { choice -> FilterChip(kind==choice,{kind=choice},label={Text(choice)},modifier=Modifier.padding(end=6.dp)) } }
            Field("URL directa HTTPS",url,{url=it.take(4096)},!vm.busy)
            Button(enabled=!vm.busy && vm.internetAllowed && url.isNotBlank(),onClick={vm.features.prepareDownload(url.trim(),kind)}) { Text("Revisar descarga…") }
        } }
        if (vm.features.downloading) item { Column { Text(vm.features.transfer); OutlinedButton(onClick=vm.features::cancelDownload) { Text("Cancelar descarga") } } }
        item { Text("Archivos privados descargados",fontWeight=FontWeight.Bold) }
        items(vm.features.files,key={it.id}) { file ->
            Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(file.name); Text("${file.bytes/(1024*1024)} MiB",fontSize=12.sp)
                Row { TextButton(enabled=!vm.busy,onClick={export(file)}) { Text("Exportar copia…") }
                    TextButton(enabled=!vm.busy,onClick={vm.features.prepareDelete(file)}) { Text("Borrar…") } }
            } }
        }
        item { FeatureReport(vm) }
    }
}

@Composable
internal fun MessagesPage(vm: SakuraViewModel, confirm: (String,()->Unit)->Unit) {
    val context = LocalContext.current
    var service by remember { mutableStateOf("WhatsApp") }
    var api by remember { mutableStateOf(false) }
    var recipient by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Info("Dos vías: preparar un mensaje en la app para que tú elijas contacto y pulses Enviar, o enviar por API tras confirmar destinatario y texto. No se automatiza la interfaz ni se leen tus conversaciones.")
        Row { listOf("WhatsApp","Telegram").forEach { choice -> FilterChip(service==choice,{service=choice;recipient=""},label={Text(choice)},modifier=Modifier.padding(end=6.dp)) } }
        Row { FilterChip(!api,{api=false},label={Text("Abrir app")}); Spacer(Modifier.width(6.dp)); FilterChip(api,{api=true},label={Text("API oficial")}) }
        if (api) {
            Info(if(service=="Telegram") "Bot API: el bot no representa tu cuenta personal y no puede escribir a usuarios que no lo hayan iniciado. Configura token en Privacidad." else "WhatsApp Cloud API: requiere cuenta Business, número registrado, token Meta y Phone Number ID. Solo texto dentro de ventana de servicio de 24 h; no incluye plantillas ni webhooks. Configura en Privacidad.")
            Field(if(service=="Telegram") "Chat ID o @canal permitido" else "Número internacional sin + ni espacios",recipient,{recipient=it.take(40)},!vm.busy)
        }
        Field("Mensaje completo (máximo 4096 caracteres)",text,{text=it.take(4096)},!vm.busy,false)
        if (api) Button(enabled=!vm.busy && vm.internetAllowed && text.isNotBlank() && recipient.isNotBlank(),onClick={vm.features.prepareMessage(service,recipient.trim(),text)}) { Text("Revisar envío real…") }
        else Button(enabled=!vm.busy && text.isNotBlank(),onClick={
            val chosen = service; val message = text
            confirm("Compartir este borrador con $chosen:\n$message\n\nTú elegirás destinatario y pulsarás Enviar en esa app. Tiene sus propios permisos y conexiones. Sakura no puede verificar si lo enviaste.") {
                try {
                    context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,message)
                        .setPackage(if(chosen=="WhatsApp") "com.whatsapp" else "org.telegram.messenger"))
                    vm.notice("Borrador entregado a $chosen. Envío NO verificado; completa la acción allí.")
                } catch (_:Exception) { vm.notice("No se pudo abrir $chosen. Instala la app oficial o usa la API configurada.") }
            }
        }) { Text("Revisar borrador para abrir app…") }
        FeatureReport(vm)
    }
}

@Composable
internal fun CreateMediaPage(vm: SakuraViewModel, confirm: (String,()->Unit)->Unit) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf("Imagen") }
    var prompt by remember { mutableStateOf("") }
    var recovery by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Info("Generación nube opcional mediante fal.ai. Solo se envía el prompt revisado, no chat, recuerdos ni archivos. Debes configurar tu clave y revisar saldo/tarifa. No hay tope monetario en Sakura ni garantía de rapidez; los resultados pueden tener URLs públicas.")
        Row { listOf("Imagen","Video").forEach { choice -> FilterChip(kind==choice,{kind=choice},label={Text(choice)},modifier=Modifier.padding(end=6.dp)) } }
        Text("Modelo nube: ${CloudClient.endpoints[kind]}",fontSize=12.sp)
        TextButton(onClick={confirm("Abrir fal.ai para revisar el modelo, licencia y tarifa. No genera ni gasta desde Sakura.") {
            try { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://fal.ai/models/${CloudClient.endpoints[kind]}"))) }
            catch (_:Exception) { vm.notice("No hay navegador disponible.") }
        }}) { Text("Revisar tarifa y licencia en fal.ai") }
        Field("Describe la escena (máximo 2000 caracteres)",prompt,{prompt=it.take(2000)},!vm.busy,false)
        Button(enabled=!vm.busy && vm.internetAllowed && prompt.isNotBlank(),onClick={vm.features.prepareCloud(kind,prompt)}) { Text("Revisar generación con posible coste…") }
        vm.features.job?.let { job ->
            SelectionContainer { Text("Último trabajo ${job.kind}\nID: ${job.id}",fontFamily=FontFamily.Monospace,fontSize=12.sp) }
            Row { TextButton(enabled=!vm.busy && vm.internetAllowed,onClick=vm.features::checkCloud) { Text("Consultar sin reenviar") }
                TextButton(enabled=!vm.busy && vm.internetAllowed,onClick=vm.features::prepareCancelCloud) { Text("Cancelar en proveedor…") } }
            vm.features.mediaUrls.forEach { url ->
                SelectionContainer { Text(url,fontSize=12.sp) }
                OutlinedButton(enabled=!vm.busy && vm.internetAllowed,onClick={vm.features.prepareDownload(url,job.kind)}) { Text("Revisar descarga del resultado…") }
            }
        }
        Field("Recuperar request_id existente",recovery,{recovery=it.take(100)},!vm.busy)
        TextButton(enabled=!vm.busy && recovery.isNotBlank(),onClick={vm.features.recoverCloud(kind,recovery)}) { Text("Preparar consulta por ID (no genera)") }
        Info("Generación local visual: no instalada. Descargar checkpoints de imagen/video no activa un motor. En Descargas está el catálogo para evaluación externa; un motor de difusión ARM64, sus componentes y pruebas de memoria/rendimiento son una integración posterior, no una función simulada.")
        TextButton(onClick={vm.page="Descargas"}) { Text("Modelos visuales para descargar") }
        FeatureReport(vm)
    }
}

@Composable
internal fun PlanPage(vm: SakuraViewModel) {
    var goal by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Info("Entender → proponer plan local → revisar cada acción → confirmar una sola acción → comprobar evidencia. El modelo no recibe credenciales ni ejecuta herramientas. Máximo 6 pasos permitidos; sin ejecución encadenada ni reintentos automáticos. Verifica destinatarios, URLs y datos: un modelo puede equivocarse.")
        Field("Objetivo (sin secretos)",goal,{goal=it.take(2000)},!vm.busy,false)
        Button(enabled=!vm.busy && goal.isNotBlank(),onClick={vm.proposePlan(goal)}) { Text("Proponer plan con GGUF local") }
        if (vm.generating) TextButton(onClick=vm::cancelGeneration) { Text("Detener planificación") }
        OutlinedTextField(value=vm.features.planJson,onValueChange={vm.features.planJson=it.take(24*1024)},enabled=!vm.busy,
            label={Text("Plan JSON editable")},modifier=Modifier.fillMaxWidth().heightIn(min=150.dp,max=320.dp),textStyle=MaterialTheme.typography.bodySmall.copy(fontFamily=FontFamily.Monospace))
        TextButton(enabled=!vm.busy && vm.features.planJson.isNotBlank(),onClick={vm.features.receivePlan(vm.features.planJson)}) { Text("Validar cambios del plan (no ejecuta)") }
        vm.features.plan.forEachIndexed { index,step ->
            Card { Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("${index+1}. ${step.action}",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
                Text(step.detail)
                SelectionContainer { Text(step.args.entries.joinToString("\n") { "${it.key}: ${it.value}" },fontSize=12.sp) }
                vm.features.evidence[index]?.let { SelectionContainer { Text("Evidencia recibida (no ejecución de pasos posteriores):\n$it",fontSize=12.sp) } }
                OutlinedButton(enabled=!vm.busy,onClick={vm.features.prepareStep(index)}) { Text(if(step.action=="REVIEW") "Llevar revisión al borrador" else "Revisar esta acción…") }
            } }
        }
        FeatureReport(vm)
    }
}

@Composable
internal fun ServiceCredentials(vm: SakuraViewModel) {
    var key by remember { mutableStateOf("telegram_token") }
    var value by remember { mutableStateOf("") }
    Text("Credenciales de servicios",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
    Info("Solo se guardan si las introduces tú. Cifradas con Keystore; no se incluyen en código, planes, prompts ni descargas. Telegram y Meta envían mensajes al proveedor. fal.ai factura en tu cuenta. Configuración guardada NO significa conexión probada.")
    val labels = mapOf("telegram_token" to "Token bot Telegram", "whatsapp_token" to "Token Meta WhatsApp", "whatsapp_phone_id" to "Phone Number ID Meta", "fal_key" to "Clave fal.ai")
    Row(Modifier.horizontalScroll(rememberScrollState())) { labels.forEach { (id,label) ->
        FilterChip(key==id,{key=id;value=""},label={Text(label)},modifier=Modifier.padding(end=6.dp))
    } }
    Text(if(key in vm.features.configured) "Valor guardado y cifrado; no se muestra." else "Sin configurar.",fontSize=12.sp)
    OutlinedTextField(value=value,onValueChange={value=it.take(512)},enabled=!vm.busy,label={Text(labels.getValue(key))},singleLine=true,
        visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
    Row { Button(enabled=!vm.busy && value.isNotBlank(),onClick={vm.features.saveCredential(key,value);value=""}) { Text("Guardar cifrado") }
        TextButton(enabled=!vm.busy && key in vm.features.configured,onClick={vm.features.saveCredential(key,"");value=""}) { Text("Borrar local") } }
    Text("Borrar localmente no revoca el acceso en el proveedor. Limita permisos y cuota en cada servicio; no uses tokens de administración innecesarios.",fontSize=12.sp)
}

@Composable
internal fun GithubExtras(vm: SakuraViewModel, owner: String, repo: String, branch: String, path: String) {
    var expanded by remember { mutableStateOf(false) }
    var newBranch by remember { mutableStateOf("feature/sakura-cambios") }
    var content by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Sakura: archivo nuevo revisado") }
    var base by remember { mutableStateOf("main") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Ocultar herramientas de repositorio" else "Ramas, commits, archivos nuevos y pull requests") }
    if (expanded) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Info("No es toda la API de GitHub: incluye operaciones habituales de código. No borra repositorios, cambia permisos, administra secretos ni mezcla PR automáticamente. Contents: write para commits; Pull requests: write para crear PR; Workflows: write para modificar workflows.")
        Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("Ramas","Commits","Pull requests").forEach { kind ->
            TextButton(enabled=!vm.busy && vm.internetAllowed,onClick={vm.features.listGithub(owner,repo,branch,kind)}) { Text("Listar $kind") }
        } }
        Field("Nueva rama (la rama principal del formulario será la base)",newBranch,{newBranch=it.take(200)},!vm.busy)
        OutlinedButton(enabled=!vm.busy && vm.internetAllowed && vm.hasToken,onClick={vm.features.prepareBranch(owner.trim(),repo.trim(),branch.trim(),newBranch.trim())}) { Text("Leer SHA base y revisar creación…") }
        Text("Archivo NUEVO en la ruta indicada: $path",fontWeight=FontWeight.Bold)
        Field("Contenido UTF-8 nuevo (≤64 KiB)",content,{content=it.take(64*1024)},!vm.busy,false)
        Field("Mensaje del commit de creación",message,{message=it.take(200)},!vm.busy)
        OutlinedButton(enabled=!vm.busy && vm.internetAllowed && vm.hasToken,onClick={vm.features.prepareCreateFile(owner.trim(),repo.trim(),branch.trim(),path.trim(),content,message.trim())}) { Text("Revisar creación de archivo…") }
        Text("Pull request: rama del formulario → base",fontWeight=FontWeight.Bold)
        Field("Rama destino del PR",base,{base=it.take(200)},!vm.busy)
        Field("Título PR",title,{title=it.take(200)},!vm.busy)
        Field("Descripción PR",body,{body=it.take(10000)},!vm.busy,false)
        OutlinedButton(enabled=!vm.busy && vm.internetAllowed && vm.hasToken && title.isNotBlank(),onClick={vm.features.preparePr(owner.trim(),repo.trim(),branch.trim(),base.trim(),title,body)}) { Text("Revisar creación de pull request…") }
        FeatureReport(vm)
        HorizontalDivider()
    }
}

@Composable
private fun FeatureReport(vm: SakuraViewModel) {
    if(vm.features.report.isNotBlank()) SelectionContainer { Text(vm.features.report,fontFamily=FontFamily.Monospace,fontSize=12.sp) }
}