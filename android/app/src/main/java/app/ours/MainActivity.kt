package app.ours

import android.net.Uri
import android.os.Bundle
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.rememberOursLayoutSpec
import app.ours.ui.screens.ChatScreen
import app.ours.ui.screens.MapScreen
import app.ours.ui.screens.MemoriesScreen
import app.ours.ui.screens.OnboardingScreen
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

        val app = application as OursApp
        if (app.prefs.isPaired && app.prefs.onboardingDone && !app.prefs.sharingPaused) {
            LocationTrackingService.start(this)
        }

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val darkTheme = when (state.themeMode) {
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
            OursTheme(darkTheme = darkTheme, language = state.language) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(820)
                    showSplash = false
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    OursRoot(vm = vm)
                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(OursMotion.exitSpec(220)) +
                            scaleOut(targetScale = 1.04f, animationSpec = OursMotion.exitSpec(220)),
                    ) {
                        OursSplashOverlay()
                    }
                }
            }
        }
    }
}

@Composable
private fun OursSplashOverlay() {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val isDark = OursTheme.isDark
    val leftRingColor = if (isDark) Color(0xFFFAF8F4) else Color(0xFF0F0F0E)
    val rightRingColor = if (isDark) Color(0xFFC8C2B6) else Color(0xFF5A5853)

    val iconScale = remember { Animatable(0.86f) }
    val iconOffsetY = remember { Animatable(14f) }
    val ringDraw = remember { Animatable(0.10f) }
    val ringConverge = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(0.92f) }
    val pulseAlpha = remember { Animatable(0.45f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(8f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, OursMotion.enterSpec(280))
    }
    LaunchedEffect(Unit) {
        iconOffsetY.animateTo(0f, OursMotion.enterSpec(280))
    }
    LaunchedEffect(Unit) {
        ringDraw.animateTo(1f, OursMotion.enterSpec(420))
    }
    LaunchedEffect(Unit) {
        delay(120)
        ringConverge.animateTo(1f, OursMotion.enterSpec(380))
    }
    LaunchedEffect(Unit) {
        delay(180)
        pulseScale.animateTo(1.32f, OursMotion.enterSpec(520))
    }
    LaunchedEffect(Unit) {
        delay(180)
        pulseAlpha.animateTo(0f, OursMotion.enterSpec(520))
    }
    LaunchedEffect(Unit) {
        delay(160)
        wordmarkAlpha.animateTo(1f, OursMotion.enterSpec(240))
    }
    LaunchedEffect(Unit) {
        delay(160)
        wordmarkOffsetY.animateTo(0f, OursMotion.enterSpec(240))
    }
    LaunchedEffect(Unit) {
        delay(260)
        subtitleAlpha.animateTo(1f, OursMotion.enterSpec(240))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OursSpacing.md),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer {
                            scaleX = pulseScale.value
                            scaleY = pulseScale.value
                            alpha = pulseAlpha.value
                        }
                        .border(1.5.dp, colors.action.copy(alpha = 0.5f), RoundedCornerShape(26.dp)),
                )
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.canvas,
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                            translationY = iconOffsetY.value * density
                        },
                ) {
                    OursInterlockedRingsMark(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        leftRingColor = leftRingColor,
                        rightRingColor = rightRingColor,
                        backgroundColor = colors.canvas,
                        progress = ringDraw.value,
                        convergeProgress = ringConverge.value,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "OURS",
                    style = OursTypography.display.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    ),
                    color = colors.ink,
                    modifier = Modifier.graphicsLayer {
                        alpha = wordmarkAlpha.value
                        translationY = wordmarkOffsetY.value * density
                    },
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value },
                ) {
                    Text(
                        text = "PRIVATE FOR TWO",
                        style = OursTypography.label.copy(letterSpacing = 2.2.sp),
                        color = colors.inkSecondary,
                    )
                    Text(
                        text = strings.splashTagline,
                        style = OursTypography.caption,
                        color = colors.inkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun OursRoot(vm: OursViewModel) {
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

    if (!state.isPaired || !state.onboardingDone) {
        OnboardingScreen(state = state, vm = vm)
        return
    }

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedCheckInUri by remember { mutableStateOf<Uri?>(null) }
    var replyTargetCheckInId by remember { mutableStateOf<Long?>(null) }
    var fullPhotoId by remember { mutableStateOf<String?>(null) }
    var fullCheckIn by remember { mutableStateOf<CheckInDto?>(null) }

    fun newCameraUri(): Uri {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "cap_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val checkInCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            capturedCheckInUri = pendingPhotoUri
        }
        pendingPhotoUri = null
    }

    val replyCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val cid = replyTargetCheckInId
        val uri = pendingPhotoUri
        if (ok && cid != null && uri != null) {
            vm.sendReply(cid, null, uri)
        }
        replyTargetCheckInId = null
        pendingPhotoUri = null
    }

    fun launchCheckInCamera() {
        val uri = newCameraUri()
        pendingPhotoUri = uri
        checkInCameraLauncher.launch(uri)
    }

    fun launchReplyCamera(checkInId: Long) {
        val uri = newCameraUri()
        pendingPhotoUri = uri
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = state.activeTab,
                transitionSpec = {
                    fadeIn(OursMotion.enterSpec()).togetherWith(fadeOut(OursMotion.exitSpec()))
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
                    .statusBarsPadding()
                    .padding(top = OursSpacing.sm),
            )
        }
    }

    if (state.sosCountdownSec != null) {
        OursBottomSheet(onDismiss = { vm.cancelSosCountdown() }) {
            Text(
                text = if (state.sosKind == "crash") "Crash detected" else "Sending SOS",
                style = OursTypography.headline,
                color = colors.danger,
            )
            Spacer(Modifier.height(OursSpacing.xs))
            Text(
                text = "Alerting ${state.partner?.name ?: "your partner"}${
                    if (state.emergencyContactPhone.isNotBlank()) " and calling ${state.emergencyContactName.ifBlank { state.emergencyContactPhone }} on WhatsApp" else ""
                } in ${state.sosCountdownSec}s.",
                style = OursTypography.body,
                color = colors.inkSecondary,
            )
            Spacer(Modifier.height(OursSpacing.md))
            OursButton(
                text = "Send SOS now",
                variant = OursButtonVariant.Destructive,
                onClick = { vm.triggerSosNow() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OursSpacing.xs))
            OursButton(
                text = "I'm okay · Cancel",
                variant = OursButtonVariant.Secondary,
                onClick = { vm.cancelSosCountdown() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Check-in note & send bottom sheet (completes the <=3-tap check-in flow)
    if (capturedCheckInUri != null) {
        var note by remember { mutableStateOf("") }
        var placeName by remember { mutableStateOf("") }
        OursBottomSheet(onDismiss = { capturedCheckInUri = null }) {
            Text("Send check-in", style = OursTypography.headline, color = colors.ink)
            Spacer(Modifier.height(OursSpacing.sm))
            AsyncImage(
                model = capturedCheckInUri,
                contentDescription = "Captured photo",
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
                label = { Text("Note (optional)") },
                placeholder = { Text("I'm here") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OursSpacing.xs))
            OutlinedTextField(
                value = placeName,
                onValueChange = { placeName = it },
                label = { Text("Place name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OursSpacing.md))
            OursButton(
                text = "Send check-in",
                loading = state.busy,
                onClick = {
                    val uri = capturedCheckInUri ?: return@OursButton
                    capturedCheckInUri = null
                    vm.sendCheckIn(uri, note, placeName)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Full-screen photo viewer
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
            .navigationBarsPadding()
            .padding(horizontal = layoutSpec.horizontalPadding, vertical = OursSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.oursContentWidth(layoutSpec),
            shape = RoundedCornerShape(OursRadius.card),
            color = colors.glassSurfaceStrong,
            border = BorderStroke(1.dp, colors.glassBorder),
            shadowElevation = 8.dp,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
            ) {
                val tabWidth = maxWidth / items.size
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = OursMotion.enterSpec(),
                    label = "nav-pill-x",
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(48.dp)
                        .clip(RoundedCornerShape(OursRadius.inner))
                        .background(colors.actionSoft),
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    items.forEach { (tab, icon, label) ->
                        val selected = tab == activeTab
                        val interactionSource = remember { MutableInteractionSource() }
                        val tint by animateColorAsState(
                            targetValue = if (selected) colors.action else colors.inkMuted,
                            animationSpec = OursMotion.enterSpec(),
                            label = "nav-tint-${tab.name}",
                        )
                        Box(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(48.dp)
                                .oursPressScale(interactionSource)
                                .clip(RoundedCornerShape(OursRadius.inner))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelect(tab) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tint,
                                    modifier = Modifier.size(22.dp),
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
                        }
                    }
                }
            }
        }
    }
}
