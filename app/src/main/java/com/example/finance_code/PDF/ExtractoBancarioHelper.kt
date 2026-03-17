package com.example.finance_code.PDF

import android.content.Context
import android.net.Uri
import com.example.finance_code.ui.transaction.CategorySuggester
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.Locale

data class MovimientoExtraido(
    val fecha: String,
    val descripcion: String,
    val monto: Double,
    val esIngreso: Boolean,
    val categoria: String,
    val categoriaId: Long,
    val banco: String
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
            stripper.sortByPosition = true
            val texto = stripper.getText(document)

            Pair(false, texto)
        } catch (e: Exception) {
            val errorMsg = e.message?.lowercase(Locale.getDefault()) ?: ""
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

    fun analizarExtracto(textoOriginal: String): List<MovimientoExtraido>? {
        val isNequi = textoOriginal.contains("nequi.com.co", ignoreCase = true) ||
                textoOriginal.contains("depósito de bajo monto", ignoreCase = true)

        val isBancolombia = textoOriginal.contains("ESTADO DE CUENTA", ignoreCase = true) &&
                (textoOriginal.contains("CUENTA DE AHORROS", ignoreCase = true) ||
                        textoOriginal.contains("Bancolombia", ignoreCase = true))

        return when {
            isNequi -> analizarExtractoNequi(textoOriginal)
            isBancolombia -> analizarExtractoBancolombia(textoOriginal)
            textoOriginal.take(500).contains("Nequi", ignoreCase = true) -> analizarExtractoNequi(textoOriginal)
            textoOriginal.take(500).contains("Bancolombia", ignoreCase = true) -> analizarExtractoBancolombia(textoOriginal)
            else -> null
        }
    }

    private fun analizarExtractoNequi(textoOriginal: String): List<MovimientoExtraido> {
        val movimientos = mutableListOf<MovimientoExtraido>()
        val lineas = textoOriginal.split('\n', '\r').filter { it.isNotBlank() }.map { it.trim() }
        val lineasAgrupadas = mutableListOf<String>()
        var lineaActual = ""

        val dateStartRegex = Regex("""^(\d{2}/\d{2}/\d{4})\b""")

        for (linea in lineas) {
            if (dateStartRegex.containsMatchIn(linea)) {
                if (lineaActual.isNotEmpty()) {
                    lineasAgrupadas.add(lineaActual)
                }
                lineaActual = linea
            } else {
                if (!linea.lowercase(Locale.getDefault()).contains("nequi") &&
                    !linea.lowercase(Locale.getDefault()).contains("saldo") &&
                    !linea.contains("www.nequi.com.co")) {
                    if (lineaActual.isNotEmpty()) {
                        lineaActual += " $linea"
                    }
                }
            }
        }
        if (lineaActual.isNotEmpty()) {
            lineasAgrupadas.add(lineaActual)
        }

        val amountRegex = Regex("""(?:^|\s|\$)([-]?\d{1,3}(?:,\d{3})*\.\d{2}|[-]?\d*\.\d{2})(?:\s|$)""")

        for (linea in lineasAgrupadas) {
            val match = dateStartRegex.find(linea)
            if (match != null) {
                val fechaStr = match.groupValues[1]
                val restoLinea = linea.substring(match.range.last + 1).trim()
                val amountMatches = amountRegex.findAll(" $restoLinea ").toList()

                if (amountMatches.isNotEmpty()) {
                    val montoStr = amountMatches.first().groupValues[1]
                    var montoStringClean = montoStr.replace(",", "")

                    if (montoStringClean.startsWith(".")) montoStringClean = "0$montoStringClean"
                    if (montoStringClean.startsWith("-.")) montoStringClean = "-0." + montoStringClean.substring(2)

                    val monto = montoStringClean.toDoubleOrNull() ?: continue
                    if (monto == 0.0) continue

                    var descripcion = restoLinea
                    for (am in amountMatches) {
                        descripcion = descripcion.replace(am.value.trim(), "")
                    }
                    descripcion = descripcion.replace("$", "")
                    descripcion = descripcion.replace(Regex("""\s{2,}"""), " ").trim()

                    if (descripcion.isEmpty()) descripcion = "Movimiento Nequi"

                    val esIngreso = monto > 0
                    val montoAbsoluto = kotlin.math.abs(monto)

                    val catNombre = CategorySuggester.suggestCategory(descripcion) ?: if (esIngreso) "Salario e Ingresos" else "Otros"
                    val catId = CategorySuggester.getCategoryIdByName(catNombre)

                    movimientos.add(
                        MovimientoExtraido(
                            fecha = fechaStr,
                            descripcion = descripcion,
                            monto = montoAbsoluto,
                            esIngreso = esIngreso,
                            categoria = catNombre,
                            categoriaId = catId,
                            banco = "Nequi"
                        )
                    )
                }
            }
        }
        return movimientos
    }

    private fun analizarExtractoBancolombia(textoOriginal: String): List<MovimientoExtraido> {
        val movimientos = mutableListOf<MovimientoExtraido>()
        var year = "2024"
        val yearRegex = Regex("""DESDE:\s*(\d{4})""")
        val yearMatch = yearRegex.find(textoOriginal)
        if (yearMatch != null) {
            year = yearMatch.groupValues[1]
        }

        val lineas = textoOriginal.split('\n', '\r').filter { it.isNotBlank() }.map { it.trim() }
        val lineasAgrupadas = mutableListOf<String>()
        var lineaActual = ""

        val dateStartRegex = Regex("""^(\d{1,2}/\d{1,2})\b""")

        for (linea in lineas) {
            if (dateStartRegex.containsMatchIn(linea)) {
                if (lineaActual.isNotEmpty()) {
                    lineasAgrupadas.add(lineaActual)
                }
                lineaActual = linea
            } else {
                if (linea.contains("Bancolombia") || linea.contains("ESTADO DE CUENTA") ||
                    linea.contains("CUENTA DE AHORROS") || linea.startsWith("PÁGINA") ||
                    linea.contains("SUCURSAL") || linea.contains("SALDO") || linea.contains("VIGILAD")) {
                    continue
                }
                if (lineaActual.isNotEmpty()) {
                    lineaActual += " $linea"
                }
            }
        }
        if (lineaActual.isNotEmpty()) {
            lineasAgrupadas.add(lineaActual)
        }

        val amountRegex = Regex("""(?:^|\s)([-]?\d{1,3}(?:,\d{3})*\.\d{2}|[-]?\d*\.\d{2})(?:\s|$)""")

        for (linea in lineasAgrupadas) {
            val match = dateStartRegex.find(linea)
            if (match != null) {
                var fechaStr = match.groupValues[1]
                val partesFecha = fechaStr.split("/")
                if (partesFecha.size == 2) {
                    val dia = partesFecha[0].padStart(2, '0')
                    val mes = partesFecha[1].padStart(2, '0')
                    fechaStr = "$dia/$mes/$year"
                }

                val restoLinea = linea.substring(match.range.last + 1).trim()
                val amountMatches = amountRegex.findAll(" $restoLinea ").toList()

                if (amountMatches.isNotEmpty()) {
                    val montoStr = amountMatches.first().groupValues[1]
                    var montoStringClean = montoStr.replace(",", "")

                    if (montoStringClean.startsWith(".")) montoStringClean = "0$montoStringClean"
                    if (montoStringClean.startsWith("-.")) montoStringClean = "-0." + montoStringClean.substring(2)

                    val monto = montoStringClean.toDoubleOrNull() ?: continue
                    if (monto == 0.0) continue

                    var descripcion = restoLinea
                    for (am in amountMatches) {
                        descripcion = descripcion.replace(am.groupValues[1], "")
                    }
                    descripcion = descripcion.replace(Regex("""\s{2,}"""), " ").trim()

                    if (descripcion.isEmpty()) descripcion = "Movimiento Bancario"

                    val esIngreso = monto > 0
                    val montoAbsoluto = kotlin.math.abs(monto)

                    val catNombre = CategorySuggester.suggestCategory(descripcion) ?: if (esIngreso) "Salario e Ingresos" else "Otros"
                    val catId = CategorySuggester.getCategoryIdByName(catNombre)

                    movimientos.add(
                        MovimientoExtraido(
                            fecha = fechaStr,
                            descripcion = descripcion,
                            monto = montoAbsoluto,
                            esIngreso = esIngreso,
                            categoria = catNombre,
                            categoriaId = catId,
                            banco = "Bancolombia"
                        )
                    )
                }
            }
        }
        return movimientos
    }
}