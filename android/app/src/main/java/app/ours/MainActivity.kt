package app.ours

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import app.ours.ui.components.OursSendConfirmation
import app.ours.ui.screens.ChatScreen
import app.ours.ui.screens.MapScreen
import app.ours.ui.screens.MemoriesScreen
import app.ours.ui.screens.OnboardingScreen
import app.ours.ui.screens.SettingsScreen
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import coil3.compose.AsyncImage
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
            OursTheme {
                OursRoot(vm = vm)
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

    Scaffold(
        containerColor = colors.canvas,
        bottomBar = {
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
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = colors.ink,
                        tonalElevation = 0.dp,
                    ) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.action,
                            selectedTextColor = colors.action,
                            unselectedIconColor = colors.inkMuted,
                            unselectedTextColor = colors.inkMuted,
                            indicatorColor = colors.actionSoft,
                        )
                        NavigationBarItem(
                            selected = state.activeTab == MainTab.Map,
                            onClick = { vm.selectTab(MainTab.Map) },
                            icon = { Icon(OursIcons.Map, contentDescription = "Map") },
                            label = { Text("Map", style = OursTypography.label) },
                            colors = navColors,
                        )
                        NavigationBarItem(
                            selected = state.activeTab == MainTab.Memories,
                            onClick = { vm.selectTab(MainTab.Memories) },
                            icon = { Icon(OursIcons.Memories, contentDescription = "Memories") },
                            label = { Text("Memories", style = OursTypography.label) },
                            colors = navColors,
                        )
                        NavigationBarItem(
                            selected = state.activeTab == MainTab.Chat,
                            onClick = { vm.selectTab(MainTab.Chat) },
                            icon = { Icon(OursIcons.Chat, contentDescription = "Chat") },
                            label = { Text("Chat", style = OursTypography.label) },
                            colors = navColors,
                        )
                        NavigationBarItem(
                            selected = state.activeTab == MainTab.Settings,
                            onClick = { vm.selectTab(MainTab.Settings) },
                            icon = { Icon(OursIcons.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", style = OursTypography.label) },
                            colors = navColors,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.activeTab) {
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
