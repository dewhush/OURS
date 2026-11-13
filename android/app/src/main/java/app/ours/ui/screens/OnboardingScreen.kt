package app.ours.ui.screens

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursInterlockedRingsMark
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.components.oursPressScale
import app.ours.ui.theme.OursMotion
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import app.ours.util.XiaomiGuideHelper
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    state: OursUiState,
    vm: OursViewModel,
) {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember(state.isPaired) {
        mutableIntStateOf(
            when {
                !state.isPaired -> 0
                else -> 2
            }
        )
    }

    var gmailAddress by remember { mutableStateOf(state.me?.email ?: "") }
    var serverUrl by remember { mutableStateOf(state.serverUrl.ifBlank { "http://192.168.100.92:8080" }) }
    var serverStatus by remember { mutableStateOf<String?>(null) }
    var checkingServer by remember { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }

    var pairModeCreate by remember { mutableStateOf(true) }
    var myName by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }

    val googlePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!picked.isNullOrBlank()) {
                gmailAddress = picked
                if (myName.isBlank()) {
                    myName = picked.substringBefore("@").replaceFirstChar { it.uppercase() }
                }
            }
        }
    }

    var fineGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var bgGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var notifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var batteryExempt by remember {
        mutableStateOf(XiaomiGuideHelper.isIgnoringBatteryOptimizations(context))
    }

    val fineLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        bgGranted = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(OursSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OursSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            ) {
                OursInterlockedRingsMark(
                    modifier = Modifier.size(28.dp),
                    leftRingColor = colors.ink,
                    rightRingColor = colors.inkMuted,
                    backgroundColor = colors.canvas,
                )
                Text(
                    text = "OURS",
                    style = OursTypography.label.copy(fontWeight = FontWeight.Bold),
                    color = colors.action,
                )
            }
            LanguagePillToggle(
                current = state.language,
                onSelect = { vm.setLanguage(it) },
            )
        }

        OnboardingProgressBar(step = step, totalSteps = 4)

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState >= initialState
                (fadeIn(OursMotion.enterSpec()) + slideInHorizontally(OursMotion.enterSpec()) { w ->
                    if (forward) w / 6 else -w / 6
                }).togetherWith(
                    fadeOut(OursMotion.exitSpec()) + slideOutHorizontally(OursMotion.exitSpec()) { w ->
                        if (forward) -w / 6 else w / 6
                    }
                )
            },
            label = "onboardingStep",
        ) { currentStep ->
            Column(verticalArrangement = Arrangement.spacedBy(OursSpacing.md)) {
                when (currentStep) {
                    0 -> {
                Surface(
                    shape = RoundedCornerShape(OursRadius.chip),
                    color = colors.actionSoft,
                ) {
                    Text(
                        text = "OURS · PRIVATE FOR TWO",
                        style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                        modifier = Modifier.padding(horizontal = OursSpacing.sm, vertical = 4.dp),
                    )
                }

                Text(strings.connectGmailTitle, style = OursTypography.display, color = colors.ink)
                Text(
                    strings.connectGmailSubtitle,
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )

                OursCard(modifier = Modifier.fillMaxWidth()) {
                    OursButton(
                        text = strings.continueWithGoogle,
                        variant = OursButtonVariant.Secondary,
                        onClick = {
                            @Suppress("DEPRECATION")
                            val intent = AccountManager.newChooseAccountIntent(
                                null,
                                null,
                                arrayOf("com.google"),
                                null,
                                null,
                                null,
                                null,
                            )
                            runCatching { googlePickerLauncher.launch(intent) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(OursSpacing.sm))

                    OutlinedTextField(
                        value = gmailAddress,
                        onValueChange = { gmailAddress = it },
                        label = { Text(strings.gmailAddressLabel) },
                        placeholder = { Text("you@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(OursSpacing.md))

                    OursButton(
                        text = strings.continueLabel,
                        onClick = {
                            if (myName.isBlank() && gmailAddress.contains("@")) {
                                myName = gmailAddress.substringBefore("@").replaceFirstChar { it.uppercase() }
                            }
                            step = 1
                        },
                        enabled = gmailAddress.contains("@"),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Text(
                    text = strings.serverSettingsLink,
                    style = OursTypography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.inkMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(OursRadius.chip))
                        .clickable { showServerSheet = true }
                        .padding(vertical = OursSpacing.xxs),
                )
            }

            1 -> {
                Text(strings.pairTitle, style = OursTypography.display, color = colors.ink)
                Text(
                    strings.pairSubtitle,
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
                    OursButton(
                        text = strings.createPair,
                        variant = if (pairModeCreate) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                        onClick = { pairModeCreate = true },
                        modifier = Modifier.weight(1f),
                    )
                    OursButton(
                        text = strings.joinWithCode,
                        variant = if (!pairModeCreate) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                        onClick = { pairModeCreate = false },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = myName,
                    onValueChange = { myName = it },
                    label = { Text(strings.yourName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!pairModeCreate) {
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it.uppercase() },
                        label = { Text(strings.inviteCodeLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.errorMessage != null) {
                    Text(state.errorMessage, style = OursTypography.caption, color = colors.error)
                }

                OursButton(
                    text = if (pairModeCreate) strings.createPair else strings.joinWithCode,
                    loading = state.busy,
                    enabled = myName.isNotBlank() && (pairModeCreate || inviteCodeInput.isNotBlank()),
                    onClick = {
                        val email = gmailAddress.trim().takeIf { it.isNotBlank() }
                        if (pairModeCreate) {
                            vm.createPair(serverUrl, "", myName, email) { step = 2 }
                        } else {
                            vm.joinPair(serverUrl, inviteCodeInput, myName, email) { step = 2 }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OursButton(
                    text = strings.backLabel,
                    variant = OursButtonVariant.Ghost,
                    onClick = { step = 0 },
                )
            }

            2 -> {
                if (!state.inviteCode.isNullOrBlank()) {
                    OursCard(modifier = Modifier.fillMaxWidth(), frosted = true) {
                        Text(strings.invitePartnerTitle, style = OursTypography.title, color = colors.ink)
                        Spacer(Modifier.height(OursSpacing.xxs))
                        Text(
                            strings.invitePartnerSubtitle,
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                        )
                        Spacer(Modifier.height(OursSpacing.xs))
                        Text(
                            text = state.inviteCode,
                            style = OursTypography.display.copy(fontFeatureSettings = "tnum"),
                            color = colors.action,
                        )
                    }
                }

                Text(strings.permissionsTitle, style = OursTypography.display, color = colors.ink)
                Text(
                    strings.permissionsSubtitle,
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )

                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Text(strings.preciseLocation, style = OursTypography.title, color = colors.ink)
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = if (fineGranted) strings.granted else strings.allowLocation,
                        variant = if (fineGranted) OursButtonVariant.Secondary else OursButtonVariant.Primary,
                        enabled = !fineGranted,
                        onClick = {
                            fineLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Text(strings.backgroundLocation, style = OursTypography.title, color = colors.ink)
                    Text(
                        strings.backgroundLocationHint,
                        style = OursTypography.caption,
                        color = colors.inkSecondary,
                    )
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = if (bgGranted) strings.backgroundGranted else strings.allowInBackground,
                        variant = if (bgGranted) OursButtonVariant.Secondary else OursButtonVariant.Primary,
                        enabled = fineGranted && !bgGranted,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 29) {
                                bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            } else {
                                bgGranted = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (Build.VERSION.SDK_INT >= 33) {
                    OursCard(modifier = Modifier.fillMaxWidth()) {
                        Text(strings.notificationsTitle, style = OursTypography.title, color = colors.ink)
                        Text(
                            strings.notificationsHint,
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                        )
                        Spacer(Modifier.height(OursSpacing.xs))
                        OursButton(
                            text = if (notifGranted) strings.notificationsGranted else strings.allowNotifications,
                            variant = if (notifGranted) OursButtonVariant.Secondary else OursButtonVariant.Primary,
                            enabled = !notifGranted,
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                OursButton(
                    text = strings.continueBatterySetup,
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            3 -> {
                val isXiaomi = XiaomiGuideHelper.isXiaomiFamily()
                Text(
                    if (isXiaomi) strings.xiaomiTitle else strings.keepAliveTitle,
                    style = OursTypography.display,
                    color = colors.ink,
                )
                Text(
                    strings.batterySubtitle,
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )

                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Text(strings.batteryOptimization, style = OursTypography.title, color = colors.ink)
                    Text(
                        if (batteryExempt) strings.alreadyUnrestricted else strings.allowUnrestrictedHint,
                        style = OursTypography.caption,
                        color = colors.inkSecondary,
                    )
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = if (batteryExempt) strings.unrestricted else strings.disableBatteryOpt,
                        variant = if (batteryExempt) OursButtonVariant.Secondary else OursButtonVariant.Primary,
                        onClick = {
                            runCatching {
                                context.startActivity(XiaomiGuideHelper.requestIgnoreBatteryOptimizationsIntent(context))
                            }
                            batteryExempt = XiaomiGuideHelper.isIgnoringBatteryOptimizations(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (isXiaomi) {
                    XiaomiStepsCard()
                }

                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = strings.onboardingReadyBadge,
                            style = OursTypography.title,
                            color = colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        OursStatusChip(
                            label = "4 / 4",
                            tone = StatusTone.Live,
                        )
                    }
                }

                OursButton(
                    text = strings.startUsingOurs,
                    onClick = { vm.finishOnboarding() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
                }
            }
        }
    }

    if (showServerSheet) {
        OursBottomSheet(onDismiss = { showServerSheet = false }) {
            Text(strings.serverSettingsTitle, style = OursTypography.headline, color = colors.ink)
            Spacer(Modifier.height(OursSpacing.sm))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(strings.serverUrlTitle) },
                placeholder = { Text("http://192.168.100.92:8080") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (serverStatus != null) {
                Spacer(Modifier.height(OursSpacing.xs))
                Text(serverStatus!!, style = OursTypography.caption, color = colors.live)
            }
            Spacer(Modifier.height(OursSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
                OursButton(
                    text = strings.testConnection,
                    variant = OursButtonVariant.Secondary,
                    loading = checkingServer,
                    onClick = {
                        scope.launch {
                            checkingServer = true
                            serverStatus = null
                            vm.testServerHealth(serverUrl)
                                .onSuccess { serverStatus = it }
                                .onFailure { serverStatus = it.message ?: "Unreachable" }
                            checkingServer = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                OursButton(
                    text = strings.saveServerUrl,
                    onClick = {
                        vm.updateServerUrl(serverUrl)
                        showServerSheet = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun LanguagePillToggle(
    current: String,
    onSelect: (String) -> Unit,
) {
    val colors = OursTheme.colors
    Surface(
        shape = RoundedCornerShape(OursRadius.chip),
        color = colors.surfaceMuted,
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf("en" to "EN", "id" to "ID").forEach { (code, label) ->
                val selected = current == code
                val interactionSource = remember { MutableInteractionSource() }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) colors.action else colors.surfaceMuted,
                    modifier = Modifier
                        .oursPressScale(interactionSource)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onSelect(code)
                        },
                ) {
                    Text(
                        text = label,
                        style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selected) colors.onAction else colors.inkSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun XiaomiStepsCard() {
    val colors = OursTheme.colors
    val context = LocalContext.current
    OursCard(modifier = Modifier.fillMaxWidth()) {
        Text("Poco / HyperOS checklist", style = OursTypography.title, color = colors.ink)
        Spacer(Modifier.height(OursSpacing.xs))
        Text(
            "1. Turn on Autostart for OURS.\n" +
                "2. In App info > Battery saver, choose \"No restrictions\".\n" +
                "3. Open Recents, long-press OURS, and tap the Lock icon.",
            style = OursTypography.body,
            color = colors.inkSecondary,
        )
        Spacer(Modifier.height(OursSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
            OursButton(
                text = "Open Autostart",
                variant = OursButtonVariant.Secondary,
                onClick = { XiaomiGuideHelper.openAutostartSettings(context) },
                modifier = Modifier.weight(1f),
            )
            OursButton(
                text = "App battery info",
                variant = OursButtonVariant.Secondary,
                onClick = {
                    runCatching { context.startActivity(XiaomiGuideHelper.openAppDetailsIntent(context)) }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OnboardingProgressBar(
    step: Int,
    totalSteps: Int,
) {
    val colors = OursTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
    ) {
        repeat(totalSteps) { index ->
            val targetFill = if (index <= step) 1f else 0f
            val fill by animateFloatAsState(
                targetValue = targetFill,
                animationSpec = OursMotion.enterSpec(),
                label = "onboarding-step-$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(OursRadius.chip))
                    .background(colors.surfaceMuted),
            ) {
                if (fill > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .height(5.dp)
                            .clip(RoundedCornerShape(OursRadius.chip))
                            .background(colors.action),
                    )
                }
            }
        }
    }
}
