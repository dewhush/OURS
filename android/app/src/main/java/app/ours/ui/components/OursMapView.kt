package app.ours.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.ours.data.api.CheckInDto
import app.ours.data.api.PartnerLocationDto
import app.ours.data.api.PlaceDto
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun OursMapView(
    modifier: Modifier = Modifier,
    partnerLocation: PartnerLocationDto? = null,
    partnerName: String = "Partner",
    places: List<PlaceDto> = emptyList(),
    highlights: List<CheckInDto> = emptyList(),
    recenterTrigger: Int = 0,
    onHighlightClick: ((CheckInDto) -> Unit)? = null,
    onMapLongClick: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(styleReady, partnerLocation, places, highlights) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        @Suppress("DEPRECATION")
        map.clear()

        if (partnerLocation != null && !partnerLocation.paused &&
            partnerLocation.lat != null && partnerLocation.lon != null
        ) {
            @Suppress("DEPRECATION")
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(partnerLocation.lat, partnerLocation.lon))
                    .title(partnerName)
            )
        }

        for (place in places) {
            @Suppress("DEPRECATION")
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(place.lat, place.lon))
                    .title(place.name)
                    .snippet("${place.radiusM.toInt()} m")
            )
        }

        for (h in highlights) {
            if (h.lat != null && h.lon != null) {
                @Suppress("DEPRECATION")
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(h.lat, h.lon))
                        .title(h.placeName ?: "Memory")
                        .snippet(h.note ?: "Saved highlight")
                )
            }
        }
    }

    LaunchedEffect(styleReady, recenterTrigger) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val target = when {
            partnerLocation != null && !partnerLocation.paused &&
                partnerLocation.lat != null && partnerLocation.lon != null ->
                LatLng(partnerLocation.lat, partnerLocation.lon)
            highlights.firstOrNull { it.lat != null && it.lon != null } != null -> {
                val h = highlights.first { it.lat != null && it.lon != null }
                LatLng(h.lat!!, h.lon!!)
            }
            places.isNotEmpty() -> LatLng(places.first().lat, places.first().lon)
            else -> null
        }
        if (target != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 14.5))
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapRef = map
                    map.uiSettings.isAttributionEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    map.setStyle(Style.Builder().fromUri("asset://ours_map_style.json")) {
                        styleReady = true
                    }
                    if (onMapLongClick != null) {
                        map.addOnMapLongClickListener { point ->
                            onMapLongClick(point.latitude, point.longitude)
                            true
                        }
                    }
                    if (onHighlightClick != null) {
                        @Suppress("DEPRECATION")
                        map.setOnMarkerClickListener { marker ->
                            val match = highlights.firstOrNull { h ->
                                h.lat != null && h.lon != null &&
                                    kotlin.math.abs(h.lat - marker.position.latitude) < 1e-5 &&
                                    kotlin.math.abs(h.lon - marker.position.longitude) < 1e-5
                            }
                            if (match != null) {
                                onHighlightClick(match)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}
