package app.ours.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.ours.location.AdaptiveLocationPolicy
import app.ours.ui.theme.OursMotion
import app.ours.ui.theme.OursRadius
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursAvatar
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursMapView
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.rememberOursLayoutSpec
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import app.ours.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    state: OursUiState,
    vm: OursViewModel,
    onQuickCheckIn: () -> Unit,
) {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val layoutSpec = rememberOursLayoutSpec()
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var showPartnerSheet by remember { mutableStateOf(false) }
    var nudgePulseKey by remember { mutableIntStateOf(0) }
    val nudgeRingScale = remember { Animatable(1f) }
    val nudgeRingAlpha = remember { Animatable(0f) }

    LaunchedEffect(nudgePulseKey) {
        if (nudgePulseKey > 0) {
            nudgeRingScale.snapTo(1f)
            nudgeRingAlpha.snapTo(0.75f)
            nudgeRingScale.animateTo(1.22f, OursMotion.enterSpec(240))
        }
    }
    LaunchedEffect(nudgePulseKey) {
        if (nudgePulseKey > 0) {
            nudgeRingAlpha.animateTo(0f, OursMotion.enterSpec(240))
        }
    }

    val context = LocalContext.current
    val partnerName = state.partner?.name ?: "Partner"
    val loc = state.partnerLocation
    val isPaused = state.partnerPaused || loc?.paused == true
    val hasCoords = !isPaused && loc?.lat != null && loc.lon != null
    val nowMs = System.currentTimeMillis()
    val isStale = loc?.recordedAt?.let { nowMs - it > 15 * 60_000L } ?: true
    val myLastCoords = remember(loc?.recordedAt, recenterTrigger) { readLastKnownCoords(context) }
    val distanceText = remember(loc?.lat, loc?.lon, myLastCoords, strings.awaySuffix) {
        if (hasCoords && loc != null && myLastCoords != null) {
            val meters = AdaptiveLocationPolicy.distanceMeters(
                myLastCoords.first,
                myLastCoords.second,
                loc.lat!!,
                loc.lon!!,
            )
            if (meters < 1000.0) {
                "${meters.toInt()} m ${strings.awaySuffix}"
            } else {
                String.format("%.1f km %s", meters / 1000.0, strings.awaySuffix)
            }
        } else null
    }

    val statusTone = when {
        state.partner == null -> StatusTone.Muted
        isPaused -> StatusTone.Paused
        hasCoords && !isStale -> StatusTone.Live
        else -> StatusTone.Muted
    }
    val statusLabel = when {
        state.partner == null -> strings.waitingForPartner
        isPaused -> strings.sharingPaused
        hasCoords && !isStale -> strings.liveStatus
        loc?.recordedAt != null -> Formatters.relativeTime(loc.recordedAt, nowMs)
        else -> strings.noFixYet
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OursMapView(
            modifier = Modifier.fillMaxSize(),
            partnerLocation = loc,
            partnerName = partnerName,
            places = state.places,
            highlights = state.memories,
            recenterTrigger = recenterTrigger,
        )

        // Top floating partner status card + optional Pause Sharing reminder banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .oursContentWidth(layoutSpec)
                .statusBarsPadding()
                .padding(horizontal = layoutSpec.horizontalPadding, vertical = OursSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
        ) {
        OursCard(
            modifier = Modifier.fillMaxWidth(),
            frosted = true,
            onClick = { showPartnerSheet = true },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OursSpacing.sm),
            ) {
                OursAvatar(
                    name = partnerName,
                    ringColor = if (statusTone == StatusTone.Live) colors.live else null,
                    accent = true,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                    ) {
                        Text(
                            text = partnerName,
                            style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ink,
                        )
                        OursStatusChip(label = statusLabel, tone = statusTone)
                    }
                    Spacer(Modifier.height(2.dp))
                    if (isPaused) {
                        Text(
                            text = strings.locationHiddenPaused,
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                        )
                    } else if (hasCoords && loc != null) {
                        val batteryText = loc.batteryPct?.let { pct ->
                            if (loc.charging == true) "$pct% charging" else "$pct%"
                        } ?: "--%"
                        val speedText = Formatters.speedKmh(loc.speedMps)
                        val activityPrefix = loc.activity
                            ?.takeIf { it.isNotBlank() && it != "still" }
                            ?.replaceFirstChar { it.uppercase() }
                            ?.let { "$it · " } ?: ""
                        val updatedText = loc.recordedAt?.let { Formatters.relativeTime(it, nowMs) } ?: "--"
                        val distPart = distanceText?.let { "  |  $it" } ?: ""
                        Text(
                            text = "$activityPrefix$speedText  |  $batteryText$distPart  |  $updatedText",
                            style = OursTypography.tabular,
                            color = colors.inkSecondary,
                        )
                    } else if (state.partner == null && !state.inviteCode.isNullOrBlank()) {
                        Text(
                            text = "Invite code: ${state.inviteCode}",
                            style = OursTypography.tabular,
                            color = colors.action,
                        )
                    } else {
                        Text(
                            text = loc?.lastSeenAt?.let { "Last seen ${Formatters.relativeTime(it, nowMs)}" }
                                ?: strings.waitingFirstLocation,
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                        )
                    }
                }
            }
        }

        if (state.sharingPaused) {
            OursCard(
                modifier = Modifier.fillMaxWidth(),
                frosted = true,
                onClick = { vm.toggleSharingPaused(false) },
            ) {
                Text(
                    text = strings.sharingPausedBanner,
                    style = OursTypography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.ink,
                )
            }
        }
        }

        // Bottom floating actions: Check-in CTA + Nudge + Recenter
        FlowRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .oursContentWidth(layoutSpec)
                .padding(horizontal = layoutSpec.horizontalPadding, vertical = OursSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
        ) {
            OursButton(
                text = strings.checkIn,
                onClick = onQuickCheckIn,
                leadingIcon = {
                    Icon(
                        imageVector = OursIcons.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.weight(1f, fill = true),
            )
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = nudgeRingScale.value
                            scaleY = nudgeRingScale.value
                            alpha = nudgeRingAlpha.value
                        }
                        .border(2.dp, colors.action, RoundedCornerShape(OursRadius.control)),
                )
                OursButton(
                    text = strings.nudge,
                    variant = OursButtonVariant.Secondary,
                    onClick = {
                        nudgePulseKey++
                        vm.sendNudge()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = OursIcons.Heart,
                            contentDescription = strings.nudge,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
            OursButton(
                text = strings.center,
                variant = OursButtonVariant.Secondary,
                onClick = {
                    recenterTrigger++
                    vm.refreshPartnerLocation()
                },
                leadingIcon = {
                    Icon(
                        imageVector = OursIcons.Crosshair,
                        contentDescription = strings.center,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }

    if (showPartnerSheet) {
        OursBottomSheet(onDismiss = { showPartnerSheet = false }) {
            Text(partnerName, style = OursTypography.headline, color = colors.ink)
            Spacer(Modifier.height(OursSpacing.xs))
            OursStatusChip(label = statusLabel, tone = statusTone)
            Spacer(Modifier.height(OursSpacing.md))
            if (loc != null && hasCoords) {
                if (distanceText != null) {
                    DetailRow(strings.distanceLabel, distanceText)
                }
                DetailRow("Speed", Formatters.speedKmh(loc.speedMps))
                DetailRow("Battery", loc.batteryPct?.let { "$it%${if (loc.charging == true) " (charging)" else ""}" } ?: "--")
                DetailRow("Accuracy", loc.accuracyM?.let { "${it.toInt()} m" } ?: "--")
                DetailRow("Last fix", loc.recordedAt?.let { Formatters.dateShort(it) } ?: "--")
            } else {
                Text(
                    if (isPaused) "$partnerName paused sharing." else "No coordinates available right now.",
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )
            }
            Spacer(Modifier.height(OursSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            ) {
                if (loc != null && hasCoords) {
                    OursButton(
                        text = strings.directionsCta,
                        variant = OursButtonVariant.Secondary,
                        onClick = {
                            val uri = Uri.parse("geo:${loc.lat},${loc.lon}?q=${loc.lat},${loc.lon}(${Uri.encode(partnerName)})")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            showPartnerSheet = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                OursButton(
                    text = strings.sendNudgeCta,
                    onClick = {
                        vm.sendNudge()
                        showPartnerSheet = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = OursTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OursSpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = OursTypography.body, color = colors.inkSecondary)
        Text(value, style = OursTypography.tabularLarge, color = colors.ink)
    }
}

@SuppressLint("MissingPermission")
private fun readLastKnownCoords(context: Context): Pair<Double, Double>? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = buildList {
        if (Build.VERSION.SDK_INT >= 31 && lm.allProviders.contains(LocationManager.FUSED_PROVIDER)) {
            add(LocationManager.FUSED_PROVIDER)
        }
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }
    val best = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time } ?: return null
    return best.latitude to best.longitude
}
