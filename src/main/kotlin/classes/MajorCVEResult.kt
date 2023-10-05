package classes

data class MajorCVEResult(
    val cve: String,
    val source: String,
    val httpCode: Int,
    val result: String
)
