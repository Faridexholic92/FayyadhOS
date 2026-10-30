package dev.farred.fayyadhos.tvnext

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphicsVersionTest {
    @Test fun formatsOpenGles32() {
        assertEquals("3.2", GraphicsVersion.formatGlesVersion(0x00030002))
    }

    @Test fun formatsOpenGles30() {
        assertEquals("3.0", GraphicsVersion.formatGlesVersion(0x00030000))
    }

    @Test fun unknownWhenNotReported() {
        assertEquals("Unknown", GraphicsVersion.formatGlesVersion(0))
    }
}
