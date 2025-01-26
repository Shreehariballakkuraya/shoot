package com.hari.shoot.ui.screens.viewer

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.view.View

class PdfView(context: Context) : View(context) {
    private var pdfRenderer: PdfRenderer? = null

    fun setPdfRenderer(renderer: PdfRenderer) {
        pdfRenderer = renderer
        // Add logic to render the PDF pages
    }

    // Override onDraw or other methods to handle rendering
} 