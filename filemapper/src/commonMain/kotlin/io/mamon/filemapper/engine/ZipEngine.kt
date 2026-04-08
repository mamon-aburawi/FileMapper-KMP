@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.engine




@PublishedApi
internal expect object ZipEngine {

    /**
     * * Extracts a zipped ByteArray into a Map of <FilePath, FileBytes>.
     */
    suspend fun unzip(zipBytes: ByteArray): Map<String, ByteArray>



    /**
     * * Compresses a Map of <FilePath, FileBytes> back into a zipped ByteArray.
     */
    suspend fun zip(files: Map<String, ByteArray>): ByteArray
}