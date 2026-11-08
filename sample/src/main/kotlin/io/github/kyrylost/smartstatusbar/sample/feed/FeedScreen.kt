package io.github.kyrylost.smartstatusbar.sample.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.kyrylost.smartstatusbar.sample.ui.SceneHeader
import io.github.kyrylost.smartstatusbar.sample.ui.LocalPanelClearance
import io.github.kyrylost.smartstatusbar.sample.ui.SampleTheme
import kotlinx.coroutines.launch

/**
 * Photos by id on picsum.photos: ids picked for a clearly bright or clearly dark top edge, alternating,
 * so the icons flip as the feed scrolls. The fallback color matches the photo's top edge, so the
 * screen behaves the same offline.
 */
private class Photo(val id: Int, val brightTop: Boolean)

private val Photos = listOf(
    Photo(12, brightTop = true),
    Photo(26, brightTop = false),
    Photo(24, brightTop = true),
    Photo(84, brightTop = false),
    Photo(46, brightTop = true),
    Photo(122, brightTop = false),
    Photo(51, brightTop = true),
    Photo(15, brightTop = false),
)

@Composable
fun FeedScreen() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        // The title sits on the first photo and scrolls away with it, clear of the captions below.
        item(key = Photos.first().id) {
            Box {
                PhotoCard(Photos.first())
                SceneHeader(title = "Photos", subtitle = "Scroll the feed")
            }
        }
        items(Photos.drop(1), key = { it.id }) { PhotoCard(it) }
        item(key = "end") {
            EndOfFeed(onBackToTop = { scope.launch { listState.animateScrollToItem(0) } })
        }
    }
}

// The night of the sky screen.
private val EndBackground = Brush.verticalGradient(listOf(Color(0xFF070B1E), Color(0xFF1A2550)))

/** A last scene after the photos, reaching down under the panel so that the feed ends without a gap. */
@Composable
private fun EndOfFeed(onBackToTop: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(EndBackground)
            .padding(top = 64.dp, bottom = LocalPanelClearance.current + 40.dp, start = 24.dp, end = 24.dp),
    ) {
        Text(text = "That's all", style = MaterialTheme.typography.displayMedium, color = Color.White)
        Text(
            text = "Photos from Lorem Picsum",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Button(
            onClick = onBackToTop,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text("Back to top")
        }
    }
}

@Composable
private fun PhotoCard(photo: Photo) {
    val fallback = if (photo.brightTop) Color(0xFFE8EDF3) else Color(0xFF14181F)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .background(fallback),
    ) {
        AsyncImage(
            model = "https://picsum.photos/id/${photo.id}/1080/1440",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000))))
                .padding(24.dp),
        ) {
            Text(
                text = "PHOTO ${photo.id}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = if (photo.brightTop) "Bright top" else "Dark top",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
        }
    }
}

@Preview
@Composable
private fun FeedPreview() {
    SampleTheme { FeedScreen() }
}
