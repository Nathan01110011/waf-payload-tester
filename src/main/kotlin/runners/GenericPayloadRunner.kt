package runners

import classes.PayloadResult
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Response
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

object GenericPayloadRunner {
    private val logger: Logger = LoggerFactory.getLogger(GenericPayloadRunner::class.java)
    private const val JSON_FORMAT = "json"
    private const val FORM_DATA_FORMAT = "form-data"
    private const val TEST_SITE = "TEST_SITE"
    private val JSON_HEADERS = mapOf("Content-Type" to "application/json")

    fun runRequests(): List<PayloadResult> = runBlocking(Dispatchers.IO) {
        val payloadsDirectoryPath = GenericPayloadRunner::class.java.classLoader.getResource("GenericPayloads")?.path
            ?: throw RuntimeException("Unable to locate the 'GenericPayloads' directory!")

        val testSite = System.getenv(TEST_SITE)
            ?: throw RuntimeException("Environment variable $TEST_SITE is not set!")

        File(payloadsDirectoryPath).listFiles { _, name -> name.endsWith(".txt") }
            ?.flatMap { file ->
                logger.info("******** Starting ${file.name} file ********")
                val payloadResults = Collections.synchronizedList(mutableListOf<PayloadResult>())
                coroutineScope {
                    file.readLines().forEach { payload ->
                        launch {
                            executeAndAddResult("$testSite/test?param=$payload", payloadResults, file.name, payload, "Query")
                            executeAndAddResult("$testSite/test", payloadResults, file.name, payload, "Header", headers = mapOf("X-Payload" to payload))
                            executeAndAddResult(testSite, payloadResults, file.name, payload, "POST form-data", body = payload)
                            executeAndAddResult(testSite, payloadResults, file.name, payload, "POST JSON", body = payload, format = JSON_FORMAT, headers = JSON_HEADERS)
                            executeAndAddResult(testSite, payloadResults, file.name, payload, "PATCH JSON", body = payload, format = JSON_FORMAT, method = Method.PATCH, headers = JSON_HEADERS)
                        }
                    }
                }
                logger.info("******** Finished ${file.name} file ********")
                payloadResults
            } ?: emptyList()
    }

    private suspend fun executeAndAddResult(
        url: String,
        resultList: MutableList<PayloadResult>,
        fileName: String,
        payload: String,
        description: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        method: Method = Method.POST,
        format: String = FORM_DATA_FORMAT
    ) {
        val response = sendRequest(url, headers, body, method, format)
        resultList.add(PayloadResult(fileName, payload, description, response.statusCode))
    }

    private suspend fun sendRequest(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        method: Method = Method.POST,
        format: String = FORM_DATA_FORMAT
    ): Response = withContext(Dispatchers.IO) {
        when (body) {
            null -> Fuel.get(url).header(headers).response().second
            else -> {
                when (format) {
                    JSON_FORMAT -> {
                        val jsonBody = """{"test_payload": "$body"}"""
                        Fuel.request(method, url).header(headers + JSON_HEADERS).body(jsonBody).response()
                    }
                    else -> {
                        val params = listOf("test_payload" to body)
                        Fuel.upload(url, method = method, parameters = params).header(headers).response()
                    }
                }.second
            }
        }
    }
}
