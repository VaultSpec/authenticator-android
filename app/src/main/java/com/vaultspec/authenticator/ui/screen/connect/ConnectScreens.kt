package com.vaultspec.authenticator.ui.screen.connect

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vaultspec.authenticator.pairing.PairingCode
import com.vaultspec.authenticator.ui.component.AppIcons
import com.vaultspec.authenticator.ui.component.CircleIconButton
import com.vaultspec.authenticator.ui.component.ErrorText
import com.vaultspec.authenticator.ui.component.PrimaryButton
import com.vaultspec.authenticator.ui.component.VaultTextField
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens
import kotlinx.coroutines.delay
import java.util.concurrent.Executors


@Composable
private fun StageScaffold(onClose: () -> Unit, closeIcon: Boolean = true, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaultSpecDimens.ScreenPadding),
        ) {
            Spacer(Modifier.height(64.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 480.dp)) { content() }
            Spacer(Modifier.height(24.dp))
        }
        CircleIconButton(
            icon = if (closeIcon) AppIcons.CircleX else AppIcons.Check,
            contentDescription = if (closeIcon) "Cancel" else "Done",
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = VaultSpecDimens.ScreenPadding, vertical = 8.dp)
                .align(Alignment.TopStart),
        )
    }
}

@Composable
private fun Headline(title: String, message: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

/** The burst start, in [animationSeconds] time, once [connected] turns true. */
@Composable
private fun burstClock(connected: Boolean): Double? {
    val t = animationSeconds()
    var start by remember { mutableStateOf<Double?>(null) }
    if (connected && start == null) start = t
    return start?.let { t - it }
}

@Composable
fun ShowPairingCodeScreen(onDone: () -> Unit, viewModel: ShowPairingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val connected = state.stage == ShowStage.Connected
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(connected) { if (connected) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }

    StageScaffold(onClose = onDone, closeIcon = !connected) {
        if (state.stage == ShowStage.Password) {
            Spacer(Modifier.height(40.dp))
            Icon(AppIcons.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(16.dp))
            Headline("Connect a Device", "Enter your vault password to create a code for your new device.")
            Spacer(Modifier.height(24.dp))
            VaultTextField(value = state.password, onValueChange = viewModel::onPasswordChange, label = "Vault Password", isPassword = true)
            state.error?.let { Spacer(Modifier.height(12.dp)); ErrorText(it) }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = "Continue", onClick = viewModel::onConfirmPassword, loading = state.working, enabled = state.password.isNotEmpty())
            return@StageScaffold
        }

        // The token stays after it's claimed so the cloud can burst
        var shownToken by remember { mutableStateOf<String?>(null) }
        (state.stage as? ShowStage.Showing)?.let { shownToken = it.token }
        val burst = burstClock(connected)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(330.dp)) {
            val token = shownToken
            if (token != null && (state.stage is ShowStage.Showing || connected)) {
                ParticleCloud(token, size = 330.dp, burstSeconds = burst)
            }
            when (state.stage) {
                ShowStage.Connected -> if ((burst ?: 0.0) > 0.45) {
                    Icon(AppIcons.CircleCheck, "Connected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(96.dp))
                }
                ShowStage.Expired -> Icon(AppIcons.Timer, "Expired", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                ShowStage.Starting -> CircularProgressIndicator()
                else -> {}
            }
        }
        Spacer(Modifier.height(20.dp))
        when (val stage = state.stage) {
            ShowStage.Connected -> Headline("Connected", "Your new device has your vault. Enter your vault password there to finish.")
            ShowStage.Expired -> {
                Headline("Code Expired", "For your security, codes last 5 minutes and work once.")
                Spacer(Modifier.height(20.dp))
                PrimaryButton(text = "Show a New Code", onClick = viewModel::onNewCode, loading = state.working)
            }
            else -> {
                Headline(
                    "Waiting for Other Device…",
                    "On your new device, open VaultSpec and tap Use Another Device. Then position this image in the frame of its camera.",
                )
                if (stage is ShowStage.Showing) Countdown(stage.expiresAt)
            }
        }
        state.error?.let { Spacer(Modifier.height(12.dp)); ErrorText(it) }
    }
}

@Composable
private fun Countdown(expiresAt: Long) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiresAt) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val left = ((expiresAt - now) / 1000).coerceAtLeast(0)
    Spacer(Modifier.height(6.dp))
    Text("%d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
}

@Composable
fun ScanToConnectScreen(onCancel: () -> Unit, onConnected: () -> Unit, viewModel: ScanToConnectViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var cameraAllowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraAllowed = it }
    LaunchedEffect(Unit) { if (!cameraAllowed) permission.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(state.done) { if (state.done) onConnected() }
    LaunchedEffect(state.stage) { if (state.stage == ScanStage.Password) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }

    if (state.stage == ScanStage.Scanning) {
        Viewfinder(cameraAllowed = cameraAllowed, error = state.error, onToken = viewModel::onToken, onCancel = onCancel)
        return
    }

    StageScaffold(onClose = onCancel) {
        val connected = state.stage == ScanStage.Password
        val burst = burstClock(connected)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            ParticleCloud(token = null, size = 240.dp, burstSeconds = burst)
            if ((burst ?: 0.0) > 0.45) {
                Icon(AppIcons.CircleCheck, "Connected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(76.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        if (!connected) {
            Headline("Connecting…", "Getting your vault from your other device.")
            return@StageScaffold
        }
        Headline("Connected", "Enter the vault password from your other device. It becomes this device's vault password.")
        Spacer(Modifier.height(24.dp))
        VaultTextField(value = state.password, onValueChange = viewModel::onPasswordChange, label = "Vault Password", isPassword = true)
        state.error?.let { Spacer(Modifier.height(12.dp)); ErrorText(it) }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Open Vault", onClick = viewModel::onUnlock, loading = state.working, enabled = state.password.isNotEmpty())
    }
}

/** The camera behind a dimmed screen with a round window, ringed by the orbiting particles. */
@Composable
private fun Viewfinder(cameraAllowed: Boolean, error: String?, onToken: (String) -> Unit, onCancel: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val hole = minOf(maxWidth - 64.dp, 300.dp)
        if (cameraAllowed) PairingCamera(onToken)

        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            val mid = Offset(size.width / 2, size.height / 2)
            drawRect(Color.Black.copy(alpha = 0.62f))
            drawCircle(Color.Transparent, radius = hole.toPx() / 2, center = mid, blendMode = BlendMode.Clear)
            drawCircle(Color.White, radius = hole.toPx() / 2, center = mid, style = Stroke(3.dp.toPx()))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
        ) {
            Text("Hold Your Other Device Here", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                if (cameraAllowed) "On your other device, open Settings → Connect a Device. Then center the cloud it shows in the circle."
                else "Scanning needs the camera. Allow camera access for VaultSpec in Settings.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center,
            )
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF6B6B), textAlign = TextAlign.Center) }
        }
        CircleIconButton(
            icon = AppIcons.CircleX,
            contentDescription = "Cancel",
            onClick = onCancel,
            tint = Color.White,
            containerColor = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = VaultSpecDimens.ScreenPadding, vertical = 8.dp)
                .align(Alignment.TopStart),
        )
    }
}

/** Back camera preview whose frames go through [PairingCode.decode]; frames are never stored. */
@Composable
private fun PairingCamera(onToken: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                var found = false
                analysis.setAnalyzer(executor) { image ->
                    image.use {
                        if (found) return@use
                        val plane = it.planes[0]
                        val buffer = plane.buffer
                        val bytes = ByteArray(buffer.remaining()).also { b -> buffer.get(b) }
                        PairingCode.decode(bytes, it.width, it.height, plane.rowStride)?.let { token ->
                            found = true
                            ContextCompat.getMainExecutor(ctx).execute { onToken(token) }
                        }
                    }
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
