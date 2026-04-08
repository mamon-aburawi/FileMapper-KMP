package io.mamon.filemapper


import io.github.mamon_aburawi.filemapper.generated.resources.Res
import io.github.mamon_aburawi.filemapper.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString


@PublishedApi
internal object FileMapperStrings {

    private fun mapErrorToResource(type: FileMapperError): StringResource = when (type) {
        FileMapperError.EMPTY_FILE -> Res.string.error_empty_file
        FileMapperError.INVALID_FORMAT -> Res.string.error_invalid_format
        FileMapperError.READ_FAILED -> Res.string.error_read_failed
        FileMapperError.EXPORT_FAILED -> Res.string.error_export_failed
        FileMapperError.NO_DATA_ROWS -> Res.string.error_no_data_rows
        FileMapperError.ALL_COLUMNS_IGNORED -> Res.string.error_all_columns_ignored
        FileMapperError.MISSING_COLUMNS -> Res.string.error_missing_columns
        FileMapperError.UNEXPECTED_COLUMNS -> Res.string.error_unexpected_columns
        FileMapperError.UNEXPECTED_ERROR -> Res.string.error_unexpected_error
    }


    fun getMessage(type: FileMapperError, reason: String?): String {
        val resource = mapErrorToResource(type)
        val message = runBlocking { getString(resource) }
        return formatMessage(message, reason)
    }


    private fun formatMessage(baseMessage: String, reason: String?): String {
        return if (reason != null && baseMessage.contains("%s")) {
            baseMessage.replace("%s", "\u200E$reason\u200E")
        } else {
            baseMessage
        }
    }
}


