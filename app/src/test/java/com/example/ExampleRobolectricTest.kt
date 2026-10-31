package com.example

import android.content.Context
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.model.BatterySaverConfig
import com.example.model.BorderShapeType
import com.example.model.ContactEdgeRule
import com.example.model.DefaultAppRules
import com.example.model.EdgeColorPalette
import com.example.model.EdgeConfig
import com.example.model.EdgeScheduleConfig
import com.example.model.NotchConfig
import com.example.model.NotchType
import com.example.model.NotificationEdgeSettings
import com.example.rendering.EdgeLightRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EdgeLighting Pro", appName)
    }

    @Test
    fun `verify exactly 48 edge color presets`() {
        assertEquals(48, EdgeColorPalette.PRESETS.size)

        val ids = EdgeColorPalette.PRESETS.map { it.id }
        assertEquals(48, ids.toSet().size)
        assertEquals(0, ids.minOrNull())
        assertEquals(47, ids.maxOrNull())

        for (preset in EdgeColorPalette.PRESETS) {
            assertTrue("Preset ${preset.name} must have at least 2 colors", preset.colors.size >= 2)
            assertNotNull(preset.category)
            assertTrue("Preset category must not be blank", preset.category.isNotBlank())
        }
    }

    @Test
    fun `verify all preset categories are covered`() {
        val categories = EdgeColorPalette.PRESETS.map { it.category }.distinct()
        assertTrue(categories.contains("Neon"))
        assertTrue(categories.contains("Cosmic"))
        assertTrue(categories.contains("Elements"))
        assertTrue(categories.contains("Pastel"))
        assertTrue(categories.contains("Vibrant"))
    }

    @Test
    fun `verify default edge configuration parameters`() {
        val config = EdgeConfig()
        assertEquals(false, config.isOverlayEnabled)
        assertEquals(false, config.isCallerIdAlertEnabled)
        assertEquals(BorderShapeType.CONTINUOUS_LINE, config.shapeType)
        assertEquals(NotchType.NONE, config.notchConfig.notchType)
        assertTrue(config.borderThickness > 0f)
        assertTrue(config.animationSpeed > 0f)
        assertTrue(config.cornerRadiusTop >= 0f)
        assertTrue(config.cornerRadiusBottom >= 0f)
    }

    @Test
    fun `verify default app rules and notification settings`() {
        val rules = DefaultAppRules.getDefaultRules()
        assertTrue(rules.isNotEmpty())

        val packages = rules.map { it.packageName }
        assertTrue(packages.contains("com.whatsapp"))
        assertTrue(packages.contains("org.telegram.messenger"))
        assertTrue(packages.contains("com.instagram.android"))

        val notifSettings = NotificationEdgeSettings()
        assertTrue(notifSettings.isNotificationAlertEnabled)
        assertTrue(notifSettings.wakeScreenOnNotification)
        assertTrue(notifSettings.defaultAlertDurationSeconds > 0)

        val contactRule = ContactEdgeRule(
            id = "test-id",
            contactIdentifier = "Mama",
            paletteId = 41
        )
        assertEquals("Mama", contactRule.contactIdentifier)
        assertEquals(BorderShapeType.HEART, contactRule.shapeType)
    }

    @Test
    fun `verify DND schedule calculation over midnight`() {
        val schedule = EdgeScheduleConfig(
            isScheduleEnabled = true,
            startHour = 22,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            activeDays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY)
        )

        // 23:30 on Monday -> should be in DND
        val calNight = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(schedule.isCurrentlyInDnd(calNight))

        // 05:15 on Tuesday -> should be in DND
        val calMorning = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(schedule.isCurrentlyInDnd(calMorning))

        // 14:00 on Monday -> should NOT be in DND
        val calDay = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(schedule.isCurrentlyInDnd(calDay))

        // 23:30 on Friday -> inactive day, should NOT be in DND
        val calFriday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertFalse(schedule.isCurrentlyInDnd(calFriday))
    }

    @Test
    fun `verify battery saver logic`() {
        val config = BatterySaverConfig(
            isBatterySaverEnabled = true,
            triggerThresholdPercent = 20,
            reduceFpsTo30 = true,
            dimIntensityPercent = 50,
            disableGlowFilter = true
        )

        // Low battery (15%) not charging -> should apply battery saver
        assertTrue(config.shouldApplyBatterySaver(currentLevel = 15, isPowerSaveMode = false, isCharging = false))

        // Charging with 15% -> should NOT apply (full performance while plugged in)
        assertFalse(config.shouldApplyBatterySaver(currentLevel = 15, isPowerSaveMode = false, isCharging = true))

        // Sufficient battery (80%) but system power save is on -> should apply
        assertTrue(config.shouldApplyBatterySaver(currentLevel = 80, isPowerSaveMode = true, isCharging = false))

        // Sufficient battery (80%) normal mode -> should NOT apply
        assertFalse(config.shouldApplyBatterySaver(currentLevel = 80, isPowerSaveMode = false, isCharging = false))
    }

    @Test
    fun `verify EdgeLightRenderer builds paths for all notch types`() {
        val renderer = EdgeLightRenderer()
        val dummyCanvas = Canvas()

        val notchTypes = listOf(
            NotchType.NONE,
            NotchType.INFINITY_U,
            NotchType.INFINITY_V,
            NotchType.PUNCH_HOLE_CENTER,
            NotchType.PUNCH_HOLE_LEFT,
            NotchType.PILL_ISLAND
        )

        for (notch in notchTypes) {
            val testConfig = EdgeConfig(
                notchConfig = NotchConfig(
                    notchType = notch,
                    notchWidth = 120f,
                    notchHeight = 60f,
                    notchOffsetX = 0f,
                    notchOffsetY = 10f,
                    notchRadius = 20f
                )
            )

            renderer.updateDimensions(1080f, 2400f, testConfig)
            renderer.render(
                canvas = dummyCanvas,
                config = testConfig,
                nowNanos = 1_000_000_000L,
                batteryDimMultiplier = 0.5f,
                disableGlow = true
            )
        }
    }

    @Test
    fun `verify EdgeLightRenderer with all border shape types`() {
        val renderer = EdgeLightRenderer()
        val dummyCanvas = Canvas()

        for (shape in BorderShapeType.values()) {
            val testConfig = EdgeConfig(
                shapeType = shape,
                customEmoji = "🔥",
                shapeSize = 32f,
                shapeSpacing = 40f
            )

            renderer.updateDimensions(1080f, 2400f, testConfig)
            renderer.render(dummyCanvas, testConfig, 2_000_000_000L)
        }
    }

    @Test
    fun `verify brightness setting and scaling in EdgeConfig and EdgeLightRenderer`() {
        val configDefault = EdgeConfig()
        assertEquals(100, configDefault.brightnessPercent)

        val configDimmed = EdgeConfig(brightnessPercent = 45)
        assertEquals(45, configDimmed.brightnessPercent)

        val renderer = EdgeLightRenderer()
        val dummyCanvas = Canvas()
        renderer.updateDimensions(1080f, 2400f, configDimmed)
        // Ensure render completes cleanly with user brightness
        renderer.render(
            canvas = dummyCanvas,
            config = configDimmed,
            nowNanos = 3_000_000_000L,
            batteryDimMultiplier = 1.0f,
            disableGlow = false
        )
    }
}
