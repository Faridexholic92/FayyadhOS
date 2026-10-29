package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.View;

/** Native bounded sprite renderer: vsync-paced motion, no video, blur, physics or network. */
final class PremiumSceneView extends View {
    private static final long LOOP_MS = 8_000L;
    private static final long APPROACH_END_MS = 2_400L;
    private static final long INTERACTION_END_MS = 3_800L;
    private static final long RETREAT_END_MS = 6_200L;
    private static final long FULL_FRAME_INTERVAL_NANOS = 33_333_333L; // target up to 30 fps
    private static final long LITE_FRAME_INTERVAL_NANOS = 66_666_667L; // target up to 15 fps
    private static final int FULL_SPRITE_FRAME_MS = 80;
    private static final int LITE_SPRITE_FRAME_MS = 100;
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
        boolean birthdayConfettiActive = !birthday
                || (partyStarted > 0L && SystemClock.uptimeMillis() - partyStarted < BIRTHDAY_CONFETTI_MS);
        return !closed && enabled && active && interactive && !obscured && !audioBusy
                && quality > 0 && birthdayConfettiActive && isAttachedToWindow()
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
            // Choreographer aligns updates to display vsync; rendering is capped below.
            choreographer.postFrameCallback(frame);
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

    /** Smoothstep gives zero velocity at both ends, avoiding abrupt stops between phases. */
    static float smoothStep(float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return clamped * clamped * (3f - 2f * clamped);
    }

    /** Fraction from far-apart to close together over a loop with no long frozen sprite pose. */
    static float approachAt(long elapsedMs) {
        long phase = Math.floorMod(elapsedMs, LOOP_MS);
        if (phase < APPROACH_END_MS) return smoothStep(phase / (float) APPROACH_END_MS);
        if (phase < INTERACTION_END_MS) return 1f;
        if (phase < RETREAT_END_MS) {
            return 1f - smoothStep((phase - INTERACTION_END_MS)
                    / (float) (RETREAT_END_MS - INTERACTION_END_MS));
        }
        return 0f;
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
        if (birthday) {
            drawConfetti(c);
            c.restoreToCount(save);
            return;
        }
        if (atlas == null) {
            c.restoreToCount(save);
            return;
        }

        long phase = Math.floorMod(elapsed, LOOP_MS);
        float approach = approachAt(elapsed);
        boolean retreating = phase >= INTERACTION_END_MS && phase < RETREAT_END_MS;
        float hop = 0f;
        if (phase >= APPROACH_END_MS && phase < INTERACTION_END_MS) {
            float interaction = (phase - APPROACH_END_MS)
                    / (float) (INTERACTION_END_MS - APPROACH_END_MS);
            // Two complete hops, starting and ending at ground level.
            hop = (float) Math.abs(Math.sin(interaction * Math.PI * 4.0)) * 8f;
        }

        // The sprite flipbook keeps cycling even while the characters are apart.
        // Their path freezes at the current pose during pause, dialog, screen-off or Azan.
        int frameDuration = quality == 1 ? LITE_SPRITE_FRAME_MS : FULL_SPRITE_FRAME_MS;
        int cell = spriteCellAt(elapsed, frameDuration);
        float papa = 345f + approach * 85f;
        float boy = 650f - approach * 110f;
        sprite(c, 0, cell, papa, 396f - hop, 206f, retreating);
        sprite(c, 1, cell, boy, 399f - hop * .65f, 156f, !retreating);

        if (phase >= APPROACH_END_MS && phase < INTERACTION_END_MS) {
            paint.setColor(0xa7f5d07d);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.3f);
            c.drawArc(461, 299 - hop, 500, 338 - hop,
                    (phase - APPROACH_END_MS) * .16f, 140f, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        c.restoreToCount(save);
    }

    private void sprite(Canvas c, int row, int cell, float x, float ground, float height, boolean mirror) {
        src.set(cell * 192, row * 320, (cell + 1) * 192, (row + 1) * 320);
        float width = height * 192f / 320f;
        dest.set(x - width / 2f, ground - height, x + width / 2f, ground);
        int save = c.save();
        if (mirror) c.scale(-1f, 1f, x, ground - height / 2f);
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
