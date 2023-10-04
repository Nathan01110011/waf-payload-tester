import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.github.kittinunf.fuel.Fuel
import org.slf4j.LoggerFactory

import classes.MajorCVE
import com.github.kittinunf.fuel.core.Response
import utils.parseMajorCVEs
import java.util.*


const val HTTP_FORBIDDEN_CODE = 403
const val TEST_SITE = "http://34.36.101.100"

fun main() = runBlocking(Dispatchers.IO) {
    val logger = LoggerFactory.getLogger("HttpRequestLogger")
    val csvFilePath = this::class.java.classLoader.getResource("CISA/TopCVEs.csv")!!.path
    val requests = parseMajorCVEs(csvFilePath)


    requests.forEach { request ->
        launch {
            val response = sendHttpRequest(TEST_SITE, request)

            if (response.statusCode == HTTP_FORBIDDEN_CODE) {
                logger.info("CVE: ${request.cve}, Status Code: ${response.statusCode} - Success")
            } else {
                logger.error("CVE: ${request.cve}, Status Code: ${response.statusCode} - Failure")
            }
        }
    }
}

suspend fun sendHttpRequest(baseURL: String, request: MajorCVE): Response {
    val urlWithQueryString = "$baseURL${request.path}${request.query_string}"

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
        else -> throw UnsupportedOperationException("HTTP method ${request.method} not supported yet!")
    }
}
