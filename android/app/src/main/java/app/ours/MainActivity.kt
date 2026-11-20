package app.ours

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import app.ours.util.ProfileAndSosHelper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ours.bridge.OursNativeBridge
import app.ours.data.api.CheckInDto
import app.ours.location.LocationTrackingService
import app.ours.ui.MainTab
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursInterlockedRingsMark
import app.ours.ui.components.OursSendConfirmation
import app.ours.ui.components.oursHeaderInsets
import app.ours.ui.components.oursLiquidGlass
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.rememberOursLayoutSpec
import app.ours.ui.screens.ChatScreen
import app.ours.ui.screens.MapScreen
import app.ours.ui.screens.MemoriesScreen
import app.ours.ui.screens.OnboardingScreen
import app.ours.auth.OursOidcManager
import app.ours.ui.screens.OursWebShellScreen
import app.ours.ui.screens.SettingsScreen
import app.ours.ui.theme.OursMotion
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursStrings
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val vm: OursViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cold-start path for the Pocket ID OIDC redirect (ours://callback).
        OursOidcManager.handleIntent(intent)

        val app = application as OursApp
        if (BuildConfig.DEBUG) {
            if (intent?.getBooleanExtra("debug_reset", false) == true) {
                app.prefs.clearSession()
            }
            val debugServer = intent?.getStringExtra("debug_server_url")?.trim()?.trimEnd('/')
            if (!debugServer.isNullOrBlank() &&
                (debugServer.startsWith("http://10.0.2.2:") || debugServer.startsWith("http://127.0.0.1:")) &&
                OursNativeBridge.isTrustedServerUrl(debugServer, isDebugBuild = true)
            ) {
                app.prefs.serverUrl = debugServer
            }
        }
        if (app.prefs.isPaired && app.prefs.onboardingDone && !app.prefs.sharingPaused) {
            LocationTrackingService.start(this)
        }

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            var liveThemeMode by rememberSaveable { mutableStateOf(app.prefs.themeMode) }
            var liveLanguage by rememberSaveable { mutableStateOf(app.prefs.language) }
            DisposableEffect(app.prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    liveThemeMode = app.prefs.themeMode
                    liveLanguage = app.prefs.language
                }
                app.prefs.registerListener(listener)
                onDispose { app.prefs.unregisterListener(listener) }
            }
            val effectiveTheme = liveThemeMode.ifBlank { state.themeMode }
            val darkTheme = when (effectiveTheme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }
            }
            OursTheme(darkTheme = darkTheme, language = liveLanguage.ifBlank { state.language }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OursRoot(vm = vm, debugNativeUi = intent?.getBooleanExtra("debug_native_ui", false) ?: false)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Warm-start path for the Pocket ID OIDC redirect (ours://callback).
        OursOidcManager.handleIntent(intent)
    }
}

@Composable
private fun OursRoot(vm: OursViewModel, debugNativeUi: Boolean) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.onForegroundChanged(true)
                Lifecycle.Event.ON_STOP -> vm.onForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val useWebShell = if (BuildConfig.DEBUG) {
        // Debug-only escape hatch: launch with --ez debug_native_ui true to exercise
        // the native Compose onboarding (Credential Manager Google sign-in) instead of
        // the WebView shell. Production builds always use the shell.
        !debugNativeUi
    } else {
        true
    }
    if (useWebShell) {
        val prefs = remember(context) { (context.applicationContext as OursApp).prefs }
        OursWebShellScreen(prefs = prefs)
        return
    }

    if (!state.isPaired || !state.onboardingDone) {
        OnboardingScreen(state = state, vm = vm)
        return
    }

    var pendingPhotoUriStr by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedCheckInUriStr by rememberSaveable { mutableStateOf<String?>(null) }
    var replyTargetCheckInId by rememberSaveable { mutableStateOf<Long?>(null) }
    val capturedCheckInUri = capturedCheckInUriStr?.let(Uri::parse)
    var fullPhotoId by remember { mutableStateOf<String?>(null) }
    var fullCheckIn by remember { mutableStateOf<CheckInDto?>(null) }
    var askedContactsPermission by rememberSaveable { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(state.isPaired, state.emergencyContactPhone) {
        if (
            state.isPaired &&
            state.emergencyContactPhone.isNotBlank() &&
            !askedContactsPermission &&
            !ProfileAndSosHelper.hasContactsPermission(context)
        ) {
            askedContactsPermission = true
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    fun newCameraUri(): Uri {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "cap_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val checkInCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            capturedCheckInUriStr = pendingPhotoUriStr
        }
        pendingPhotoUriStr = null
    }

    val replyCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val cid = replyTargetCheckInId
        val uri = pendingPhotoUriStr?.let(Uri::parse)
        if (ok && cid != null && uri != null) {
            vm.sendReply(cid, null, uri)
        }
        replyTargetCheckInId = null
        pendingPhotoUriStr = null
    }

    fun launchCheckInCamera() {
        val uri = newCameraUri()
        pendingPhotoUriStr = uri.toString()
        checkInCameraLauncher.launch(uri)
    }

    fun launchReplyCamera(checkInId: Long) {
        val uri = newCameraUri()
        pendingPhotoUriStr = uri.toString()
        replyTargetCheckInId = checkInId
        replyCameraLauncher.launch(uri)
    }

    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        containerColor = colors.canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = !imeVisible,
                enter = fadeIn(OursMotion.enterSpec()),
                exit = fadeOut(OursMotion.exitSpec()),
            ) {
                OursFloatingBottomNav(
                    activeTab = state.activeTab,
                    strings = strings,
                    unreadChatCount = state.unreadChatCount,
                    onSelect = vm::selectTab,
                )
            }
        },
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = state.activeTab,
                transitionSpec = {
                    (fadeIn(OursMotion.enterSpec(200)) + scaleIn(initialScale = 0.985f, animationSpec = OursMotion.springFluid()))
                        .togetherWith(fadeOut(OursMotion.exitSpec(150)))
                },
                label = "mainTabTransition",
            ) { tab ->
                when (tab) {
                    MainTab.Map -> MapScreen(
                        state = state,
                        vm = vm,
                        onQuickCheckIn = { launchCheckInCamera() },
                    )
                    MainTab.Memories -> MemoriesScreen(
                        state = state,
                        vm = vm,
                        onOpenFullPhoto = { item ->
                            fullCheckIn = item
                            fullPhotoId = item.photoId
                        },
                        onQuickCheckIn = { launchCheckInCamera() },
                    )
                    MainTab.Chat -> ChatScreen(
                        state = state,
                        vm = vm,
                        onLaunchCameraCheckIn = { launchCheckInCamera() },
                        onLaunchCameraReply = { cid -> launchReplyCamera(cid) },
                        onOpenFullPhoto = { item ->
                            fullCheckIn = item
                            fullPhotoId = item.photoId
                        },
                        onOpenFullPhotoId = { pid ->
                            fullCheckIn = null
                            fullPhotoId = pid
                        },
                    )
                    MainTab.Settings -> SettingsScreen(state = state, vm = vm)
                }
            }

            OursSendConfirmation(
                visible = state.confirmationMessage != null,
                message = state.confirmationMessage ?: "",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .oursHeaderInsets()
                    .padding(top = 88.dp),
            )

            AnimatedVisibility(
                visible = state.errorMessage != null && state.confirmationMessage == null,
                enter = fadeIn(OursMotion.enterSpec()),
                exit = fadeOut(OursMotion.exitSpec()),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .oursHeaderInsets()
                    .padding(top = 88.dp, start = OursSpacing.md, end = OursSpacing.md),
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.glassSurfaceStrong,
                    border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.45f)),
                    shadowElevation = 4.dp,
                    onClick = { vm.clearError() },
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = OursTypography.label,
                        color = colors.danger,
                        modifier = Modifier.padding(horizontal = OursSpacing.md, vertical = OursSpacing.xs),
                    )
                }
            }
        }
    }

    if (state.sosCountdownSec != null) {
        OursBottomSheet(onDismiss = { vm.cancelSosCountdown() }) {
            Text(
                text = if (state.sosKind == "crash") strings.sosCrashDetectedTitle else strings.sosSendingTitle,
                style = OursTypography.headline,
                color = colors.danger,
            )
            Spacer(Modifier.height(OursSpacing.xs))
            Text(
                text = "${strings.sosCountdownDetail} (${state.sosCountdownSec}s)",
                style = OursTypography.body,
                color = colors.inkSecondary,
            )
            Spacer(Modifier.height(OursSpacing.md))
            OursButton(
                text = strings.sosSendNowCta,
                variant = OursButtonVariant.Destructive,
                onClick = { vm.triggerSosNow() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OursSpacing.xs))
            OursButton(
                text = strings.sosCancelCta,
                variant = OursButtonVariant.Secondary,
                onClick = { vm.cancelSosCountdown() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (capturedCheckInUri != null) {
        var note by remember { mutableStateOf("") }
        var placeName by remember { mutableStateOf("") }
        OursBottomSheet(onDismiss = { capturedCheckInUriStr = null }) {
            Text(strings.sendCheckInTitle, style = OursTypography.headline, color = colors.ink)
            Spacer(Modifier.height(OursSpacing.sm))
            AsyncImage(
                model = capturedCheckInUri,
                contentDescription = strings.sendCheckInTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(OursRadius.inner)),
            )
            Spacer(Modifier.height(OursSpacing.sm))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.noteOptional) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OursSpacing.xs))
            OutlinedTextField(
                value = placeName,
                onValueChange = { placeName = it },
                label = { Text(strings.placeOptional) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OursSpacing.md))
            OursButton(
                text = strings.sendCheckInCta,
                loading = state.busy,
                onClick = {
                    val uri = capturedCheckInUri ?: return@OursButton
                    capturedCheckInUriStr = null
                    vm.sendCheckIn(uri, note, placeName)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (fullPhotoId != null) {
        Dialog(
            onDismissRequest = {
                fullPhotoId = null
                fullCheckIn = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                AsyncImage(
                    model = vm.photoUrl(fullPhotoId!!, thumb = false),
                    contentDescription = fullCheckIn?.note ?: "Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = {
                        fullPhotoId = null
                        fullCheckIn = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(OursSpacing.md),
                ) {
                    Icon(
                        imageVector = OursIcons.Close,
                        contentDescription = if (strings.langCode == "id") "Tutup" else "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                if (fullCheckIn != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(OursSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (!fullCheckIn!!.note.isNullOrBlank()) {
                            Text(
                                text = fullCheckIn!!.note!!,
                                style = OursTypography.body,
                                color = Color.White,
                            )
                        }
                        Text(
                            text = buildString {
                                if (!fullCheckIn!!.placeName.isNullOrBlank()) {
                                    append(fullCheckIn!!.placeName)
                                    append(" · ")
                                }
                                append(app.ours.util.Formatters.dateShort(fullCheckIn!!.createdAt))
                            },
                            style = OursTypography.caption,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OursFloatingBottomNav(
    activeTab: MainTab,
    strings: OursStrings,
    unreadChatCount: Int = 0,
    onSelect: (MainTab) -> Unit,
) {
    val colors = OursTheme.colors
    val layoutSpec = rememberOursLayoutSpec()
    val items = listOf(
        Triple(MainTab.Map, OursIcons.Map, strings.navMap),
        Triple(MainTab.Memories, OursIcons.Memories, strings.navMemories),
        Triple(MainTab.Chat, OursIcons.Chat, strings.navChat),
        Triple(MainTab.Settings, OursIcons.Settings, strings.navSettings),
    )
    val selectedIndex = items.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (activeTab != MainTab.Map) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.22f to colors.canvas.copy(alpha = 0.95f),
                            0.38f to colors.canvas,
                            1.0f to colors.canvas,
                        ),
                    )
                } else {
                    Modifier
                }
            )
            .navigationBarsPadding()
            .padding(
                start = layoutSpec.horizontalPadding,
                end = layoutSpec.horizontalPadding,
                top = if (activeTab != MainTab.Map) 16.dp else OursSpacing.xs,
                bottom = OursSpacing.xs,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 336.dp)
                .height(60.dp)
                .oursLiquidGlass(radius = 999.dp, colors = colors, elevation = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
            ) {
                val tabWidth = maxWidth / items.size
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = OursMotion.springSnappy(),
                    label = "nav-pill-x",
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(50.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.actionSoft),
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    items.forEach { (tab, icon, label) ->
                        val selected = tab == activeTab
                        val interactionSource = remember { MutableInteractionSource() }
                        val tint by animateColorAsState(
                            targetValue = if (selected) colors.action else colors.inkMuted,
                            animationSpec = OursMotion.enterSpec(180),
                            label = "nav-tint-${tab.name}",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.07f else 1f,
                            animationSpec = OursMotion.springBouncy(),
                            label = "nav-scale-${tab.name}",
                        )
                        Column(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(50.dp)
                                .semantics {
                                    role = Role.Tab
                                    this.selected = selected
                                }
                                .oursPressScale(interactionSource)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelect(tab) },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tint,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                )
                                if (tab == MainTab.Chat && unreadChatCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.danger),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = OursTypography.caption.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
