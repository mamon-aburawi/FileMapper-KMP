@file:OptIn(ExperimentalWasmJsInterop::class)

package io.mamon.filemapper.engine


import org.khronos.webgl.Uint8Array
import kotlin.js.Promise


external interface JsZipInstance : JsAny {
    fun file(path: String): JsZipFile?
}

//
external interface JsZipFile : JsAny {
    fun async(type: String): Promise<Uint8Array>
}