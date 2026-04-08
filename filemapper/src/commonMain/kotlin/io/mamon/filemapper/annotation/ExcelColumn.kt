@file:OptIn(ExperimentalSerializationApi::class)

package io.mamon.filemapper.annotation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Maps a Kotlin data class property to a specific column header in an Excel sheet.
 *
 * @param name The exact name of the column header in the Excel sheet (e.g., "Full Name").
 * @param ignore If set to `true`, this property is completely detached from the Excel engine.
 *
 * ---
 * * ### IMPORT BEHAVIOR (When ignore = true)
 * The engine will **not** look for this column in the uploaded Excel file, and it will not throw a
 * "Missing Column" error. Instead, it auto-fills the property using the following rules:
 * 1. **Data Class Default:** If your property has a default value (e.g., `val isSynced: Boolean = true`), it will use that.
 * 2. **Engine Fallback:** If no default value exists, it safely injects `""` for Strings, `0` for Numbers, and `false` for Booleans.
 *
 * ### EXPORT BEHAVIOR (When ignore = true)
 * The engine will completely skip this property when generating the Excel file.
 * No column will be created for it, ensuring your internal app data (like local IDs or sync states)
 * remains private and is never exported to the user's downloaded file.
 */

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExcelColumn(
    val name: String,
    val ignore: Boolean = false
)

