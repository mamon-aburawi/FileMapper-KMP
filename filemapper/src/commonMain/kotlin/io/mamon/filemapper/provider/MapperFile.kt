@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider


/**
 * A platform-agnostic wrapper for selected files.
 * Provides metadata and asynchronous access to raw file data across all KMP targets.
 */
expect class MapperFile {
    /** The name of the file including its extension. */
    val name: String

    /** The file extension (e.g., "xlsx" or "json"). */
    val extension: String

    /**
     * Reads the file content into a [ByteArray].
     * Handles platform-specific input streams or browser-based file readers.
     */
    suspend fun readBytes(): ByteArray
}







