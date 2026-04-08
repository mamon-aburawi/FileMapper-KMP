@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider



import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.mamon.filemapper.FileMapperType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


actual class MapperFile(private val file: File) {
    actual val name: String get() = file.name
    actual val extension: String get() = file.name.substringAfterLast('.', "")

    actual suspend fun readBytes(): ByteArray = suspendCancellableCoroutine { cont ->
        val reader = FileReader()
        reader.onload = {
            val arrayBuffer = reader.result as ArrayBuffer
            // In Kotlin/JS, we can safely and instantly cast JS Int8Array to Kotlin ByteArray
            val bytes = Int8Array(arrayBuffer).unsafeCast<ByteArray>()
            cont.resume(bytes)
        }
        reader.onerror = {
            cont.resumeWithException(Exception("Failed to read file from browser"))
        }
        reader.readAsArrayBuffer(file)
    }
}

actual class FileMapperPickerLauncher(private val launchAction: () -> Unit) {
    actual fun launch() = launchAction()
}

@Composable
actual fun rememberFileMapperPicker(type: FileMapperType, onResult: (MapperFile?) -> Unit): FileMapperPickerLauncher {
    return remember(type) {
        FileMapperPickerLauncher {
            launchWebPicker(
                type,
                onResult
            )
        }
    }
}

actual object FileMapperPicker {
    actual suspend fun pickFile(type: FileMapperType): MapperFile? {
        return suspendCancellableCoroutine { cont -> launchWebPicker(type) { cont.resume(it) } }
    }
}

// Helper to trigger the browser's hidden file input
internal fun launchWebPicker(type: FileMapperType, onResult: (MapperFile?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = when (type) {
        FileMapperType.XLSX -> ".xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        FileMapperType.JSON -> ".json, application/json"
    }
    input.onchange = {
        val fileList = input.files
        if (fileList != null && fileList.length > 0) {
            onResult(MapperFile(fileList.item(0)!!))
        } else {
            onResult(null)
        }
    }
    document.body?.appendChild(input)
    input.style.display = "none"
    input.click()
    document.body?.removeChild(input)
}