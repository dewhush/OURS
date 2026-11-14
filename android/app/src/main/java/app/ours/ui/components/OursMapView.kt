package app.ours.ui.components

import androidx.compose.animation.core.Animatable
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
import app.ours.ui.theme.OursMotion
import app.ours.ui.theme.OursTheme
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
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
    val isDark = OursTheme.isDark
    val styleUri = if (isDark) "asset://ours_map_style_dark.json" else "asset://ours_map_style.json"

    val mapView = remember { MapView(context) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var hasMovedInitialCamera by remember { mutableStateOf(false) }

    var partnerMarker by remember { mutableStateOf<Marker?>(null) }
    var lastPartnerLatLng by remember { mutableStateOf<LatLng?>(null) }
    val staticMarkers = remember { mutableListOf<Marker>() }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
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

    LaunchedEffect(mapRef, styleUri) {
        val map = mapRef ?: return@LaunchedEffect
        styleReady = false
        map.setStyle(Style.Builder().fromUri(styleUri)) {
            styleReady = true
        }
    }

    LaunchedEffect(styleReady, places, highlights) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        @Suppress("DEPRECATION")
        for (m in staticMarkers) {
            map.removeMarker(m)
        }
        staticMarkers.clear()

        val iconFactory = IconFactory.getInstance(context)
        for (place in places) {
            val placeIcon = iconFactory.fromBitmap(MapMarkerFactory.placePin(context, place.name))
            @Suppress("DEPRECATION")
            val m = map.addMarker(
                MarkerOptions()
                    .position(LatLng(place.lat, place.lon))
                    .title(place.name)
                    .snippet("${place.radiusM.toInt()} m")
                    .icon(placeIcon)
            )
            staticMarkers.add(m)
        }

        val memoryIcon = iconFactory.fromBitmap(MapMarkerFactory.highlightPin(context))
        for (h in highlights) {
            if (h.lat != null && h.lon != null) {
                @Suppress("DEPRECATION")
                val m = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(h.lat, h.lon))
                        .title(h.placeName ?: "Memory")
                        .snippet(h.note ?: "Saved highlight")
                        .icon(memoryIcon)
                )
                staticMarkers.add(m)
            }
        }
    }

    LaunchedEffect(styleReady, partnerLocation, partnerName) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        if (partnerLocation == null || partnerLocation.paused ||
            partnerLocation.lat == null || partnerLocation.lon == null
        ) {
            partnerMarker?.let {
                @Suppress("DEPRECATION")
                map.removeMarker(it)
            }
            partnerMarker = null
            lastPartnerLatLng = null
            return@LaunchedEffect
        }

        val target = LatLng(partnerLocation.lat, partnerLocation.lon)
        val iconFactory = IconFactory.getInstance(context)
        val isLive = partnerLocation.recordedAt?.let { System.currentTimeMillis() - it <= 15 * 60_000L } ?: false
        val partnerIcon = iconFactory.fromBitmap(
            MapMarkerFactory.partnerAvatarPin(
                context = context,
                initial = partnerName.ifBlank { "P" },
                isLive = isLive,
            )
        )

        val existing = partnerMarker
        val from = lastPartnerLatLng
        if (existing == null || from == null) {
            existing?.let {
                @Suppress("DEPRECATION")
                map.removeMarker(it)
            }
            @Suppress("DEPRECATION")
            partnerMarker = map.addMarker(
                MarkerOptions()
                    .position(target)
                    .title(partnerName)
                    .icon(partnerIcon)
            )
            lastPartnerLatLng = target
        } else {
            existing.setIcon(partnerIcon)
            existing.title = partnerName
            val fraction = Animatable(0f)
            fraction.animateTo(1f, OursMotion.enterSpec(450)) {
                val t = value.toDouble()
                val lat = from.latitude + (target.latitude - from.latitude) * t
                val lon = from.longitude + (target.longitude - from.longitude) * t
                existing.position = LatLng(lat, lon)
                @Suppress("DEPRECATION")
                map.updateMarker(existing)
            }
            lastPartnerLatLng = target
        }
    }

    var lastRecenterTrigger by remember { mutableStateOf(recenterTrigger) }
    LaunchedEffect(styleReady, partnerLocation, places, highlights, recenterTrigger) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val manualRecenter = recenterTrigger != lastRecenterTrigger
        lastRecenterTrigger = recenterTrigger
        if (hasMovedInitialCamera && !manualRecenter) return@LaunchedEffect

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
            val update = CameraUpdateFactory.newLatLngZoom(target, 14.5)
            if (!hasMovedInitialCamera) {
                hasMovedInitialCamera = true
                map.moveCamera(update)
            } else if (manualRecenter) {
                map.animateCamera(update)
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapRef = map
                    map.uiSettings.isAttributionEnabled = false
                    map.uiSettings.isLogoEnabled = false
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
