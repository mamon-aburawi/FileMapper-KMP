@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider

import io.mamon.filemapper.FileMapperType

/**
 * Opens the system file picker.
 * @param type The file extension filter (XLSX or JSON).
 */
expect object FileMapperPicker {
    suspend fun pickFile(type: FileMapperType): MapperFile?
}