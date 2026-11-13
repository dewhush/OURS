package app.ours.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.theme.OursRadius
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
    val strings = OursTheme.strings
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
            Text(strings.settingsTitle, style = OursTypography.display, color = colors.ink)
        }

        // Notion-style structured property block: Pair + Language + Quiet Pause
        item {
            OursCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(0.dp),
            ) {
                Column(modifier = Modifier.padding(OursSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(strings.pairCardTitle, style = OursTypography.title, color = colors.ink)
                        OursStatusChip(
                            label = if (state.partner != null) state.partner.name else "Waiting",
                            tone = if (state.partner != null) StatusTone.Live else StatusTone.Muted,
                        )
                    }
                    Spacer(Modifier.height(OursSpacing.xs))
                    val meEmail = state.me?.email?.let { " ($it)" } ?: ""
                    Text(
                        "${strings.signedInAs} ${state.me?.name ?: "You"}$meEmail",
                        style = OursTypography.body,
                        color = colors.inkSecondary,
                    )
                    if (state.partner != null) {
                        val pEmail = state.partner.email?.let { " ($it)" } ?: ""
                        Text(
                            "${strings.pairedWith} ${state.partner.name}$pEmail",
                            style = OursTypography.body,
                            color = colors.live,
                        )
                    } else {
                        Spacer(Modifier.height(OursSpacing.xs))
                        if (!state.inviteCode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(OursRadius.chip),
                                color = colors.actionSoft,
                                border = BorderStroke(1.dp, colors.action.copy(alpha = 0.25f)),
                            ) {
                                Text(
                                    "Invite code: ${state.inviteCode}",
                                    style = OursTypography.tabular,
                                    color = colors.action,
                                    modifier = Modifier.padding(horizontal = OursSpacing.sm, vertical = 4.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(OursSpacing.xs))
                        OursButton(
                            text = strings.generateInvite,
                            variant = OursButtonVariant.Secondary,
                            onClick = { vm.refreshInviteCode() },
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OursSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(strings.languageTitle, style = OursTypography.title, color = colors.ink)
                    LanguagePillToggle(
                        current = state.language,
                        onSelect = { vm.setLanguage(it) },
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OursSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.pauseSharingTitle, style = OursTypography.title, color = colors.ink)
                        Text(
                            strings.pauseSharingSubtitle,
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
                Text(strings.savedPlacesTitle, style = OursTypography.headline, color = colors.ink)
                OursButton(
                    text = strings.addPlace,
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
                        strings.noPlacesHint,
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
                Text(strings.backgroundReliability, style = OursTypography.title, color = colors.ink)
                Text(
                    strings.xiaomiChecklistHint,
                    style = OursTypography.caption,
                    color = colors.inkSecondary,
                )
                Spacer(Modifier.height(OursSpacing.sm))
                OursButton(
                    text = strings.openXiaomiGuide,
                    variant = OursButtonVariant.Secondary,
                    onClick = { showXiaomiGuide = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Server URL
        item {
            OursCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.serverUrlTitle, style = OursTypography.title, color = colors.ink)
                Spacer(Modifier.height(OursSpacing.xs))
                OutlinedTextField(
                    value = serverInput,
                    onValueChange = { serverInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(OursSpacing.xs))
                OursButton(
                    text = strings.saveServerUrl,
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
                text = strings.signOutCta,
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
