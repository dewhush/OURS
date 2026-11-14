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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.ours.R
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursGlassBackdrop
import app.ours.ui.components.OursInterlockedRingsMark
import app.ours.ui.components.OursSegmentedControl
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursHeaderInsets
import app.ours.ui.components.oursLiquidGlass
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.rememberOursLayoutSpec
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

    val layoutSpec = rememberOursLayoutSpec()

    OursGlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier = Modifier
                .oursContentWidth(layoutSpec)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = layoutSpec.horizontalPadding, vertical = OursSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OursSpacing.md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .oursHeaderInsets(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_ours_pair_rings),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = "OURS",
                        style = OursTypography.title.copy(fontWeight = FontWeight.Bold),
                        color = colors.ink,
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
                    (fadeIn(OursMotion.enterSpec(220)) + slideInHorizontally(OursMotion.springFluid()) { w ->
                        if (forward) w / 5 else -w / 5
                    }).togetherWith(
                        fadeOut(OursMotion.exitSpec(150)) + slideOutHorizontally(OursMotion.exitSpec(150)) { w ->
                            if (forward) -w / 5 else w / 5
                        }
                    )
                },
                label = "onboardingStep",
            ) { currentStep ->
                Column(verticalArrangement = Arrangement.spacedBy(OursSpacing.md)) {
                    when (currentStep) {
                        0 -> {
                            Text(strings.connectGmailTitle, style = OursTypography.display, color = colors.ink)
                            Text(
                                strings.connectGmailSubtitle,
                                style = OursTypography.body,
                                color = colors.inkSecondary,
                            )

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

                            Text(
                                text = strings.orEnterGmailManually,
                                style = OursTypography.caption,
                                color = colors.inkMuted,
                            )

                            OutlinedTextField(
                                value = gmailAddress,
                                onValueChange = { gmailAddress = it },
                                label = { Text(strings.gmailAddressLabel) },
                                placeholder = { Text("you@gmail.com") },
                                singleLine = true,
                                shape = RoundedCornerShape(OursRadius.control),
                                modifier = Modifier.fillMaxWidth(),
                            )

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

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .semantics { role = Role.Button }
                                    .clip(RoundedCornerShape(OursRadius.control))
                                    .clickable { showServerSheet = true }
                                    .padding(horizontal = OursSpacing.xs),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = "${strings.serverSettingsLink} · ${serverUrl.removePrefix("http://").removePrefix("https://")}",
                                    style = OursTypography.caption.copy(fontWeight = FontWeight.Medium),
                                    color = colors.action,
                                )
                            }
                        }

                        1 -> {
                            Text(strings.pairTitle, style = OursTypography.display, color = colors.ink)
                            Text(
                                strings.pairSubtitle,
                                style = OursTypography.body,
                                color = colors.inkSecondary,
                            )
                            OursSegmentedControl(
                                options = listOf(true to strings.createPair, false to strings.joinWithCode),
                                selected = pairModeCreate,
                                onSelect = { pairModeCreate = it },
                                modifier = Modifier.fillMaxWidth(),
                            )

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
                        onValueChange = { raw ->
                            inviteCodeInput = raw.filter { it.isLetterOrDigit() }.uppercase().take(6)
                        },
                        label = { Text(strings.inviteCodeLabel) },
                        placeholder = { Text("A1B2C3") },
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
            val trimmedUrl = serverUrl.trim().lowercase()
            val isNonLocalHttp = trimmedUrl.startsWith("http://") &&
                !trimmedUrl.startsWith("http://127.0.0.1") &&
                !trimmedUrl.startsWith("http://localhost") &&
                !trimmedUrl.startsWith("http://10.0.2.2")
            if (isNonLocalHttp) {
                Spacer(Modifier.height(OursSpacing.xxs))
                Text(strings.httpsRecommended, style = OursTypography.caption, color = colors.inkMuted)
            }
            if (serverStatus != null) {
                Spacer(Modifier.height(OursSpacing.xs))
                val statusText = serverStatus!!
                val isFailure = statusText.startsWith("Unreachable") ||
                    statusText.startsWith("Gagal") ||
                    statusText.startsWith("Failed")
                Text(
                    text = statusText,
                    style = OursTypography.caption,
                    color = if (isFailure) colors.danger else colors.success,
                )
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
}

@Composable
fun LanguagePillToggle(
    current: String,
    onSelect: (String) -> Unit,
) {
    OursSegmentedControl(
        options = listOf("en" to "EN", "id" to "ID"),
        selected = current,
        onSelect = onSelect,
        modifier = Modifier.width(108.dp),
    )
}

@Composable
fun XiaomiStepsCard() {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val context = LocalContext.current
    OursCard(modifier = Modifier.fillMaxWidth()) {
        Text(strings.xiaomiChecklistHint, style = OursTypography.title, color = colors.ink)
        Spacer(Modifier.height(OursSpacing.xs))
        Text(
            "${strings.xiaomiStep1}\n${strings.xiaomiStep2}\n${strings.xiaomiStep3}\n${strings.xiaomiStep4}",
            style = OursTypography.body,
            color = colors.inkSecondary,
        )
        Spacer(Modifier.height(OursSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
            OursButton(
                text = strings.xiaomiOpenAutostart,
                variant = OursButtonVariant.Secondary,
                onClick = { XiaomiGuideHelper.openAutostartSettings(context) },
                modifier = Modifier.weight(1f),
            )
            OursButton(
                text = strings.xiaomiAppBatteryInfo,
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
                animationSpec = OursMotion.springFluid(),
                label = "onboarding-step-$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(OursRadius.chip))
                    .background(colors.surfaceMuted),
            ) {
                if (fill > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .height(4.dp)
                            .clip(RoundedCornerShape(OursRadius.chip))
                            .background(colors.action),
                    )
                }
            }
        }
    }
}
