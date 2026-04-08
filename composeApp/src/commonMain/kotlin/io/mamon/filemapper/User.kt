package io.mamon.filemapper

import io.mamon.filemapper.annotation.ExcelColumn
import kotlinx.serialization.Serializable


@Serializable
data class User(
    @ExcelColumn("User ID")
    val id: String = "",
    @ExcelColumn("User Name")
    val name: String
)
