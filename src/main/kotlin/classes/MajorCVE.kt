package classes

data class MajorCVE(
    val cve: String,
    val method: String,
    val path: String,
    val query_string: String?,
    val headers: Map<String, String>,
    val post_data: String?,
    val files: String?,
    val source: String
)