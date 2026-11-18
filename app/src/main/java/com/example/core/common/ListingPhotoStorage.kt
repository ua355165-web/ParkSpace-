package com.example.core.common

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies photos selected via the zero-permission Android Photo Picker into
 * app-private storage so URIs remain readable across app restarts without
 * requesting broad media or external storage permissions.
 */
object ListingPhotoStorage {
  private const val PHOTOS_DIR = "listing_photos"
  private const val MAX_BYTES = 5 * 1024 * 1024 // 5 MB safety cap per image

  suspend fun copyPickedPhotoToInternalStorage(
    context: Context,
    sourceUri: Uri,
    spaceIdHint: String = "listing",
  ): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val dir = File(context.filesDir, PHOTOS_DIR).apply { if (!exists()) mkdirs() }
        val safePrefix = spaceIdHint.replace(Regex("[^A-Za-z0-9_-]"), "").take(16).ifBlank { "space" }
        val targetFile = File(dir, "${safePrefix}_${UUID.randomUUID().toString().take(8)}.jpg")

        val stream =
          context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalStateException("Could not open selected photo.")
        stream.use { input ->
          targetFile.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
              val read = input.read(buffer)
              if (read <= 0) break
              total += read
              if (total > MAX_BYTES) {
                throw IllegalStateException("Selected photo exceeds 5 MB limit.")
              }
              output.write(buffer, 0, read)
            }
          }
        }
        Uri.fromFile(targetFile).toString()
      }
    }
}
