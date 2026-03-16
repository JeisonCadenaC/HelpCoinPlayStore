package com.example.finance_code.ui.home

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

data class MovimientoExtraido(
    val fecha: String,
    val descripcion: String,
    val monto: Double,
    val esIngreso: Boolean
)

class ExtractoBancarioHelper(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun extraerTextoDePDF(uri: Uri, password: String = ""): Pair<Boolean, String?> {
        var document: PDDocument? = null
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            document = PDDocument.load(inputStream, password)
            val stripper = PDFTextStripper()
            val texto = stripper.getText(document)
            Pair(false, texto)
        } catch (e: Exception) {
            val errorMsg = e.message?.lowercase() ?: ""
            if (errorMsg.contains("password") || errorMsg.contains("encrypted") || errorMsg.contains("bad user password")) {
                Pair(true, null)
            } else {
                Pair(false, null)
            }
        } finally {
            try {
                document?.close()
                inputStream?.close()
            } catch (e: Exception) {}
        }
    }

    fun analizarExtractoBancolombia(textoOriginal: String): List<MovimientoExtraido> {
        val movimientos = mutableListOf<MovimientoExtraido>()

        var year = "2024"
        val yearRegex = Regex("""DESDE:\s*(\d{4})""")
        val yearMatch = yearRegex.find(textoOriginal)
        if (yearMatch != null) {
            year = yearMatch.groupValues[1]
        }

        val regex = Regex("""(\d{1,2}/\d{1,2})\s+(.+?)\s+([-]?\d{1,3}(?:,\d{3})*\.\d{2})""")

        val lineas = textoOriginal.split("\n", "\r")

        for (linea in lineas) {
            val match = regex.find(linea.trim())
            if (match != null) {
                var fechaStr = match.groupValues[1]
                val partesFecha = fechaStr.split("/")
                if (partesFecha.size == 2) {
                    val dia = partesFecha[0].padStart(2, '0')
                    val mes = partesFecha[1].padStart(2, '0')
                    fechaStr = "$dia/$mes/$year"
                }

                val descripcion = match.groupValues[2].trim()
                if (descripcion.isEmpty()) continue

                val montoString = match.groupValues[3].replace(",", "")
                val monto = montoString.toDoubleOrNull() ?: continue
                val esIngreso = monto > 0
                val montoAbsoluto = kotlin.math.abs(monto)

                movimientos.add(
                    MovimientoExtraido(
                        fecha = fechaStr,
                        descripcion = descripcion,
                        monto = montoAbsoluto,
                        esIngreso = esIngreso
                    )
                )
            }
        }
        return movimientos
    }
}