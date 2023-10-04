package utils

import classes.MajorCVE
import java.io.File

fun parseMajorCVEs(filename: String): List<MajorCVE> {
    return File(filename).readLines()
        .drop(1) // Drop the header row
        .map { line ->
            val parts = line.split("|")
            MajorCVE(
                cve = parts[0],
                method = parts[1],
                path = parts[2],
                query_string = parts[3].takeIf { it.isNotBlank() },
                headers = if (parts[4].isNotEmpty()) parts[4].let {
                    it.substring(1, it.length - 1)
                        .split(",")
                        .associate {
                            val (key, value) = it.split(": ")
                            key.trim() to value.trim().removeSurrounding("\"")
                        }
                } else emptyMap(),
                post_data = parts[5].takeIf { it.isNotBlank() },
                files = parts[6].takeIf { it.isNotBlank() },
                source = parts[7]
            )
        }
}
