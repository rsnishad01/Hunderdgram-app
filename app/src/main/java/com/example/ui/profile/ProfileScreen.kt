package com.example.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PostEntity
import com.example.ui.MainViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.components.GradientActionButton
import com.example.ui.components.SystemCompatibilityBadgeRow
import com.example.ui.feed.EditPostBottomSheet
import com.example.ui.feed.PostOptionsSheet
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import com.example.ui.components.GradientActionButton
import com.example.ui.components.SecondaryActionButton
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramBorderSubtle
import com.example.ui.theme.HundredGramButtonGradient
import com.example.ui.theme.HundredGramCardGlass

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val reels by viewModel.reels.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showProfileMenuSheet by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showSystemCompatSheet by remember { mutableStateOf(false) }
    var activeOptionsPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeEditPost by remember { mutableStateOf<PostEntity?>(null) }

    val userPosts = remember(posts, currentUser.userId) {
        posts.filter { it.authorId == currentUser.userId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        // Profile Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentUser.username,
                color = HundredGramTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(HundredGramCardElevated)
                    .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.5f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 19.dp),
                        onClick = { showProfileMenuSheet = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Avatar and Stats Row inside Elevated Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HundredGramCardBackground)
                .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarUrl = currentUser.avatarUrl,
                    size = 76.dp,
                    hasActiveStory = true,
                    isVerified = currentUser.isVerified
                )
                Spacer(modifier = Modifier.width(24.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProfileStatColumn(count = userPosts.size, label = "Posts")
                    ProfileStatColumn(count = currentUser.followersCount, label = "Followers")
                    ProfileStatColumn(count = currentUser.followingCount, label = "Following")
                }
            }
        }

        // Bio Section
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text(
                text = currentUser.displayName,
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (currentUser.bio.isNotBlank()) {
                Text(
                    text = currentUser.bio,
                    color = HundredGramTextPrimary,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (currentUser.website.isNotBlank()) {
                Text(
                    text = currentUser.website,
                    color = Color(0xFF3897F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryActionButton(
                text = "Edit Profile",
                onClick = { showEditProfileDialog = true },
                modifier = Modifier.weight(1f),
                height = 38.dp
            )
            SecondaryActionButton(
                text = "Share Profile",
                onClick = { /* Share profile */ },
                modifier = Modifier.weight(1f),
                height = 38.dp
            )
            SecondaryActionButton(
                text = "Account / Login",
                onClick = { viewModel.navigateTo(com.example.ui.ScreenDestination.Auth) },
                modifier = Modifier.weight(1.2f),
                height = 38.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Profile Tabs (Posts Grid, Reels, Tagged)
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = HundredGramDarkBackground,
            contentColor = HundredGramPink,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = HundredGramPink,
                    height = 2.5.dp
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                icon = { Icon(Icons.Default.GridOn, contentDescription = "Posts") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                icon = { Icon(Icons.Default.Movie, contentDescription = "Reels") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                icon = { Icon(Icons.Default.PersonPin, contentDescription = "Tagged") }
            )
        }

        // Grid Content
        when (selectedTabIndex) {
            0 -> {
                if (userPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Posts Yet", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("When you share photos, they will appear on your profile.", color = HundredGramTextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(userPosts) { post ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(HundredGramCardElevated)
                                    .clickable { activeOptionsPost = post }
                            ) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = post.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                if (reels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Reels Yet", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Create and watch short fun clips.", color = HundredGramTextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(reels) { reel ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.65f)
                                    .background(HundredGramCardElevated)
                                    .clickable { /* Play reel */ }
                            ) {
                                AsyncImage(
                                    model = reel.thumbnailUri,
                                    contentDescription = reel.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tagged photos yet", color = HundredGramTextSecondary)
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            user = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, bio, site ->
                viewModel.onUpdateProfile(name, bio, site)
                showEditProfileDialog = false
            }
        )
    }

    // Post Options Sheet
    activeOptionsPost?.let { post ->
        PostOptionsSheet(
            post = post,
            isOwner = true,
            onDismiss = { activeOptionsPost = null },
            onSaveToggle = { viewModel.onBookmarkPost(post.id) },
            onShare = { /* Share link */ },
            onEdit = {
                activeOptionsPost = null
                activeEditPost = post
            },
            onDelete = { viewModel.onDeletePost(post.id) }
        )
    }

    // Edit Post Sheet
    activeEditPost?.let { post ->
        EditPostBottomSheet(
            post = post,
            viewModel = viewModel,
            onDismiss = { activeEditPost = null },
            onSave = { newCaption, newLocation ->
                viewModel.onEditPost(post.id, newCaption, newLocation)
                activeEditPost = null
            }
        )
    }

    if (showProfileMenuSheet) {
        AlertDialog(
            onDismissRequest = { showProfileMenuSheet = false },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Options & Settings", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                showPermissionsDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔐", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App Permissions (ऐप अनुमति)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Camera, Audio, Location & Storage", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                showSystemCompatSheet = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("System Specs & Compatibility", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Architecture & Hardware status", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                viewModel.navigateTo(ScreenDestination.Auth)
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Switch Account / Log In (खाता बदलें)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Google, Facebook or Phone OTP", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileMenuSheet = false }) {
                    Text("Close", color = HundredGramPink)
                }
            }
        )
    }

    if (showPermissionsDialog) {
        AppPermissionsDialog(
            onDismiss = { showPermissionsDialog = false }
        )
    }

    if (showSystemCompatSheet) {
        SystemCompatibilityDialog(
            onDismiss = { showSystemCompatSheet = false }
        )
    }
}


@Composable
fun ProfileStatColumn(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            color = HundredGramTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Text(
            text = label,
            color = HundredGramTextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EditProfileDialog(
    user: com.example.data.UserData,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, site: String) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var bio by remember { mutableStateOf(user.bio) }
    var site by remember { mutableStateOf(user.website) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Edit Profile",
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = site,
                    onValueChange = { site = it },
                    label = { Text("Website") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GradientActionButton(
                text = "Save Changes",
                onClick = { onSave(name, bio, site) },
                height = 38.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HundredGramTextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun SystemCompatibilityDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "System Specs & Compatibility",
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hardware profiles and architecture compatibility matrix for optimum social media streaming & HD rendering:",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Text(
                    text = "HARDWARE & RUNTIME BADGES",
                    color = HundredGramTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                // The expanded badge-row showing architecture and memory requirements
                SystemCompatibilityBadgeRow(
                    modifier = Modifier.fillMaxWidth(),
                    scrollable = true,
                    showDetailedRequirements = true
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HundredGramCardElevated)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "⚡ System Architecture Details",
                            color = Color(0xFF6C5CE7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "Target: 64-bit ABI (arm64-v8a, x86_64). Dynamic fallback to 32-bit (armeabi-v7a) with NEON hardware vector instructions.",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🧠 Memory Recommendations",
                            color = Color(0xFF00CEC9),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "Recommended: 4 GB+ RAM for 1080p 60fps video feeds. Minimum: 2 GB RAM. Large heap allocation enabled (256 MB+).",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            GradientActionButton(
                text = "Got It",
                onClick = onDismiss,
                height = 38.dp
            )
        }
    )
}

@Composable
fun AppPermissionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshKey++
    }

    val hasCamera = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val hasAudio = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasLocation = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val hasMedia = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔐 ", fontSize = 18.sp)
                Text(
                    text = "App Permissions (ऐप अनुमति)",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HundredGram requires permissions for camera recording, audio filters, reels, and media uploads:",
                    color = Color.LightGray,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp
                )

                PermissionStatusItem(
                    emoji = "📸",
                    title = "Camera (कैमरा)",
                    subtitle = "For photos, stories & reels",
                    isGranted = hasCamera
                )

                PermissionStatusItem(
                    emoji = "🎤",
                    title = "Microphone (ऑडियो)",
                    subtitle = "For reel sound & voice calls",
                    isGranted = hasAudio
                )

                PermissionStatusItem(
                    emoji = "📍",
                    title = "Location (लोकेशन)",
                    subtitle = "For geotagging posts & explore",
                    isGranted = hasLocation
                )

                PermissionStatusItem(
                    emoji = "🖼️",
                    title = "Photos & Videos (स्टोरेज)",
                    subtitle = "To select & compress media",
                    isGranted = hasMedia
                )
            }
        },
        confirmButton = {
            GradientActionButton(
                text = "अनुमति दें (Grant All)",
                onClick = {
                    val list = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                        list.add(Manifest.permission.READ_MEDIA_IMAGES)
                        list.add(Manifest.permission.READ_MEDIA_VIDEO)
                    }
                    permissionLauncher.launch(list.toTypedArray())
                },
                height = 38.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HundredGramTextSecondary)
            }
        }
    )
}

@Composable
private fun PermissionStatusItem(
    emoji: String,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HundredGramCardElevated)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = HundredGramTextSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isGranted) "✓ Granted" else "✕ Denied",
                    color = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

