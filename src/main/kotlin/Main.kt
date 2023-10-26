
import Constants.ONLY_FAILURES_IN_REPORT
import Constants.REPORT_PATH
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import runners.GenericPayloadRunner
import utils.createPDF

private val logger: Logger = LoggerFactory.getLogger(GenericPayloadRunner::class.java)

fun main() {
    logger.info("START: Starting benchmarking")
    startBenchmarkTool()
    logger.info("END: Benchmarking complete")
}

private fun startBenchmarkTool() {
    // Payload Testers
//    val cveResultList = CVEPayloadsRunner.runRequests()
    val genericResultsList = GenericPayloadRunner.runRequests()

    // Generating Report
    val outputPath = REPORT_PATH
    createPDF(outputPath, genericResultsList, ONLY_FAILURES_IN_REPORT)
//    createPDF(outputPath, genericResultsList, ONLY_FAILURES_IN_REPORT)
}

