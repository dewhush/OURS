package app.ours.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.ours.data.api.CheckInDto
import app.ours.data.api.LocationPointDto
import app.ours.data.api.PartnerLocationDto
import app.ours.data.api.PlaceDto
import app.ours.ui.theme.OursMotion
import app.ours.ui.theme.OursTheme
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun OursMapView(
    modifier: Modifier = Modifier,
    partnerLocation: PartnerLocationDto? = null,
    partnerName: String = "Partner",
    myLocation: LocationPointDto? = null,
    myAzimuthDeg: Float? = null,
    compassMode: Boolean = false,
    places: List<PlaceDto> = emptyList(),
    highlights: List<CheckInDto> = emptyList(),
    recenterTrigger: Int = 0,
    nudgeTrigger: Int = 0,
    onHighlightClick: ((CheckInDto) -> Unit)? = null,
    onMapLongClick: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = OursTheme.colors
    val isDark = OursTheme.isDark
    val styleUri = if (isDark) "asset://ours_map_style_dark.json" else "asset://ours_map_style.json"

    val mapView = remember { MapView(context) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var hasMovedInitialCamera by remember { mutableStateOf(false) }
    var cameraMoveTick by remember { mutableIntStateOf(0) }
    val nudgeProgress = remember { Animatable(0f) }

    var partnerMarker by remember { mutableStateOf<Marker?>(null) }
    var myMarker by remember { mutableStateOf<Marker?>(null) }
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

    LaunchedEffect(styleReady, myLocation, myAzimuthDeg) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        if (myLocation == null) {
            myMarker?.let {
                @Suppress("DEPRECATION")
                map.removeMarker(it)
            }
            myMarker = null
            return@LaunchedEffect
        }
        val pos = LatLng(myLocation.lat, myLocation.lon)
        val icon = IconFactory.getInstance(context)
            .fromBitmap(MapMarkerFactory.myLocationPin(context, myAzimuthDeg))
        val existing = myMarker
        if (existing == null) {
            @Suppress("DEPRECATION")
            myMarker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("You")
                    .icon(icon)
            )
        } else {
            existing.position = pos
            existing.setIcon(icon)
            @Suppress("DEPRECATION")
            map.updateMarker(existing)
        }
    }

    LaunchedEffect(styleReady, compassMode, myAzimuthDeg) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val targetBearing = if (compassMode && myAzimuthDeg != null && myAzimuthDeg.isFinite()) {
            myAzimuthDeg.toDouble()
        } else {
            0.0
        }
        if (compassMode || kotlin.math.abs(map.cameraPosition.bearing) > 0.5) {
            val pos = CameraPosition.Builder(map.cameraPosition)
                .bearing(targetBearing)
                .build()
            map.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
        }
    }

    var lastRecenterTrigger by remember { mutableStateOf(recenterTrigger) }
    LaunchedEffect(styleReady, partnerLocation, places, highlights, recenterTrigger) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val manualRecenter = recenterTrigger != lastRecenterTrigger
        lastRecenterTrigger = recenterTrigger
        if (hasMovedInitialCamera && !manualRecenter) return@LaunchedEffect

        val partnerLatLng = if (partnerLocation != null && !partnerLocation.paused &&
            partnerLocation.lat != null && partnerLocation.lon != null
        ) LatLng(partnerLocation.lat, partnerLocation.lon) else null
        val myLatLng = myLocation?.let { LatLng(it.lat, it.lon) }

        val update = when {
            partnerLatLng != null && myLatLng != null &&
                app.ours.location.AdaptiveLocationPolicy.distanceMeters(
                    myLatLng.latitude, myLatLng.longitude,
                    partnerLatLng.latitude, partnerLatLng.longitude,
                ) > 60.0 -> {
                val bounds = LatLngBounds.Builder()
                    .include(myLatLng)
                    .include(partnerLatLng)
                    .build()
                val padPx = (96f * context.resources.displayMetrics.density).toInt()
                CameraUpdateFactory.newLatLngBounds(bounds, padPx)
            }
            partnerLatLng != null -> CameraUpdateFactory.newLatLngZoom(partnerLatLng, 14.5)
            highlights.firstOrNull { it.lat != null && it.lon != null } != null -> {
                val h = highlights.first { it.lat != null && it.lon != null }
                CameraUpdateFactory.newLatLngZoom(LatLng(h.lat!!, h.lon!!), 14.5)
            }
            places.isNotEmpty() -> CameraUpdateFactory.newLatLngZoom(LatLng(places.first().lat, places.first().lon), 14.5)
            myLatLng != null -> CameraUpdateFactory.newLatLngZoom(myLatLng, 14.5)
            else -> null
        }
        if (update != null) {
            if (!hasMovedInitialCamera) {
                hasMovedInitialCamera = true
                map.moveCamera(update)
            } else if (manualRecenter) {
                map.animateCamera(update)
            }
        }
    }

    LaunchedEffect(nudgeTrigger) {
        if (nudgeTrigger <= 0) return@LaunchedEffect
        val map = mapRef
        val partnerLatLng = if (partnerLocation != null && !partnerLocation.paused &&
            partnerLocation.lat != null && partnerLocation.lon != null
        ) LatLng(partnerLocation.lat, partnerLocation.lon) else null
        val myLatLng = myLocation?.let { LatLng(it.lat, it.lon) }
        if (map != null && styleReady && partnerLatLng != null && myLatLng != null) {
            val dist = app.ours.location.AdaptiveLocationPolicy.distanceMeters(
                myLatLng.latitude, myLatLng.longitude,
                partnerLatLng.latitude, partnerLatLng.longitude,
            )
            if (dist > 60.0) {
                val bounds = LatLngBounds.Builder()
                    .include(myLatLng)
                    .include(partnerLatLng)
                    .build()
                val padPx = (96f * context.resources.displayMetrics.density).toInt()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padPx), 420)
            }
        }
        nudgeProgress.snapTo(0f)
        nudgeProgress.animateTo(1f, animationSpec = OursMotion.enterSpec(1050))
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapRef = map
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.addOnCameraMoveListener {
                            cameraMoveTick++
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
            modifier = Modifier.fillMaxSize(),
        )

        val progress = nudgeProgress.value
        if (nudgeTrigger > 0 && progress in 0.001f..0.999f) {
            val map = mapRef
            val partnerLatLng = if (partnerLocation != null && !partnerLocation.paused &&
                partnerLocation.lat != null && partnerLocation.lon != null
            ) LatLng(partnerLocation.lat, partnerLocation.lon) else null
            val myLatLng = myLocation?.let { LatLng(it.lat, it.lon) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION")
                cameraMoveTick
                val startPt = if (map != null && myLatLng != null) {
                    val p = map.projection.toScreenLocation(myLatLng)
                    Offset(p.x, p.y)
                } else {
                    Offset(size.width * 0.5f, size.height * 0.78f)
                }
                val endPt = if (map != null && partnerLatLng != null) {
                    val p = map.projection.toScreenLocation(partnerLatLng)
                    Offset(p.x, p.y)
                } else {
                    Offset(size.width * 0.5f, size.height * 0.36f)
                }

                val dx = endPt.x - startPt.x
                val dy = endPt.y - startPt.y
                val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val arcLift = (dist * 0.24f).coerceIn(48.dp.toPx(), 160.dp.toPx())
                val ctrlPt = Offset(
                    x = (startPt.x + endPt.x) * 0.5f,
                    y = minOf(startPt.y, endPt.y) - arcLift,
                )

                // Stage 1: Sender pin burst (0.0 -> 0.35)
                if (progress <= 0.35f) {
                    val p1 = (progress / 0.35f).coerceIn(0f, 1f)
                    drawCircle(
                        color = colors.action.copy(alpha = (1f - p1) * 0.55f),
                        radius = 12.dp.toPx() + p1 * 44.dp.toPx(),
                        center = startPt,
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                }

                // Stage 2: Curved Bezier trail + glowing pulse traveling from Me -> Partner (0.08 -> 0.78)
                val flightT = ((progress - 0.08f) / 0.70f).coerceIn(0f, 1f)
                if (progress >= 0.08f && progress <= 0.85f) {
                    val trailAlpha = if (progress <= 0.74f) 0.55f else ((0.85f - progress) / 0.11f).coerceIn(0f, 1f) * 0.55f
                    val path = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        val steps = 28
                        val maxStep = (steps * flightT).toInt().coerceAtLeast(1)
                        for (i in 1..maxStep) {
                            val t = (i.toFloat() / steps).coerceAtMost(flightT)
                            val omt = 1f - t
                            val px = omt * omt * startPt.x + 2f * omt * t * ctrlPt.x + t * t * endPt.x
                            val py = omt * omt * startPt.y + 2f * omt * t * ctrlPt.y + t * t * endPt.y
                            lineTo(px, py)
                        }
                    }
                    drawPath(
                        path = path,
                        color = colors.action.copy(alpha = trailAlpha),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                        ),
                    )
                }

                if (progress in 0.08f..0.80f) {
                    val omt = 1f - flightT
                    val orb = Offset(
                        x = omt * omt * startPt.x + 2f * omt * flightT * ctrlPt.x + flightT * flightT * endPt.x,
                        y = omt * omt * startPt.y + 2f * omt * flightT * ctrlPt.y + flightT * flightT * endPt.y,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.action.copy(alpha = 0.55f), Color.Transparent),
                            center = orb,
                            radius = 24.dp.toPx(),
                        ),
                        radius = 24.dp.toPx(),
                        center = orb,
                    )
                    drawCircle(
                        color = colors.action,
                        radius = 7.dp.toPx(),
                        center = orb,
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = orb,
                    )
                }

                // Stage 3: Receiver pin arrival bloom (0.70 -> 1.0)
                if (progress >= 0.70f) {
                    val p3 = ((progress - 0.70f) / 0.30f).coerceIn(0f, 1f)
                    drawCircle(
                        color = colors.action.copy(alpha = (1f - p3) * 0.75f),
                        radius = 16.dp.toPx() + p3 * 68.dp.toPx(),
                        center = endPt,
                        style = Stroke(width = (3.5f * (1f - p3 * 0.4f)).dp.toPx()),
                    )
                    val p3b = ((progress - 0.78f) / 0.22f).coerceIn(0f, 1f)
                    if (p3b > 0f) {
                        drawCircle(
                            color = colors.action.copy(alpha = (1f - p3b) * 0.55f),
                            radius = 14.dp.toPx() + p3b * 46.dp.toPx(),
                            center = endPt,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}
