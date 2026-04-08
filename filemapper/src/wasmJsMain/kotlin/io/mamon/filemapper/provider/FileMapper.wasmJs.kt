@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider


import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.mamon.filemapper.FileMapperType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@JsFun("(buffer) => new Int8Array(buffer)")
private external fun createInt8Array(buffer: JsAny): JsAny

@JsFun("(array) => array.length")
private external fun getArrayLength(array: JsAny): Int

@JsFun("(array, index) => array[index]")
private external fun getArrayElement(array: JsAny, index: Int): Byte


actual class MapperFile(private val file: File) {
    actual val name: String get() = file.name
    actual val extension: String get() = file.name.substringAfterLast('.', "")

    actual suspend fun readBytes(): ByteArray = suspendCancellableCoroutine { cont ->
        val reader = FileReader()
        reader.onload = {
            val buffer = reader.result!!

            val jsArray = createInt8Array(buffer)

            val length = getArrayLength(jsArray)

            val bytes = ByteArray(length) { i ->
                getArrayElement(jsArray, i)
            }

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