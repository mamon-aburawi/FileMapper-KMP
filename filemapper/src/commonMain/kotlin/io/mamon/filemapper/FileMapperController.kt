package io.mamon.filemapper



import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.mamon.filemapper.provider.FileMapperPickerLauncher
import io.mamon.filemapper.provider.rememberFileMapperPicker
import kotlinx.coroutines.launch


class FileMapperController<T>(
    private val picker: FileMapperPickerLauncher,
    private val exportAction: (List<T>, String, Set<String>) -> Unit
)
{

    var currentImportIgnoredColumns: Set<String> = emptySet()
        private set


    fun import(ignoredColumns: Set<String> = emptySet()) {
        this.currentImportIgnoredColumns = ignoredColumns
        picker.launch()
    }

    fun export(
        data: List<T>,
        fileName: String,
        ignoredColumns: Set<String> = emptySet()
    ) {
        exportAction(data, fileName, ignoredColumns)
    }
}



@Composable
inline fun <reified T> rememberFileMapper(
    fileType: FileMapperType,
    crossinline onImportSuccess: (List<T>) -> Unit,
    crossinline onImportFailed: (FileMapperException) -> Unit,
    crossinline onExportSuccess: (String) -> Unit = {},
    crossinline onExportFailed: (FileMapperException) -> Unit = {}
): FileMapperController<T> {
    val scope = rememberCoroutineScope()
    val worker = remember { FileMapper() }

    val controllerRef = remember { object { var instance: FileMapperController<T>? = null } }


    val picker = rememberFileMapperPicker(
        type = fileType
    ) { platformFile ->
        platformFile?.let { file ->
            scope.launch {
                try {
                    val bytes = file.readBytes()

                    worker.importData<T>(
                        bytes = bytes,
                        fileType = fileType,
                        ignoreColumns = controllerRef.instance?.currentImportIgnoredColumns ?: emptySet(),
                        onSuccess = { onImportSuccess(it) },
                        onFailed = { onImportFailed(it) }
                    )
                } catch (e: Exception) {

                    val exception = e as? FileMapperException
                        ?: FileMapperException(FileMapperError.READ_FAILED, e.message)

                    onImportFailed(exception)
                }
            }
        }
    }

    val exportAction = remember(fileType) {
        val action: (List<T>, String, Set<String>) -> Unit = { data, fileName, requestedIgnoredCols ->
            scope.launch {
                try {
                    worker.exportData<T>(
                        data = data,
                        fileType = fileType,
                        ignoreColumns = requestedIgnoredCols,
                        fileName = fileName,
                        onSuccess = { savedPath ->
                            onExportSuccess(savedPath)
                        },
                        onFailed = { error ->
                            onExportFailed(error)
                        }
                    )
                } catch (e: Exception) {

                    val exception = e as? FileMapperException
                        ?: FileMapperException(FileMapperError.EXPORT_FAILED, e.message)

                    onExportFailed(exception)
                }
            }
        }
        action
    }

    return remember(picker, fileType) {
        FileMapperController(
            picker = picker,
            exportAction = exportAction
        ).also { controllerRef.instance = it }
    }
}


