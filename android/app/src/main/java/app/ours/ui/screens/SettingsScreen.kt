package app.ours.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursIcons
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: OursUiState,
    vm: OursViewModel,
) {
    val colors = OursTheme.colors
    var serverInput by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }
    var showAddPlace by remember { mutableStateOf(false) }
    var showXiaomiGuide by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(OursSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OursSpacing.md),
    ) {
        item {
            Text("Settings", style = OursTypography.display, color = colors.ink)
        }

        // Pairing info
        item {
            OursCard(modifier = Modifier.fillMaxWidth()) {
                Text("Pair", style = OursTypography.title, color = colors.ink)
                Spacer(Modifier.height(OursSpacing.xs))
                Text(
                    "Signed in as ${state.me?.name ?: "You"}",
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )
                if (state.partner != null) {
                    Text(
                        "Paired with ${state.partner.name}",
                        style = OursTypography.body,
                        color = colors.live,
                    )
                } else {
                    Spacer(Modifier.height(OursSpacing.xs))
                    if (!state.inviteCode.isNullOrBlank()) {
                        Text(
                            "Invite code: ${state.inviteCode}",
                            style = OursTypography.tabularLarge,
                            color = colors.action,
                        )
                    }
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = "Generate new invite code",
                        variant = OursButtonVariant.Secondary,
                        onClick = { vm.refreshInviteCode() },
                    )
                }
            }
        }

        // Quiet pause toggle
        item {
            OursCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pause location sharing", style = OursTypography.title, color = colors.ink)
                        Text(
                            "Quietly hides your coordinates until you turn sharing back on.",
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                        )
                    }
                    Switch(
                        checked = state.sharingPaused,
                        onCheckedChange = { vm.toggleSharingPaused(it) },
                    )
                }
            }
        }

        // Saved places header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Saved places", style = OursTypography.headline, color = colors.ink)
                OursButton(
                    text = "Add place",
                    variant = OursButtonVariant.Secondary,
                    onClick = { showAddPlace = true },
                    leadingIcon = {
                        Icon(imageVector = OursIcons.Plus, contentDescription = null)
                    },
                )
            }
        }

        if (state.places.isEmpty()) {
            item {
                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No saved places yet. Add Home or Work to get arrival and departure alerts.",
                        style = OursTypography.caption,
                        color = colors.inkSecondary,
                    )
                }
            }
        } else {
            items(state.places, key = { it.id }) { place ->
                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                place.name,
                                style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.ink,
                            )
                            Text(
                                "${place.radiusM.toInt()} m radius  |  ${String.format("%.4f, %.4f", place.lat, place.lon)}",
                                style = OursTypography.tabular,
                                color = colors.inkSecondary,
                            )
                        }
                        IconButton(onClick = { vm.deletePlace(place.id) }) {
                            Icon(
                                imageVector = OursIcons.Trash,
                                contentDescription = "Delete place",
                                tint = colors.error,
                            )
                        }
                    }
                }
            }
        }

        // Xiaomi / HyperOS guide
        item {
            OursCard(modifier = Modifier.fillMaxWidth()) {
                Text("Background reliability", style = OursTypography.title, color = colors.ink)
                Text(
                    "Xiaomi / Poco HyperOS checklist to keep live location running.",
                    style = OursTypography.caption,
                    color = colors.inkSecondary,
                )
                Spacer(Modifier.height(OursSpacing.sm))
                OursButton(
                    text = "Open Xiaomi setup guide",
                    variant = OursButtonVariant.Secondary,
                    onClick = { showXiaomiGuide = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Server URL
        item {
            OursCard(modifier = Modifier.fillMaxWidth()) {
                Text("Server URL", style = OursTypography.title, color = colors.ink)
                Spacer(Modifier.height(OursSpacing.xs))
                OutlinedTextField(
                    value = serverInput,
                    onValueChange = { serverInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(OursSpacing.xs))
                OursButton(
                    text = "Save server URL",
                    variant = OursButtonVariant.Secondary,
                    enabled = serverInput.isNotBlank() && serverInput != state.serverUrl,
                    onClick = { vm.updateServerUrl(serverInput) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Sign out
        item {
            OursButton(
                text = "Sign out & unpair this phone",
                variant = OursButtonVariant.Destructive,
                onClick = { vm.signOut() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showXiaomiGuide) {
        OursBottomSheet(onDismiss = { showXiaomiGuide = false }) {
            Text("Xiaomi / HyperOS guide", style = OursTypography.headline, color = colors.ink)
            Spacer(Modifier.height(OursSpacing.sm))
            XiaomiStepsCard()
            Spacer(Modifier.height(OursSpacing.md))
            OursButton(
                text = "Done",
                onClick = { showXiaomiGuide = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showAddPlace) {
        var name by remember { mutableStateOf("") }
        var latText by remember {
            mutableStateOf(state.partnerLocation?.lat?.toString() ?: "-6.2000")
        }
        var lonText by remember {
            mutableStateOf(state.partnerLocation?.lon?.toString() ?: "106.8167")
        }
        var radius by remember { mutableFloatStateOf(100f) }

        OursBottomSheet(onDismiss = { showAddPlace = false }) {
            Text("Add saved place", style = OursTypography.headline, color = colors.ink)
            Spacer(Modifier.height(OursSpacing.sm))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Place name (e.g. Home)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OursSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(OursSpacing.sm))
            Text(
                "Radius: ${radius.roundToInt()} m",
                style = OursTypography.tabular,
                color = colors.inkSecondary,
            )
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 20f..1000f,
            )
            Spacer(Modifier.height(OursSpacing.sm))
            OursButton(
                text = "Save place",
                enabled = name.isNotBlank() && latText.toDoubleOrNull() != null && lonText.toDoubleOrNull() != null,
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: return@OursButton
                    val lon = lonText.toDoubleOrNull() ?: return@OursButton
                    vm.addPlace(name, lat, lon, radius.roundToInt().toDouble())
                    showAddPlace = false
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
