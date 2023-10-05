package runners

import classes.PayloadResult
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object GenericPayloadRunner {
    private val logger: Logger = LoggerFactory.getLogger(GenericPayloadRunner::class.java)

    fun runRequests(): List<PayloadResult> = runBlocking(Dispatchers.IO) {
        val payloadsDirectoryPath = GenericPayloadRunner::class.java.classLoader.getResource("GenericPayloads")?.path
            ?: throw RuntimeException("Payloads directory not found!")

        val payloadsDirectory = File(payloadsDirectoryPath)
        val files = payloadsDirectory.listFiles { _, name -> name.endsWith(".txt") }
        val allResults = mutableListOf<PayloadResult>()
        val testSite = System.getenv("TEST_SITE")

        files?.forEach { file ->
            val payloadResults = mutableListOf<PayloadResult>()
            logger.info("******** Starting ${file.name} file ********")
            coroutineScope {
                file.readLines().forEach { payload ->
                    launch {
                        // As Query Parameter
                        val queryResponse = sendRequest("$testSite/test?param=$payload")
                        synchronized(payloadResults) {
                            payloadResults.add(PayloadResult(file.name, payload, "Query", queryResponse.statusCode))
                        }

                        // As Header
                        val headerResponse = sendRequest("$testSite/test", mapOf("X-Payload" to payload))
                        synchronized(payloadResults) {
                            payloadResults.add(PayloadResult(file.name, payload, "Header", headerResponse.statusCode))
                        }

                        // As POST data
                        val postResponse = sendRequest(testSite, body = payload)
                        synchronized(payloadResults) {
                            payloadResults.add(PayloadResult(file.name, payload, "POST data", postResponse.statusCode))
                        }
                    }
                }
            }

            allResults.addAll(payloadResults)
        }

        allResults
    }

    private suspend fun sendRequest(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): Response = coroutineScope {
        when (body) {
            null -> async(Dispatchers.IO) {
                val (_, response, _) = Fuel.get(url).header(headers).response()
                response
            }

            else -> async(Dispatchers.IO) {
                val (_, response, _) = Fuel.post(url).header(headers).body(body).response()
                response
            }
        }.await()
    }
}
