package com.example.ui.stories

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.UserAvatar
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private const val STORY_DURATION_MS = 5000L

@Composable
fun StoryViewerScreen(
    viewModel: MainViewModel,
    initialStoryIndex: Int = 0
) {
    val coroutineScope = rememberCoroutineScope()
    val stories by viewModel.stories.collectAsState()
    if (stories.isEmpty()) {
        viewModel.navigateTo(ScreenDestination.Feed)
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialStoryIndex.coerceIn(0, stories.size - 1)) }
    val currentStory = stories[currentIndex]
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isLiked by remember { mutableStateOf(false) }

    // Auto-advance linear progress controller (5 seconds per story)
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val remainingRatio = 1f - progress.value
            val remainingTime = (remainingRatio * STORY_DURATION_MS).toLong().coerceAtLeast(0L)

            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingTime.toInt(),
                    easing = LinearEasing
                )
            )

            if (progress.value >= 1f) {
                if (currentIndex < stories.size - 1) {
                    progress.snapTo(0f)
                    currentIndex++
                } else {
                    viewModel.navigateTo(ScreenDestination.Feed)
                }
            }
        }
    }

    // Reset progress when index changes
    LaunchedEffect(currentIndex) {
        progress.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        coroutineScope.launch {
                            if (offset.x < size.width * 0.35f) {
                                // Tap left: previous story or restart current
                                if (progress.value > 0.25f || currentIndex == 0) {
                                    progress.snapTo(0f)
                                } else {
                                    progress.snapTo(0f)
                                    currentIndex--
                                }
                            } else {
                                // Tap right: next story
                                if (currentIndex < stories.size - 1) {
                                    progress.snapTo(0f)
                                    currentIndex++
                                } else {
                                    viewModel.navigateTo(ScreenDestination.Feed)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // Story Background Media
        AsyncImage(
            model = currentStory.mediaUrl,
            contentDescription = currentStory.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top Gradient Shadow for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Gradient Shadow for actions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Story Top Overlay: Linear Progress Indicators & User Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 12.dp, end = 12.dp)
        ) {
            // Linear Progress Bar Segment Row at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("story_progress_bar_row"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User info row & time stamp
            val timeText = remember(currentStory.timestamp) {
                val diffMinutes = (System.currentTimeMillis() - currentStory.timestamp) / (1000 * 60)
                when {
                    diffMinutes < 1 -> "Just now"
                    diffMinutes < 60 -> "${diffMinutes}m"
                    diffMinutes < 1440 -> "${diffMinutes / 60}h"
                    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(currentStory.timestamp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(avatarUrl = currentStory.userAvatarUrl, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentStory.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $timeText",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.Feed) },
                    modifier = Modifier.testTag("close_story_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Story",
                        tint = Color.White
                    )
                }
            }
        }

        // Caption in center/bottom
        if (currentStory.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = currentStory.caption,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Reply & Reaction Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text("Send message...", color = Color.LightGray, fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("story_reply_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { isLiked = !isLiked },
                modifier = Modifier.testTag("story_like_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like Story",
                    tint = if (isLiked) HundredGramLikeRed else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            if (replyText.isNotBlank()) {
                IconButton(
                    onClick = {
                        viewModel.onSendChatMessage("t_direct", replyText)
                        replyText = ""
                    },
                    modifier = Modifier.testTag("story_send_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Reply",
                        tint = HundredGramPink
                    )
                }
            }
        }
    }
}
