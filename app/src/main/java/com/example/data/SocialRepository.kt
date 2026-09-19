package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.audio.ComprehensiveAudioTrack
import com.example.data.audio.ReelAudioService
import com.example.data.audio.YouTubeSongSearchService
import com.example.data.auth.DeviceAccountsHelper
import com.example.data.auth.FacebookAuthManager
import com.example.data.auth.FirebasePhoneAuthManager
import com.example.data.auth.GoogleAuthManager
import com.example.data.firestore.FirestoreRepository
import com.example.data.location.AppLocationManager
import com.example.data.network.NetworkConnectivityObserver
import com.example.data.network.OnlineSyncService
import com.example.data.network.SmsOtpService
import com.example.data.storage.FirebaseStorageManager
import com.example.data.storage.MediaStorageHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SocialRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val googleAuthManager = GoogleAuthManager(context)
    val facebookAuthManager = FacebookAuthManager(context)
    val phoneAuthManager = FirebasePhoneAuthManager(context)
    val deviceAccountsHelper = DeviceAccountsHelper(context)
    val smsOtpService = SmsOtpService(context)
    val onlineSyncService = OnlineSyncService()
    val firestoreRepository = FirestoreRepository(context)
    val locationManager = AppLocationManager(context)
    val mediaStorageHelper = MediaStorageHelper(context)
    val storageManager = FirebaseStorageManager(context)
    val reelAudioService = ReelAudioService(context)
    val youtubeSearchService = YouTubeSongSearchService(context)
    val nearbyService = com.example.data.nearby.NearbyService(context)
    val connectivityObserver = NetworkConnectivityObserver(context)

    private val _isLoggedIn = MutableStateFlow(checkInitialLoginState())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow(createInitialUserData())
    val currentUser: StateFlow<UserData> = _currentUser.asStateFlow()

    private val _posts = MutableStateFlow<List<PostEntity>>(emptyList())
    val posts: StateFlow<List<PostEntity>> = _posts.asStateFlow()

    private val _stories = MutableStateFlow<List<StoryEntity>>(emptyList())
    val stories: StateFlow<List<StoryEntity>> = _stories.asStateFlow()

    private val _reels = MutableStateFlow<List<ReelVideo>>(emptyList())
    val reels: StateFlow<List<ReelVideo>> = _reels.asStateFlow()

    private val _comments = MutableStateFlow<Map<String, List<CommentEntity>>>(emptyMap())
    val comments: StateFlow<Map<String, List<CommentEntity>>> = _comments.asStateFlow()

    private val _chatThreads = MutableStateFlow<List<ChatThreadEntity>>(emptyList())
    val chatThreads: StateFlow<List<ChatThreadEntity>> = _chatThreads.asStateFlow()

    private val _chatMessages = MutableStateFlow<Map<String, List<ChatMessageEntity>>>(emptyMap())
    val chatMessages: StateFlow<Map<String, List<ChatMessageEntity>>> = _chatMessages.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallLogItem>>(emptyList())
    val callLogs: StateFlow<List<CallLogItem>> = _callLogs.asStateFlow()

    init {
        // Sync online posts, reels, and stories from live Firestore backend
        scope.launch {
            syncOnlineData()
            val current = _currentUser.value
            if (_isLoggedIn.value && current.userId.isNotBlank()) {
                val savedProfile = firestoreRepository.getUserFromFirestore(current.userId)
                if (savedProfile != null) {
                    _currentUser.value = savedProfile
                } else {
                    firestoreRepository.syncUserToFirestore(current)
                }
            }
        }
    }

    private fun checkInitialLoginState(): Boolean {
        return try {
            val fbUser = FirebaseAuth.getInstance().currentUser
            fbUser != null && !fbUser.isAnonymous
        } catch (e: Exception) {
            false
        }
    }

    fun loginUser(user: UserData) {
        _currentUser.value = user
        _isLoggedIn.value = true
        scope.launch {
            firestoreRepository.syncUserToFirestore(user)
        }
    }

    fun logoutUser() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        _currentUser.value = createGuestUserData()
        _isLoggedIn.value = false
    }

    fun setCurrentUser(user: UserData) {
        loginUser(user)
    }

    private fun createGuestUserData(): UserData {
        return UserData(
            userId = "",
            username = "guest",
            displayName = "Guest User",
            bio = "Please log in to manage your account",
            avatarUrl = "",
            followersCount = 0,
            followingCount = 0,
            postsCount = 0,
            isVerified = false,
            website = "",
            location = ""
        )
    }

    private fun createInitialUserData(): UserData {
        val fbUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }

        return if (fbUser != null && !fbUser.isAnonymous) {
            UserData(
                userId = fbUser.uid,
                username = fbUser.email?.substringBefore("@") ?: "user_${fbUser.uid.take(6)}",
                displayName = fbUser.displayName ?: "HundredGram Member",
                bio = "HundredGram Creator ✨",
                avatarUrl = fbUser.photoUrl?.toString() ?: "",
                followersCount = 0,
                followingCount = 0,
                postsCount = 0,
                isVerified = false,
                website = "",
                location = ""
            )
        } else {
            createGuestUserData()
        }
    }

    fun addCallLog(
        callerName: String,
        callerAvatar: String,
        isVideo: Boolean,
        durationSeconds: Int,
        isOutgoing: Boolean = true
    ) {
        val newCall = CallLogItem(
            id = "cl_${UUID.randomUUID()}",
            callerId = "caller_${callerName.lowercase().replace(" ", "_")}",
            callerName = callerName,
            callerAvatar = callerAvatar,
            type = if (isVideo) CallType.VIDEO else CallType.VOICE,
            direction = if (durationSeconds == 0 && !isOutgoing) CallDirection.MISSED else if (isOutgoing) CallDirection.OUTGOING else CallDirection.INCOMING,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        _callLogs.value = listOf(newCall) + _callLogs.value
    }

    fun clearCallLogs() {
        _callLogs.value = emptyList()
    }

    fun toggleLikePost(postId: String) {
        val updated = _posts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLiked
                val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post.copy(isLiked = newLiked, likesCount = newCount)
            } else post
        }
        _posts.value = updated
    }

    fun toggleBookmarkPost(postId: String) {
        val updated = _posts.value.map { post ->
            if (post.id == postId) post.copy(isSaved = !post.isSaved) else post
        }
        _posts.value = updated
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        val author = _currentUser.value
        val newComment = CommentEntity(
            id = "c_${UUID.randomUUID()}",
            postId = postId,
            authorId = author.userId,
            authorUsername = author.username,
            authorAvatarUrl = author.avatarUrl,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        val currentComments = _comments.value[postId]?.toMutableList() ?: mutableListOf()
        currentComments.add(newComment)
        _comments.value = _comments.value + (postId to currentComments)

        // Increment comments count on post
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(commentsCount = it.commentsCount + 1) else it
        }
    }

    fun toggleLikeReel(reelId: String) {
        val updated = _reels.value.map { reel ->
            if (reel.id == reelId) {
                val newLiked = !reel.isLiked
                val newCount = if (newLiked) reel.likesCount + 1 else (reel.likesCount - 1).coerceAtLeast(0)
                reel.copy(isLiked = newLiked, likesCount = newCount)
            } else reel
        }
        _reels.value = updated
    }

    fun toggleBookmarkReel(reelId: String) {
        val updated = _reels.value.map { reel ->
            if (reel.id == reelId) reel.copy(isSaved = !reel.isSaved) else reel
        }
        _reels.value = updated
    }

    suspend fun publishPostAfterUpload(mediaUrl: String, caption: String, location: String = "") {
        val author = _currentUser.value
        val finalLocation = if (location.isNotBlank()) location else locationManager.getCurrentLocationName()

        val newPost = PostEntity(
            id = "p_${UUID.randomUUID()}",
            authorId = author.userId,
            authorUsername = author.username,
            authorAvatarUrl = author.avatarUrl,
            imageUrl = mediaUrl,
            caption = caption,
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            timestamp = System.currentTimeMillis(),
            location = if (finalLocation != "Location unavailable") finalLocation else ""
        )
        _posts.value = listOf(newPost) + _posts.value
        _currentUser.value = author.copy(postsCount = author.postsCount + 1)
        firestoreRepository.publishPost(newPost)
    }

    suspend fun publishStoryAfterUpload(mediaUrl: String, caption: String = "") {
        val author = _currentUser.value
        val newStory = StoryEntity(
            id = "s_${UUID.randomUUID()}",
            userId = author.userId,
            username = author.username,
            userAvatarUrl = author.avatarUrl,
            mediaUrl = mediaUrl,
            caption = caption,
            timestamp = System.currentTimeMillis()
        )
        _stories.value = listOf(newStory) + _stories.value
        firestoreRepository.publishStory(newStory)
    }

    suspend fun publishReelAfterUpload(mediaUrl: String, caption: String, audioTitle: String, audioArtist: String) {
        val author = _currentUser.value
        val newReel = ReelVideo(
            id = "r_${UUID.randomUUID()}",
            authorId = author.userId,
            authorUsername = author.username,
            authorAvatarUrl = author.avatarUrl,
            videoUrl = mediaUrl,
            thumbnailUri = mediaUrl,
            caption = caption,
            audioTitle = audioTitle.ifBlank { "Original Audio" },
            audioArtist = audioArtist.ifBlank { author.displayName },
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            timestamp = System.currentTimeMillis()
        )
        _reels.value = listOf(newReel) + _reels.value
        firestoreRepository.publishReel(newReel)
    }

    fun editPost(postId: String, newCaption: String, newLocation: String) {
        val updated = _posts.value.map { post ->
            if (post.id == postId) {
                post.copy(
                    caption = newCaption,
                    location = newLocation.ifBlank { post.location }
                )
            } else post
        }
        _posts.value = updated
    }

    fun deletePost(postId: String) {
        _posts.value = _posts.value.filterNot { it.id == postId }
        val author = _currentUser.value
        _currentUser.value = author.copy(postsCount = (author.postsCount - 1).coerceAtLeast(0))
    }

    fun sendChatMessage(threadId: String, text: String, mediaUrl: String = "") {
        if (text.isBlank() && mediaUrl.isBlank()) return
        val author = _currentUser.value
        val message = ChatMessageEntity(
            messageId = "m_${UUID.randomUUID()}",
            threadId = threadId,
            senderId = author.userId,
            senderUsername = author.username,
            text = text,
            mediaUrl = mediaUrl,
            isMedia = mediaUrl.isNotBlank(),
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            isRead = true
        )

        val currentList = _chatMessages.value[threadId]?.toMutableList() ?: mutableListOf()
        currentList.add(message)
        _chatMessages.value = _chatMessages.value + (threadId to currentList)

        // Update or create thread
        val existingIndex = _chatThreads.value.indexOfFirst { it.threadId == threadId }
        if (existingIndex >= 0) {
            _chatThreads.value = _chatThreads.value.map {
                if (it.threadId == threadId) {
                    it.copy(
                        lastMessageText = if (text.isNotBlank()) text else "📷 Photo message",
                        lastMessageTimestamp = System.currentTimeMillis(),
                        unreadCount = 0
                    )
                } else it
            }
        } else {
            val newThread = ChatThreadEntity(
                threadId = threadId,
                recipientId = threadId,
                recipientUsername = "User",
                recipientAvatarUrl = "",
                lastMessageText = if (text.isNotBlank()) text else "📷 Photo message",
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                isOnline = false
            )
            _chatThreads.value = listOf(newThread) + _chatThreads.value
        }

        scope.launch {
            firestoreRepository.sendChatMessage(message)
        }
    }

    fun reactToMessage(threadId: String, messageId: String, emoji: String) {
        val list = _chatMessages.value[threadId]?.map {
            if (it.messageId == messageId) it.copy(reaction = if (it.reaction == emoji) "" else emoji) else it
        } ?: return
        _chatMessages.value = _chatMessages.value + (threadId to list)
    }

    fun updateUserProfile(displayName: String, bio: String, website: String, avatarUrl: String? = null) {
        val updated = _currentUser.value.copy(
            displayName = displayName,
            bio = bio,
            website = website,
            avatarUrl = avatarUrl ?: _currentUser.value.avatarUrl
        )
        _currentUser.value = updated
        scope.launch {
            firestoreRepository.syncUserToFirestore(updated)
        }
    }

    suspend fun syncOnlineData() {
        // Sync online posts
        val onlinePosts = onlineSyncService.fetchLiveOnlinePosts()
        if (onlinePosts.isNotEmpty()) {
            val existingIds = _posts.value.map { it.id }.toSet()
            val newItems = onlinePosts.filter { it.id !in existingIds }
            _posts.value = newItems + _posts.value
        }

        // Sync online reels
        val onlineReels = onlineSyncService.fetchLiveOnlineReels()
        if (onlineReels.isNotEmpty()) {
            val existingReelIds = _reels.value.map { it.id }.toSet()
            val newReels = onlineReels.filter { it.id !in existingReelIds }
            _reels.value = newReels + _reels.value
        }

        // Sync online stories
        val onlineStories = onlineSyncService.fetchLiveOnlineStories()
        if (onlineStories.isNotEmpty()) {
            val existingStoryIds = _stories.value.map { it.id }.toSet()
            val newStories = onlineStories.filter { it.id !in existingStoryIds }
            _stories.value = newStories + _stories.value
        }
    }

    suspend fun syncOnlinePosts() {
        syncOnlineData()
    }

    fun getAvailableAudioTracks(): List<ComprehensiveAudioTrack> {
        return listOf(
            ComprehensiveAudioTrack("trk_1", "Desi Dhol Beats", "Punjab Express", 32, "", R.raw.track_punjabi_dhol),
            ComprehensiveAudioTrack("trk_2", "Groovy Funk Vibes", "Metro Groove", 45, "", R.raw.track_funk_groove),
            ComprehensiveAudioTrack("trk_3", "Home Relaxed Lo-fi", "Chill Sessions", 60, "", R.raw.track_home_groove),
            ComprehensiveAudioTrack("trk_4", "Festive Dhak Dhamaal", "Kolkata Rhythms", 50, "", R.raw.track_festive_dhak),
            ComprehensiveAudioTrack("trk_5", "Sitar Melody & Flute", "Indian Classical Trio", 40, "", R.raw.track_sitar_melody),
            ComprehensiveAudioTrack("trk_6", "Energetic Dhol Trance", "Club Dholaks", 35, "", R.raw.track_energetic_dhol)
        )
    }
}

