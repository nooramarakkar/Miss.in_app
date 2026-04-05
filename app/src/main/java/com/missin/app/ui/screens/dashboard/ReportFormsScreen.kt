package com.missin.app.ui.screens.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missin.app.ui.viewmodel.ReportFormsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.missin.app.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReportFormsScreen(
    isFoundForm: Boolean,
    onBack: () -> Unit,
    viewModel: ReportFormsViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var category by remember { mutableStateOf("") }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var locationAddress by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val categories = listOf("Wallet/Purse", "Mobile/Electronics", "Keys", "Bag/Backpack", "Documents/ID", "Jewelry/Watch", "Clothing", "Pets", "Other")
    var expanded by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }
    
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val currentDate = datePickerState.selectedDateMillis?.let { sdf.format(Date(it)) } ?: sdf.format(Date())

    val context = LocalContext.current
    val deepNavy = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val electricBlue = com.missin.app.ui.theme.MutedEmerald

    fun getCategoryIcon(cat: String): ImageVector {
        return when (cat) {
            "Wallet/Purse" -> Icons.Default.AccountBalanceWallet
            "Mobile/Electronics" -> Icons.Default.PhoneAndroid
            "Keys" -> Icons.Default.VpnKey
            "Bag/Backpack" -> Icons.Default.Backpack
            "Documents/ID" -> Icons.Default.Assignment
            "Jewelry/Watch" -> Icons.Default.Watch
            "Clothing" -> Icons.Default.Checkroom
            "Pets" -> Icons.Default.Pets
            else -> Icons.Default.Pending
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = if (isFoundForm) "Report Found Item" else "Report Lost Item", 
                        color = com.missin.app.ui.theme.BoneWhite,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = com.missin.app.ui.theme.BoneWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = deepNavy)
            )
        },
        containerColor = deepNavy
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category", color = MaterialTheme.colorScheme.onSurface) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = deepNavy,
                        unfocusedContainerColor = deepNavy,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = com.missin.app.ui.theme.BoneWhite,
                        unfocusedBorderColor = com.missin.app.ui.theme.BoneWhite
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(deepNavy)
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(getCategoryIcon(selectionOption), contentDescription = null, tint = electricBlue, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(selectionOption, color = textColor)
                                }
                            },
                            onClick = {
                                category = selectionOption
                                expanded = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                OutlinedTextField(
                    value = currentDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Date", color = MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = deepNavy,
                        unfocusedContainerColor = deepNavy,
                        disabledContainerColor = deepNavy,
                        disabledTextColor = textColor.copy(alpha = 0.8f),
                        disabledBorderColor = com.missin.app.ui.theme.BoneWhite,
                        disabledLabelColor = textColor.copy(alpha = 0.7f)
                    )
                )
            }
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("OK", color = electricBlue) }
                    },
                    colors = DatePickerDefaults.colors(containerColor = deepNavy)
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            titleContentColor = textColor,
                            headlineContentColor = textColor,
                            weekdayContentColor = textColor.copy(0.7f),
                            dayContentColor = textColor,
                            selectedDayContainerColor = electricBlue
                        )
                    )
                }
            }

            // Location Map Picker Trigger
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showMap = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.missin.app.ui.theme.BoneWhite),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = com.missin.app.ui.theme.BoneWhite)
            ) {
                Text(
                    if (selectedLatLng == null) "Select on Map" else locationAddress.ifEmpty { "Location Selected" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Item Description", color = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = deepNavy,
                    unfocusedContainerColor = deepNavy,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = com.missin.app.ui.theme.BoneWhite,
                    unfocusedBorderColor = com.missin.app.ui.theme.BoneWhite
                )
            )

            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = com.missin.app.ui.theme.BoneWhite)
                    }
                }
            } else {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isFoundForm) "Attach Verification Image (Required)" else "Attach Reference Image (Optional)", color = textColor)
                }
            }
            if (isFoundForm) {
                Text(
                    "Note: Images are secured and not shown in the public feed to prevent fraudulent claims.", 
                    color = textColor.copy(alpha = 0.5f), 
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val imageFile = imageUri?.let { com.missin.app.utils.FileUtils.uriToFile(context, it) }
                    if (isFoundForm) {
                        viewModel.submitFoundReport(category, category, selectedLatLng?.latitude ?: 0.0, selectedLatLng?.longitude ?: 0.0, locationAddress, description, imageFile)
                    } else {
                        viewModel.submitLostReport(category, category, selectedLatLng?.latitude ?: 0.0, selectedLatLng?.longitude ?: 0.0, locationAddress, description, imageFile)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && category.isNotBlank() && selectedLatLng != null && description.isNotBlank() && (!isFoundForm || imageUri != null),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Report", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium) 
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showMap) {
        Dialog(onDismissRequest = { showMap = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            val context = LocalContext.current
            var tempLatLng by remember { mutableStateOf(selectedLatLng ?: LatLng(37.4221, -122.0841)) }
            
            val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
            val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
            
            val lifecycleOwner = LocalLifecycleOwner.current
            val mapView = remember { 
                MapLibre.getInstance(context)
                MapView(context)
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { 
                        mapView.apply {
                            getMapAsync { maplibreMap ->
                                val styleJson = """
                                {
                                    "version": 8,
                                    "sources": {
                                        "osm": {
                                            "type": "raster",
                                            "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
                                            "tileSize": 256,
                                            "attribution": "&copy; OpenStreetMap Contributors"
                                        }
                                    },
                                    "layers": [
                                        {
                                            "id": "osm-layer",
                                            "type": "raster",
                                            "source": "osm"
                                        }
                                    ]
                                }
                                """.trimIndent()

                                maplibreMap.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                                    maplibreMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                        .target(tempLatLng)
                                        .zoom(15.0)
                                        .build()

                                    val geoJsonSource = org.maplibre.android.style.sources.GeoJsonSource("marker-source")
                                    geoJsonSource.setGeoJson(org.maplibre.geojson.Point.fromLngLat(tempLatLng.longitude, tempLatLng.latitude))
                                    style.addSource(geoJsonSource)

                                    val circleLayer = org.maplibre.android.style.layers.CircleLayer("marker-layer", "marker-source")
                                    circleLayer.setProperties(
                                        org.maplibre.android.style.layers.PropertyFactory.circleRadius(8f),
                                        org.maplibre.android.style.layers.PropertyFactory.circleColor(android.graphics.Color.RED),
                                        org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                                        org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
                                    )
                                    style.addLayer(circleLayer)
                                }

                                maplibreMap.addOnMapClickListener { point ->
                                    tempLatLng = point
                                    maplibreMap.style?.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("marker-source")?.let { source ->
                                        source.setGeoJson(org.maplibre.geojson.Point.fromLngLat(point.longitude, point.latitude))
                                    }
                                    true
                                }
                            }
                        }
                    }
                )

                FloatingActionButton(
                    onClick = {
                        if (locationPermissionState.status.isGranted) {
                            try {
                                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener { location ->
                                        if (location != null) {
                                            val currentLatLng = LatLng(location.latitude, location.longitude)
                                            tempLatLng = currentLatLng
                                            mapView.getMapAsync { maplibreMap ->
                                                maplibreMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                                    .target(currentLatLng)
                                                    .zoom(15.0)
                                                    .build()
                                                maplibreMap.style?.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("marker-source")?.let { source ->
                                                    source.setGeoJson(org.maplibre.geojson.Point.fromLngLat(currentLatLng.longitude, currentLatLng.latitude))
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } catch (e: SecurityException) {
                                Toast.makeText(context, "Location permission missing", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 32.dp, end = 16.dp),
                    containerColor = com.missin.app.ui.theme.BoneWhite,
                    contentColor = deepNavy
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Current Location")
                }

                Button(
                    onClick = {
                        selectedLatLng = tempLatLng
                        showMap = false
                        // Reverse Geocode off main thread implicitly using coroutines or handlers, for simple UI we fall back
                        try {
                            val geocoder = android.location.Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(tempLatLng.latitude, tempLatLng.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                locationAddress = addresses[0].getAddressLine(0) ?: "Location selected"
                            }
                        } catch (e: Exception) {
                            locationAddress = "Location Coordinates Tagged"
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(32.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = electricBlue)
                ) {
                    Text("Confirm Location", color = com.missin.app.ui.theme.BoneWhite, style = MaterialTheme.typography.titleMedium)
                }

                IconButton(
                    onClick = { showMap = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(32.dp).background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = com.missin.app.ui.theme.BoneWhite)
                }
            }
        }
    }
}
