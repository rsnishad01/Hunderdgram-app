package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogItem
import com.example.data.ChatMessageEntity
import com.example.data.ChatThreadEntity
import com.example.data.CommentEntity
import com.example.data.PostEntity
import com.example.data.ReelVideo
import com.example.data.SocialRepository
import com.example.data.StoryEntity
import com.example.data.UserData
import com.example.data.audio.ComprehensiveAudioTrack
import com.example.data.nearby.NearbyEvent
import com.example.data.nearby.NearbyUser
import com.example.data.network.NetworkStatus
import com.example.data.storage.UploadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Auth : ScreenDestination()
    object Feed : ScreenDestination()
    object Explore : ScreenDestination()
    object Reels : ScreenDestination()
    object DirectMessages : ScreenDestination()
    data class ChatDetail(val threadId: String, val recipientUsername: String, val recipientAvatar: String) : ScreenDestination()
    data class StoryViewer(val storyIndex: Int) : ScreenDestination()
    object Camera : ScreenDestination()
    object FilterStudio : ScreenDestination()
    object CreateReel : ScreenDestination()
    object Profile : ScreenDestination()
    data class Call(val callName: String, val isVideo: Boolean) : ScreenDestination()
    object LiveRoom : ScreenDestination()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = SocialRepository(application)

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val currentUser: StateFlow<UserData> = repository.currentUser
    val posts: StateFlow<List<PostEntity>> = repository.posts
    val stories: StateFlow<List<StoryEntity>> = repository.stories
    val reels: StateFlow<List<ReelVideo>> = repository.reels
    val chatThreads: StateFlow<List<ChatThreadEntity>> = repository.chatThreads
    val chatMessages: StateFlow<Map<String, List<ChatMessageEntity>>> = repository.chatMessages
    val comments: StateFlow<Map<String, List<CommentEntity>>> = repository.comments
    val callLogs: StateFlow<List<CallLogItem>> = repository.callLogs
    val audioPlaybackInfo = repository.reelAudioService.playbackInfo

    val networkStatus: StateFlow<NetworkStatus> = repository.connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStatus.Available)

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Feed)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _uploadMessage = MutableStateFlow("Uploading...")
    val uploadMessage: StateFlow<String> = _uploadMessage.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    private val _loginPromptReason = MutableStateFlow("इस एक्शन के लिए पहले लॉगिन करें")
    val loginPromptReason: StateFlow<String> = _loginPromptReason.asStateFlow()

    private val _nearbyUsers = MutableStateFlow<List<NearbyUser>>(emptyList())
    val nearbyUsers: StateFlow<List<NearbyUser>> = _nearbyUsers.asStateFlow()

    private val _nearbyEvents = MutableStateFlow<List<NearbyEvent>>(emptyList())
    val nearbyEvents: StateFlow<List<NearbyEvent>> = _nearbyEvents.asStateFlow()

    private val _selectedFilterImageUri = MutableStateFlow<Uri?>(null)
    val selectedFilterImageUri: StateFlow<Uri?> = _selectedFilterImageUri.asStateFlow()

    init {
        loadNearbyData()
    }

    fun requireAuth(reason: String = "इस फीचर के लिए पहले लॉगिन करें", onAuthorized: () -> Unit) {
        if (isLoggedIn.value) {
            onAuthorized()
        } else {
            _loginPromptReason.value = reason
            _showLoginPrompt.value = true
        }
    }

    fun dismissLoginPrompt() {
        _showLoginPrompt.value = false
    }

    fun openLoginScreen() {
        _showLoginPrompt.value = false
        navigateTo(ScreenDestination.Auth)
    }

    fun logout() {
        repository.logoutUser()
        navigateTo(ScreenDestination.Feed)
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    fun onLikePost(postId: String) {
        requireAuth("पोस्ट को लाइक / रिएक्ट करने के लिए पहले लॉगिन करें") {
            repository.toggleLikePost(postId)
        }
    }

    fun onBookmarkPost(postId: String) {
        requireAuth("पोस्ट को सेव करने के लिए पहले लॉगिन करें") {
            repository.toggleBookmarkPost(postId)
        }
    }

    fun onAddComment(postId: String, text: String) {
        requireAuth("कमेंट करने के लिए पहले लॉगिन करें") {
            repository.addComment(postId, text)
        }
    }

    fun onLikeReel(reelId: String) {
        requireAuth("रील को लाइक / रिएक्ट करने के लिए पहले लॉगिन करें") {
            repository.toggleLikeReel(reelId)
        }
    }

    fun onBookmarkReel(reelId: String) {
        requireAuth("रील को सेव करने के लिए पहले लॉगिन करें") {
            repository.toggleBookmarkReel(reelId)
        }
    }

    fun onSendChatMessage(threadId: String, text: String, mediaUrl: String = "") {
        requireAuth("मैसेज भेजने के लिए पहले लॉगिन करें") {
            repository.sendChatMessage(threadId, text, mediaUrl)
        }
    }

    fun onReactToMessage(threadId: String, messageId: String, emoji: String) {
        requireAuth("रिएक्शन देने के लिए पहले लॉगिन करें") {
            repository.reactToMessage(threadId, messageId, emoji)
        }
    }

    fun onDeletePost(postId: String) {
        repository.deletePost(postId)
    }

    fun onEditPost(postId: String, newCaption: String, newLocation: String) {
        repository.editPost(postId, newCaption, newLocation)
    }

    fun onEndCall(
        callerName: String,
        callerAvatar: String = "",
        isVideo: Boolean,
        durationSeconds: Int,
        isOutgoing: Boolean = true
    ) {
        repository.addCallLog(
            callerName = callerName,
            callerAvatar = callerAvatar,
            isVideo = isVideo,
            durationSeconds = durationSeconds,
            isOutgoing = isOutgoing
        )
        navigateTo(ScreenDestination.DirectMessages)
    }

    fun initiateCall(name: String, isVideo: Boolean) {
        navigateTo(ScreenDestination.Call(callName = name, isVideo = isVideo))
    }

    fun clearCallHistory() {
        repository.clearCallLogs()
    }

    suspend fun getPopularLocations(): List<String> {
        return repository.locationManager.getPopularLocations()
    }

    suspend fun getCurrentLocationName(): String {
        return repository.locationManager.getCurrentLocationName()
    }

    fun onUpdateProfile(displayName: String, bio: String, website: String, avatarUrl: String? = null) {
        repository.updateUserProfile(displayName, bio, website, avatarUrl)
    }

    fun loginUser(user: UserData) {
        repository.loginUser(user)
    }

    fun onRefreshFeed() {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Syncing live feed..."
            _uploadProgress.value = 0.5f
            repository.syncOnlinePosts()
            _uploadProgress.value = 1f
            _isUploading.value = false
        }
    }

    fun createPost(uri: Uri, caption: String, location: String) {
        requireAuth("पोस्ट अपलोड करने के लिए पहले लॉगिन करें") {
            viewModelScope.launch {
                _uploadError.value = null
                _isUploading.value = true
                _uploadProgress.value = 0.05f
                _uploadMessage.value = "Uploading..."

                val authorId = currentUser.value.userId
                repository.storageManager.uploadMediaFlow(
                    uri = uri,
                    folder = "posts",
                    contentType = "image/jpeg",
                    userId = authorId
                ).collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            _isUploading.value = true
                            _uploadProgress.value = status.progressPercent
                            _uploadMessage.value = status.statusMessage
                        }
                        is UploadStatus.Error -> {
                            _isUploading.value = false
                            _uploadError.value = status.errorMessage
                            _uploadMessage.value = status.errorMessage
                        }
                        is UploadStatus.Success -> {
                            _uploadProgress.value = 1f
                            _uploadMessage.value = "Published to HundredGram!"
                            repository.publishPostAfterUpload(status.downloadUrl, caption, location)
                            _isUploading.value = false
                            navigateTo(ScreenDestination.Feed)
                        }
                        is UploadStatus.Idle -> {}
                    }
                }
            }
        }
    }

    fun createStory(uri: Uri, caption: String) {
        requireAuth("स्टोरी अपलोड करने के लिए पहले लॉगिन करें") {
            viewModelScope.launch {
                _uploadError.value = null
                _isUploading.value = true
                _uploadProgress.value = 0.05f
                _uploadMessage.value = "Uploading..."

                val authorId = currentUser.value.userId
                repository.storageManager.uploadMediaFlow(
                    uri = uri,
                    folder = "stories",
                    contentType = "image/jpeg",
                    userId = authorId
                ).collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            _isUploading.value = true
                            _uploadProgress.value = status.progressPercent
                            _uploadMessage.value = status.statusMessage
                        }
                        is UploadStatus.Error -> {
                            _isUploading.value = false
                            _uploadError.value = status.errorMessage
                            _uploadMessage.value = status.errorMessage
                        }
                        is UploadStatus.Success -> {
                            _uploadProgress.value = 1f
                            _uploadMessage.value = "Story added!"
                            repository.publishStoryAfterUpload(status.downloadUrl, caption)
                            _isUploading.value = false
                            navigateTo(ScreenDestination.Feed)
                        }
                        is UploadStatus.Idle -> {}
                    }
                }
            }
        }
    }

    fun createReel(uri: Uri, caption: String, audioTitle: String, audioArtist: String) {
        requireAuth("रील अपलोड करने के लिए पहले लॉगिन करें") {
            viewModelScope.launch {
                _uploadError.value = null
                _isUploading.value = true
                _uploadProgress.value = 0.05f
                _uploadMessage.value = "Uploading..."

                val authorId = currentUser.value.userId
                repository.storageManager.uploadMediaFlow(
                    uri = uri,
                    folder = "reels",
                    contentType = "video/mp4",
                    userId = authorId
                ).collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            _isUploading.value = true
                            _uploadProgress.value = status.progressPercent
                            _uploadMessage.value = status.statusMessage
                        }
                        is UploadStatus.Error -> {
                            _isUploading.value = false
                            _uploadError.value = status.errorMessage
                            _uploadMessage.value = status.errorMessage
                        }
                        is UploadStatus.Success -> {
                            _uploadProgress.value = 1f
                            _uploadMessage.value = "Reel published!"
                            repository.publishReelAfterUpload(status.downloadUrl, caption, audioTitle, audioArtist)
                            _isUploading.value = false
                            navigateTo(ScreenDestination.Reels)
                        }
                        is UploadStatus.Idle -> {}
                    }
                }
            }
        }
    }

    fun setSelectedFilterImage(uri: Uri) {
        _selectedFilterImageUri.value = uri
        navigateTo(ScreenDestination.FilterStudio)
    }

    fun playAudioTrack(track: ComprehensiveAudioTrack) {
        repository.reelAudioService.playAudioTrack(track)
    }

    fun stopAudio() {
        repository.reelAudioService.stop()
    }

    fun toggleMuteAudio() {
        repository.reelAudioService.toggleMute()
    }

    private fun loadNearbyData() {
        viewModelScope.launch {
            val result = repository.nearbyService.searchNearby()
            _nearbyUsers.value = result.users
            _nearbyEvents.value = result.events
        }
    }
}
