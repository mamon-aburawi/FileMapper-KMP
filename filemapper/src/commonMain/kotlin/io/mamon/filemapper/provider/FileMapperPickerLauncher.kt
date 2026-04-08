@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider

import androidx.compose.runtime.Composable
import io.mamon.filemapper.FileMapperType


expect class FileMapperPickerLauncher {
    fun launch()
}


@Composable
expect fun rememberFileMapperPicker(
    type: FileMapperType,
    onResult: (MapperFile?) -> Unit
): FileMapperPickerLauncher
