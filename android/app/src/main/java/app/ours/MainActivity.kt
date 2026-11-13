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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ours.data.api.CheckInDto
import app.ours.location.LocationTrackingService
import app.ours.ui.MainTab
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursBottomSheet
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursInterlockedRingsMark
import app.ours.ui.components.OursSendConfirmation
import app.ours.ui.components.oursPressScale
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
            OursTheme(darkTheme = darkTheme, language = state.language) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(650)
                    showSplash = false
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    OursRoot(vm = vm)
                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(OursMotion.exitSpec()),
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

    val iconScale = remember { Animatable(0.88f) }
    val iconOffsetY = remember { Animatable(12f) }
    val ringDraw = remember { Animatable(0.15f) }
    val pulseScale = remember { Animatable(0.92f) }
    val pulseAlpha = remember { Animatable(0.55f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, OursMotion.enterSpec(240))
    }
    LaunchedEffect(Unit) {
        iconOffsetY.animateTo(0f, OursMotion.enterSpec(240))
    }
    LaunchedEffect(Unit) {
        ringDraw.animateTo(1f, OursMotion.enterSpec(380))
    }
    LaunchedEffect(Unit) {
        pulseScale.animateTo(1.28f, OursMotion.enterSpec(480))
    }
    LaunchedEffect(Unit) {
        pulseAlpha.animateTo(0f, OursMotion.enterSpec(480))
    }
    LaunchedEffect(Unit) {
        delay(90)
        textAlpha.animateTo(1f, OursMotion.enterSpec(220))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OursSpacing.sm),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            scaleX = pulseScale.value
                            scaleY = pulseScale.value
                            alpha = pulseAlpha.value
                        }
                        .border(2.dp, colors.action, RoundedCornerShape(24.dp)),
                )
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F0F0E),
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                            translationY = iconOffsetY.value * density
                        },
                ) {
                    OursInterlockedRingsMark(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        leftRingColor = Color(0xFFFAF8F4),
                        rightRingColor = Color(0xFFC8C2B6),
                        backgroundColor = Color(0xFF0F0F0E),
                        progress = ringDraw.value,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
            ) {
                Text(
                    text = "OURS",
                    style = OursTypography.display.copy(fontWeight = FontWeight.Bold),
                    color = colors.ink,
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

    Scaffold(
        containerColor = colors.canvas,
        bottomBar = {
            OursFloatingBottomNav(
                activeTab = state.activeTab,
                strings = strings,
                unreadChatCount = state.unreadChatCount,
                onSelect = vm::selectTab,
            )
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
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                if (fullCheckIn != null && !fullCheckIn!!.note.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(OursSpacing.md),
                    ) {
                        Text(
                            text = fullCheckIn!!.note!!,
                            style = OursTypography.body,
                            color = Color.White,
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
            .padding(horizontal = OursSpacing.md, vertical = OursSpacing.xs),
    ) {
        Surface(
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
                        .height(52.dp)
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
                        Column(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(52.dp)
                                .oursPressScale(interactionSource)
                                .clip(RoundedCornerShape(OursRadius.inner))
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
                                    modifier = Modifier.size(20.dp),
                                )
                                if (tab == MainTab.Chat && unreadChatCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-3).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.ink),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = OursTypography.label.copy(
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
