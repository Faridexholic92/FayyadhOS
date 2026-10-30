package dev.farred.fayyadhos.tvnext

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TV-first engineering shell for the next launcher.
 *
 * This is intentionally not feature-complete. It proves the parallel Home package,
 * app discovery, remote focus and device information before feature migration.
 */
class HomeActivity : Activity() {

    private data class AppEntry(
        val key: String,
        val packageName: String,
        val activityName: String,
        val title: String,
        val icon: Drawable,
        val launchCategory: String,
    )

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var appStrip: LinearLayout
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var appCountText: TextView
    private var initialFocusRequested = false

    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.navigationBarColor = Color.rgb(7, 21, 37)
        setContentView(buildHome())
        refreshApps()
        updateClock()
    }

    override fun onResume() {
        super.onResume()
        refreshApps()
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun buildHome(): View {
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 15, 28), Color.rgb(10, 32, 53), Color.rgb(8, 18, 31)),
            )
        }
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), dp(18), dp(30), dp(18))
        }
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
        shell.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(58)))

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        shell.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        body.addView(buildClockPrayerPanel(), LinearLayout.LayoutParams(dp(238), -1))
        body.addView(buildHeroStage(), LinearLayout.LayoutParams(0, -1, 1f))
        body.addView(buildWeatherPanel(), LinearLayout.LayoutParams(dp(228), -1))

        shell.addView(buildDockHeader(), LinearLayout.LayoutParams(-1, dp(32)))
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }
        appStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        scroll.addView(appStrip, FrameLayout.LayoutParams(-2, -1))
        shell.addView(scroll, LinearLayout.LayoutParams(-1, dp(112)))
        return root
    }

    private fun buildHeader(): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(text("FAYYADHOS", 22f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, -1, 1f))
        row.addView(actionButton("JARVIS") {
            showFoundationMessage("Jarvis integration is scheduled after the Home shell and renderer gates.")
        }, LinearLayout.LayoutParams(dp(100), dp(42)))
        row.addView(actionButton("DEVICE INFO", ::showDeviceInfo),
            LinearLayout.LayoutParams(dp(128), dp(42)).apply { marginStart = dp(8) })
        row.addView(actionButton("SETTINGS") {
            showFoundationMessage("The new Settings hub is not migrated yet. Keep the current FayyadhOS launcher as your fallback.")
        }, LinearLayout.LayoutParams(dp(114), dp(42)).apply { marginStart = dp(8) })
        return row
    }

    private fun buildClockPrayerPanel(): View {
        val panel = panel()
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(16), dp(18), dp(16), dp(18))
        panel.addView(text("LOCAL TIME", 11f, Color.rgb(139, 180, 207), true))
        clockText = text("--:--", 38f, Color.WHITE, true)
        panel.addView(clockText, LinearLayout.LayoutParams(-1, dp(56)))
        dateText = text("Loading date…", 13f, Color.rgb(196, 216, 231), false)
        panel.addView(dateText, LinearLayout.LayoutParams(-1, dp(28)))

        addSpacer(panel, dp(22))
        panel.addView(text("JAKIM PRAYER TIMES", 12f, Color.rgb(121, 213, 225), true))
        panel.addView(text("Zone selection pending", 13f, Color.WHITE, true))
        listOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak").forEach { prayer ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(text(prayer, 13f, Color.rgb(206, 222, 233), false),
                LinearLayout.LayoutParams(0, dp(30), 1f))
            row.addView(text("--:--", 13f, Color.WHITE, true))
            panel.addView(row)
        }
        addSpacer(panel, dp(8))
        panel.addView(text("JAKIM schedule integration pending.", 10f,
            Color.rgb(145, 171, 192), false))
        return panel
    }

    private fun buildHeroStage(): View {
        val stage = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(15, 48, 76), Color.rgb(12, 33, 55), Color.rgb(7, 21, 37)),
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(50, 83, 112))
            }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        content.addView(text("LIVING HERO WORLD", 14f, Color.rgb(125, 221, 234), true))
        addSpacer(content, dp(12))
        content.addView(text("PapaZola  ×  BoBoiBoy", 26f, Color.WHITE, true))
        addSpacer(content, dp(12))
        content.addView(text("3D character scene foundation", 16f, Color.rgb(206, 221, 234), false))
        addSpacer(content, dp(20))
        content.addView(text(
            "Filament runtime is being wired in. Character-specific rigged GLB assets are required before the heroes can be rendered.",
            13f, Color.rgb(161, 190, 211), false,
        ).apply { gravity = Gravity.CENTER; textAlignment = View.TEXT_ALIGNMENT_CENTER })
        stage.addView(content, FrameLayout.LayoutParams(-1, -1))
        return stage
    }

    private fun buildWeatherPanel(): View {
        val panel = panel()
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(16), dp(18), dp(16), dp(18))
        panel.addView(text("WEATHER", 12f, Color.rgb(121, 213, 225), true))
        panel.addView(text("MET Malaysia", 16f, Color.WHITE, true))
        panel.addView(text("Forecast integration pending", 12f, Color.rgb(185, 207, 224), false))
        addSpacer(panel, dp(20))
        panel.addView(text("OPEN-METEO", 12f, Color.rgb(121, 213, 225), true))
        listOf("Now", "+3 hours", "+6 hours").forEach {
            panel.addView(text("$it     --°", 14f, Color.WHITE, false),
                LinearLayout.LayoutParams(-1, dp(35)))
        }
        addSpacer(panel, dp(16))
        panel.addView(text("Weather data loads after feature migration.", 10f,
            Color.rgb(145, 171, 192), false))
        return panel
    }

    private fun buildDockHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(text("YOUR APPS", 12f, Color.rgb(126, 214, 225), true),
            LinearLayout.LayoutParams(0, -1, 1f))
        appCountText = text("Discovering apps…", 11f, Color.rgb(163, 188, 207), false)
        row.addView(appCountText)
        return row
    }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        background = GradientDrawable().apply {
            setColor(Color.argb(100, 28, 55, 79))
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), Color.argb(75, 133, 179, 203))
        }
    }

    private fun actionButton(label: String, action: () -> Unit): TextView =
        text(label, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = focusBackground(false)
            setOnFocusChangeListener { view, focused -> view.background = focusBackground(focused) }
            setOnClickListener { action() }
        }

    private fun focusBackground(focused: Boolean): GradientDrawable =
        GradientDrawable().apply {
            setColor(if (focused) Color.rgb(28, 84, 110) else Color.argb(110, 23, 48, 70))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(if (focused) 2 else 1),
                if (focused) Color.rgb(101, 217, 232) else Color.rgb(58, 90, 116))
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 2
        }

    private fun addSpacer(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, height))
    }

    private fun refreshApps() {
        if (!::appStrip.isInitialized) return
        val apps = discoverApps()
        appStrip.removeAllViews()

        if (apps.isEmpty()) {
            appCountText.text = "No launchable applications discovered"
            appStrip.addView(text("No apps found", 16f, Color.WHITE, false))
            return
        }

        appCountText.text = "${apps.size} installed apps"
        apps.forEach { app ->
            appStrip.addView(buildAppCard(app), LinearLayout.LayoutParams(dp(126), dp(104)).apply {
                marginEnd = dp(10)
            })
        }

        if (!initialFocusRequested) {
            initialFocusRequested = true
            appStrip.post { appStrip.getChildAt(0)?.requestFocus() }
        }
    }

    @Suppress("DEPRECATION")
    private fun discoverApps(): List<AppEntry> {
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        )
        val found = LinkedHashMap<String, AppEntry>()
        intents.forEach { query ->
            packageManager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { resolveInfo ->
                    val activityInfo = resolveInfo.activityInfo ?: return@forEach
                    if (activityInfo.packageName == packageName) return@forEach
                    val key = AppEntryKey.of(activityInfo.packageName, activityInfo.name)
                    if (!found.containsKey(key)) {
                        val category = if (query.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)) {
                            Intent.CATEGORY_LEANBACK_LAUNCHER
                        } else Intent.CATEGORY_LAUNCHER
                        found[key] = AppEntry(
                            key = key,
                            packageName = activityInfo.packageName,
                            activityName = activityInfo.name,
                            title = resolveInfo.loadLabel(packageManager)?.toString()
                                ?.ifBlank { activityInfo.packageName } ?: activityInfo.packageName,
                            icon = resolveInfo.loadIcon(packageManager),
                            launchCategory = category,
                        )
                    }
                }
        }
        return found.values.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun buildAppCard(app: AppEntry): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isFocusable = true
            isClickable = true
            background = focusBackground(false)
            contentDescription = "Open ${app.title}"
            setOnFocusChangeListener { view, focused ->
                view.background = focusBackground(focused)
                if (focused) {
                    (appStrip.parent as? HorizontalScrollView)?.smoothScrollTo(
                        (view.left - dp(20)).coerceAtLeast(0), 0
                    )
                }
            }
            setOnClickListener { launchApp(app) }
            addView(ImageView(this@HomeActivity).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(42), dp(42)))
            addView(text(app.title, 11f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(-1, dp(30)))
        }
    }

    private fun launchApp(app: AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(app.launchCategory)
                .setClassName(app.packageName, app.activityName)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(intent)
        } catch (error: Exception) {
            showFoundationMessage("Could not open ${app.title}: ${error.javaClass.simpleName}")
        }
    }

    private fun updateClock() {
        if (!::clockText.isInitialized) return
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(now)
    }

    private fun showDeviceInfo() {
        val device = DeviceProfileReader.read(this)
        val details = """
            Device: ${device.manufacturer} ${device.model}
            Android: ${device.androidRelease} (API ${device.sdkInt})
            Total RAM: ${device.totalRamMb} MB
            Available RAM: ${device.availableRamMb} MB
            App memory class: ${device.memoryClassMb} MB
            Requested OpenGL ES: ${device.requestedGlesVersion}
            Display: ${device.displayWidth} × ${device.displayHeight}

            Note: the actual GL renderer / driver string will be captured from a live EGL context in the renderer probe.
        """.trimIndent()
        showFoundationMessage(details)
    }

    private fun showFoundationMessage(message: String) {
        AlertDialog.Builder(this)
            .setTitle("FayyadhOS TV Next · Foundation")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showFoundationMessage("Engineering build. Full Settings and legacy feature migration are not complete.")
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
