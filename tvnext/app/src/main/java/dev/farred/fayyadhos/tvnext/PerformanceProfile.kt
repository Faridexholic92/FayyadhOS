package dev.farred.fayyadhos.tvnext

/**
 * Conservative renderer budget selected from device-reported memory.
 *
 * This is a startup budget only. It does not claim a measured frame rate or
 * certify the GPU. Live renderer and thermal profiling are still required.
 */
internal data class RenderBudget(
    val name: String,
    val targetFps: Int,
    val sceneScale: Float,
    val realTimeShadows: Boolean,
    val particleEffects: Boolean,
)

internal object PerformanceProfileSelector {
    fun select(totalRamMb: Long): RenderBudget =
        if (totalRamMb in 1 until 3_000) {
            RenderBudget(
                name = "Lite",
                targetFps = 30,
                sceneScale = 0.75f,
                realTimeShadows = false,
                particleEffects = false,
            )
        } else {
            // Hako Pro's nominal 4 GB is not permission to enable 60 FPS.
            // Keep the initial load conservative until actual GPU/thermal tests.
            RenderBudget(
                name = "Balanced",
                targetFps = 30,
                sceneScale = 1.0f,
                realTimeShadows = false,
                particleEffects = false,
            )
        }
}
