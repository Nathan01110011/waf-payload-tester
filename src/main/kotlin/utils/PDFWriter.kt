package utils

import Constants.HTTP_FORBIDDEN_CODE
import classes.MajorCVEResult
import classes.PayloadResult
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.VerticalAlignment
import org.jetbrains.letsPlot.Stat
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomPie
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.label.labs
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleFillManual
import org.jetbrains.letsPlot.themes.elementBlank
import org.jetbrains.letsPlot.themes.elementText
import org.jetbrains.letsPlot.themes.theme
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO


fun createPDF(
    outputPath: String,
    cveResultList: List<MajorCVEResult>,
    genericResultsList: List<PayloadResult>,
    onlyFailures: Boolean = false
) {
    val writer = PdfWriter(outputPath)
    val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(writer)
    val document = Document(pdfDoc)

    val titleFont: PdfFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
    val title = Paragraph("Vulnerability Report")
        .setFont(titleFont)
        .setFontSize(24f)
        .setTextAlignment(TextAlignment.CENTER)
        .setFixedLeading(30f)

    val separator = LineSeparator(DashedLine())
    document.add(title)
    document.add(separator)

    addCVEResultsTable(document, cveResultList, onlyFailures)
    addPayloadResultsTable(document, genericResultsList, onlyFailures)

    document.close()
}

fun addCVEResultsTable(document: Document, cveResultList: List<MajorCVEResult>, onlyFailures: Boolean) {
    val table = Table(floatArrayOf(25f, 40f, 15f, 20f))
    val headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
    val headerBgColor = DeviceRgb(224, 236, 255)

    val blocked = cveResultList.filter { it.httpCode == HTTP_FORBIDDEN_CODE }.size
    val total = cveResultList.size
    val successRate = (blocked.toDouble() / total) * 100
    val fails = total - blocked

    val fileHeading = "Specific CVEs - Success Rate: $successRate%"
    document.add(Paragraph(fileHeading).setBold().setFontSize(14f))

    arrayOf("CVE Code", "POC Link", "HTTP Code", "Result").forEach { header ->
        val cell = Cell()
        cell.add(Paragraph(header).setBold().setFontSize(12f))
        cell.setFont(headerFont).setFontSize(12f)
        cell.setBackgroundColor(headerBgColor)
        cell.setTextAlignment(TextAlignment.CENTER)
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE)
        table.addCell(cell)
    }

    val filteredList = if (onlyFailures) {
        cveResultList.filter { it.result == "Failed" }
    } else {
        cveResultList
    }

    filteredList.forEach { result ->
        table.addCell(result.cve)
        table.addCell(result.source)
        table.addCell(result.httpCode.toString()).setTextAlignment(TextAlignment.CENTER)
        table.addCell(result.result)
    }

    val pieChartImage = generatePieChart(blocked, fails)
    val baos = ByteArrayOutputStream()
    ImageIO.write(pieChartImage, "png", baos)
    val pieImageData = ImageDataFactory.create(baos.toByteArray())
    document.add(Image(pieImageData))

    document.add(table)
    document.add(AreaBreak());
}

fun addPayloadResultsTable(document: Document, payloadResultsList: List<PayloadResult>, onlyFailures: Boolean) {

    val groupedResults = payloadResultsList.groupBy { it.type } // Group results by file type

    val headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
    val headerBgColor = DeviceRgb(224, 236, 255) // A light blue color

    groupedResults.forEach { (type, results) ->
        // Calculate the success rate for this file type
        val blocked = results.filter { it.statusCode == HTTP_FORBIDDEN_CODE }.size
        val total = results.size
        val successRate = (blocked.toDouble() / total) * 100
        val fails = total - blocked

        val fileHeading = "$type - Success Rate: $successRate%"
        document.add(Paragraph(fileHeading).setBold().setFontSize(14f))

        val table = Table(floatArrayOf(61f, 13f, 13f, 13f))

        // Add the column headers
        arrayOf("Payload", "Source", "HTTP Code", "Result").forEach { header ->
            val cell = Cell()
            cell.add(Paragraph(header).setBold().setFontSize(12f))
            cell.setFont(headerFont).setFontSize(12f)
            cell.setBackgroundColor(headerBgColor)
            cell.setTextAlignment(TextAlignment.CENTER)
            cell.setVerticalAlignment(VerticalAlignment.MIDDLE)
            table.addCell(cell)
        }

        // Filter table with only failed payloads if enabled, remove ones that worked for clarity
        val filteredList = if (onlyFailures) {
            results.filter { it.statusCode != HTTP_FORBIDDEN_CODE }
        } else {
            results
        }

        // Fill the table with data
        filteredList.forEach { result ->
            table.addCell(Paragraph(result.payload.insertBreaksEvery(55)).setFontSize(10f))
            table.addCell(result.source).setFontSize(10f)
            table.addCell(result.statusCode.toString()).setTextAlignment(TextAlignment.CENTER).setFontSize(10f)
            table.addCell(if (result.statusCode == HTTP_FORBIDDEN_CODE) "Success" else "Failed")
        }

        val pieChartImage = generatePieChart(blocked, fails)
        val baos = ByteArrayOutputStream()
        ImageIO.write(pieChartImage, "png", baos)
        val pieImageData = ImageDataFactory.create(baos.toByteArray())
        document.add(Image(pieImageData))
        document.add(table)
        document.add(AreaBreak());
    }
}

fun generatePieChart(success: Int, failure: Int): BufferedImage {
    // Structuring data for letsPlot
    val dataMap: Map<String, List<Any>> = mapOf(
        "name" to listOf("Success", "Failure"),
        "value" to listOf(success, failure)
    )
    val p = letsPlot(dataMap) +
            geomPie(stat = Stat.identity, size = 0.7, sizeUnit = "x") {
                slice = "value"
                fill = "name"
            } +
            scaleFillManual(values = listOf(Color(152, 251, 152), Color(250, 128, 114))) +
            theme(
                axisTitle = elementBlank(),
                axisText = elementBlank(),
                axisLine = elementBlank(),
                axisTicks = elementBlank(),
                panelBackground = elementBlank(),
                panelGrid = elementBlank(),
                plotBackground = elementBlank(),
                legendText = elementText(size = 10)
            ) +
            ggsize(217, 100) +
            labs(fill = "")

    val tempFile = File.createTempFile("pieChart", ".png").apply {
        deleteOnExit()
    }
    ggsave(p, tempFile.absolutePath)

    return ImageIO.read(tempFile)
}

fun String.insertBreaksEvery(n: Int): String {
    return this.chunked(n).joinToString("\n")
}