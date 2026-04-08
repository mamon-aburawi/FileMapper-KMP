@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.engine

import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise


@JsModule("jszip")
@JsNonModule
external class JSZip {
    fun file(name: String, data: Uint8Array)
    fun file(name: String): JSZipObject?
    fun generateAsync(options: dynamic): Promise<Uint8Array>
    fun loadAsync(data: Uint8Array): Promise<JSZip>
    val files: dynamic // JavaScript Object containing file keys
}

external interface JSZipObject {
    val name: String
    val dir: Boolean
    fun async(type: String): Promise<Uint8Array>
}


@PublishedApi
internal actual object ZipEngine {

    actual suspend fun unzip(zipBytes: ByteArray): Map<String, ByteArray> {
        val zip = JSZip()
        val loadedZip = zip.loadAsync(zipBytes.toUint8Array()).await()

        val fileMap = mutableMapOf<String, ByteArray>()
        val fileKeys = js("Object.keys(loadedZip.files)") as Array<String>

        for (key in fileKeys) {
            val fileObj = loadedZip.file(key)
            if (fileObj != null && !fileObj.dir) {
                // Extract each file as a Uint8Array, then convert to Kotlin ByteArray
                val uInt8Array = fileObj.async("uint8array").await()
                fileMap[key] = uInt8Array.toByteArray()
            }
        }
        return fileMap
    }

    actual suspend fun zip(files: Map<String, ByteArray>): ByteArray {
        val zip = JSZip()

        files.forEach { (fileName, bytes) ->
            zip.file(fileName, bytes.toUint8Array())
        }

        val options = js("{ type: 'uint8array', compression: 'DEFLATE' }")
        val uInt8Array = zip.generateAsync(options).await()

        return uInt8Array.toByteArray()
    }

    private fun ByteArray.toUint8Array(): Uint8Array {
        return Uint8Array(this.unsafeCast<Int8Array>().buffer)
    }

    private fun Uint8Array.toByteArray(): ByteArray {
        return Int8Array(this.buffer).unsafeCast<ByteArray>()
    }
}