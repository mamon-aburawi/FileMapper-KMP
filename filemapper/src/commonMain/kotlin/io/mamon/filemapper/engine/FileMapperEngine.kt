@file:OptIn(ExperimentalSerializationApi::class)

package io.mamon.filemapper.engine


import io.mamon.filemapper.FileMapperType
import io.mamon.filemapper.FileMapperException
import io.mamon.filemapper.annotation.ExcelColumn
import io.mamon.filemapper.annotation.IgnoreUnmappedColumns
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import kotlinx.coroutines.yield
import io.mamon.filemapper.FileMapperError



@PublishedApi
internal object FileMapperEngine {

    @PublishedApi
    internal val jsonEngine = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @PublishedApi
    internal fun normalizeHeader(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]"), "").trim()

    suspend inline fun <reified T> importBytes(
        unzippedFiles: Map<String, ByteArray>,
        ignoreColumns: Set<String> = emptySet(),
        crossinline onSuccess: (List<T>) -> Unit,
        crossinline onError: (FileMapperException) -> Unit,
        crossinline onWarning: (String) -> Unit = {}
    ) {
        val sequenceRows = FileMapperXlsxEngine.parseXlsxFilesAsSequence(unzippedFiles)
        val descriptor = serializer<T>().descriptor
        val allResults = mutableListOf<T>()
        var hasDataRows = false
        var isHeaderValidated = false

        val ignoreUnknownColumns = descriptor.annotations.filterIsInstance<IgnoreUnmappedColumns>().isNotEmpty()

        val expectedHeaders = (0 until descriptor.elementsCount).mapNotNull { i ->
            val propName = descriptor.getElementName(i)
            val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
            val customHeaderName = excelAnno?.name

            val isIgnored = (excelAnno?.ignore == true) ||
                    ignoreColumns.contains(propName) ||
                    (customHeaderName != null && ignoreColumns.contains(customHeaderName))

            if (isIgnored) null
            else {
                val displayName = customHeaderName ?: propName
                Pair(displayName, normalizeHeader(displayName))
            }
        }

        if (expectedHeaders.isEmpty()) {
            onError(FileMapperException(FileMapperError.ALL_COLUMNS_IGNORED))
            return
        }

        for (row in sequenceRows) {
            if (!isHeaderValidated) {
                val actualHeadersNormalized = row.keys.map { normalizeHeader(it) }.toSet()
                val expectedHeadersNormalized = expectedHeaders.map { it.second }.toSet()

                val missingHeaders = expectedHeaders.filter { it.second !in actualHeadersNormalized }.map { it.first }
                val extraHeaders = row.keys.filter { normalizeHeader(it) !in expectedHeadersNormalized && it.isNotBlank() }

                if (extraHeaders.isNotEmpty() && ignoreUnknownColumns) {
                    onWarning("${FileMapperError.UNEXPECTED_COLUMNS.message} ${extraHeaders.joinToString(", ")}")
                }

                if (missingHeaders.isNotEmpty() || (extraHeaders.isNotEmpty() && !ignoreUnknownColumns)) {
                    val details = buildString {
                        if (missingHeaders.isNotEmpty()) append(missingHeaders.joinToString(", "))
                        if (extraHeaders.isNotEmpty() && !ignoreUnknownColumns) {
                            if (isNotEmpty()) append(" | ")
                            append(extraHeaders.joinToString(", "))
                        }
                    }

                    val errorType = if (missingHeaders.isNotEmpty()) FileMapperError.MISSING_COLUMNS else FileMapperError.UNEXPECTED_COLUMNS
                    onError(FileMapperException(errorType, details))
                    return
                }
                isHeaderValidated = true
            }

            hasDataRows = true

            try {
                val jsonMap = mutableMapOf<String, JsonElement>()
                for (i in 0 until descriptor.elementsCount) {
                    val propName = descriptor.getElementName(i)
                    val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
                    val customHeaderName = excelAnno?.name

                    val isIgnored = (excelAnno?.ignore == true) ||
                            ignoreColumns.contains(propName) ||
                            (customHeaderName != null && ignoreColumns.contains(customHeaderName))

                    val cellValue = if (isIgnored) "" else {
                        val expectedHeader = customHeaderName ?: propName
                        val normalizedExpected = normalizeHeader(expectedHeader)
                        row.entries.find { normalizeHeader(it.key) == normalizedExpected }?.value?.trim() ?: ""
                    }

                    if (cellValue.isEmpty()) {
                        if (!descriptor.isElementOptional(i)) {
                            val serialName = descriptor.getElementDescriptor(i).serialName
                            jsonMap[propName] = when {
                                serialName.contains("Int") || serialName.contains("Double") ||
                                        serialName.contains("Float") || serialName.contains("Long") -> JsonPrimitive(0)
                                serialName.contains("Boolean") -> JsonPrimitive(false)
                                else -> JsonPrimitive("")
                            }
                        }
                        continue
                    }
                    jsonMap[propName] = JsonPrimitive(cellValue)
                }

                allResults.add(jsonEngine.decodeFromJsonElement(serializer<T>(), JsonObject(jsonMap)))
                yield()
            } catch (e: Exception) {
                continue
            }
        }

        if (!hasDataRows) {
            onError(FileMapperException(FileMapperError.NO_DATA_ROWS))
            return
        }

        onSuccess(allResults)
    }

    suspend inline fun <reified T> exportToBytes(
        data: List<T>,
        type: FileMapperType,
        ignoreColumns: Set<String> = emptySet(),
        crossinline onFailed: (FileMapperException) -> Unit
    ): ByteArray {
        val descriptor = serializer<T>().descriptor
        val propertyNames = mutableListOf<String>()
        val headers = mutableListOf<String>()

        for (i in 0 until descriptor.elementsCount) {
            val propName = descriptor.getElementName(i)
            val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
            val customHeaderName = excelAnno?.name

            val isIgnored = (excelAnno?.ignore == true) ||
                    ignoreColumns.contains(propName) ||
                    (customHeaderName != null && ignoreColumns.contains(customHeaderName))

            if (!isIgnored) {
                propertyNames.add(propName)
                headers.add(customHeaderName ?: propName)
            }
        }

        if (propertyNames.isEmpty()) {
            onFailed(FileMapperException(FileMapperError.ALL_COLUMNS_IGNORED))
            return byteArrayOf()
        }

        return try {
            when (type) {
                FileMapperType.XLSX -> {
                    val exportJsonEngine = Json(jsonEngine) { encodeDefaults = true; explicitNulls = true }
                    val dataMatrix = data.map { item ->
                        val json = exportJsonEngine.encodeToJsonElement(serializer<T>(), item).jsonObject
                        propertyNames.map { prop ->
                            val element = json[prop]
                            if (element is JsonPrimitive) element.content else ""
                        }
                    }
                    ZipEngine.zip(FileMapperXlsxEngine.createXlsxFiles(headers, dataMatrix))
                }
                FileMapperType.JSON -> {
                    val prettyJsonEngine = Json(jsonEngine) {
                        prettyPrint = true
                        encodeDefaults = true
                    }
                    val filteredData = data.map { item ->
                        val json = jsonEngine.encodeToJsonElement(serializer<T>(), item).jsonObject
                        JsonObject(json.filterKeys { it in propertyNames })
                    }
                    prettyJsonEngine.encodeToString(filteredData).encodeToByteArray()
                }
            }
        } catch (e: Exception) {
            onFailed(FileMapperException(FileMapperError.EXPORT_FAILED, e.message))
            byteArrayOf()
        }
    }
}



//@PublishedApi
//internal object FileMapperEngine {
//
//    @PublishedApi
//    internal val jsonEngine = Json {
//        ignoreUnknownKeys = true
//        isLenient = true
//        coerceInputValues = true
//        explicitNulls = false
//    }
//
//
//
//    @PublishedApi
//    internal fun normalizeHeader(s: String): String =
//        s.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
//
//
//    suspend inline fun <reified T> importInBatches(
//        unzippedFiles: Map<String, ByteArray>,
//        ignoreColumns: Set<String> = emptySet(),
//        crossinline onSuccess: (List<T>) -> Unit,
//        crossinline onError: (FileMapperException) -> Unit,
//        crossinline onWarning: (String) -> Unit = {}
//    ) {
//        val sequenceRows = FileMapperXlsxEngine.parseXlsxFilesAsSequence(unzippedFiles)
//        val descriptor = serializer<T>().descriptor
//        val allResults = mutableListOf<T>()
//        var hasDataRows = false
//        var isHeaderValidated = false
//
//        val ignoreUnknownColumns = descriptor.annotations.filterIsInstance<IgnoreUnmappedColumns>().isNotEmpty()
//
//        val expectedHeaders = (0 until descriptor.elementsCount).mapNotNull { i ->
//            val propName = descriptor.getElementName(i)
//            val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
//            val customHeaderName = excelAnno?.name
//
//            val isIgnored = (excelAnno?.ignore == true) ||
//                    ignoreColumns.contains(propName) ||
//                    (customHeaderName != null && ignoreColumns.contains(customHeaderName))
//
//            if (isIgnored) null
//            else {
//                val displayName = customHeaderName ?: propName
//                Pair(displayName, normalizeHeader(displayName))
//            }
//        }
//
//        if (expectedHeaders.isEmpty()) {
//            onError(FileMapperException("Import Failed: All columns for ${T::class.simpleName} are ignored."))
//            return
//        }
//
//        for (row in sequenceRows) {
//            if (!isHeaderValidated) {
//                val actualHeadersNormalized = row.keys.map { normalizeHeader(it) }.toSet()
//                val expectedHeadersNormalized = expectedHeaders.map { it.second }.toSet()
//
//                val missingHeaders = expectedHeaders.filter { it.second !in actualHeadersNormalized }.map { it.first }
//                val extraHeaders = row.keys.filter { normalizeHeader(it) !in expectedHeadersNormalized && it.isNotBlank() }
//
//                if (extraHeaders.isNotEmpty() && ignoreUnknownColumns) {
//                    onWarning("Import Error: ignored excel columns [${extraHeaders.joinToString(", ")}]")
//                }
//
//                if (missingHeaders.isNotEmpty() || (extraHeaders.isNotEmpty() && !ignoreUnknownColumns)) {
//                    val errorMsg = buildString {
//                        if (missingHeaders.isNotEmpty()) append("\n\nImport Error: Missing Required Column: ${missingHeaders.joinToString(", ")}")
//                        if (extraHeaders.isNotEmpty() && !ignoreUnknownColumns) append("\n\nImport Error: Unexpected Column: ${extraHeaders.joinToString(", ")}")
//                    }
//                    onError(FileMapperException(errorMsg))
//                    return
//                }
//                isHeaderValidated = true
//            }
//
//            hasDataRows = true
//
//            try {
//                val jsonMap = mutableMapOf<String, JsonElement>()
//                for (i in 0 until descriptor.elementsCount) {
//                    val propName = descriptor.getElementName(i)
//                    val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
//                    val customHeaderName = excelAnno?.name
//
//                    val isIgnored = (excelAnno?.ignore == true) ||
//                            ignoreColumns.contains(propName) ||
//                            (customHeaderName != null && ignoreColumns.contains(customHeaderName))
//
//                    val cellValue = if (isIgnored) "" else {
//                        val expectedHeader = customHeaderName ?: propName
//                        val normalizedExpected = normalizeHeader(expectedHeader)
//                        row.entries.find { normalizeHeader(it.key) == normalizedExpected }?.value?.trim() ?: ""
//                    }
//
//                    if (cellValue.isEmpty()) {
//                        if (!descriptor.isElementOptional(i)) {
//                            val serialName = descriptor.getElementDescriptor(i).serialName
//                            jsonMap[propName] = when {
//                                serialName.contains("Int") || serialName.contains("Double") ||
//                                        serialName.contains("Float") || serialName.contains("Long") -> JsonPrimitive(0)
//                                serialName.contains("Boolean") -> JsonPrimitive(false)
//                                else -> JsonPrimitive("")
//                            }
//                        }
//                        continue
//                    }
//                    jsonMap[propName] = JsonPrimitive(cellValue)
//                }
//
//                allResults.add(jsonEngine.decodeFromJsonElement(serializer<T>(), JsonObject(jsonMap)))
//                yield()
//            } catch (e: Exception) {
//                continue
//            }
//        }
//
//        if (!hasDataRows) {
//            onError(FileMapperException("Import Failed: The file is empty or contains no data rows."))
//            return
//        }
//
//        onSuccess(allResults)
//    }
//
//
//
////    suspend inline fun <reified T> importInBatches(
////        unzippedFiles: Map<String, ByteArray>,
////        chunkSize: Int,
////        ignoreColumns: Set<String> = emptySet(),
////        crossinline onBatchReady: suspend (List<T>, Int) -> Unit,
////        crossinline onError: (FlexException) -> Unit,
////        crossinline onWarning: (String) -> Unit = {}
////    ) {
////        val sequenceRows = FileMapperXlsxEngine.parseXlsxFilesAsSequence(unzippedFiles)
////        val descriptor = serializer<T>().descriptor
////        var currentBatch = mutableListOf<T>()
////        var totalCount = 0
////
////        // Track if we actually found any data rows
////        var hasDataRows = false
////
////        val ignoreUnknownColumns = descriptor.annotations.filterIsInstance<IgnoreUnmappedColumns>().isNotEmpty()
////        val ignoredPropertiesLog = mutableListOf<String>()
////
////        // 1. PRE-COMPUTE EXPECTED HEADERS
////        val expectedHeaders = (0 until descriptor.elementsCount).mapNotNull { i ->
////            val propName = descriptor.getElementName(i)
////            val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
////            val customHeaderName = excelAnno?.name
////
////            val isIgnored = (excelAnno?.ignore == true) ||
////                    ignoreColumns.contains(propName) ||
////                    (customHeaderName != null && ignoreColumns.contains(customHeaderName))
////
////            if (isIgnored) {
////                ignoredPropertiesLog.add(customHeaderName ?: propName)
////                null
////            } else {
////                val displayName = customHeaderName ?: propName
////                Pair(displayName, normalizeFlexHeader(displayName))
////            }
////        }
////
////        if (expectedHeaders.isEmpty()) {
////            onError(FlexException("Import Failed: All columns for ${T::class.simpleName} data class are ignored."))
////            return
////        }
////
////        var isHeaderValidated = false
////
////        for (row in sequenceRows) {
////
////            if (!isHeaderValidated) {
////                val actualHeadersNormalized = row.keys.map { normalizeFlexHeader(it) }.toSet()
////                val expectedHeadersNormalized = expectedHeaders.map { it.second }.toSet()
////
////                val missingHeaders = expectedHeaders.filter { it.second !in actualHeadersNormalized }.map { it.first }
////                val extraHeaders = row.keys.filter { normalizeFlexHeader(it) !in expectedHeadersNormalized && it.isNotBlank() }
////
////                if (extraHeaders.isNotEmpty() && ignoreUnknownColumns) {
////                    onWarning("Import Error: ignored excel columns [${extraHeaders.joinToString(", ")}]")
////                }
////
////                if (missingHeaders.isNotEmpty() || (extraHeaders.isNotEmpty() && !ignoreUnknownColumns)) {
////                    val errorMsg = buildString {
////                        if (missingHeaders.isNotEmpty()) append("\n\nImport Error: Missing Required Column in your Excel File: ${missingHeaders.joinToString(", ")}")
////                        if (extraHeaders.isNotEmpty() && !ignoreUnknownColumns) append("\n\nImport Error: Unexpected Columns in your Excel File: ${extraHeaders.joinToString(", ")}")
////                    }
////                    onError(FlexException(errorMsg))
////                    return
////                }
////
////                // Mark as validated but DO NOT 'continue'.
////                // We want to process this current row as the first data item.
////                isHeaderValidated = true
////            }
////
////            // --- DATA PROCESSING PHASE ---
////            // If we are here, we have a valid row
////            hasDataRows = true
////            totalCount++
////
////            try {
////                val jsonMap = mutableMapOf<String, JsonElement>()
////                for (i in 0 until descriptor.elementsCount) {
////                    val propName = descriptor.getElementName(i)
////                    val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
////                    val customHeaderName = excelAnno?.name
////
////                    val isIgnored = (excelAnno?.ignore == true) ||
////                            ignoreColumns.contains(propName) ||
////                            (customHeaderName != null && ignoreColumns.contains(customHeaderName))
////
////                    val cellValue = if (isIgnored) "" else {
////                        val expectedHeader = customHeaderName ?: propName
////                        val normalizedExpected = normalizeFlexHeader(expectedHeader)
////                        row.entries.find { normalizeFlexHeader(it.key) == normalizedExpected }?.value?.trim() ?: ""
////                    }
////
////                    val hasDataClassDefault = descriptor.isElementOptional(i)
////                    if (cellValue.isEmpty()) {
////                        if (!hasDataClassDefault) {
////                            val serialName = descriptor.getElementDescriptor(i).serialName
////                            jsonMap[propName] = when {
////                                serialName.contains("Int") || serialName.contains("Double") || serialName.contains("Float") || serialName.contains("Long") -> JsonPrimitive(0)
////                                serialName.contains("Boolean") -> JsonPrimitive(false)
////                                else -> JsonPrimitive("")
////                            }
////                        }
////                        continue
////                    }
////                    jsonMap[propName] = JsonPrimitive(cellValue)
////                }
////
////                currentBatch.add(jsonEngine.decodeFromJsonElement(serializer<T>(), JsonObject(jsonMap)))
////
////                if (currentBatch.size >= chunkSize) {
////                    onBatchReady(currentBatch, totalCount)
////                    currentBatch = mutableListOf()
////                    yield()
////                }
////            } catch (e: Exception) {
////                // Log parse errors for specific rows if necessary
////                continue
////            }
////        }
////
////        // --- FINAL VALIDATION ---
////        if (!hasDataRows) {
////            onError(FlexException("Import Failed\n\nThe file is empty or contains no data rows."))
////            return
////        }
////
////        if (currentBatch.isNotEmpty()) {
////            onBatchReady(currentBatch, totalCount)
////        }
////    }
//
//
//
//    suspend inline fun <reified T> exportToBytes(
//        data: List<T>,
//        type: FileMapperType,
//        ignoreColumns: Set<String> = emptySet(),
//        crossinline onFailed: (FileMapperException) -> Unit
//    ): ByteArray {
//        val descriptor = serializer<T>().descriptor
//        val propertyNames = mutableListOf<String>()
//        val headers = mutableListOf<String>()
//        val ignoredLog = mutableListOf<String>()
//
//        for (i in 0 until descriptor.elementsCount) {
//            val propName = descriptor.getElementName(i)
//            val excelAnno = descriptor.getElementAnnotations(i).filterIsInstance<ExcelColumn>().firstOrNull()
//            val customHeaderName = excelAnno?.name
//
//            // DUAL-CHECK: Compare against variable name AND annotation name
//            val isIgnored = (excelAnno?.ignore == true) ||
//                    ignoreColumns.contains(propName) ||
//                    (customHeaderName != null && ignoreColumns.contains(customHeaderName))
//
//            if (isIgnored) {
//                ignoredLog.add(customHeaderName ?: propName)
//            } else {
//                propertyNames.add(propName)
//                headers.add(customHeaderName ?: propName)
//            }
//        }
//
//        if (propertyNames.isEmpty()) {
//            val dataClassName = T::class.simpleName ?: "Data Class"
//            val errorMsg = buildString {
//                append("**Export Failed**\n\n")
//                append("No columns are available to export for the class **$dataClassName**.\n\n")
//                append("**Reason:** Every property has been ignored either via the `@ExcelColumn(ignore = true)` annotation ")
//                append("or the runtime `ignoreColumns` parameter.\n\n")
//                append("Please ensure at least one column is visible before exporting.")
//            }
//
//            onFailed(FileMapperException(errorMsg))
//            return byteArrayOf()
//        }
//
//        if (ignoredLog.isNotEmpty()) {
//            println("Export data: ignored data class columns [${ignoredLog.joinToString(", ")}]")
//        }
//
//        return when (type) {
//            FileMapperType.XLSX -> {
//                val exportJsonEngine = Json(jsonEngine) { encodeDefaults = true; explicitNulls = true }
//                val dataMatrix = data.map { item ->
//                    val json = exportJsonEngine.encodeToJsonElement(serializer<T>(), item).jsonObject
//                    propertyNames.map { prop ->
//                        val element = json[prop]
//                        if (element is JsonPrimitive) element.content else ""
//                    }
//                }
//                ZipEngine.zip(FileMapperXlsxEngine.createXlsxFiles(headers, dataMatrix))
//            }
//            FileMapperType.JSON -> {
//                // 2. CREATE PRETTY PRINT ENGINE
//                val prettyJsonEngine = Json(jsonEngine) {
//                    prettyPrint = true
//                    encodeDefaults = true
//                }
//
//
//                val filteredData = data.map { item ->
//                    val json = jsonEngine.encodeToJsonElement(serializer<T>(), item).jsonObject
//                    JsonObject(json.filterKeys { it in propertyNames })
//                }
//
//                // Encode to a pretty-printed string
//                prettyJsonEngine.encodeToString(filteredData).encodeToByteArray()
//
//            }
//        }
//    }
//
//
//
//
//
//
//}

