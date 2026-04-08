package io.mamon.filemapper

enum class FileMapperError(val message: String) {
    EMPTY_FILE("The selected file is empty."),
    INVALID_FORMAT("Invalid file format. Expected XLSX or JSON."),
    READ_FAILED("Failed to read the selected file."),
    EXPORT_FAILED("An error occurred during export."),
    NO_DATA_ROWS("The file contains no data rows."),
    ALL_COLUMNS_IGNORED("All columns for the target data class are ignored."),
    MISSING_COLUMNS("Missing Required Columns:"),
    UNEXPECTED_COLUMNS("Unexpected Columns found:"),
    UNEXPECTED_ERROR("An unexpected error occurred.")
}




