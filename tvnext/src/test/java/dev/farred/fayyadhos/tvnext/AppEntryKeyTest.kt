package dev.farred.fayyadhos.tvnext

import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntryKeyTest {
    @Test fun keysSpecificLaunchActivitiesNotOnlyPackages() {
        assertEquals(
            "com.example.tv/com.example.tv.HomeActivity",
            AppEntryKey.of("com.example.tv", "com.example.tv.HomeActivity"),
        )
    }
}
