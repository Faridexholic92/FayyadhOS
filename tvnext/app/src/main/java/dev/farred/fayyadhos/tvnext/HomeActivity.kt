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
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Responsive TV-first Home shell.
 *
 * This release focuses on layout, app discovery, remote navigation and device
 * diagnostics. Character artwork, live services and the real 3D stage are
 * deliberately not represented by fake assets.
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
    private lateinit var appScroll: HorizontalScrollView
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
        window.navigationBarColor = Color.rgb(5, 14, 25)
        hideSystemUi()
        setContentView(buildHome())
        refreshApps()
        updateClock()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
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
                intArrayOf(Color.rgb(4, 13, 25), Color.rgb(7, 24, 42), Color.rgb(5, 15, 29)),
            )
        }

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(12), dp(28), dp(12))
        }
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
        shell.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(54)))

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        shell.addView(main, LinearLayout.LayoutParams(-1, 0, 1f))
        main.addView(buildPrayerPanel(), LinearLayout.LayoutParams(dp(218), -1))
        main.addView(buildHeroStage(), LinearLayout.LayoutParams(0, -1, 1f).apply {
            marginStart = dp(10)
            marginEnd = dp(10)
        })
        main.addView(buildWeatherPanel(), LinearLayout.LayoutParams(dp(210), -1))

        shell.addView(buildDockHeader(), LinearLayout.LayoutParams(-1, dp(34)))
        appScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            clipToPadding = false
            setPadding(dp(4), dp(2), dp(4), dp(4))
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        appStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        appScroll.addView(appStrip, FrameLayout.LayoutParams(-2, -1))
        shell.addView(appScroll, LinearLayout.LayoutParams(-1, dp(108)))
        return root
    }

    private fun buildHeader(): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brand.addView(text("FAYYADHOS", 21f, Color.WHITE, true))
        brand.addView(text("TV NEXT  ·  HAKO PRO EDITION", 9f, CYAN_SOFT, true))
        row.addView(brand, LinearLayout.LayoutParams(0, -1, 1f))

        val timeBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_END
            setPadding(0, 0, dp(18), 0)
        }
        clockText = text("--:--", 23f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER_END
        }
        dateText = text("Loading date…", 10f, MUTED, false).apply {
            gravity = Gravity.CENTER_END
        }
        timeBox.addView(clockText)
        timeBox.addView(dateText)
        row.addView(timeBox, LinearLayout.LayoutParams(dp(170), -1))

        row.addView(actionButton("JARVIS") {
            showMessage("Jarvis AI integration will be migrated after the new Home and renderer are validated.")
        }, LinearLayout.LayoutParams(dp(88), dp(40)))
        row.addView(actionButton("DEVICE INFO", ::showDeviceInfo),
            LinearLayout.LayoutParams(dp(108), dp(40)).apply { marginStart = dp(8) })
        row.addView(actionButton("SETTINGS") {
            showMessage("The full Settings hub is still being migrated. Your current FayyadhOS launcher remains available as fallback.")
        }, LinearLayout.LayoutParams(dp(92), dp(40)).apply { marginStart = dp(8) })
        return row
    }

    private fun buildPrayerPanel(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(15), dp(15), dp(12))
        }
        content.addView(eyebrow("IBADAH"))
        addGap(content, 5)
        content.addView(text("Waktu Solat", 18f, Color.WHITE, true))
        content.addView(text("Zon belum dipilih", 11f, MUTED, false))
        addGap(content, 12)

        val prayerNames = listOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak")
        prayerNames.forEachIndexed { index, prayer ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(27)
            }
            item.addView(text(prayer, 12f, if (index == 0) CYAN else TEXT, index == 0),
                LinearLayout.LayoutParams(0, -1, 1f))
            item.addView(text("--:--", 12f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER_END
            })
            content.addView(item, LinearLayout.LayoutParams(-1, dp(27)))
            if (index != prayerNames.lastIndex) {
                content.addView(View(this).apply {
                    setBackgroundColor(Color.argb(35, 150, 190, 215))
                }, LinearLayout.LayoutParams(-1, dp(1)))
            }
        }
        addGap(content, 9)
        content.addView(text("JAKIM  ·  Jadual harian", 9f, MUTED, false))
        return panelScroll(content)
    }

    private fun buildHeroStage(): View {
        val stage = FrameLayout(this).apply {
            background = roundedBackground(
                intArrayOf(Color.rgb(12, 36, 59), Color.rgb(8, 25, 43), Color.rgb(7, 19, 34)),
                Color.argb(115, 91, 143, 177),
                24,
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        content.addView(eyebrow("THE DUAL-HERO UNIVERSE"))
        addGap(content, 5)
        content.addView(text("Dua dunia. Satu skrin.", 22f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        })
        addGap(content, 14)

        val pair = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        pair.addView(heroIdentityCard(
            "P", "PAPAZOLA", "HERO SLOT 01",
            intArrayOf(Color.rgb(117, 43, 43), Color.rgb(48, 28, 36)),
            Color.rgb(255, 177, 119),
        ), LinearLayout.LayoutParams(0, dp(142), 1f).apply {
            marginEnd = dp(7)
        })
        pair.addView(heroIdentityCard(
            "B", "BOBOIBOY", "HERO SLOT 02",
            intArrayOf(Color.rgb(24, 76, 126), Color.rgb(18, 37, 68)),
            Color.rgb(117, 220, 255),
        ), LinearLayout.LayoutParams(0, dp(142), 1f).apply {
            marginStart = dp(7)
        })
        content.addView(pair, LinearLayout.LayoutParams(-1, dp(142)))

        addGap(content, 12)
        content.addView(text(
            "Model 3D dan animasi watak sedang menunggu integrasi aset sebenar.",
            11f, MUTED, false,
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            maxLines = 2
        })
        stage.addView(content, FrameLayout.LayoutParams(-1, -1))
        return stage
    }

    private fun heroIdentityCard(
        initial: String,
        name: String,
        slot: String,
        colors: IntArray,
        accent: Int,
    ): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBackground(colors, Color.argb(105, accent), 18)
        }
        val orb = TextView(this).apply {
            text = initial
            textSize = 29f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(accent, colors[0]),
            ).apply { shape = GradientDrawable.OVAL }
        }
        card.addView(orb, LinearLayout.LayoutParams(dp(58), dp(58)))
        addGap(card, 7)
        card.addView(text(name, 14f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        card.addView(text(slot, 8f, accent, true).apply { gravity = Gravity.CENTER })
        return card
    }

    private fun buildWeatherPanel(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(15), dp(15), dp(12))
        }
        content.addView(eyebrow("FORECAST"))
        addGap(content, 5)
        content.addView(text("Cuaca", 18f, Color.WHITE, true))
        content.addView(text("MET Malaysia", 11f, MUTED, false))
        addGap(content, 12)

        val now = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        now.addView(text("SEKARANG", 10f, CYAN, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        now.addView(text("--°", 31f, Color.WHITE, true))
        content.addView(now)

        addGap(content, 8)
        listOf("3 JAM LAGI", "6 JAM LAGI").forEach { label ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(text(label, 10f, TEXT, false),
                LinearLayout.LayoutParams(0, dp(25), 1f))
            row.addView(text("--°", 12f, Color.WHITE, true))
            content.addView(row, LinearLayout.LayoutParams(-1, dp(25)))
        }
        addGap(content, 9)
        content.addView(text("Data langsung belum disambungkan.", 9f, MUTED, false))
        return panelScroll(content)
    }

    private fun panelScroll(content: View): View {
        return ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            background = roundedBackground(
                intArrayOf(Color.argb(168, 22, 48, 70), Color.argb(145, 13, 34, 53)),
                Color.argb(70, 133, 179, 203),
                20,
            )
            addView(content, ViewGroup.LayoutParams(-1, -2))
        }
    }

    private fun buildDockHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(text("APLIKASI ANDA", 11f, CYAN, true),
            LinearLayout.LayoutParams(0, -1, 1f))
        appCountText = text("Mencari aplikasi…", 10f, MUTED, false).apply {
            gravity = Gravity.CENTER_END
        }
        row.addView(appCountText)
        return row
    }

    private fun roundedBackground(colors: IntArray, stroke: Int, radiusDp: Int) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun actionButton(label: String, action: () -> Unit): TextView =
        text(label, 10f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = focusBackground(false)
            setOnFocusChangeListener { view, focused -> view.background = focusBackground(focused) }
            setOnClickListener { action() }
        }

    private fun focusBackground(focused: Boolean): GradientDrawable =
        GradientDrawable().apply {
            setColor(if (focused) Color.rgb(24, 80, 108) else Color.argb(110, 23, 48, 70))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(if (focused) 2 else 1), if (focused) CYAN else Color.rgb(58, 90, 116))
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 2
            includeFontPadding = false
        }

    private fun eyebrow(value: String): TextView = text(value, 9f, CYAN, true)

    private fun addGap(parent: LinearLayout, heightDp: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(heightDp)))
    }

    private fun refreshApps() {
        if (!::appStrip.isInitialized) return
        val apps = discoverApps()
        appStrip.removeAllViews()

        if (apps.isEmpty()) {
            appCountText.text = "Tiada aplikasi boleh dibuka"
            appStrip.addView(text("Tiada aplikasi dijumpai", 14f, Color.WHITE, false))
            return
        }

        appCountText.text = "${apps.size} aplikasi dipasang"
        apps.forEach { app ->
            appStrip.addView(
                buildAppCard(app),
                LinearLayout.LayoutParams(dp(126), dp(96)).apply {
                    marginStart = dp(4)
                    marginEnd = dp(8)
                },
            )
        }

        if (!initialFocusRequested) {
            initialFocusRequested = true
            appScroll.post {
                appScroll.scrollTo(0, 0)
                appStrip.getChildAt(0)?.requestFocus()
            }
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
                .forEach inner@{ resolveInfo ->
                    val activityInfo = resolveInfo.activityInfo ?: return@inner
                    if (activityInfo.packageName == packageName) return@inner
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

    private fun buildAppCard(app: AppEntry): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(6), dp(7), dp(6))
            isFocusable = true
            isClickable = true
            background = focusBackground(false)
            contentDescription = "Buka ${app.title}"
            setOnFocusChangeListener { view, focused ->
                view.background = focusBackground(focused)
                if (focused) {
                    val left = view.left
                    val right = left + view.width
                    when {
                        left < appScroll.scrollX -> appScroll.smoothScrollTo(left, 0)
                        right > appScroll.scrollX + appScroll.width ->
                            appScroll.smoothScrollTo(right - appScroll.width + dp(12), 0)
                    }
                }
            }
            setOnClickListener { launchApp(app) }
            addView(ImageView(this@HomeActivity).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
            addView(text(app.title, 10f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(-1, dp(25)))
        }

    private fun launchApp(app: AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(app.launchCategory)
                .setClassName(app.packageName, app.activityName)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(intent)
        } catch (error: Exception) {
            showMessage("Tidak dapat membuka ${app.title}: ${error.javaClass.simpleName}")
        }
    }

    private fun updateClock() {
        if (!::clockText.isInitialized) return
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(now)
    }

    private fun showDeviceInfo() {
        val device = DeviceProfileReader.read(this)
        val details = """
            Peranti: ${device.manufacturer} ${device.model}
            Android: ${device.androidRelease} (API ${device.sdkInt})
            Jumlah RAM: ${device.totalRamMb} MB
            RAM tersedia: ${device.availableRamMb} MB
            App memory class: ${device.memoryClassMb} MB
            OpenGL ES dilaporkan: ${device.requestedGlesVersion}
            Resolusi skrin: ${device.displayWidth} × ${device.displayHeight}

            Nama GPU dan driver sebenar akan dibaca melalui ujian renderer EGL.
        """.trimIndent()
        showMessage(details)
    }

    private fun showMessage(message: String) {
        AlertDialog.Builder(this)
            .setTitle("FayyadhOS TV Next")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showMessage("Tetapan penuh dan migrasi fungsi lama masih dalam pembangunan.")
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private companion object {
        val CYAN = Color.rgb(121, 217, 232)
        val CYAN_SOFT = Color.rgb(141, 187, 206)
        val TEXT = Color.rgb(225, 237, 245)
        val MUTED = Color.rgb(166, 189, 207)
    }
}
