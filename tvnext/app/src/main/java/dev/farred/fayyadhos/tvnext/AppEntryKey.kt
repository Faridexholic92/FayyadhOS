package dev.farred.fayyadhos.tvnext

internal object AppEntryKey {
    fun of(packageName: String, activityName: String): String =
        "$packageName/$activityName"
}
