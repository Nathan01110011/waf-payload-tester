package classes

data class PayloadResult(
    val type: String,
    val payload: String,
    val source: String,
    val statusCode: Int
)
