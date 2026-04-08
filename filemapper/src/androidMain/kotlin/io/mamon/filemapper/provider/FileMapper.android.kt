@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.mamon.filemapper.provider

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.mamon.filemapper.FileMapperType
import io.mamon.filemapper.FlexContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

actual class MapperFile(private val uri: Uri, private val context: Context) {
    actual val name: String
        get() {
            var result = "unknown_file"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
            return result
        }

    actual val extension: String
        get() = name.substringAfterLast('.', "")

    actual suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
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
    val context = LocalContext.current

    val mimeType = when (type) {
        FileMapperType.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        FileMapperType.JSON -> "application/json"
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        onResult(uri?.let { MapperFile(it, context) })
    }

    return remember(launcher, mimeType) {
        FileMapperPickerLauncher { launcher.launch(arrayOf(mimeType)) }
    }
}

actual object FileMapperPicker {

    actual suspend fun pickFile(type: FileMapperType): MapperFile? {
        val activity = FlexContextProvider.currentActivity
            ?: throw IllegalArgumentException(" platformContext must be a ComponentActivity on Android for non-compose picking.")


        val mimeType = when (type) {
            FileMapperType.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            FileMapperType.JSON -> "application/json"
        }

        return suspendCancellableCoroutine { cont ->
            val key = "flex_native_picker_${UUID.randomUUID()}"
            var launcher: ActivityResultLauncher<Array<String>>? = null

            launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                launcher?.unregister()

                if (uri != null) {
                    cont.resume(MapperFile(uri, activity))
                } else {
                    cont.resume(null)
                }
            }

            launcher.launch(arrayOf(mimeType))

            cont.invokeOnCancellation {
                launcher.unregister()
            }
        }
    }
}