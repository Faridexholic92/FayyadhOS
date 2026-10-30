package dev.farred.fayyadhos.tvnext

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

internal data class DeviceProfile(
    val model: String,
    val manufacturer: String,
    val androidRelease: String,
    val sdkInt: Int,
    val memoryClassMb: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val requestedGlesVersion: String,
    val displayWidth: Int,
    val displayHeight: Int,
)

internal object DeviceProfileReader {
    fun read(context: Context): DeviceProfile {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memory)
        val encodedGles = activityManager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val display: Display? = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?.defaultDisplay
        @Suppress("DEPRECATION")
        display?.getRealMetrics(metrics)

        return DeviceProfile(
            model = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            androidRelease = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            memoryClassMb = activityManager?.memoryClass ?: 0,
            totalRamMb = memory.totalMem / (1024L * 1024L),
            availableRamMb = memory.availMem / (1024L * 1024L),
            requestedGlesVersion = GraphicsVersion.formatGlesVersion(encodedGles),
            displayWidth = metrics.widthPixels,
            displayHeight = metrics.heightPixels,
        )
    }
}
