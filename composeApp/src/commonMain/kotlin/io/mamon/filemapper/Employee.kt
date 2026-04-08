package io.mamon.filemapper



import io.mamon.filemapper.Constants.DEPARTMENT_COLUMN
import io.mamon.filemapper.Constants.EMPLOYEE_ID_COLUMN
import io.mamon.filemapper.Constants.FULL_NAME_COLUMN
import io.mamon.filemapper.Constants.IS_FAVORITE_COLUMN
import io.mamon.filemapper.Constants.RULE_COLUMN
import io.mamon.filemapper.annotation.ExcelColumn
import io.mamon.filemapper.annotation.IgnoreUnmappedColumns
import kotlinx.serialization.Serializable


//@IgnoreUnmappedColumns
@Serializable
data class Employee(
    @ExcelColumn(name = EMPLOYEE_ID_COLUMN, )
    val id: Int,

    @ExcelColumn(name = FULL_NAME_COLUMN, )
    val name: String,

    @ExcelColumn(name = DEPARTMENT_COLUMN, )
    val department: String,

    @ExcelColumn(name = IS_FAVORITE_COLUMN, )
    val isFavorite: Boolean,

)


