package com.example.crashcourse.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.crashcourse.R
import com.example.crashcourse.db.CheckInRecord
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExportUtils {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
    // Format super ringkas: "04/02 07:00"
    private val compactDateFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm") 

    fun writePdfToUri(context: Context, uri: Uri, records: List<CheckInRecord>) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = PdfWriter(outputStream)
                val pdf = PdfDocument(writer)
                
                // Gunakan Landscape agar muat banyak timestamp ke samping
                pdf.setDefaultPageSize(PageSize.A4.rotate())
                
                val document = Document(pdf)

                // --- 1. PRE-PROCESSING DATA ---
                // Kelompokkan berdasarkan Nama
                val groupedByStudent = records.groupBy { it.name }
                    .toSortedMap() // Urutkan abjad nama A-Z

                // --- 2. HEADER & LOGO ---
                try {
                    val drawable = ContextCompat.getDrawable(context, R.drawable.logo_azura)
                    if (drawable is BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val imageData = ImageDataFactory.create(stream.toByteArray())
                        val logo = Image(imageData).setWidth(60f).setFixedPosition(750f, 500f)
                        document.add(logo)
                    }
                } catch (e: Exception) { e.printStackTrace() }

                document.add(
                    Paragraph("LAPORAN RINGKAS (HEMAT KERTAS)")
                        .setFontSize(16f).setBold()
                        .setFontColor(DeviceRgb(33, 150, 243))
                )
                val rangeInfo = if(records.isNotEmpty()) {
                    val first = records.minOf { it.timestamp }.format(DateTimeFormatter.ofPattern("dd MMM"))
                    val last = records.maxOf { it.timestamp }.format(DateTimeFormatter.ofPattern("dd MMM"))
                    "Periode: $first - $last"
                } else "Data Kosong"
                
                document.add(Paragraph(rangeInfo).setFontSize(10f).setItalic())
                document.add(Paragraph("\n"))

                // --- 3. TABEL COMPACT ---
                // Kolom: [No, Nama/Kelas, Jml, Riwayat Lengkap]
                // Lebar: 5%, 20%, 5%, 70%
                val columnWidths = floatArrayOf(0.5f, 2f, 0.5f, 7f)
                val table = Table(UnitValue.createPercentArray(columnWidths))
                table.useAllAvailableWidth()

                // HEADER
                val headers = listOf("No", "Nama & Kelas", "Jml", "Riwayat Kehadiran (Tgl & Jam)")
                headers.forEach { title ->
                    table.addHeaderCell(
                        Paragraph(title)
                            .setBold()
                            .setFontSize(10f)
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                }

                // ISI DATA
                var number = 1
                groupedByStudent.forEach { (name, studentRecords) ->
                    // Kolom 1: Nomor
                    table.addCell(Paragraph(number.toString()).setTextAlignment(TextAlignment.CENTER).setFontSize(9f))
                    
                    // Kolom 2: Nama & Kelas (Digabung biar hemat kolom)
                    val className = studentRecords.firstOrNull()?.className ?: "-"
                    table.addCell(
                        Paragraph("$name\n($className)")
                            .setFontSize(9f)
                            .setBold()
                    )

                    // Kolom 3: Jumlah Kehadiran
                    table.addCell(
                        Paragraph(studentRecords.size.toString())
                            .setTextAlignment(TextAlignment.CENTER)
                            .setBold()
                            .setFontSize(9f)
                    )

                    // Kolom 4: RIWAYAT PADAT (Inti Penghematan Kertas)
                    // Hasil: "01/02 07:00, 02/02 07:15, 03/02 07:10..."
                    val historyString = studentRecords
                        .sortedBy { it.timestamp }
                        .joinToString(",  ") { it.timestamp.format(compactDateFormatter) }

                    table.addCell(
                        Paragraph(historyString)
                            .setFontSize(9f)
                            .setTextAlignment(TextAlignment.LEFT)
                    )

                    number++
                }

                document.add(table)
                
                // Footer kecil
                document.add(Paragraph("\nGenerated by AzuraTech Ecosystem").setFontSize(7f).setItalic().setTextAlignment(TextAlignment.RIGHT))
                
                document.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}