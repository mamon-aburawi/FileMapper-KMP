@file:OptIn(ExperimentalSerializationApi::class)

package io.mamon.filemapper

import io.mamon.filemapper.engine.FileMapperEngine
import io.mamon.filemapper.engine.ZipEngine
import io.mamon.filemapper.provider.saveToDownloads
import kotlinx.coroutines.yield
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.coroutines.cancellation.CancellationException



class FileMapper {

    suspend inline fun <reified T> importData(
        bytes: ByteArray,
        fileType: FileMapperType,
        ignoreColumns: Set<String> = emptySet(),
        crossinline onSuccess: (List<T>) -> Unit,
        crossinline onFailed: (FileMapperException) -> Unit
    ) {
        try {
            yield()

            if (bytes.isEmpty()) {
                onFailed(FileMapperException(FileMapperError.EMPTY_FILE))
                return
            }

            if (fileType == FileMapperType.XLSX) {
                val unzippedFiles = ZipEngine.unzip(bytes)
                FileMapperEngine.importBytes<T>(
                    unzippedFiles = unzippedFiles,
                    ignoreColumns = ignoreColumns,
                    onSuccess = { onSuccess(it) },
                    onError = { onFailed(it) }
                )
            } else {
                try {
                    val jsonString = bytes.decodeToString()
                    val list: List<T> = FileMapperEngine.jsonEngine.decodeFromString(jsonString)
                    onSuccess(list)
                } catch (e: Exception) {
                    onFailed(FileMapperException(FileMapperError.INVALID_FORMAT, e.message))
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            val exception = when (e) {
                is FileMapperException -> e
                else -> FileMapperException(FileMapperError.READ_FAILED, e.message)
            }
            onFailed(exception)
        }
    }




    suspend inline fun <reified T> exportData(
        data: List<T>,
        fileName: String,
        fileType: FileMapperType,
        ignoreColumns: Set<String> = emptySet(),
        crossinline onSuccess: (savedPath: String) -> Unit,
        crossinline onFailed: (FileMapperException) -> Unit
    ) {
        try {
            yield()

            val fileBytes = FileMapperEngine.exportToBytes(
                data = data,
                type = fileType,
                ignoreColumns = ignoreColumns,
                onFailed = { onFailed(it) }
            )

            // Kill-switch if the engine returned empty bytes due to an internal error
            if (fileBytes.isEmpty()) return

            val extension = if (fileType == FileMapperType.XLSX) ".xlsx" else ".json"
            val finalName = if (fileName.endsWith(extension)) fileName else "$fileName$extension"

            val savedPath = saveToDownloads(fileBytes, finalName)
            onSuccess(savedPath)

        } catch (e: Exception) {
            if (e is CancellationException) throw e

            val exception = when (e) {
                is FileMapperException -> e
                else -> FileMapperException(FileMapperError.EXPORT_FAILED, e.message)
            }
            onFailed(exception)
        }
    }


}




//class FileMapper {
//
//
//    suspend inline fun <reified T> importData(
//        bytes: ByteArray,
//        fileType: FileMapperType,
//        ignoreColumns: Set<String> = emptySet(),
//        crossinline onSuccess: (List<T>) -> Unit,
//        crossinline onFailed: (FileMapperException) -> Unit
//    ) {
//        try {
//            yield()
//            if (bytes.isEmpty()) {
//                onFailed(FileMapperException("The provided file is empty."))
//                return
//            }
//
//            if (fileType == FileMapperType.XLSX) {
//                val unzippedFiles = ZipEngine.unzip(bytes)
//                FileMapperEngine.importInBatches<T>(
//                    unzippedFiles = unzippedFiles,
//                    ignoreColumns = ignoreColumns,
//                    onSuccess = { onSuccess(it) },
//                    onError = { onFailed(it) }
//                )
//            } else {
//                val jsonString = bytes.decodeToString()
//                val list: List<T> = FileMapperEngine.jsonEngine.decodeFromString(jsonString)
//                onSuccess(list)
//            }
//        } catch (e: Exception) {
//            if (e is CancellationException) throw e
//            onFailed(if (e is FileMapperException) e else FileMapperException(e.message ?: "Unknown Error"))
//        }
//    }
//
//
//
//    suspend inline fun <reified T> exportData(
//        data: List<T>,
//        fileName: String,
//        fileType: FileMapperType,
//        ignoreColumns: Set<String> = emptySet(),
//        crossinline onSuccess: (savedPath: String) -> Unit,
//        crossinline onFailed: (FileMapperException) -> Unit
//    ) {
//        try {
//            yield()
//            val fileBytes = FileMapperEngine.exportToBytes(
//                data = data,
//                type = fileType,
//                ignoreColumns = ignoreColumns,
//                onFailed = { onFailed(it) }
//            )
//
//            // THE KILL-SWITCH: Prevents saving 0-byte broken files
//            if (fileBytes.isEmpty()) return
//
//            val extension = if (fileType == FileMapperType.XLSX) ".xlsx" else ".json"
//            val finalName = if (fileName.endsWith(extension)) fileName else "$fileName$extension"
//            val savedPath = saveToDownloads(fileBytes, finalName)
//
//            onSuccess(savedPath)
//        } catch (e: Exception) {
//            if (e is CancellationException) throw e
//            onFailed(FileMapperException(e.message ?: "Export Failed"))
//        }
//    }
//
//}
