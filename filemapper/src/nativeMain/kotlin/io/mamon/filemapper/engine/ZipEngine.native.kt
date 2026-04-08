@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import no.synth.kmpzip.io.ByteArrayInputStream
import no.synth.kmpzip.io.ByteArrayOutputStream
import no.synth.kmpzip.zip.ZipInputStream
import no.synth.kmpzip.zip.ZipOutputStream
import no.synth.kmpzip.zip.ZipEntry

@PublishedApi
internal actual object ZipEngine {

    actual suspend fun unzip(zipBytes: ByteArray): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        val fileMap = mutableMapOf<String, ByteArray>()

        val zis = ZipInputStream(ByteArrayInputStream(zipBytes))

        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                fileMap[entry.name] = zis.readBytes()
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()

        return@withContext fileMap
    }


    actual suspend fun zip(files: Map<String, ByteArray>): ByteArray = withContext(Dispatchers.IO) {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        files.forEach { (fileName, bytes) ->
            zos.putNextEntry(ZipEntry(fileName))
            zos.write(bytes)
            zos.closeEntry()
        }

        zos.finish()
        zos.close()

        return@withContext bos.toByteArray()
    }

}