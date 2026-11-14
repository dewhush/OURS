package app.ours.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ours.R
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursAvatar
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursGlassBackdrop
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursInterlockedRingsMark
import app.ours.ui.components.OursSegmentedControl
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursHeaderInsets
import app.ours.ui.components.oursLiquidGlass
import app.ours.ui.components.rememberOursLayoutSpec
import app.ours.ui.theme.OursMotion
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
    val layoutSpec = rememberOursLayoutSpec()
    val context = LocalContext.current
    var serverInput by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }
    var serverExpanded by remember { mutableStateOf(false) }
    var placesExpanded by remember { mutableStateOf(false) }
    var showAddPlace by remember { mutableStateOf(false) }
    var showXiaomiGuide by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var safetyExpanded by remember { mutableStateOf(false) }
    var emergencyNameInput by remember(state.emergencyContactName) { mutableStateOf(state.emergencyContactName) }
    var emergencyPhoneInput by remember(state.emergencyContactPhone) { mutableStateOf(state.emergencyContactPhone) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vm.uploadAvatar(uri)
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                        val phone = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                        if (name.isNotBlank()) emergencyNameInput = name
                        if (phone.isNotBlank()) emergencyPhoneInput = phone
                    }
                }
            }
        }
    }

    val topHeaderInset = WindowInsets.statusBars.union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateTopPadding()

    OursGlassBackdrop {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.oursContentWidth(layoutSpec),
                contentPadding = PaddingValues(
                    start = layoutSpec.horizontalPadding,
                    top = topHeaderInset + 96.dp,
                    end = layoutSpec.horizontalPadding,
                    bottom = 148.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(OursSpacing.sm),
            ) {
                item {
                    OursCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(OursMotion.springFluid()),
                        padding = PaddingValues(OursSpacing.md),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OursSpacing.sm),
                        ) {
                            val meName = state.me?.name?.ifBlank { null } ?: strings.youFallback
                            Box(
                                modifier = Modifier.clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentAlignment = Alignment.BottomEnd,
                            ) {
                                OursAvatar(
                                    name = meName,
                                    photoUrl = state.myAvatarUrl,
                                    size = 54.dp,
                                    ringColor = colors.action,
                                    accent = true,
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = colors.action,
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = OursIcons.Camera,
                                            contentDescription = strings.takePhoto,
                                            tint = colors.onAction,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                val coupleTitle = if (state.partner != null) {
                                    "$meName & ${state.partner.name}"
                                } else {
                                    meName
                                }
                                Text(
                                    text = coupleTitle,
                                    style = OursTypography.headline,
                                    color = colors.ink,
                                )
                                Text(
                                    text = if (state.partner != null) {
                                        "${strings.pairedWith} ${state.partner.name}"
                                    } else {
                                        "${strings.signedInAs} $meName"
                                    },
                                    style = OursTypography.caption,
                                    color = colors.inkSecondary,
                                )
                                if (!state.me?.email.isNullOrBlank()) {
                                    Text(
                                        text = state.me!!.email!!,
                                        style = OursTypography.caption,
                                        color = colors.inkMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        if (state.partner == null) {
                            Spacer(Modifier.height(OursSpacing.sm))
                            if (!state.inviteCode.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(OursRadius.chip),
                                    color = colors.actionSoft,
                                ) {
                                    Text(
                                        "${strings.inviteCodeLabel}: ${state.inviteCode}",
                                        style = OursTypography.tabular,
                                        color = colors.action,
                                        modifier = Modifier.padding(horizontal = OursSpacing.sm, vertical = 6.dp),
                                    )
                                }
                                Spacer(Modifier.height(OursSpacing.xs))
                            }
                            OursButton(
                                text = strings.generateInvite,
                                variant = OursButtonVariant.Secondary,
                                onClick = { vm.refreshInviteCode() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader(strings.sectionAppearance)
                    OursCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(0.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                        ) {
                            Text(strings.themeTitle, style = OursTypography.title, color = colors.ink)
                            ThemePillToggle(
                                current = state.themeMode,
                                labels = Triple(strings.themeSystem, strings.themeLight, strings.themeDark),
                                onSelect = { vm.setThemeMode(it) },
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(strings.languageTitle, style = OursTypography.title, color = colors.ink)
                            LanguagePillToggle(
                                current = state.language,
                                onSelect = { vm.setLanguage(it) },
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader(strings.sectionLocationPrivacy)
                    OursCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(OursMotion.springFluid()),
                        padding = PaddingValues(0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 60.dp)
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = OursSpacing.sm)) {
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
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.action,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = colors.surfaceMuted,
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                        val placesChevronDeg by animateFloatAsState(
                            targetValue = if (placesExpanded) 90f else 0f,
                            animationSpec = OursMotion.springSnappy(),
                            label = "places-chevron",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable { placesExpanded = !placesExpanded }
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.savedPlacesTitle, style = OursTypography.title, color = colors.ink)
                                Text(
                                    if (state.places.isEmpty()) strings.noPlacesHint else "${state.places.size} ${strings.savedCountSuffix}",
                                    style = OursTypography.caption,
                                    color = colors.inkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = OursIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = placesChevronDeg },
                            )
                        }

                        AnimatedVisibility(
                            visible = placesExpanded,
                            enter = fadeIn(OursMotion.enterSpec()) + expandVertically(OursMotion.springFluid()),
                            exit = fadeOut(OursMotion.exitSpec()) + shrinkVertically(OursMotion.exitSpec()),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = OursSpacing.md, end = OursSpacing.md, bottom = OursSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                            ) {
                                state.places.forEach { place ->
                                    Surface(
                                        shape = RoundedCornerShape(OursRadius.inner),
                                        color = colors.surfaceMuted,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = OursSpacing.sm, vertical = OursSpacing.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    place.name,
                                                    style = OursTypography.body.copy(fontWeight = FontWeight.Medium),
                                                    color = colors.ink,
                                                )
                                                Text(
                                                    "${place.radiusM.toInt()} m · ${String.format("%.4f, %.4f", place.lat, place.lon)}",
                                                    style = OursTypography.tabular,
                                                    color = colors.inkSecondary,
                                                )
                                            }
                                            IconButton(onClick = { vm.deletePlace(place.id) }) {
                                                Icon(
                                                    imageVector = OursIcons.Trash,
                                                    contentDescription = strings.deleteCheckInLabel,
                                                    tint = colors.danger,
                                                )
                                            }
                                        }
                                    }
                                }
                                OursButton(
                                    text = strings.addPlace,
                                    variant = OursButtonVariant.Secondary,
                                    onClick = { showAddPlace = true },
                                    leadingIcon = {
                                        Icon(imageVector = OursIcons.Plus, contentDescription = null)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable { showXiaomiGuide = true }
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = OursSpacing.sm)) {
                                Text(strings.backgroundReliability, style = OursTypography.title, color = colors.ink)
                                Text(
                                    strings.openXiaomiGuide,
                                    style = OursTypography.caption,
                                    color = colors.inkSecondary,
                                )
                            }
                            Icon(
                                imageVector = OursIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader(strings.sectionSafetySos)
                    OursCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(OursMotion.springFluid()),
                        padding = PaddingValues(0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 60.dp)
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = OursSpacing.sm)) {
                                Text(strings.crashDetectionTitle, style = OursTypography.title, color = colors.ink)
                                Text(
                                    strings.crashDetectionSubtitle,
                                    style = OursTypography.caption,
                                    color = colors.inkSecondary,
                                )
                            }
                            Switch(
                                checked = state.crashDetectionEnabled,
                                onCheckedChange = { vm.setCrashDetectionEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.action,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = colors.surfaceMuted,
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                        val safetyChevronDeg by animateFloatAsState(
                            targetValue = if (safetyExpanded) 90f else 0f,
                            animationSpec = OursMotion.springSnappy(),
                            label = "safety-chevron",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable { safetyExpanded = !safetyExpanded }
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = OursSpacing.sm)) {
                                Text(strings.emergencyWhatsappTitle, style = OursTypography.title, color = colors.ink)
                                Text(
                                    state.emergencyContactPhone.ifBlank { strings.tapToSetPhone },
                                    style = OursTypography.caption,
                                    color = colors.inkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = OursIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = safetyChevronDeg },
                            )
                        }

                        AnimatedVisibility(
                            visible = safetyExpanded,
                            enter = fadeIn(OursMotion.enterSpec()) + expandVertically(OursMotion.springFluid()),
                            exit = fadeOut(OursMotion.exitSpec()) + shrinkVertically(OursMotion.exitSpec()),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = OursSpacing.md, end = OursSpacing.md, bottom = OursSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                            ) {
                                OutlinedTextField(
                                    value = emergencyNameInput,
                                    onValueChange = { emergencyNameInput = it },
                                    label = { Text(strings.contactNameLabel) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = emergencyPhoneInput,
                                    onValueChange = { emergencyPhoneInput = it },
                                    label = { Text(strings.whatsappPhoneLabel) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                                ) {
                                    OursButton(
                                        text = strings.pickFromContacts,
                                        variant = OursButtonVariant.Secondary,
                                        onClick = {
                                            val phoneIntent = Intent(Intent.ACTION_PICK).apply {
                                                type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                                            }
                                            runCatching {
                                                contactPickerLauncher.launch(phoneIntent)
                                            }.onFailure {
                                                val fallbackIntent = Intent(
                                                    Intent.ACTION_PICK,
                                                    ContactsContract.Contacts.CONTENT_URI,
                                                )
                                                runCatching { contactPickerLauncher.launch(fallbackIntent) }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    OursButton(
                                        text = strings.saveContactCta,
                                        enabled = emergencyPhoneInput.isNotBlank(),
                                        onClick = {
                                            vm.setEmergencyContact(emergencyNameInput, emergencyPhoneInput)
                                            safetyExpanded = false
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = colors.border)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                                .clickable { vm.startSosCountdown("manual") }
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = strings.triggerEmergencySos,
                                style = OursTypography.title,
                                color = colors.danger,
                            )
                            Icon(
                                imageVector = OursIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.danger,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader(strings.sectionSystem)
                    OursCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(OursMotion.springFluid()),
                        padding = PaddingValues(0.dp),
                    ) {
                        val serverChevronDeg by animateFloatAsState(
                            targetValue = if (serverExpanded) 90f else 0f,
                            animationSpec = OursMotion.springSnappy(),
                            label = "server-chevron",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable { serverExpanded = !serverExpanded }
                                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = OursSpacing.sm)) {
                                Text(strings.serverUrlTitle, style = OursTypography.title, color = colors.ink)
                                Text(
                                    state.serverUrl.ifBlank { strings.notConfigured },
                                    style = OursTypography.caption,
                                    color = colors.inkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = OursIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = serverChevronDeg },
                            )
                        }

                        AnimatedVisibility(
                            visible = serverExpanded,
                            enter = fadeIn(OursMotion.enterSpec()) + expandVertically(OursMotion.springFluid()),
                            exit = fadeOut(OursMotion.exitSpec()) + shrinkVertically(OursMotion.exitSpec()),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = OursSpacing.md, end = OursSpacing.md, bottom = OursSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                            ) {
                                OutlinedTextField(
                                    value = serverInput,
                                    onValueChange = { serverInput = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OursButton(
                                    text = strings.saveServerUrl,
                                    variant = OursButtonVariant.Secondary,
                                    enabled = serverInput.isNotBlank() && serverInput != state.serverUrl,
                                    onClick = {
                                        vm.updateServerUrl(serverInput)
                                        serverExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = strings.signOutCta,
                        variant = OursButtonVariant.Destructive,
                        onClick = { showSignOutConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(OursSpacing.lg))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topHeaderInset + 100.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to colors.canvas,
                            0.78f to colors.canvas,
                            0.92f to colors.canvas.copy(alpha = 0.85f),
                            1.0f to Color.Transparent,
                        ),
                    ),
            )

            Surface(
                shape = RoundedCornerShape(OursRadius.card),
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .oursContentWidth(layoutSpec)
                    .oursHeaderInsets()
                    .padding(horizontal = layoutSpec.horizontalPadding)
                    .oursLiquidGlass(radius = OursRadius.card, colors = colors, elevation = 6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = strings.settingsTitle,
                        style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                    )
                    OursStatusChip(
                        label = if (state.partner != null) strings.pairedBadge else strings.waitingForPartner,
                        tone = if (state.partner != null) StatusTone.Live else StatusTone.Muted,
                    )
                }
            }
        }

        if (showSignOutConfirm) {
            OursBottomSheet(onDismiss = { showSignOutConfirm = false }) {
                Text(strings.signOutCta, style = OursTypography.headline, color = colors.danger)
                Spacer(Modifier.height(OursSpacing.xs))
                Text(
                    strings.signOutConfirmDetail,
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )
                Spacer(Modifier.height(OursSpacing.md))
                OursButton(
                    text = strings.signOutCta,
                    variant = OursButtonVariant.Destructive,
                    onClick = {
                        showSignOutConfirm = false
                        vm.signOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(OursSpacing.xs))
                OursButton(
                    text = strings.cancelLabel,
                    variant = OursButtonVariant.Secondary,
                    onClick = { showSignOutConfirm = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (showXiaomiGuide) {
            OursBottomSheet(onDismiss = { showXiaomiGuide = false }) {
                Text(strings.xiaomiGuideSheetTitle, style = OursTypography.headline, color = colors.ink)
                Spacer(Modifier.height(OursSpacing.sm))
                XiaomiStepsCard()
                Spacer(Modifier.height(OursSpacing.md))
                OursButton(
                    text = strings.doneLabel,
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
                Text(strings.addPlaceSheetTitle, style = OursTypography.headline, color = colors.ink)
                Spacer(Modifier.height(OursSpacing.sm))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.placeNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(OursSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text(strings.latitudeLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text(strings.longitudeLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(OursSpacing.sm))
                Text(
                    "${strings.radiusMetersLabel}: ${radius.roundToInt()} m",
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
                    text = strings.savePlaceCta,
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
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = OursTheme.colors
    Text(
        text = title,
        style = OursTypography.label.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        ),
        color = colors.inkMuted,
        modifier = Modifier.padding(start = OursSpacing.xs, bottom = 6.dp, top = 4.dp),
    )
}

@Composable
private fun ThemePillToggle(
    current: String,
    labels: Triple<String, String, String>,
    onSelect: (String) -> Unit,
) {
    OursSegmentedControl(
        options = listOf(
            "system" to labels.first,
            "light" to labels.second,
            "dark" to labels.third,
        ),
        selected = current,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
    )
}
