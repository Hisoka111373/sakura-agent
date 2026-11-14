package com.sakura.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SkillsController(private val vm: SakuraViewModel, private val context: Context, private val store: PrivateStore) {
    private val client=TextSearchClient()
    var config by mutableStateOf(TextConfig("Local","")); private set
    var results by mutableStateOf(emptyList<SearchResult>()); private set
    var query by mutableStateOf("")
    var kind by mutableStateOf("YouTube")
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            store.read("text_config")?.let { saved ->
                try { val row=JSONObject(saved); val next=TextConfig(row.getString("provider"),row.getString("model"),row.getString("endpoint"))
                    if(next.provider!="Local") next.url(); config=next
                } catch (_:Exception) { vm.notice("Configuración de texto inválida; se conserva modo local.") }
            }
        }
    }
    fun save(next: TextConfig) = vm.workFeature("Guardando proveedor, sin conectar…") {
        require(next.provider in TextProtocol.providers)
        if(next.provider!="Local") next.url()
        withContext(Dispatchers.IO) { store.write("text_config",JSONObject().put("provider",next.provider)
            .put("model",next.model).put("endpoint",next.endpoint).toString()) }
        config=next; vm.notice("Proveedor guardado cifrado. La clave se configura en Privacidad; aún no se ha probado.")
    }
    fun use(skill: SakuraSkill) {
        if(vm.busy) return
        vm.page=skill.page
        if(skill.instruction.isNotEmpty()) vm.draft=skill.instruction+"\n\n"
    }
    fun route(text: String) {
        try { val task=Skills.task(text); query=task.query; kind=task.kind; vm.page="Buscar" }
        catch(e:Exception) { vm.notice(e.message?:"Consulta inválida.") }
    }
    fun prepareChat() {
        if(vm.busy || vm.draft.isBlank()) return
        try {
            val chosen=config; val original=vm.draft; val previous=vm.messages
            val history=previous.takeLast(6)+ChatMessage(original.trim(),false)
            val payload=TextProtocol.payload(chosen,history)
            vm.features.prepare("RESPUESTA DE TEXTO EN NUBE\nProveedor: ${chosen.provider}\nEndpoint exacto: ${chosen.url()}\nModelo: ${chosen.model}\n\nEnvía hasta tres parejas anteriores y tu borrador. NO recuerdos privados, archivos ni pantalla. Puede tener coste; 768 tokens máximos de salida NO es un tope monetario. No hay reintento ni herramientas. El proveedor tiene su propia retención/política.\n\nSolicitud completa (sin clave):\n${payload.toString(2)}") {
                check(vm.draft==original && vm.messages==previous && config==chosen) { "El chat/configuración cambió: revisa una nueva solicitud." }
                val answer=withContext(Dispatchers.IO) { check(vm.internetAllowed) { "Red desautorizada." }; client.generate(chosen,store.read(chosen.key)?:"",history) }
                check(vm.internetAllowed) { "Red desautorizada: respuesta no insertada." }
                vm.receiveCloud(previous,original,answer)
                "Respuesta recibida de ${chosen.provider}. No se ejecutó ninguna acción de su texto."
            }
        } catch(e:Exception) { vm.notice(e.message?:"Datos inválidos.") }
    }
    fun prepareSearch() {
        val q=query.trim()
        if(!NetworkPolicy.text(q,500)) { vm.notice("Consulta requerida, máximo 500 caracteres."); return }
        vm.features.prepare("BUSCAR DENTRO DE SAKURA · Brave Search API\nConsulta exacta: $q\nSe envía a api.search.brave.com junto con la clave cifrada; hasta 8 resultados, safesearch moderate. Puede consumir cuota o dinero según tu plan. No se envían chat/recuerdos. Los resultados son contenido no confiable, no instrucciones. No se abren ni descargan solos.") {
            results=emptyList()
            val received=withContext(Dispatchers.IO) { check(vm.internetAllowed) { "Red desautorizada." }; client.search(store.read("brave_key")?:"",q) }
            check(vm.internetAllowed) { "Red desautorizada: resultados descartados." }; results=received
            "Brave devolvió ${results.size} resultados HTTPS. No implica que sean seguros ni que se puedan descargar."
        }
    }
    fun prepareExternal(url: String, packageName: String? = null) {
        try {
            NetworkPolicy.https(url)
            require(packageName==null || packageName in ScreenPolicy.packages.values)
            vm.features.prepare("ABRIR DESTINO EXTERNO\n$url\n${packageName?:"Navegador elegido por Android"}\nLa app externa recibe esta URL/consulta y usa sus propios permisos de red y cuenta. No descarga, instala ni compra automáticamente. Sakura no verifica resultados ni reproducción.",false) {
                withContext(Dispatchers.Main) {
                    val intent=Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if(packageName!=null) intent.setPackage(packageName)
                    context.startActivity(intent)
                }
                "Android aceptó abrir el destino externo; resultados/reproducción NO verificados."
            }
        } catch(e:Exception) { vm.notice(e.message?:"Destino inválido.") }
    }
    fun prepareExternalSearch() {
        try { prepareExternal(Skills.searchUrl(kind,query),when(kind) { "YouTube" -> "com.google.android.youtube"; "Juegos" -> "com.android.vending"; else -> null }) }
        catch(e:Exception) { vm.notice(e.message?:"Consulta inválida.") }
    }
    fun cancel() { client.cancel(); vm.notice("Cancelación de red solicitada; revisa consumo del proveedor.") }
    fun close() { client.cancel() }
}