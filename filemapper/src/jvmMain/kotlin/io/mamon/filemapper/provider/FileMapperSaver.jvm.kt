@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider

import java.io.File


actual class FileMapperSaver actual constructor() {

    actual fun saveBytes(fileName: String, bytes: ByteArray): Boolean {
        return try {
            File(fileName).writeBytes(bytes)
            true
        } catch (e: Exception) { false }
    }

}



@PublishedApi
internal actual suspend fun saveToDownloads(bytes: ByteArray, fileName: String): String {
    val home = System.getProperty("user.home")
    val downloadsDir = File(home, "Downloads")

    if (!downloadsDir.exists()) {
        downloadsDir.mkdirs()
    }

    val targetFile = File(downloadsDir, fileName)
    targetFile.writeBytes(bytes)

    return targetFile.absolutePath
}