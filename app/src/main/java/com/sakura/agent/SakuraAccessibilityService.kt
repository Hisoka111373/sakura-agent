package com.sakura.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** No model, gestures, screenshots or event-driven reads/actions. Manual short search sessions only. */
class SakuraAccessibilityService : AccessibilityService() {
    companion object { var connected: SakuraAccessibilityService? = null; private set }
    private val handler=Handler(Looper.getMainLooper())
    private var overlay: LinearLayout? = null
    private var content: LinearLayout? = null
    private var target=""
    private var expires=0L
    private var initialQuery=""
    private val timeout=Runnable { stopSession() }
    private data class Candidate(val path: List<Int>, val label: String, val window: Int, val bounds: String, val editable: Boolean, val value: String)

    override fun onServiceConnected() {
        connected=this
        serviceInfo=serviceInfo.apply { eventTypes=0; packageNames=emptyArray(); feedbackType=AccessibilityServiceInfo.FEEDBACK_GENERIC }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Deliberately no screen reads or automation. */ }
    override fun onInterrupt() { stopSession() }
    override fun onDestroy() { stopSession(); if(connected===this) connected=null; super.onDestroy() }
    fun startSession(packageName: String, query: String) {
        require(packageName in ScreenPolicy.packages.values && NetworkPolicy.text(query,500))
        stopSession(); target=packageName; initialQuery=query; expires=SystemClock.elapsedRealtime()+180_000
        val panel=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFF1B172B.toInt()) }
        panel.addView(Button(this).apply { text="DETENER sesión de Sakura"; setOnClickListener { stopSession() } })
        val scroll=ScrollView(this)
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,12,16,12); setBackgroundColor(0xFF1B172B.toInt()) }
        scroll.addView(column); panel.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); overlay=panel; content=column
        val params=WindowManager.LayoutParams((resources.displayMetrics.widthPixels*0.92).toInt(),(resources.displayMetrics.density*260).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE,
            android.graphics.PixelFormat.TRANSLUCENT).apply { gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }
        try {
            getSystemService(WindowManager::class.java).addView(panel,params)
            handler.postDelayed(timeout,180_000); menu("Sesión de búsqueda: $target · máximo 3 minutos. Abre la app autorizada. No lee nada hasta pulsar Ver controles.")
        } catch(e:Exception) { stopSession(); throw e }
    }
    fun stopSession() {
        handler.removeCallbacks(timeout); target=""; expires=0L; initialQuery=""
        overlay?.let { try { getSystemService(WindowManager::class.java).removeView(it) } catch(_:Exception) {} }
        overlay=null; content=null
    }
    private fun text(value: String) { content?.addView(TextView(this).apply { text=value; setTextColor(0xFF73E6E0.toInt()); textSize=13f }) }
    private fun button(label: String, action: () -> Unit) { content?.addView(Button(this).apply { text=label; setOnClickListener { action() } }) }
    private fun reset(message: String) {
        content?.removeAllViews(); text(message)
    }
    private fun menu(message: String) { reset(message); button("Ver controles de búsqueda",::scan) }
    private fun root(): AccessibilityNodeInfo {
        check(target.isNotBlank() && SystemClock.elapsedRealtime()<expires) { "Sesión vencida." }
        val root=rootInActiveWindow ?: error("No hay pantalla accesible.")
        check(root.packageName?.toString()==target) { "Abre $target. No se lee ni actúa en otra app." }
        return root
    }
    private fun label(node: AccessibilityNodeInfo): String =
        listOfNotNull(node.contentDescription?.toString(),node.hintText?.toString(),node.viewIdResourceName,
            if(!node.isEditable) node.text?.toString() else null).joinToString(" ").take(300)
    private fun bounds(node: AccessibilityNodeInfo): String = Rect().also(node::getBoundsInScreen).toShortString()
    private fun scan() {
        try {
            val root=root(); val candidates=mutableListOf<Candidate>(); var visited=0
            fun visit(node: AccessibilityNodeInfo, path: List<Int>) {
                if(++visited>200 || path.size>12 || node.isPassword || !node.isVisibleToUser) return
                val label=label(node)
                val searchable=node.isEditable && ScreenPolicy.searchField(label)
                // Click labels use human-readable descriptions/text, not concatenated IDs.
                val clickLabel=(node.contentDescription?:node.text)?.toString().orEmpty()
                if(node.isEnabled && (searchable || node.isClickable && ScreenPolicy.click(clickLabel)))
                    candidates.add(Candidate(path,label,node.windowId,bounds(node),searchable,if(searchable) node.text?.toString().orEmpty().take(500) else ""))
                for(i in 0 until minOf(node.childCount,50)) node.getChild(i)?.let { visit(it,path+i) }
            }
            visit(root,emptyList()); reset("Solo controles de búsqueda permitidos. Se excluyen contraseñas, mensajes, pagos, permisos e instalación. Esta lista NO prueba compatibilidad con toda pantalla.")
            candidates.take(12).forEach { candidate -> button(candidate.label.take(90)) { choose(candidate) } }
            if(candidates.isEmpty()) text("No hay controles compatibles. Usa la app manualmente o la búsqueda por enlace.")
            button("Actualizar controles",::scan)
        } catch(e:Exception) { menu(e.message?:"Pantalla no disponible.") }
    }
    private fun choose(candidate: Candidate) {
        reset("REVISAR acción en $target\n${candidate.label}\nNo se ejecuta sin confirmar. Si cambia la pantalla, se rechaza.")
        if(candidate.editable) {
            text("Escribir reemplaza el campo por: $initialQuery\nBuscar envía el texto actual: ${candidate.value}")
            button("Confirmar escribir consulta") { execute(candidate,AccessibilityNodeInfo.ACTION_SET_TEXT,initialQuery) }
            button("Confirmar buscar con texto actual") { execute(candidate,AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id,null) }
        } else button("Confirmar pulsar control") { execute(candidate,AccessibilityNodeInfo.ACTION_CLICK,null) }
        button("Cancelar / volver",::scan)
    }
    private fun execute(candidate: Candidate, action: Int, value: String?) {
        try {
            var node=root()
            candidate.path.forEach { index -> node=node.getChild(index) ?: error("Pantalla cambió.")
                check(!node.isPassword) { "Control protegido." } }
            check(node.windowId==candidate.window && bounds(node)==candidate.bounds && label(node)==candidate.label && node.isVisibleToUser && node.isEnabled && !node.isPassword) { "Pantalla cambió. Vuelve a revisar." }
            check(node.packageName?.toString()==target && ScreenPolicy.safe(candidate.label,false)) { "Control no permitido." }
            if(action==AccessibilityNodeInfo.ACTION_CLICK) check(ScreenPolicy.click((node.contentDescription?:node.text)?.toString().orEmpty()))
            else {
                check(node.isEditable && ScreenPolicy.searchField(label(node)))
                check(node.text?.toString().orEmpty()==candidate.value) { "El texto cambió; revisa controles de nuevo." }
                if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id) require(NetworkPolicy.text(candidate.value,500)) { "Campo vacío o demasiado largo." }
            }
            val args=if(value!=null) Bundle().apply {
                require(NetworkPolicy.text(value,500)); putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)
            } else null
            // Require the platform to expose the chosen action, especially IME enter.
            check(node.actionList.any { it.id==action }) { "La app no ofrece esta acción accesible. Usa su teclado manualmente." }
            val accepted=node.performAction(action,args)
            menu(if(accepted) "Android aceptó una acción. Búsqueda/resultados NO verificados. No continúa sola." else "La app rechazó la acción. No se reintenta.")
        } catch(e:Exception) { menu(e.message?:"Acción rechazada.") }
    }
}