package app.ours.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import app.ours.R
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursAvatar
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursInterlockedRingsMark
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursHeaderInsets
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentAlignment = Alignment.TopCenter,
    ) {
    LazyColumn(
        modifier = Modifier.oursContentWidth(layoutSpec),
        contentPadding = PaddingValues(
            start = layoutSpec.horizontalPadding,
            top = topHeaderInset + 84.dp,
            end = layoutSpec.horizontalPadding,
            bottom = 116.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(OursSpacing.md),
    ) {

        // 1. Couple Header Card
        item {
            OursCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(OursMotion.enterSpec()),
                padding = PaddingValues(OursSpacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OursSpacing.sm),
                ) {
                    val meName = state.me?.name?.ifBlank { null } ?: "You"
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
                            size = 52.dp,
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
                                    contentDescription = "Change avatar",
                                    tint = colors.onAction,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val coupleTitle = if (state.partner != null) {
                            "$meName & ${state.partner.name}"
                        } else {
                            meName
                        }
                        Text(
                            text = coupleTitle,
                            style = OursTypography.headline,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val meEmail = state.me?.email?.let { " · $it" } ?: ""
                        Text(
                            text = if (state.partner != null) {
                                "${strings.pairedWith} ${state.partner.name}$meEmail"
                            } else {
                                "${strings.signedInAs} $meName$meEmail"
                            },
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OursStatusChip(
                        label = if (state.partner != null) "Paired" else "Waiting",
                        tone = if (state.partner != null) StatusTone.Live else StatusTone.Muted,
                    )
                }

                if (state.partner == null) {
                    Spacer(Modifier.height(OursSpacing.sm))
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

        // 2. Appearance & Language Card
        item {
            OursCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(strings.themeTitle, style = OursTypography.title, color = colors.ink)
                    ThemePillToggle(
                        current = state.themeMode,
                        labels = Triple(strings.themeSystem, strings.themeLight, strings.themeDark),
                        onSelect = { vm.setThemeMode(it) },
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
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

        // 3. Location & Places Modular Card
        item {
            OursCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(OursMotion.enterSpec()),
                padding = PaddingValues(0.dp),
            ) {
                // Saved places row (expands inline)
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
                            if (state.places.isEmpty()) strings.noPlacesHint else "${state.places.size} saved",
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = if (placesExpanded) "⌄" else "›",
                        style = OursTypography.headline,
                        color = colors.inkMuted,
                    )
                }

                AnimatedVisibility(
                    visible = placesExpanded,
                    enter = fadeIn(OursMotion.enterSpec()) + expandVertically(OursMotion.enterSpec()),
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
                                            contentDescription = "Delete place",
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

                HorizontalDivider(thickness = 1.dp, color = colors.border)

                // Xiaomi / HyperOS guide row
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
                    Text(
                        text = "›",
                        style = OursTypography.headline,
                        color = colors.inkMuted,
                    )
                }
            }
        }

        // 4. Safety, Crash Detection & Emergency WhatsApp Contact
        item {
            OursCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(OursMotion.enterSpec()),
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
                    Row(
                        modifier = Modifier.weight(1f).padding(end = OursSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OursSpacing.sm),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_ours_safety_shield),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Column {
                            Text("Crash Detection", style = OursTypography.title, color = colors.ink)
                            Text(
                                "10s countdown + auto SOS & WhatsApp call on impact",
                                style = OursTypography.caption,
                                color = colors.inkSecondary,
                            )
                        }
                    }
                    Switch(
                        checked = state.crashDetectionEnabled,
                        onCheckedChange = { vm.setCrashDetectionEnabled(it) },
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border)

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
                        Text("Emergency WhatsApp Contact", style = OursTypography.title, color = colors.ink)
                        Text(
                            state.emergencyContactPhone.ifBlank { "Tap to set phone number" },
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = if (safetyExpanded) "⌄" else "›",
                        style = OursTypography.headline,
                        color = colors.inkMuted,
                    )
                }

                AnimatedVisibility(
                    visible = safetyExpanded,
                    enter = fadeIn(OursMotion.enterSpec()) + expandVertically(OursMotion.enterSpec()),
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
                            label = { Text("Contact name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = emergencyPhoneInput,
                            onValueChange = { emergencyPhoneInput = it },
                            label = { Text("WhatsApp phone (e.g. +62812...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                        ) {
                            OursButton(
                                text = "Pick contact",
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
                                text = "Save contact",
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
            }
        }

        // 5. Collapsible Server Row
        item {
            OursCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(OursMotion.enterSpec()),
                padding = PaddingValues(0.dp),
            ) {
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
                            state.serverUrl.ifBlank { "Not configured" },
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = if (serverExpanded) "⌄" else "›",
                        style = OursTypography.headline,
                        color = colors.inkMuted,
                    )
                }

                AnimatedVisibility(
                    visible = serverExpanded,
                    enter = fadeIn(OursMotion.enterSpec()) + expandVertically(OursMotion.enterSpec()),
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

        // 5. Destructive Red Sign Out Button
        item {
            OursButton(
                text = strings.signOutCta,
                variant = OursButtonVariant.Destructive,
                onClick = { showSignOutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

        // Fixed floating frosted top header aligned below HyperIsland
        Surface(
            shape = RoundedCornerShape(OursRadius.card),
            color = colors.glassSurfaceStrong,
            border = BorderStroke(1.dp, colors.glassBorder),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .oursContentWidth(layoutSpec)
                .oursHeaderInsets()
                .padding(horizontal = layoutSpec.horizontalPadding),
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
                    label = if (state.partner != null) {
                        if (strings.langCode == "id") "Terhubung" else "Paired"
                    } else {
                        strings.waitingForPartner
                    },
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
                "This signs out of this device and removes your local session. You can sign back in or re-pair anytime.",
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
                text = "Cancel",
                variant = OursButtonVariant.Secondary,
                onClick = { showSignOutConfirm = false },
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

@Composable
private fun ThemePillToggle(
    current: String,
    labels: Triple<String, String, String>,
    onSelect: (String) -> Unit,
) {
    val colors = OursTheme.colors
    val options = listOf(
        "system" to labels.first,
        "light" to labels.second,
        "dark" to labels.third,
    )
    Surface(
        shape = RoundedCornerShape(OursRadius.chip),
        color = colors.surfaceMuted,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEach { (mode, label) ->
                val selected = current == mode
                val interactionSource = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .oursPressScale(interactionSource)
                        .clip(RoundedCornerShape(OursRadius.chip))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(mode) },
                        ),
                    shape = RoundedCornerShape(OursRadius.chip),
                    color = if (selected) colors.action else colors.surfaceMuted,
                ) {
                    Text(
                        text = label,
                        style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selected) colors.onAction else colors.inkSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}
