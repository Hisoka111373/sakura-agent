package com.sakura.agent

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri

/**
 * Creates a simple text PDF in a destination chosen with Android's SAF.
 *
 * No new third-party dependency: Android's own PdfDocument is used. Content is
 * always written to the URI the user picked; Sakura never guesses a path and
 * never overwrites silently.
 */
object PdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val LEADING = 16f
    private const val MAX_LINES_PER_PAGE = 45
    private const val MAX_CHARS_PER_LINE = 95

    /** Returns the number of pages written. */
    fun write(context: Context, destination: Uri, title: String, lines: List<String>): Int {
        require(title.isNotBlank() && title.length <= 160) { "Título de PDF inválido." }
        require(lines.size <= 20_000) { "El PDF supera 20.000 líneas." }
        val document = PdfDocument()
        val paint = Paint().apply {
            color = Color.rgb(0x30, 0x24, 0x3C)
            textSize = 11f
            isAntiAlias = true
        }
        val heading = Paint(paint).apply { textSize = 16f; isFakeBoldText = true }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        try {
            val clean = lines.map { it.replace('\t', ' ').take(MAX_CHARS_PER_LINE) }
            val pages = clean.chunked(MAX_LINES_PER_PAGE).ifEmpty { listOf(emptyList()) }
            pages.forEachIndexed { index, chunk ->
                pageNumber = index + 1
                val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val current = document.startPage(info)
                page = current
                val canvas: Canvas = current.canvas
                var y = MARGIN + 18f
                if (index == 0) {
                    canvas.drawText(title.take(80), MARGIN, y, heading)
                    y += 26f
                }
                chunk.forEach { line ->
                    canvas.drawText(line, MARGIN, y, paint)
                    y += LEADING
                }
                document.finishPage(current)
                page = null
            }
            val output = context.contentResolver.openOutputStream(destination)
                ?: error("Android no permitió escribir en el destino elegido.")
            output.use { document.writeTo(it) }
            return pageNumber
        } finally {
            if (page != null) runCatching { document.finishPage(page) }
            document.close()
        }
    }
}
