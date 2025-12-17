package com.example.patienttracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patienttracker.data.PatientLocation
import com.example.patienttracker.data.displayTitle
import com.example.patienttracker.ui.theme.PatientTrackerTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderContent()
    }

    private fun renderContent() {
        setContent {
            PatientTrackerTheme {
                val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                val patientViewModel: PatientViewModel = viewModel(factory = factory)
                PatientTrackerApp(viewModel = patientViewModel)
            }
        }
    }
}

private enum class ViewMode { Map, List }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientTrackerApp(viewModel: PatientViewModel) {
    val (patients, selected) = viewModel.patients.collectAsState().value
    var viewMode by remember { mutableStateOf(ViewMode.Map) }
    var editing by remember { mutableStateOf<PatientLocation?>(null) }
    var deleting by remember { mutableStateOf<PatientLocation?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    var hasLocation by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            hasLocation = true
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Patient Tracker") },
                actions = {
                    OutlinedButton(onClick = {
                        viewMode = if (viewMode == ViewMode.Map) ViewMode.List else ViewMode.Map
                    }) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.Map) Icons.Default.TableView else Icons.Default.Map,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(text = if (viewMode == ViewMode.Map) "List" else "Map")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    showForm = true
                },
                icon = { Icon(Icons.Default.Edit, contentDescription = "Add patient") },
                text = { Text("New entry") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (viewMode) {
                ViewMode.Map -> PatientMapView(
                    patients = patients,
                    selectedPatient = selected,
                    onSelect = viewModel::selectPatient,
                    hasLocationPermission = hasLocation
                )
                ViewMode.List -> PatientList(
                    patients = patients,
                    onEdit = {
                        editing = it
                        showForm = true
                    },
                    onDelete = { deleting = it },
                    onFocusOnMap = {
                        viewModel.selectPatient(it)
                        viewMode = ViewMode.Map
                    }
                )
            }
        }
    }

    if (showForm) {
        PatientFormDialog(
            existing = editing,
            onDismiss = { showForm = false },
            onSave = { input ->
                viewModel.savePatient(input)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Saved ${input.displayTitle()}")
                }
                showForm = false
            }
        )
    }

    if (deleting != null) {
        val patient = deleting!!
        ConfirmDeleteDialog(
            patient = patient,
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deletePatient(patient.bil)
                deleting = null
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted ${patient.displayTitle()}",
                        actionLabel = "Undo"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.savePatient(patient)
                    }
                }
            }
        )
    }
}

@Composable
fun PatientMapView(
    patients: List<PatientLocation>,
    selectedPatient: PatientLocation?,
    onSelect: (PatientLocation) -> Unit,
    hasLocationPermission: Boolean
) {
    val initialLatLng = selectedPatient?.let { LatLng(it.latitude, it.longitude) }
        ?: patients.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(3.1390, 101.6869) // Kuala Lumpur default

    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 11f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            PermissionBanner()
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission)
        ) {
            patients.forEach { patient ->
                val position = LatLng(patient.latitude, patient.longitude)
                Marker(
                    position = position,
                    title = patient.displayTitle(),
                    snippet = patient.address,
                    onClick = {
                        onSelect(patient)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(position, 14f)
                        true
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Location access helps orient the map.", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Grant location permission to show your position alongside patient markers.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
        }
    }
}

@Composable
fun PatientList(
    patients: List<PatientLocation>,
    onEdit: (PatientLocation) -> Unit,
    onDelete: (PatientLocation) -> Unit,
    onFocusOnMap: (PatientLocation) -> Unit
) {
    if (patients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No patient records yet. Add one to get started.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(patients, key = { it.bil }) { patient ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = patient.displayTitle(), style = MaterialTheme.typography.titleLarge)
                        Text(text = patient.address, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Patients: ${patient.patients}")
                        if (patient.status.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Status: ${patient.status}")
                        }
                        if (patient.contactName.isNotBlank() || patient.contactPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOfNotNull(
                                    patient.contactName.takeIf { it.isNotBlank() },
                                    patient.contactPhone.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { onFocusOnMap(patient) }) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Map")
                            }
                            OutlinedButton(onClick = { onEdit(patient) }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Edit")
                            }
                            IconButton(onClick = { onDelete(patient) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientFormDialog(
    existing: PatientLocation?,
    onDismiss: () -> Unit,
    onSave: (PatientLocation) -> Unit
) {
    var zon by remember { mutableStateOf(existing?.zon ?: "") }
    var region by remember { mutableStateOf(existing?.region ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var patients by remember { mutableStateOf(existing?.patients?.toString() ?: "") }
    var latitude by remember { mutableStateOf(existing?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(existing?.longitude?.toString() ?: "") }
    var contactName by remember { mutableStateOf(existing?.contactName ?: "") }
    var contactPhone by remember { mutableStateOf(existing?.contactPhone ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val patientCount = patients.toIntOrNull()
                val lat = latitude.toDoubleOrNull()
                val lng = longitude.toDoubleOrNull()
                if (zon.isBlank() || region.isBlank() || address.isBlank()) {
                    error = "All text fields are required"
                    return@TextButton
                }
                if (patientCount == null || lat == null || lng == null) {
                    error = "Patients, latitude, and longitude must be valid numbers"
                    return@TextButton
                }
                val record = PatientLocation(
                    bil = existing?.bil ?: 0,
                    zon = zon,
                    region = region,
                    address = address,
                    patients = patientCount,
                    latitude = lat,
                    longitude = lng,
                    contactName = contactName,
                    contactPhone = contactPhone,
                    status = status
                )
                onSave(record)
            }) {
                Text(text = if (existing == null) "Create" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(text = if (existing == null) "New patient location" else "Edit patient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = zon,
                    onValueChange = { zon = it },
                    label = { Text("Zone") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Region") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") }
                )
                OutlinedTextField(
                    value = patients,
                    onValueChange = { patients = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Patients") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Status / Notes") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Contact phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    patient: PatientLocation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Delete ${patient.displayTitle()}?") },
        text = { Text("This will remove the row from the CSV and map.") }
    )
}
