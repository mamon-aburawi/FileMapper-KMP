@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.engine


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@PublishedApi
internal actual object ZipEngine {

    actual suspend fun unzip(zipBytes: ByteArray): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        val fileMap = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    fileMap[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return@withContext fileMap
    }

    actual suspend fun zip(files: Map<String, ByteArray>): ByteArray = withContext(Dispatchers.IO) {
        val bos = ByteArrayOutputStream()

        ZipOutputStream(bos).use { zos ->
            files.forEach { (fileName, bytes) ->
                val entry = ZipEntry(fileName)
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return@withContext bos.toByteArray()
    }
}