@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider

expect class FileMapperSaver() {
    fun saveBytes(fileName: String, bytes: ByteArray): Boolean
}



@PublishedApi
internal expect suspend fun saveToDownloads(bytes: ByteArray, fileName: String): String
