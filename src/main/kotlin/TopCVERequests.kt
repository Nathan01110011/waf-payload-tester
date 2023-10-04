import com.github.kittinunf.fuel.Fuel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

fun main() = runBlocking(Dispatchers.IO) {
    val logger = LoggerFactory.getLogger("HttpRequestLogger")
    val payloads =
        this::class.java.classLoader.getResourceAsStream("owasp/sql.txt")?.bufferedReader().use { it?.readLines() }
            ?: listOf()

    payloads.forEach { payload ->
        launch {
            val urlWithQueryParam = "$TEST_SITE?test=$payload"
            val (_, response, _) = Fuel.get(urlWithQueryParam).response()

            if (response.statusCode == HTTP_FORBIDDEN_CODE) {
                logger.info("Payload: $payload, Status Code: ${response.statusCode} - Success")
            } else {
                logger.error("Payload: $payload, Status Code: ${response.statusCode} - Failure")
            }
        }
    }
}
