package dev.farred.fayyadhos.tvnext

internal object GraphicsVersion {
    fun formatGlesVersion(encoded: Int): String {
        if (encoded <= 0) return "Unknown"
        val major = encoded shr 16
        val minor = encoded and 0xffff
        return "$major.$minor"
    }
}
