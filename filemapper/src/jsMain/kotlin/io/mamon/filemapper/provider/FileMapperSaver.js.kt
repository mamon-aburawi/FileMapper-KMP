@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider

import kotlinx.browser.document
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

actual class FileMapperSaver actual constructor() {
    actual fun saveBytes(fileName: String, bytes: ByteArray): Boolean {
        return try {
            // 1. Cast the Kotlin ByteArray to a JavaScript Int8Array
            val intArray = bytes.unsafeCast<Int8Array>()

            // 2. Create a JS Blob representing the file data
            // We use generic octet-stream, the browser will infer from the file extension
            val blob = Blob(
                blobParts = arrayOf(intArray),
                options = BlobPropertyBag(type = "application/octet-stream")
            )

            // 3. Create a temporary Object URL pointing to the Blob
            val url = URL.createObjectURL(blob)

            // 4. Create an invisible HTML <a> tag
            val anchor = document.createElement("a") as HTMLAnchorElement
            anchor.apply {
                href = url
                download = fileName // This tells the browser to download, not navigate
            }

            // 5. Append to DOM, click it to trigger download, and remove it instantly
            document.body?.appendChild(anchor)
            anchor.click()
            document.body?.removeChild(anchor)

            // 6. Free up browser memory
            URL.revokeObjectURL(url)

            true
        } catch (e: Exception) {
            console.error("FlexRender Web Export Error: ${e.message}")
            false
        }
    }
}

@PublishedApi
internal actual suspend fun saveToDownloads(bytes: ByteArray, fileName: String): String {
    val mimeType = if (fileName.endsWith(".json")) "application/json" else "application/octet-stream"

    // 1. Instantly cast ByteArray to JS Int8Array (No loop needed in standard JS!)
    val int8Array = bytes.unsafeCast<Int8Array>()

    // 2. Create the Blob using arrayOf<dynamic>
    val blob = Blob(arrayOf<dynamic>(int8Array), BlobPropertyBag(type = mimeType))

    // 3. Create a temporary download link
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.download = fileName
    document.body?.appendChild(a)

    // 4. Simulate click (Directly available in JS target)
    a.click()

    // 5. Cleanup
    document.body?.removeChild(a)
    URL.revokeObjectURL(url)

    return "Download triggered in browser: $fileName"
}