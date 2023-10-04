package utils

import classes.MajorCVE
import java.io.File

fun parseMajorCVEs(filename: String): List<MajorCVE> {
    return File(filename).readLines()
        .drop(1) // Drop the header row
        .map { line ->
            println("Processing line: $line") // Add this debug line
            val parts = line.split("|")
            MajorCVE(
                cve = parts[0],
                method = parts[1],
                path = parts[2],
                query_string = parts[3].takeIf { it.isNotBlank() },
                headers = if (parts[4].isNotEmpty()) {
                    val pattern = Regex("\"([^\"]+)\":\\s\"([^\"]+)\"")
                    val matches = pattern.findAll(parts[4])
                    matches.associate { it.groupValues[1] to it.groupValues[2] }
                } else emptyMap(),
                post_data = parts[5].takeIf { it.isNotBlank() },
                files = parts[6].takeIf { it.isNotBlank() },
                source = parts[7]
            )
        }
}
