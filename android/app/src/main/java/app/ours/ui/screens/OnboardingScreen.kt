package app.ours.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
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

    var serverUrl by remember { mutableStateOf(state.serverUrl.ifBlank { "http://192.168.100.92:8080" }) }
    var serverStatus by remember { mutableStateOf<String?>(null) }
    var checkingServer by remember { mutableStateOf(false) }

    var pairModeCreate by remember { mutableStateOf(true) }
    var myName by remember { mutableStateOf("") }
    var setupKey by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }

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
        Text(
            text = "OURS",
            style = OursTypography.label.copy(fontWeight = FontWeight.Bold),
            color = colors.action,
        )

        when (step) {
            0 -> {
                Text("Connect to your server", style = OursTypography.display, color = colors.ink)
                Text(
                    "OURS runs on your own server. Enter its LAN or tunnel address.",
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.100.92:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (serverStatus != null) {
                    Text(serverStatus!!, style = OursTypography.caption, color = colors.live)
                }
                if (state.errorMessage != null) {
                    Text(state.errorMessage, style = OursTypography.caption, color = colors.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.sm)) {
                    OursButton(
                        text = "Test connection",
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
                        text = "Continue",
                        onClick = {
                            vm.updateServerUrl(serverUrl)
                            step = 1
                        },
                        enabled = serverUrl.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            1 -> {
                Text("Pair your phones", style = OursTypography.display, color = colors.ink)
                Text(
                    "Only two people can join this server.",
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
                    OursButton(
                        text = "Create pair",
                        variant = if (pairModeCreate) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                        onClick = { pairModeCreate = true },
                        modifier = Modifier.weight(1f),
                    )
                    OursButton(
                        text = "Join with code",
                        variant = if (!pairModeCreate) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                        onClick = { pairModeCreate = false },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = myName,
                    onValueChange = { myName = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (pairModeCreate) {
                    OutlinedTextField(
                        value = setupKey,
                        onValueChange = { setupKey = it },
                        label = { Text("Server setup key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it.uppercase() },
                        label = { Text("6-character invite code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.errorMessage != null) {
                    Text(state.errorMessage, style = OursTypography.caption, color = colors.error)
                }

                OursButton(
                    text = if (pairModeCreate) "Create pair" else "Join pair",
                    loading = state.busy,
                    enabled = myName.isNotBlank() && (if (pairModeCreate) setupKey.isNotBlank() else inviteCodeInput.isNotBlank()),
                    onClick = {
                        if (pairModeCreate) {
                            vm.createPair(serverUrl, setupKey, myName) { step = 2 }
                        } else {
                            vm.joinPair(serverUrl, inviteCodeInput, myName) { step = 2 }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OursButton(
                    text = "Back",
                    variant = OursButtonVariant.Ghost,
                    onClick = { step = 0 },
                )
            }

            2 -> {
                if (!state.inviteCode.isNullOrBlank()) {
                    OursCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Invite your partner", style = OursTypography.title, color = colors.ink)
                        Spacer(Modifier.height(OursSpacing.xxs))
                        Text(
                            "Share this code so they can join:",
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

                Text("Location & alerts", style = OursTypography.display, color = colors.ink)
                Text(
                    "Grant permissions step by step so location stays live in the background.",
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )

                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Text("1. Precise location", style = OursTypography.title, color = colors.ink)
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = if (fineGranted) "Granted" else "Allow location",
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
                    Text("2. Background location", style = OursTypography.title, color = colors.ink)
                    Text(
                        "Pick \"Allow all the time\" on the next screen.",
                        style = OursTypography.caption,
                        color = colors.inkSecondary,
                    )
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = if (bgGranted) "Allow all the time granted" else "Allow in background",
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
                        Text("3. Notifications", style = OursTypography.title, color = colors.ink)
                        Text(
                            "For check-ins, replies, place arrivals, and the foreground service.",
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                        )
                        Spacer(Modifier.height(OursSpacing.xs))
                        OursButton(
                            text = if (notifGranted) "Notifications allowed" else "Allow notifications",
                            variant = if (notifGranted) OursButtonVariant.Secondary else OursButtonVariant.Primary,
                            enabled = !notifGranted,
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                OursButton(
                    text = "Continue to battery setup",
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            3 -> {
                val isXiaomi = XiaomiGuideHelper.isXiaomiFamily()
                Text(
                    if (isXiaomi) "Xiaomi / HyperOS setup" else "Keep location alive",
                    style = OursTypography.display,
                    color = colors.ink,
                )
                Text(
                    "Exempt OURS from battery saving so updates stay fresh without opening the app.",
                    style = OursTypography.body,
                    color = colors.inkSecondary,
                )

                OursCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Battery optimization", style = OursTypography.title, color = colors.ink)
                    Text(
                        if (batteryExempt) "Already unrestricted." else "Allow OURS to run without battery limits.",
                        style = OursTypography.caption,
                        color = colors.inkSecondary,
                    )
                    Spacer(Modifier.height(OursSpacing.xs))
                    OursButton(
                        text = if (batteryExempt) "Unrestricted" else "Disable battery optimization",
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

                OursButton(
                    text = "Start using OURS",
                    onClick = { vm.finishOnboarding() },
                    modifier = Modifier.fillMaxWidth(),
                )
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
