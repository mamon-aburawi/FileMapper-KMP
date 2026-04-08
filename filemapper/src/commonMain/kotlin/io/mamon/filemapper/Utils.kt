package io.mamon.filemapper



internal fun <T> runBlocking(block: suspend () -> T): T {

    throw IllegalStateException("Synchronous resource loading is not supported.")
}
