package com.example.patienttracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class PatientRepository(private val context: Context) {
    private val storageFile: File by lazy { File(context.filesDir, CSV_NAME) }
    private val _patients = MutableStateFlow<List<PatientLocation>>(emptyList())
    val patients: StateFlow<List<PatientLocation>> = _patients

    suspend fun load() {
        val loaded = withContext(Dispatchers.IO) {
            val source = if (storageFile.exists()) storageFile else null
            if (source != null) {
                parseCsv(source.readText())
            } else {
                context.assets.open(CSV_NAME).use { stream ->
                    val text = BufferedReader(InputStreamReader(stream)).readText()
                    persist(text)
                    parseCsv(text)
                }
            }
        }
        _patients.value = loaded
    }

    suspend fun addOrUpdate(patient: PatientLocation) {
        val updated = withContext(Dispatchers.IO) {
            val nextList = _patients.value.toMutableList()
            val index = nextList.indexOfFirst { it.bil == patient.bil }
            if (index >= 0) {
                nextList[index] = patient
            } else {
                nextList.add(patient.copy(bil = nextBil(nextList)))
            }
            nextList.sortedBy { it.bil }
        }
        _patients.value = updated
        saveToDisk(updated)
    }

    suspend fun delete(bil: Int) {
        val updated = _patients.value.filterNot { it.bil == bil }
        _patients.value = updated
        saveToDisk(updated)
    }

    private fun parseCsv(csvText: String): List<PatientLocation> {
        return csvText
            .lineSequence()
            .drop(1) // drop header
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val columns = parseCsvLine(line)
                if (columns.size < 10) return@mapNotNull null
                val bil = columns[0].toIntOrNull() ?: return@mapNotNull null
                val patients = columns[4].toIntOrNull() ?: 0
                val latitude = columns[5].toDoubleOrNull() ?: return@mapNotNull null
                val longitude = columns[6].toDoubleOrNull() ?: return@mapNotNull null
                PatientLocation(
                    bil = bil,
                    zon = columns[1],
                    region = columns[2],
                    address = columns[3],
                    patients = patients,
                    latitude = latitude,
                    longitude = longitude,
                    contactName = columns[7],
                    contactPhone = columns[8],
                    status = columns[9]
                )
            }.toList()
    }

    private suspend fun saveToDisk(items: List<PatientLocation>) {
        withContext(Dispatchers.IO) {
            val csv = buildString {
                appendLine(HEADER)
                items.forEach { p ->
                    appendLine(
                        listOf(
                            p.bil.toString(),
                            p.zon,
                            p.region,
                            p.address,
                            p.patients.toString(),
                            p.latitude.toString(),
                            p.longitude.toString(),
                            p.contactName,
                            p.contactPhone,
                            p.status
                        ).joinToString(",") { value -> value.toCsvCell() }
                    )
                }
            }
            persist(csv)
        }
    }

    private fun nextBil(current: List<PatientLocation>): Int =
        (current.maxOfOrNull { it.bil } ?: 0) + 1

    private fun persist(contents: String) {
        storageFile.writeText(contents)
    }

    private fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' -> {
                    val nextIsQuote = index + 1 < line.length && line[index + 1] == '"'
                    if (inQuotes && nextIsQuote) {
                        current.append('"')
                        index++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    cells.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        cells.add(current.toString().trim())
        return cells
    }

    private fun String.toCsvCell(): String {
        return if (contains('"') || contains(',') || contains('\n')) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }
    }

    companion object {
        private const val CSV_NAME = "patients.csv"
        private const val HEADER = "bil,zon,region,address,patients,latitude,longitude,contact_name,contact_phone,status"
    }
}
