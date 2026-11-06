package dev.kyrylost.smartstatusbar.sample.photo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.kyrylost.smartstatusbar.SmartStatusBarConfig
import dev.kyrylost.smartstatusbar.SmartStatusBarOverride
import dev.kyrylost.smartstatusbar.UpdateStrategy
import dev.kyrylost.smartstatusbar.rememberSmartStatusBarController
import dev.kyrylost.smartstatusbar.sample.ui.SampleTheme
import dev.kyrylost.smartstatusbar.sample.ui.SceneHeader
import dev.kyrylost.smartstatusbar.sample.ui.sceneContentColor

// The dusk of the sky screen, while no photo is picked.
private val EmptyBackground = Brush.verticalGradient(listOf(Color(0xFF2E2357), Color(0xFFC0627D)))

/** The strategy of this screen's override: it knows when what is under the status bar changes. */
val PhotoStrategy: UpdateStrategy = UpdateStrategy.Manual

/**
 * A photo from the user's gallery, full-bleed, to drag and zoom: whatever part ends up under the status
 * bar decides the icons. The photo picker needs no permission.
 *
 * Unlike the other screens, this one tells the library when to check: once it has [shown] (the tab
 * transition is over), once a photo has loaded and whenever the finger lifts, so the icons never
 * change in the middle of a gesture. [libraryEnabled] only mirrors the switch of the control panel.
 */
@Composable
fun PhotoScreen(libraryEnabled: Boolean, shown: Boolean) {
    if (libraryEnabled) {
        SmartStatusBarOverride(config = SmartStatusBarConfig(strategy = PhotoStrategy))
    }
    val controller = rememberSmartStatusBarController()
    LaunchedEffect(shown) {
        if (shown) controller.requestUpdate()
    }

    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = uri
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Box(Modifier.fillMaxSize().background(EmptyBackground)) {
        photo?.let { DraggablePhoto(it, onSettled = controller::requestUpdate) }
        SceneHeader(
            title = "Your photo",
            subtitle = if (photo == null) "Any image from your gallery" else "Drag or pinch it under the status bar",
        ) {
            PickButton(text = if (photo == null) "Pick a photo" else "Pick another", onClick = pick)
        }
    }
}

/**
 * Fills the screen like a photo viewer: drag to move it, pinch to zoom. It starts zoomed in a little so
 * that it can be moved up and down as well as sideways, whatever its proportions. [onSettled] runs once
 * the photo has loaded and whenever the last finger lifts.
 */
@Composable
private fun DraggablePhoto(uri: Uri, onSettled: () -> Unit) {
    var zoom by rememberSaveable(uri) { mutableFloatStateOf(1.5f) }
    var offsetX by rememberSaveable(uri) { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable(uri) { mutableFloatStateOf(0f) }
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { onSettled() },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(uri) {
                // Only watches: the transform gestures below move the photo.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (awaitPointerEvent().changes.any { it.pressed }) Unit
                    onSettled()
                }
            }
            .pointerInput(uri) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                    // Keep the screen covered: the zoomed layer may move by its overflow on each side. (The
                    // image is cropped to the screen before it is zoomed, so that is all there is to show.)
                    val maxX = (zoom - 1) * size.width / 2
                    val maxY = (zoom - 1) * size.height / 2
                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                }
            }
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = offsetX
                translationY = offsetY
            },
    )
}

@Composable
private fun PickButton(text: String, onClick: () -> Unit) {
    val content = sceneContentColor()
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = content.copy(alpha = 0.18f),
            contentColor = content,
        ),
        modifier = Modifier.padding(top = 20.dp),
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun PhotoPreview() {
    SampleTheme { PhotoScreen(libraryEnabled = false, shown = true) }
}
