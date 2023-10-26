package utils

import Constants.HTTP_FORBIDDEN_CODE
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import runners.GenericPayloadRunner
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

private val logger: Logger = LoggerFactory.getLogger(GenericPayloadRunner::class.java)

fun createPDF(outputPath: String, genericResultsList: List<PayloadResult>, onlyFailures: Boolean = false) {
    logger.info("******** Starting Report PDF File Generation ********")
    Document(PdfWriter(outputPath).let { com.itextpdf.kernel.pdf.PdfDocument(it) }).apply {
        add(createTitle())
        add(LineSeparator(DashedLine()))
        addResultsTables(this, genericResultsList, onlyFailures)
        close()
    }
    logger.info("******** Report PDF File Generation Complete ********")
}
fun createTitle(): Paragraph = Paragraph("Vulnerability Report")
    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
    .setFontSize(24f)
    .setTextAlignment(TextAlignment.CENTER)
    .setFixedLeading(30f)

fun addResultsTables(document: Document, payloadResultsList: List<PayloadResult>, onlyFailures: Boolean) {
    val headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
    val headerBgColor = DeviceRgb(224, 236, 255) // A light blue color

    payloadResultsList.groupBy { it.type }.forEach { (type, results) ->
        val (blocked, total, fails) = calculateStatistics(results)
        val successRate = "%.2f".format((blocked.toDouble() / total) * 100)

        document.apply {
            add(Paragraph("$type - Success Rate: $successRate%").setBold().setFontSize(14f))
            add(createImageFromChart(generatePieChart(blocked, fails)))
            add(Paragraph("Test Type Success Rate Breakdown").setBold().setFontSize(14f).setTextAlignment(TextAlignment.CENTER))
            add(createSummaryTable(results, headerFont, headerBgColor))
            add(Paragraph("Payloads and Results").setBold().setFontSize(14f).setTextAlignment(TextAlignment.CENTER))
            add(createTableWithResults(type, results, onlyFailures, headerFont, headerBgColor))
            add(AreaBreak())
        }
    }
}

fun calculateStatistics(results: List<PayloadResult>) = Triple(
    results.count { it.statusCode == HTTP_FORBIDDEN_CODE },
    results.size,
    results.size - results.count { it.statusCode == HTTP_FORBIDDEN_CODE }
)

fun createTableWithResults(type: String, results: List<PayloadResult>, onlyFailures: Boolean, headerFont: PdfFont, headerBgColor: DeviceRgb) = Table(floatArrayOf(50f, 15f, 15f, 20f)).apply {
    // Directly create and add the header cells to the table
    val headers = arrayOf("Payload", "Source", "HTTP Code", "Result")
    headers.forEach { addCell(createStyledCell(it, headerFont, headerBgColor, 12f)) }

    val filteredResults = if (onlyFailures) results.filter { it.statusCode != HTTP_FORBIDDEN_CODE } else results
    filteredResults.forEach { result ->

        addCell(Paragraph(result.payload.chunked(55).joinToString("\n")).setFontSize(10f))
        addCell(Paragraph(result.source).setFontSize(10f))
        addCell(Paragraph(result.statusCode.toString()).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))

        // Adding result cell, assuming a logic like: if statusCode is HTTP_FORBIDDEN_CODE then it's "Blocked" otherwise "Failed"
        val resultText = if (result.statusCode == HTTP_FORBIDDEN_CODE) "Blocked" else "Failed"
        addCell(Paragraph(resultText).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
    }
}

fun createStyledCell(text: String, font: PdfFont, bgColor: DeviceRgb, fontSize: Float) = Cell().apply {
    add(Paragraph(text).setBold().setFontSize(fontSize))
    setFont(font).setFontSize(fontSize)
    setBackgroundColor(bgColor)
    setTextAlignment(TextAlignment.CENTER)
    setVerticalAlignment(VerticalAlignment.MIDDLE)
}

fun createImageFromChart(image: BufferedImage) = ByteArrayOutputStream().apply {
    ImageIO.write(image, "png", this)
}.let { Image(ImageDataFactory.create(it.toByteArray())) }

fun createSummaryTable(results: List<PayloadResult>, headerFont: PdfFont, headerBgColor: DeviceRgb): Table {
    val summaryTable = Table(floatArrayOf(80f, 20f))

    summaryTable.addCell(
        Cell().add(Paragraph("Test Type").setBold().setFontSize(12f))
            .setBackgroundColor(headerBgColor)
            .setTextAlignment(TextAlignment.CENTER)
            .setFont(headerFont)
    )
    summaryTable.addCell(
        Cell().add(Paragraph("Success Rate (%)").setBold().setFontSize(12f))
            .setBackgroundColor(headerBgColor)
            .setTextAlignment(TextAlignment.CENTER)
            .setFont(headerFont)
    )

    val groupedByDescription = results.groupBy { it.source }
    groupedByDescription.forEach { (description, descriptionResults) ->
        val successCount = descriptionResults.count { it.statusCode == HTTP_FORBIDDEN_CODE }
        val successRateForTestType = (successCount.toDouble() / descriptionResults.size) * 100
        summaryTable.addCell(Paragraph(description).setFontSize(10f))
        summaryTable.addCell(
            Paragraph("%.2f".format(successRateForTestType)).setFontSize(10f).setTextAlignment(TextAlignment.CENTER)
        )
    }
    return summaryTable
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
