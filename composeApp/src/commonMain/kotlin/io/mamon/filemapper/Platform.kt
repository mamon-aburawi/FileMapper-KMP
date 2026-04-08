package io.mamon.filemapper

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform