package io.mamon.filemapper


class FileMapperException(
    val type: FileMapperError,
    internal val reason: String? = null
) : Exception(if (reason != null) "${type.message} $reason" else type.message)




fun FileMapperException.getLocalizedMessage(): String {
    return  FileMapperStrings.getMessage(this.type, this.message)
}

