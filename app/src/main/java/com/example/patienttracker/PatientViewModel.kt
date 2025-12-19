package com.example.patienttracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.patienttracker.data.PatientLocation
import com.example.patienttracker.data.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PatientRepository(application.applicationContext)

    private val _selectedPatient = MutableStateFlow<PatientLocation?>(null)
    val selectedPatient: StateFlow<PatientLocation?> = _selectedPatient

    val patients = repository.patients
        .combine(_selectedPatient) { list, selected ->
            list to selected
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, Pair(emptyList(), null))

    init {
        viewModelScope.launch { repository.load() }
    }

    fun selectPatient(patient: PatientLocation?) {
        _selectedPatient.value = patient
    }

    fun savePatient(patient: PatientLocation) {
        viewModelScope.launch { repository.addOrUpdate(patient) }
    }

    fun deletePatient(bil: Int) {
        viewModelScope.launch { repository.delete(bil) }
    }
}
