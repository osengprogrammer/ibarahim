package com.example.crashcourse.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.crashcourse.R
import com.example.crashcourse.db.CheckInRecord
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExportUtils {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun writePdfToUri(context: Context, uri: Uri, records: List<CheckInRecord>) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = PdfWriter(outputStream)
                val pdf = PdfDocument(writer)
                val document = Document(pdf)

                // --- 1. INSERT LOGO AZURA ---
                try {
                    // Pastikan file 'logo_azura.png' ada di res/drawable
                    val drawable = ContextCompat.getDrawable(context, R.drawable.logo_azura)
                    if (drawable is BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val imageData = ImageDataFactory.create(stream.toByteArray())
                        
                        val logo = Image(imageData)
                            .setWidth(80f)
                            .setFixedPosition(480f, 750f) 
                        document.add(logo)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // --- 2. HEADER ---
                document.add(
                    Paragraph("LAPORAN ABSENSI AZURATECH")
                        .setFontSize(20f)
                        .setBold()
                        .setFontColor(com.itextpdf.kernel.colors.DeviceRgb(33, 150, 243))
                )

                document.add(
                    Paragraph("Dicetak pada: ${LocalDateTime.now().format(dateTimeFormatter)}")
                        .setFontSize(10f)
                        .setItalic()
                        .setMarginBottom(20f)
                )

                // --- 3. TABEL ---
                val columnWidths = floatArrayOf(4f, 2f, 2f, 2f, 2f)
                val table = Table(UnitValue.createPercentArray(columnWidths))
                table.useAllAvailableWidth()

                val headers = listOf("Nama Murid", "Kelas", "Grade", "Status", "Waktu")
                headers.forEach { header ->
                    table.addHeaderCell(
                        Paragraph(header)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)
                            .setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(240, 240, 240))
                    )
                }

                records.forEach { record ->
                    table.addCell(Paragraph(record.name).setFontSize(10f))
                    table.addCell(Paragraph(record.className ?: "-").setTextAlignment(TextAlignment.CENTER).setFontSize(10f))
                    table.addCell(Paragraph(record.gradeName ?: "-").setTextAlignment(TextAlignment.CENTER).setFontSize(10f))
                    table.addCell(Paragraph(record.status).setTextAlignment(TextAlignment.CENTER).setFontSize(10f))
                    table.addCell(Paragraph(record.timestamp.format(timeOnlyFormatter)).setTextAlignment(TextAlignment.CENTER).setFontSize(10f))
                }

                document.add(table)

                // --- 4. FOOTER ---
                document.add(
                    Paragraph("\n\nAzuraTech Ecosystem - Intelligent School Management System")
                        .setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(com.itextpdf.kernel.colors.DeviceGray.GRAY)
                )

                document.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e 
        }
    }
}