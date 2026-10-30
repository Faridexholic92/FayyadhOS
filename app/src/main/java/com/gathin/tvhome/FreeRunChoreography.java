package com.gathin.tvhome;

/**
 * Arc-length sampled free-run paths for the approved PapaZola / BoBoiBoy atlas.
 * The routes reserve the left greeting column and bottom app dock, while each
 * character roams a separate curved lane so their sprites never stack on top
 * of one another. All coordinates use the 960 x 540 home canvas.
 */
final class FreeRunChoreography {
    static final long LOOP_MS = 11_000L;
    private static final int SAMPLES_PER_SEGMENT = 48;
    private static final long PAPA_JUMP_PERIOD_MS = 5_200L;
    private static final long BOY_JUMP_PERIOD_MS = 4_350L;

    // Keep PapaZola clear of the birthday headline; keep BoBoiBoy clear of the
    // weather card above and the outer screen edge.
    private static final float[][][] PATHS = {
        {
            {370f,410f},{378f,385f},{395f,360f},{430f,350f},
            {475f,352f},{520f,372f},{555f,398f},{548f,415f},
            {510f,420f},{465f,410f},{420f,392f},{385f,370f}
        },
        {
            {880f,410f},{872f,385f},{850f,360f},{820f,350f},
            {780f,352f},{740f,372f},{705f,398f},{712f,415f},
            {750f,420f},{795f,410f},{840f,392f},{865f,370f}
        }
    };
    private static final long[] START_OFFSETS_MS = {0L, 2_150L};
    private static final Route[] ROUTES = {
        new Route(PATHS[0]),
        new Route(PATHS[1])
    };

    private FreeRunChoreography() {}

    static final class Pose {
        final float x;
        final float groundY;
        final float scale;
        final float directionX;
        final float directionY;
        final float leanDegrees;
        final float hop;

        Pose(float x, float groundY, float scale, float directionX,
             float directionY, float leanDegrees, float hop) {
            this.x = x;
            this.groundY = groundY;
            this.scale = scale;
            this.directionX = directionX;
            this.directionY = directionY;
            this.leanDegrees = leanDegrees;
            this.hop = hop;
        }
    }

    static Pose sample(long elapsedMs, int character) {
        int index = Math.floorMod(character, ROUTES.length);
        long localTime = Math.floorMod(elapsedMs + START_OFFSETS_MS[index], LOOP_MS);
        Route route = ROUTES[index];
        Route.Point point = route.sample(localTime / (float) LOOP_MS);
        float depth = clamp((point.y - 348f) / (420f - 348f), 0f, 1f);
        float scale = 0.79f + depth * 0.19f;
        float lean = clamp(-point.directionY * 5.2f, -4.2f, 4.2f);
        return new Pose(point.x, point.y, scale, point.directionX,
                point.directionY, lean, hopAt(elapsedMs, index));
    }

    static boolean mirrorForDirection(int character, float directionX) {
        // Both atlas rows face right; mirror only while travelling left.
        return directionX < 0f;
    }

    static float bobAt(long elapsedMs, int character) {
        long cycle = character == 0 ? 398L : 366L;
        long offset = character == 0 ? 0L : 91L;
        double angle = Math.floorMod(elapsedMs + offset, cycle) / (double) cycle * Math.PI * 2.0;
        return (float) Math.sin(angle) * (character == 0 ? 1.55f : 1.2f);
    }

    static int spriteCellAt(long elapsedMs, int frameDurationMs) {
        return spriteCellAt(elapsedMs, frameDurationMs, 0);
    }

    static int spriteCellAt(long elapsedMs, int frameDurationMs, int character) {
        long interval = Math.max(1L, frameDurationMs - (character == 0 ? 0L : 7L));
        long offset = character == 0 ? 0L : 97L;
        return (int) (Math.floorMod(elapsedMs + offset, interval * 6L) / interval);
    }

    static float hopAt(long elapsedMs, int character) {
        long period = character == 0 ? PAPA_JUMP_PERIOD_MS : BOY_JUMP_PERIOD_MS;
        long offset = character == 0 ? 650L : 1_850L;
        long phase = Math.floorMod(elapsedMs + offset, period);
        final long takeoff = 720L;
        final long duration = 860L;
        if (phase < takeoff || phase >= takeoff + duration) return 0f;
        float t = (phase - takeoff) / (float) duration;
        // Half sine gives a soft take-off and landing without a vertical snap.
        return (float) Math.sin(Math.PI * t) * (character == 0 ? 18f : 14f);
    }

    private static final class Route {
        final float[] x;
        final float[] y;
        final float[] directionX;
        final float[] directionY;
        final float[] cumulative;
        final float length;

        Route(float[][] knots) {
            int count = knots.length * SAMPLES_PER_SEGMENT;
            x = new float[count];
            y = new float[count];
            directionX = new float[count];
            directionY = new float[count];
            cumulative = new float[count];

            for (int i = 0; i < count; i++) {
                int segment = i / SAMPLES_PER_SEGMENT;
                float t = (i % SAMPLES_PER_SEGMENT) / (float) SAMPLES_PER_SEGMENT;
                float[] p0 = knots[Math.floorMod(segment - 1, knots.length)];
                float[] p1 = knots[segment % knots.length];
                float[] p2 = knots[(segment + 1) % knots.length];
                float[] p3 = knots[(segment + 2) % knots.length];
                x[i] = catmull(p0[0], p1[0], p2[0], p3[0], t);
                y[i] = catmull(p0[1], p1[1], p2[1], p3[1], t);
                float dx = tangent(p0[0], p1[0], p2[0], p3[0], t);
                float dy = tangent(p0[1], p1[1], p2[1], p3[1], t);
                float magnitude = (float) Math.hypot(dx, dy);
                if (magnitude < 0.001f) {
                    dx = p2[0] - p1[0];
                    dy = p2[1] - p1[1];
                    magnitude = (float) Math.max(0.001, Math.hypot(dx, dy));
                }
                directionX[i] = dx / magnitude;
                directionY[i] = dy / magnitude;
                if (i > 0) {
                    cumulative[i] = cumulative[i - 1]
                            + (float) Math.hypot(x[i] - x[i - 1], y[i] - y[i - 1]);
                }
            }
            length = cumulative[count - 1]
                    + (float) Math.hypot(x[0] - x[count - 1], y[0] - y[count - 1]);
        }

        Point sample(float fraction) {
            float target = clamp(fraction, 0f, 1f) * length;
            int last = cumulative.length - 1;
            int previous;
            int next;
            float startDistance;
            float endDistance;
            float adjustedTarget = target;

            if (target >= cumulative[last]) {
                // Final arc-length interval runs from the final sample to sample zero.
                previous = last;
                next = 0;
                startDistance = cumulative[last];
                endDistance = length;
            } else {
                int low = 0, high = last;
                while (low < high) {
                    int mid = (low + high) >>> 1;
                    if (cumulative[mid] < target) low = mid + 1;
                    else high = mid;
                }
                next = low;
                previous = next == 0 ? last : next - 1;
                startDistance = cumulative[previous];
                endDistance = next == 0 ? length : cumulative[next];
                // At the beginning of the loop, interpolate through the seam
                // from the final sample to the first instead of clamping to the end.
                if (next == 0) adjustedTarget += length;
            }

            float span = Math.max(0.001f, endDistance - startDistance);
            float local = clamp((adjustedTarget - startDistance) / span, 0f, 1f);
            float px = lerp(x[previous], x[next], local);
            float py = lerp(y[previous], y[next], local);
            float dx = lerp(directionX[previous], directionX[next], local);
            float dy = lerp(directionY[previous], directionY[next], local);
            float magnitude = (float) Math.max(0.001, Math.hypot(dx, dy));
            return new Point(px, py, dx / magnitude, dy / magnitude);
        }

        static final class Point {
            final float x, y, directionX, directionY;
            Point(float x, float y, float directionX, float directionY) {
                this.x = x; this.y = y;
                this.directionX = directionX; this.directionY = directionY;
            }
        }
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

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }
}
