package runners

import Constants.HTTP_FORBIDDEN_CODE
import classes.MajorCVEResult
import classes.MajorCVE
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.parseMajorCVEs
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


object CVEPayloadsRunner {

    private val logger: Logger = LoggerFactory.getLogger(CVEPayloadsRunner::class.java)

    fun runRequests(): List<MajorCVEResult> = runBlocking(Dispatchers.IO) {
        val csvFilePath = this::class.java.classLoader.getResource("CVEPayloads/payloads.csv")!!.path
        val requests = parseMajorCVEs(csvFilePath)
        val results = ConcurrentLinkedQueue<MajorCVEResult>()
        val testSite = System.getenv("TEST_SITE")
        coroutineScope {
            requests.forEach { request ->
                launch {
                    val response = sendHttpRequest(testSite, request)

                    val status = if (response.statusCode == HTTP_FORBIDDEN_CODE) {
                        logger.info("CVE: ${request.cve}, Status Code: ${response.statusCode} - Success")
                        "Success"
                    } else {
                        logger.error("CVE: ${request.cve}, Status Code: ${response.statusCode} - Failure")
                        "Failure"
                    }
                    results.add(MajorCVEResult(request.cve, request.source, response.statusCode, status))
                }
            }
        }
        return@runBlocking results.toList()
    }

    private fun sendHttpRequest(baseURL: String, request: MajorCVE): Response {
        val urlWithQueryString = "$baseURL${request.path}?${request.query_string}"

        return when (request.method.uppercase(Locale.getDefault())) {
            "GET" -> {
                val (_, response, _) = Fuel.get(urlWithQueryString).header(request.headers).response()
                response
            }

            "POST" -> {
                if (request.headers["Content-Type"] == "application/json") {
                    val (_, response, _) = Fuel.post(urlWithQueryString)
                        .header(request.headers)
                        .body(request.post_data ?: "")
                        .response()
                    response
                } else {
                    // TODO: Handle non-JSON POST requests. Adjust as necessary.
                    val (_, response, _) = Fuel.post(urlWithQueryString).header(request.headers).response()
                    response
                }
            }

            // TODO: Other HTTP methods as necessary

            else -> throw UnsupportedOperationException("HTTP method ${request.method} not supported yet!")
        }
    }
}