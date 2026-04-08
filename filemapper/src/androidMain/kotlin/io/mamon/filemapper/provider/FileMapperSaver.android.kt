@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider


import android.content.ContentValues
import android.provider.MediaStore
import io.mamon.filemapper.FlexContextProvider
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment

import androidx.core.content.ContextCompat


actual class FileMapperSaver actual constructor() {

    actual fun saveBytes(fileName: String, bytes: ByteArray): Boolean {

        return try {
            val file = File("/storage/emulated/0/Download/$fileName")
            FileOutputStream(file).use { it.write(bytes) }
            true
        } catch (e: Exception) { false }
    }

}




@PublishedApi
internal actual suspend fun saveToDownloads(bytes: ByteArray, fileName: String): String {
    val context = FlexContextProvider.applicationContext!!
    val mimeType = if (fileName.endsWith(".json")) "application/json" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    // Android 10+ (API 29 and higher): MediaStore (No permissions required)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create MediaStore record for $fileName")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()
        } ?: throw Exception("Failed to open output stream")

        return "Saved to Downloads folder: $fileName"
    }
    // Android 9 and below (API 28 and lower): Legacy File API (Requires Permission)
    else {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            throw SecurityException("WRITE_EXTERNAL_STORAGE")
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()
        }

        return "Saved to Downloads folder: ${file.absolutePath}"
    }
}
