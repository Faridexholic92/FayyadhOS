package com.gathin.tvhome;

/**
 * Deterministic free-run choreography for the existing character atlas.
 *
 * Coordinates use the renderer's 960x540 logical canvas. Closed Catmull-Rom
 * routes give continuous tangent changes at segment boundaries and loop wrap;
 * each character has its own path and jump rhythm. This is intentionally
 * separate from Android drawing so bounds and continuity can be unit-tested.
 */
final class FreeRunChoreography {
    static final long LOOP_MS = 18_000L;
    private static final long PAPA_JUMP_PERIOD_MS = 6_000L;
    private static final long BOY_JUMP_PERIOD_MS = 4_500L;
    private static final float MIN_X = 92f;
    private static final float MAX_X = 868f;
    private static final float MIN_GROUND = 330f;
    private static final float MAX_GROUND = 423f;

    private static final float[][][] PATHS = {
        {
            {112f,402f},{145f,374f},{238f,350f},{360f,352f},
            {490f,382f},{620f,412f},{760f,405f},{852f,370f},
            {810f,338f},{690f,342f},{565f,360f},{450f,398f},
            {310f,418f},{185f,420f}
        },
        {
            {842f,402f},{822f,372f},{752f,347f},{655f,352f},
            {555f,376f},{452f,411f},{328f,418f},{218f,400f},
            {148f,366f},{205f,344f},{320f,354f},{438f,378f},
            {560f,412f},{700f,422f}
        }
    };

    private FreeRunChoreography() {}

    static final class Pose {
        final float x;
        final float groundY;
        final float scale;
        final float directionX;
        final float hop;

        Pose(float x, float groundY, float scale, float directionX, float hop) {
            this.x = x;
            this.groundY = groundY;
            this.scale = scale;
            this.directionX = directionX;
            this.hop = hop;
        }
    }

    static Pose sample(long elapsedMs, int character) {
        int index = Math.floorMod(character, PATHS.length);
        float[][] path = PATHS[index];
        long phaseMs = Math.floorMod(elapsedMs, LOOP_MS);
        float progress = phaseMs / (float) LOOP_MS * path.length;
        int segment = (int) progress;
        float t = progress - segment;
        float[] p0 = path[Math.floorMod(segment - 1, path.length)];
        float[] p1 = path[segment % path.length];
        float[] p2 = path[(segment + 1) % path.length];
        float[] p3 = path[(segment + 2) % path.length];

        float x = catmull(p0[0], p1[0], p2[0], p3[0], t);
        float y = catmull(p0[1], p1[1], p2[1], p3[1], t);
        float dx = tangent(p0[0], p1[0], p2[0], p3[0], t);
        if (Math.abs(dx) < 0.01f) dx = p2[0] - p1[0];
        x = clamp(x, MIN_X, MAX_X);
        y = clamp(y, MIN_GROUND, MAX_GROUND);

        float depth = clamp((y - MIN_GROUND) / (MAX_GROUND - MIN_GROUND), 0f, 1f);
        float scale = 0.76f + depth * 0.22f;
        return new Pose(x, y, scale, dx, hopAt(elapsedMs, index));
    }

    static boolean mirrorForDirection(int character, float directionX) {
        // The PapaZola atlas faces right by default; the BoBoiBoy row faces left.
        return character == 0 ? directionX < 0f : directionX > 0f;
    }

    static float bobAt(long elapsedMs, int character) {
        long offset = character == 0 ? 0L : 113L;
        double angle = Math.floorMod(elapsedMs + offset, 400L) / 400.0 * Math.PI * 2.0;
        return (float) Math.sin(angle) * (character == 0 ? 1.35f : 1.05f);
    }

    private static float hopAt(long elapsedMs, int character) {
        long period = character == 0 ? PAPA_JUMP_PERIOD_MS : BOY_JUMP_PERIOD_MS;
        long offset = character == 0 ? 0L : 1_700L;
        long phase = Math.floorMod(elapsedMs + offset, period);
        final long start = 1_000L;
        final long duration = 1_000L;
        if (phase < start || phase >= start + duration) return 0f;
        float t = (phase - start) / (float) duration;
        return (float) Math.sin(Math.PI * t) * (character == 0 ? 16f : 13f);
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * ((2f * p1) + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }

    private static float tangent(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        return 0.5f * ((-p0 + p2)
                + 2f * (2f * p0 - 5f * p1 + 4f * p2 - p3) * t
                + 3f * (-p0 + 3f * p1 - 3f * p2 + p3) * t2);
    }

    private static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }
}
