@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.engine


import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.collections.iterator
import kotlin.js.Promise

@JsModule("jszip")
external class JSZip() : JsAny {
    fun loadAsync(data: Uint8Array): Promise<JsZipInstance>
    fun file(name: String, data: Uint8Array)
    fun generateAsync(options: JsAny): Promise<Uint8Array>
}


private fun getZipOptions(): JsAny = js("({type: 'uint8array'})")

@PublishedApi
internal actual object ZipEngine {

    actual suspend fun zip(files: Map<String, ByteArray>): ByteArray {
        try {
            val jsZip = JSZip()

            // Add each file to the zip
            for ((path, bytes) in files) {
                val uint8Array = Uint8Array(bytes.size)
                for (i in bytes.indices) {
                    uint8Array[i] = bytes[i]
                }
                jsZip.file(path, uint8Array)
            }

            val zippedUint8 = jsZip.generateAsync(getZipOptions()).await<Uint8Array>()

            val resultBytes = ByteArray(zippedUint8.length)
            for (i in 0 until zippedUint8.length) {
                resultBytes[i] = zippedUint8[i]
            }

            println("FlexRender: Zipped ${files.size} files into ${resultBytes.size} bytes.")
            return resultBytes

        } catch (e: Exception) {
            println("FlexRender Wasm Zip Error: ${e.message}")
            return byteArrayOf()
        }
    }


    actual suspend fun unzip(zipBytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()

        try {
            val uint8Array = Uint8Array(zipBytes.size)
            for (i in zipBytes.indices) {
                uint8Array[i] = zipBytes[i]
            }

            val jsZip = JSZip()

            val zipInstance: JsZipInstance = jsZip.loadAsync(uint8Array).await<JsZipInstance>()

            val filesToExtract = listOf("xl/sharedStrings.xml", "xl/worksheets/sheet1.xml")

            for (path in filesToExtract) {
                val jsFile: JsZipFile? = zipInstance.file(path)
                if (jsFile != null) {
                    val uint8Content: Uint8Array = jsFile.async("uint8array").await<Uint8Array>()

                    val ktArray = ByteArray(uint8Content.length)
                    for (i in 0 until uint8Content.length) {
                        ktArray[i] = uint8Content[i]
                    }
                    result[path] = ktArray
                }
            }
        } catch (e: Exception) {
            println("Wasm Zip Error: ${e.message}")
        }

        return result
    }



}

