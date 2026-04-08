@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider

import org.w3c.dom.url.URL
import kotlinx.browser.document
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.JsAny
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import kotlin.js.JsArray


@JsFun("(size) => new Int8Array(size)")
private external fun createJsIntArray(size: Int): JsAny

@JsFun("(arr, index, value) => { arr[index] = value; }")
private external fun setJsIntArrayValue(arr: JsAny, index: Int, value: Byte)

@JsFun("""
(arr, fileName) => { 
    const blob = new Blob([arr], { type: 'application/octet-stream' }); 
    const url = URL.createObjectURL(blob); 
    const a = document.createElement('a'); 
    a.href = url; 
    a.download = fileName; 
    document.body.appendChild(a); 
    a.click(); 
    document.body.removeChild(a); 
    URL.revokeObjectURL(url); 
}
""")
private external fun triggerBrowserDownload(arr: JsAny, fileName: String)

actual class FileMapperSaver actual constructor() {

    actual fun saveBytes(fileName: String, bytes: ByteArray): Boolean {
        return try {
            val jsArray = createJsIntArray(bytes.size)

            for (i in bytes.indices) {
                setJsIntArrayValue(jsArray, i, bytes[i])
            }

            triggerBrowserDownload(jsArray, fileName)

            true
        } catch (e: Exception) {
            println("FlexRender Wasm Export Error: ${e.message}")
            false
        }
    }

}



private fun wrapInJsArray(element: Uint8Array): JsArray<JsAny?> = js("[element]")

@PublishedApi
internal actual suspend fun saveToDownloads(bytes: ByteArray, fileName: String): String {
    println("FlexRender: Preparing to save ${bytes.size} bytes.")
    if (bytes.isEmpty()) throw Exception("Cannot save empty file. Zip engine failed.")

    val uint8Array = Uint8Array(bytes.size)
    for (i in bytes.indices) {
        uint8Array[i] = bytes[i]
    }

    val mimeType = if (fileName.endsWith(".json")) {
        "application/json"
    } else {
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }

    val blob = Blob(wrapInJsArray(uint8Array), BlobPropertyBag(type = mimeType))

    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.setAttribute("href", url)
    a.setAttribute("download", fileName)
    document.body?.appendChild(a)

    val clickEvent = document.createEvent("MouseEvents")
    clickEvent.initEvent("click", true, true)
    a.dispatchEvent(clickEvent)

    document.body?.removeChild(a)
    URL.revokeObjectURL(url)

    return "Download triggered in browser: $fileName"
}


