@file:OptIn(ExperimentalSerializationApi::class)

package io.mamon.filemapper.annotation


import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Ignores any columns in the Excel sheet that does not exist in data class.
 * Only the columns defined in this data class will be imported.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IgnoreUnmappedColumns