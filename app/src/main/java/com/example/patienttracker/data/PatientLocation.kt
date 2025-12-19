package com.example.patienttracker.data

data class PatientLocation(
    val bil: Int,
    val zon: String,
    val region: String,
    val address: String,
    val patients: Int,
    val latitude: Double,
    val longitude: Double,
    val contactName: String,
    val contactPhone: String,
    val status: String
)

fun PatientLocation.displayTitle(): String = "${zon.uppercase()} • ${region}"
