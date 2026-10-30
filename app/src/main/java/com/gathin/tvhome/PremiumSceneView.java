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
    private final Paint groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Shader groundShade = new RadialGradient(0,0,1f,new int[]{0x99030b10,0x4d030b10,0x00030b10},new float[]{0f,.4f,1f},Shader.TileMode.CLAMP);
    private final Rect src = new Rect();
    private final RectF dest = new RectF();
    private final WallpaperDissolveView wallpaper;
    private Bitmap atlas, poseAtlas;
    private boolean pose = true;
    // heroes-pose.webp: PapaZola plus the exact user-supplied Halilintar RGBA pixels.
    static final Rect PAPA_POSE = new Rect(0, 0, 373, 483), BOY_POSE = new Rect(381, 0, 631, 250);
    // 960 x 540 canvas placement: centre x, feet y, drawn height.
    static final float[] PAPA_PLACE = {400f, 462f, 318f}, BOY_PLACE = {607f, 455f, 242f};
    private static final float[][] RIBBONS = {
            {30f, 300f, -28f, 1.25f}, {196f, 306f, 34f, .9f}, {226f, 340f, -18f, 1f},
            {160f, 376f, 52f, .8f}, {14f, 352f, 20f, .85f}, {704f, 410f, -38f, .9f}, {300f, 318f, 12f, .7f}};
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
        try (java.io.InputStream in = c.getAssets().open("premium/heroes-blended.webp")) {
            poseAtlas = BitmapFactory.decodeStream(in);
        } catch (java.io.IOException ignored) {
            // Falls back to the free-run sprites below.
        }
    }

    void characters(boolean heroPose) { if (pose != heroPose) { pose = heroPose; sync(); } }
    boolean heroPose() { return pose && poseAtlas != null; }

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
        return !heroPose() && !closed && enabled && active && interactive && !obscured && !audioBusy
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
        // The wallpaper is always static, independently of the Rain-on-Glass overlay.
        resetDrift();
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
        if (birthday) drawRibbons(c);
        if (heroPose()) {
            drawHeroPose(c);
            c.restoreToCount(save);
            return;
        }
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
        float spread=Math.max(.58f,1f-hop/22f);
        int save=c.save();c.translate(x-8f,ground+1f);c.scale(width*spread,9f);
        groundPaint.setShader(groundShade);c.drawCircle(0,0,1f,groundPaint);c.restoreToCount(save);
    }

    private void sprite(Canvas c, int row, int cell, float x, float ground,
                         float height, boolean mirror, float leanDegrees) {
        src.set(cell * 192, row * 320, (cell + 1) * 192, (row + 1) * 320);
        if (row == 1 && poseAtlas == null) return;
        float width = row == 1 ? height * BOY_POSE.width() / (float) BOY_POSE.height() : height * 192f / 320f;
        dest.set(x - width / 2f, ground - height, x + width / 2f, ground);
        float pivotY = ground - height * .48f;
        int save = c.save();
        c.rotate(leanDegrees, x, pivotY);
        if (mirror) c.scale(-1f, 1f, x, pivotY);
        paint.setAlpha(255);
        paint.setColorFilter(wallpaper.getColorFilter());
        if (row == 1) c.drawBitmap(poseAtlas, BOY_POSE, dest, paint);
        else c.drawBitmap(atlas, src, dest, paint);
        paint.setColorFilter(null);
        c.restoreToCount(save);
    }

    /** Static hero pose: no time-based bounce, even when rain movement is enabled. */
    static float poseBob(long elapsedMs, int character) { return 0f; }

    private void drawHeroPose(Canvas c) {
        float papaBob = poseBob(elapsed, 0), boyBob = poseBob(elapsed, 1);
        float papaLean = 0f;
        float boyLean = 0f;
        groundShadow(c, BOY_PLACE[0], BOY_PLACE[1], 64f, -boyBob * 2f);
        heroCell(c, BOY_POSE, BOY_PLACE, boyBob, boyLean);
        groundShadow(c, PAPA_PLACE[0], PAPA_PLACE[1], 86f, -papaBob * 2f);
        heroCell(c, PAPA_POSE, PAPA_PLACE, papaBob, papaLean);
    }

    private void heroCell(Canvas c, Rect cell, float[] place, float bob, float lean) {
        float height = place[2], width = height * cell.width() / (float) cell.height();
        float ground = place[1] + bob;
        dest.set(place[0] - width / 2f, ground - height, place[0] + width / 2f, ground);
        int save = c.save();
        c.rotate(lean, place[0], ground);
        paint.setAlpha(255);
        // Match weather brightness as well as the baked dusk ambient/ground grading.
        paint.setColorFilter(wallpaper.getColorFilter());
        c.drawBitmap(poseAtlas, cell, dest, paint);
        paint.setColorFilter(null);
        c.restoreToCount(save);
    }

    private void drawRibbons(Canvas c) {
        boolean moving = canAnimate();
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < RIBBONS.length; i++) {
            float[] r = RIBBONS[i];
            float flutter = moving ? (float) Math.sin(elapsed / 520.0 + i * 1.7) * 9f : 0f;
            int save = c.save();
            c.translate(r[0], r[1] + (moving ? (float) Math.sin(elapsed / 900.0 + i) * 2f : 0f));
            c.rotate(r[2] + flutter);
            c.scale(r[3], r[3]);
            paint.setColor(0xf2e9b44c);
            c.drawRoundRect(-3f, -9f, 3f, 9f, 2f, 2f, paint);
            paint.setColor(0xb3fff0b8);
            c.drawRoundRect(-1f, -8f, 1f, 2f, 1f, 1f, paint);
            c.restoreToCount(save);
        }
        paint.setColor(Color.WHITE);
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
        if (poseAtlas != null) {
            poseAtlas.recycle();
            poseAtlas = null;
        }
    }
}
