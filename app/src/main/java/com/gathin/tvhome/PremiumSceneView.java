package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.View;

/** Native bounded sprite renderer: vsync-paced motion, no video, blur, physics or network. */
final class PremiumSceneView extends View {
    private static final long FULL_FRAME_INTERVAL_NANOS = 33_333_333L; // target up to 30 fps
    private static final long LITE_FRAME_INTERVAL_NANOS = 66_666_667L; // target up to 15 fps
    private static final int FULL_SPRITE_FRAME_MS = 67;
    private static final int LITE_SPRITE_FRAME_MS = 67;
    private static final long BIRTHDAY_CONFETTI_MS = 10_000L;

    private final Choreographer choreographer = Choreographer.getInstance();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect src = new Rect();
    private final RectF dest = new RectF();
    private final WallpaperDissolveView wallpaper;
    private Bitmap atlas;
    private boolean active, interactive = true, obscured, audioBusy, enabled = true, birthday, closed, scheduled;
    private boolean timelineRunning;
    private int quality = 2;
    private long started = SystemClock.uptimeMillis(), elapsed, partyStarted, lastRenderedFrameNanos;

    private final Choreographer.FrameCallback frame = frameTimeNanos -> {
        scheduled = false;
        if (!canAnimate()) {
            sync();
            return;
        }

        // Derive positions from elapsed time rather than adding fixed per-frame deltas.
        // Slow frames can skip a render, but never accumulate timing drift.
        elapsed = Math.max(0L, SystemClock.uptimeMillis() - started);
        long interval = frameIntervalNanos(quality);
        if (lastRenderedFrameNanos == 0L || frameTimeNanos - lastRenderedFrameNanos >= interval) {
            lastRenderedFrameNanos = frameTimeNanos;
            invalidate();
            drift();
        }
        schedule();
    };

    PremiumSceneView(Context c, WallpaperDissolveView wallpaper) {
        super(c);
        this.wallpaper = wallpaper;
        setTag("premium-scene");
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        try (java.io.InputStream in = c.getAssets().open("premium/heroes-run.webp")) {
            atlas = BitmapFactory.decodeStream(in);
        } catch (java.io.IOException ignored) {
            // The home screen remains usable if optional animation artwork is missing.
        }
    }

    void configure(boolean enabled, boolean birthday, int quality) {
        if (birthday && !this.birthday) partyStarted = SystemClock.uptimeMillis();
        boolean qualityChanged = this.quality != quality;
        this.enabled = enabled;
        this.birthday = birthday;
        this.quality = quality;
        if (qualityChanged) lastRenderedFrameNanos = 0L;
        sync();
    }

    void hostActive(boolean value) { active = value; sync(); }
    void screen(boolean value) { interactive = value; sync(); }
    void obscured(boolean value) { obscured = value; sync(); }
    void audioBusy(boolean value) { audioBusy = value; sync(); }

    boolean canAnimate() {
        // Confetti has its own ten-second lifetime; it must not stop the character loop.
        return !closed && enabled && active && interactive && !obscured && !audioBusy
                && quality > 0 && isAttachedToWindow()
                && RainOptions.prefs(getContext()).getBoolean("moving", true)
                && Settings.Global.getFloat(getContext().getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0;
    }

    boolean hasFrame() { return scheduled; }

    private void sync() {
        long now = SystemClock.uptimeMillis();
        if (canAnimate()) {
            if (!timelineRunning) {
                started = now - elapsed;
                timelineRunning = true;
            }
            schedule();
        } else {
            pauseTimeline(now);
            cancelFrame();
            resetDrift();
        }
        invalidate();
    }

    private void pauseTimeline(long now) {
        if (timelineRunning) {
            elapsed = Math.max(0L, now - started);
            timelineRunning = false;
        }
    }

    private void schedule() {
        if (!scheduled && canAnimate()) {
            scheduled = true;
            // Delay the callback to the selected cadence; Choreographer still aligns it to vsync.
            choreographer.postFrameCallbackDelayed(frame, frameDelayMillis(quality));
        }
    }

    private void cancelFrame() {
        choreographer.removeFrameCallback(frame);
        scheduled = false;
    }

    static long frameIntervalNanos(int quality) {
        if (quality <= 0) return Long.MAX_VALUE;
        return quality == 1 ? LITE_FRAME_INTERVAL_NANOS : FULL_FRAME_INTERVAL_NANOS;
    }

    static long frameDelayMillis(int quality) {
        if (quality <= 0) return 0L;
        return quality == 1 ? 67L : 33L;
    }

    static int spriteCellAt(long elapsedMs, int frameDurationMs) {
        long safeInterval = Math.max(1, frameDurationMs);
        return (int) ((Math.max(0L, elapsedMs) / safeInterval) % 6L);
    }

    private void drift() {
        if (quality < 2) {
            resetDrift();
            return;
        }
        wallpaper.setScaleX(1.018f);
        wallpaper.setScaleY(1.018f);
        wallpaper.setTranslationX((float) Math.sin(elapsed / 16000.0) * getWidth() * .003f);
    }

    private void resetDrift() {
        wallpaper.setScaleX(1f);
        wallpaper.setScaleY(1f);
        wallpaper.setTranslationX(0f);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (!enabled || closed) return;

        int save = c.save();
        c.scale(getWidth() / 960f, getHeight() / 540f);
        // Birthday confetti expires independently. Keep drawing the characters afterward.
        if (birthday) drawConfetti(c);
        if (atlas == null) {
            c.restoreToCount(save);
            return;
        }

        // Each character follows a different closed Catmull-Rom path. Paths loop
        // seamlessly, but they are not a single horizontal approach/retreat. Their
        // run-cycle frames, depth scaling, turns and jumps are time-derived.
        int frameDuration = quality == 1 ? LITE_SPRITE_FRAME_MS : FULL_SPRITE_FRAME_MS;
        FreeRunChoreography.Pose papa = FreeRunChoreography.sample(elapsed, 0);
        FreeRunChoreography.Pose boy = FreeRunChoreography.sample(elapsed, 1);
        int papaCell = FreeRunChoreography.spriteCellAt(elapsed, frameDuration, 0);
        int boyCell = FreeRunChoreography.spriteCellAt(elapsed, frameDuration, 1);
        float papaBob = FreeRunChoreography.bobAt(elapsed, 0);
        float boyBob = FreeRunChoreography.bobAt(elapsed, 1);

        // The left greeting remains unobstructed; the two runners occupy
        // separate curved lanes, with depth-scaled shadows and non-synced gaits.
        groundShadow(c, papa.x, papa.groundY, 62f * papa.scale, papa.hop);
        groundShadow(c, boy.x, boy.groundY, 48f * boy.scale, boy.hop);
        sprite(c, 0, papaCell, papa.x, papa.groundY - papa.hop + papaBob,
                206f * papa.scale, FreeRunChoreography.mirrorForDirection(0, papa.directionX),
                papa.leanDegrees);
        sprite(c, 1, boyCell, boy.x, boy.groundY - boy.hop + boyBob,
                156f * boy.scale, FreeRunChoreography.mirrorForDirection(1, boy.directionX),
                boy.leanDegrees);
        c.restoreToCount(save);
    }

    private void groundShadow(Canvas c, float x, float ground, float width, float hop) {
        float spread = Math.max(0.58f, 1f - hop / 22f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x50020a12);
        c.drawOval(x - width * spread, ground - 2f, x + width * spread, ground + 5f, paint);
        paint.setColor(0x26020a12);
        c.drawOval(x - width * .62f * spread, ground - 1f, x + width * .62f * spread, ground + 3f, paint);
        paint.setColor(Color.WHITE);
    }

    private void sprite(Canvas c, int row, int cell, float x, float ground,
                         float height, boolean mirror, float leanDegrees) {
        src.set(cell * 192, row * 320, (cell + 1) * 192, (row + 1) * 320);
        float width = height * 192f / 320f;
        dest.set(x - width / 2f, ground - height, x + width / 2f, ground);
        float pivotY = ground - height * .48f;
        int save = c.save();
        c.rotate(leanDegrees, x, pivotY);
        if (mirror) c.scale(-1f, 1f, x, pivotY);
        paint.setAlpha(255);
        c.drawBitmap(atlas, src, dest, paint);
        c.restoreToCount(save);
    }

    private void drawConfetti(Canvas c) {
        if (!canAnimate()) return;
        long age = SystemClock.uptimeMillis() - partyStarted;
        if (age >= BIRTHDAY_CONFETTI_MS) return;
        for (int i = 0; i < 18; i++) {
            float x = 48 + (i * 131 % 864);
            float y = ((age * .045f + i * 37) % 290) + 180;
            paint.setColor(i % 2 == 0 ? 0xa8edc975 : 0x99bedff1);
            c.drawRect(x, y, x + 3, y + 6, paint);
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sync();
    }

    @Override protected void onDetachedFromWindow() {
        pauseTimeline(SystemClock.uptimeMillis());
        cancelFrame();
        resetDrift();
        super.onDetachedFromWindow();
    }

    void destroy() {
        closed = true;
        pauseTimeline(SystemClock.uptimeMillis());
        cancelFrame();
        resetDrift();
        if (atlas != null) {
            atlas.recycle();
            atlas = null;
        }
    }
}
