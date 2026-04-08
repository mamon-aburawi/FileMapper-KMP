@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(ExperimentalForeignApi::class)

package io.mamon.filemapper.provider
import platform.Foundation.*
import kotlinx.cinterop.*
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned


actual class FileMapperSaver actual constructor() {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun saveBytes(fileName: String, bytes: ByteArray): Boolean {
        val nsData = memScoped {
            NSData.create(bytes = allocArrayOf(bytes), length = bytes.size.toULong())
        }
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentDirectory = paths.first() as String
        val filePath = "$documentDirectory/$fileName"
        return nsData.writeToFile(filePath, atomically = true)
    }


}



@PublishedApi
internal actual suspend fun saveToDownloads(bytes: ByteArray, fileName: String): String {
    val fileManager = NSFileManager.defaultManager
    val documentDirectory = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
    val fileUrl = documentDirectory.URLByAppendingPathComponent(fileName)
        ?: throw Exception("Could not create file URL")

    val data = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    }

    val success = data.writeToURL(fileUrl, atomically = true)
    if (!success) {
        throw Exception("Failed to write data to $fileUrl")
    }

    return fileUrl.path ?: "Saved to iOS Documents"
}