@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider


import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.mamon.filemapper.FileMapperType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


actual class MapperFile(private val file: File) {
    actual val name: String get() = file.name
    actual val extension: String get() = file.extension

    actual suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        file.readBytes()
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
    return remember(type) {
        FileMapperPickerLauncher {
            val dialog = FileDialog(null as Frame?, "Select ${type.name} File", FileDialog.LOAD)

            val extension = when (type) {
                FileMapperType.XLSX -> "*.xlsx"
                FileMapperType.JSON -> "*.json"
            }
            dialog.file = extension

            dialog.isVisible = true

            val selectedFile = dialog.files.firstOrNull()
            onResult(selectedFile?.let { MapperFile(it) })
        }
    }
}


actual object FileMapperPicker {
    actual suspend fun pickFile(type: FileMapperType): MapperFile? = withContext(Dispatchers.IO) {
        // null as Frame? is fine for a standalone dialog
        val dialog = FileDialog(null as Frame?, "Select ${type.name} File", FileDialog.LOAD)

        val extension = when (type) {
            FileMapperType.XLSX -> ".xlsx"
            FileMapperType.JSON -> ".json"
        }

        // 1. Filter the view (OS dependent support)
        dialog.setFilenameFilter { _, name ->
            name.lowercase().endsWith(extension)
        }

        // Use the glob pattern for Windows
        dialog.file = "*$extension"

        dialog.isVisible = true

        // 2. Validate the output
        val selectedFile = dialog.files.firstOrNull()

        if (selectedFile != null && selectedFile.exists() && selectedFile.isFile) {
            // Final check: did they bypass the filter by typing a name?
            if (selectedFile.name.lowercase().endsWith(extension)) {
                MapperFile(selectedFile)
            } else {
                null // Or throw an exception/show error
            }
        } else {
            null
        }
    }
}
//actual object FileMapperPicker {
//    actual suspend fun pickFile(type: FileMapperType): MapperFile? = withContext(Dispatchers.IO) {
//
//        val dialog = FileDialog(null as Frame?, "Select ${type.name} File", FileDialog.LOAD)
//
//        dialog.file = when (type) {
//            FileMapperType.XLSX -> "*.xlsx"
//            FileMapperType.JSON -> "*.json"
//        }
//        dialog.isVisible = true
//        dialog.files.firstOrNull()?.let { MapperFile(it) }
//    }
//}
//
