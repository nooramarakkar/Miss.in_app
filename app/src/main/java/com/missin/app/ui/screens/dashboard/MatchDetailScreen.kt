package com.missin.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.missin.app.ui.theme.BoneWhite
import com.missin.app.ui.theme.MutedEmerald
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.LineString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    theirLat: Double,
    theirLng: Double,
    myLat: Double,
    myLng: Double,
    onBack: () -> Unit,
    onStartVerification: () -> Unit
) {
    val context = LocalContext.current
    val deepNavy = MaterialTheme.colorScheme.background
    
    // Calculate the midpoint for camera position
    val midLat = (theirLat + myLat) / 2
    val midLng = (theirLng + myLng) / 2
    val midPoint = LatLng(midLat, midLng)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("Potential Match", color = BoneWhite, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BoneWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = deepNavy)
            )
        },
        bottomBar = {
            Surface(
                color = deepNavy,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Button(
                    onClick = onStartVerification,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MutedEmerald)
                ) {
                    Text("Start Verification", style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                                    .target(midPoint)
                                    .zoom(12.0)
                                    .build()

                                val geoJsonSource = org.maplibre.android.style.sources.GeoJsonSource("match-source")
                                
                                val point1 = Point.fromLngLat(myLng, myLat)
                                val point2 = Point.fromLngLat(theirLng, theirLat)
                                val lineString = LineString.fromLngLats(listOf(point1, point2))
                                
                                val features = mutableListOf<Feature>()
                                features.add(Feature.fromGeometry(point1))
                                features.add(Feature.fromGeometry(point2))
                                features.add(Feature.fromGeometry(lineString))
                                
                                geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(features))
                                style.addSource(geoJsonSource)

                                val lineLayer = org.maplibre.android.style.layers.LineLayer("line-layer", "match-source")
                                lineLayer.setFilter(org.maplibre.android.style.expressions.Expression.eq(org.maplibre.android.style.expressions.Expression.geometryType(), "LineString"))
                                lineLayer.setProperties(
                                    org.maplibre.android.style.layers.PropertyFactory.lineColor(android.graphics.Color.parseColor("#4B8B6E")),
                                    org.maplibre.android.style.layers.PropertyFactory.lineWidth(5f)
                                )
                                style.addLayer(lineLayer)

                                val circleLayer = org.maplibre.android.style.layers.CircleLayer("points-layer", "match-source")
                                circleLayer.setFilter(org.maplibre.android.style.expressions.Expression.eq(org.maplibre.android.style.expressions.Expression.geometryType(), "Point"))
                                circleLayer.setProperties(
                                    org.maplibre.android.style.layers.PropertyFactory.circleRadius(8f),
                                    org.maplibre.android.style.layers.PropertyFactory.circleColor(android.graphics.Color.RED),
                                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
                                )
                                style.addLayer(circleLayer)
                            }
                        }
                    }
                }
            )
        }
    }
}
