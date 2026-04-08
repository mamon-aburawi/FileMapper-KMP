@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.mamon.filemapper.FileMapperType
import platform.UIKit.*
import platform.Foundation.*
import platform.UniformTypeIdentifiers.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.darwin.NSObject
import platform.posix.memcpy

actual class MapperFile(private val url: NSURL) {
    actual val name: String
        get() = url.lastPathComponent ?: "unknown"

    actual val extension: String
        get() = url.pathExtension ?: ""

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun readBytes(): ByteArray = withContext(Dispatchers.Default) {
        val secure = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url)
            val bytes = data?.bytes ?: return@withContext byteArrayOf()
            val length = data.length.toInt()
            ByteArray(length).apply {
                usePinned { pinned ->
                    memcpy(pinned.addressOf(0), bytes, data.length)
                }
            }
        } finally {
            if (secure) url.stopAccessingSecurityScopedResource()
        }
    }
}

actual class FileMapperPickerLauncher(private val launchAction: () -> Unit) {
    actual fun launch() = launchAction()
}


@Composable
actual fun rememberFileMapperPicker(
    type: FileMapperType,
    onResult: (MapperFile?) -> Unit
): FileMapperPickerLauncher {
    val pickerDelegate = remember { FlexDocumentPickerDelegate(onResult) }

    return remember(type) {
        FileMapperPickerLauncher {
            val types = when (type) {
                FileMapperType.XLSX -> listOf(UTTypeSpreadsheet.identifier)
                FileMapperType.JSON -> listOf(UTTypeJSON.identifier)
            }

            val picker = UIDocumentPickerViewController(
                documentTypes = types,
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            picker.delegate = pickerDelegate
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                picker, animated = true, completion = null
            )
        }
    }
}


class FlexDocumentPickerDelegate(
    private val onResult: (MapperFile?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onResult(url?.let { MapperFile(it) })
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

actual object FileMapperPicker {
    actual suspend fun pickFile(type: FileMapperType): MapperFile? {
        throw NotImplementedError("Implement Continuation wrapper for iOS non-compose")
    }
}