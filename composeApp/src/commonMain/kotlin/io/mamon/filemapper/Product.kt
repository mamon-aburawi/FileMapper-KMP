package io.mamon.filemapper

import io.mamon.filemapper.annotation.ExcelColumn
import kotlinx.serialization.Serializable



@Serializable
data class Product(
    @ExcelColumn(name = "Product ID")
    val id: String,

    @ExcelColumn(name = "Name")
    val title: String,

    @ExcelColumn(name = "Price")
    val price: Long = 0,

    @ExcelColumn(name = "Category")
    val category: String,

    @ExcelColumn(name = "Stock Quantity")
    val stock: Int = 0,

    @ExcelColumn(name = "Date")
    val date: String,

    @ExcelColumn(name = "Hero")
    val hero: String,


)


