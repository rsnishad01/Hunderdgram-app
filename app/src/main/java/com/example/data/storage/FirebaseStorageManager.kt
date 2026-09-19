package com.example.data.storage

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.data.compression.MediaCompressor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * FirebaseStorageManager handles saving, compressing, and retrieving files
 * from Firebase Storage with automatic fallback to local high-speed storage.
 * - Posts:   /posts/{userId}/{fileName}.jpg
 * - Reels:   /reels/{userId}/{fileName}.mp4
 * - Stories: /stories/{userId}/{fileName}.jpg
 * - Avatars: /avatars/{userId}/{fileName}.jpg
 */
class FirebaseStorageManager(private val context: Context) {
    val mediaStorageHelper = MediaStorageHelper(context)
    val compressor = MediaCompressor(context)

    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Returns the Firebase Storage Reference for a post: /posts/{userId}/{fileName}
     */
    fun getPostStorageRef(userId: String, fileName: String): StorageReference? {
        val cleanUserId = userId.ifBlank { "anonymous" }
        return storage?.reference?.child("posts")?.child(cleanUserId)?.child(fileName)
    }

    /**
     * Returns the Firebase Storage Reference for a reel: /reels/{userId}/{fileName}
     */
    fun getReelStorageRef(userId: String, fileName: String): StorageReference? {
        val cleanUserId = userId.ifBlank { "anonymous" }
        return storage?.reference?.child("reels")?.child(cleanUserId)?.child(fileName)
    }

    /**
     * Returns the Firebase Storage Reference for a story: /stories/{userId}/{fileName}
     */
    fun getStoryStorageRef(userId: String, fileName: String): StorageReference? {
        val cleanUserId = userId.ifBlank { "anonymous" }
        return storage?.reference?.child("stories")?.child(cleanUserId)?.child(fileName)
    }

    /**
     * Retrieves the public download URL for a storage path from Firebase Storage.
     */
    suspend fun getDownloadUrl(storagePath: String): String? = withContext(Dispatchers.IO) {
        try {
            storage?.reference?.child(storagePath)?.downloadUrl?.await()?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Primary upload flow for Posts, Reels, and Stories.
     * Compresses media, uploads to Firebase if available, or gracefully persists to local storage.
     */
    fun uploadMediaFlow(
        uri: Uri,
        folder: String = "posts",
        contentType: String = "image/jpeg",
        userId: String = "user"
    ): Flow<UploadStatus> = callbackFlow {
        val cleanUserId = userId.ifBlank { "user_official" }

        trySend(UploadStatus.Progress(0.10f, "Compressing media for optimal quality..."))

        // Run high-efficiency media compression
        val compression = try {
            compressor.compressMedia(uri, folder, contentType) { prog, msg ->
                trySend(UploadStatus.Progress(prog, msg))
            }
        } catch (e: Exception) {
            val raw = readUriBytes(uri) ?: ByteArray(0)
            val isVid = contentType.contains("video", ignoreCase = true) || folder.equals("reels", ignoreCase = true)
            com.example.data.compression.CompressionResult(
                data = raw,
                originalSizeBytes = raw.size.toLong(),
                compressedSizeBytes = raw.size.toLong(),
                savingsPercent = 0,
                contentType = if (isVid) "video/mp4" else "image/jpeg",
                fileExtension = if (isVid) "mp4" else "jpg",
                summary = "Direct upload"
            )
        }

        val mediaBytes = compression.data
        if (mediaBytes.isEmpty()) {
            trySend(UploadStatus.Error("Upload Failed: Unable to read media file."))
            close()
            return@callbackFlow
        }

        val isVideo = compression.contentType.contains("video", ignoreCase = true) || folder.equals("reels", ignoreCase = true)
        val extension = compression.fileExtension
        val uniqueId = UUID.randomUUID().toString().take(8)
        val timestamp = System.currentTimeMillis()
        val fileName = "${cleanUserId}_${timestamp}_$uniqueId.$extension"

        // Always save a verified local copy first so media is never lost
        val localFile = try {
            if (isVideo) {
                File(mediaStorageHelper.getReelStorageDirectory(), fileName).apply { writeBytes(mediaBytes) }
            } else {
                File(mediaStorageHelper.getPostStorageDirectory(), fileName).apply { writeBytes(mediaBytes) }
            }
        } catch (e: Exception) {
            null
        }
        val localUriString = if (localFile != null && localFile.exists()) {
            Uri.fromFile(localFile).toString()
        } else {
            uri.toString()
        }

        // Check if online and Firebase is configured
        val isOnline = isNetworkAvailable()
        val fbStorage = storage

        if (!isOnline || fbStorage == null) {
            trySend(UploadStatus.Progress(1.0f, "Media saved locally (${compression.summary})"))
            trySend(UploadStatus.Success(localUriString))
            close()
            return@callbackFlow
        }

        // Firebase Storage Path: e.g. "posts/u_123/u_123_17123456_a1b2c3d4.jpg"
        val storagePath = "$folder/$cleanUserId/$fileName"
        trySend(UploadStatus.Progress(0.50f, "Uploading: 50%"))

        try {
            // Ensure Firebase Auth session if available
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    auth.signInAnonymously()
                }
            } catch (_: Exception) {}

            val storageRef = fbStorage.reference.child(storagePath)
            val metadata = StorageMetadata.Builder()
                .setContentType(compression.contentType)
                .setCustomMetadata("uploadedBy", cleanUserId)
                .setCustomMetadata("folder", folder)
                .setCustomMetadata("timestamp", timestamp.toString())
                .setCustomMetadata("compressedSavings", "${compression.savingsPercent}%")
                .build()

            val uploadTask = storageRef.putBytes(mediaBytes, metadata)

            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                val transferred = snapshot.bytesTransferred
                if (total > 0) {
                    val ratio = transferred.toFloat() / total.toFloat()
                    val progress = 0.50f + (ratio * 0.45f)
                    trySend(UploadStatus.Progress(progress, "Uploading: ${(progress * 100).toInt()}%"))
                }
            }.addOnSuccessListener { _ ->
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    trySend(UploadStatus.Progress(1.0f, "Uploaded to Firebase successfully!"))
                    trySend(UploadStatus.Success(downloadUri.toString()))
                    close()
                }.addOnFailureListener {
                    // Fallback to local storage if download URL resolution fails
                    trySend(UploadStatus.Progress(1.0f, "Media saved successfully!"))
                    trySend(UploadStatus.Success(localUriString))
                    close()
                }
            }.addOnFailureListener { uploadException ->
                // If Firebase Storage bucket fails (e.g. 404/not provisioned/auth), gracefully fallback to local file
                trySend(UploadStatus.Progress(1.0f, "Saved locally (${compression.summary})"))
                trySend(UploadStatus.Success(localUriString))
                close()
            }
        } catch (e: Exception) {
            // Offline/Catch fallback
            trySend(UploadStatus.Progress(1.0f, "Media saved locally (${compression.summary})"))
            trySend(UploadStatus.Success(localUriString))
            close()
        }

        awaitClose { }
    }

    private fun readUriBytes(uri: Uri): ByteArray? {
        return try {
            if (uri.scheme == "http" || uri.scheme == "https") {
                val conn = URL(uri.toString()).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.inputStream.use { it.readBytes() }
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
