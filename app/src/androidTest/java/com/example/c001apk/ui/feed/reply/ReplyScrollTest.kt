package com.example.c001apk.ui.feed.reply

import android.app.UiAutomation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.R
import com.example.c001apk.testing.ReplyScrollTestActivity
import com.example.c001apk.view.SmoothInputLayout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReplyScrollTest(private val orientation: Int, private val entry: String) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun rotateDevice() {
        // Rotate the device, not just the host: the translucent activity must keep
        // the same orientation instead of rotating back on launch.
        assertTrue(instrumentation.uiAutomation.setRotation(
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                UiAutomation.ROTATION_FREEZE_0 else UiAutomation.ROTATION_FREEZE_90
        ))
    }

    @After
    fun restoreRotation() {
        instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    @Test
    fun openingAndReopeningReplyPreservesBackgroundPosition() {
        val context = instrumentation.targetContext
        val intent = Intent(context, ReplyScrollTestActivity::class.java)
        ActivityScenario.launch<ReplyScrollTestActivity>(intent).use { scenario ->
            settle()
            if (entry == "nested") {
                scenario.onActivity { it.showReplies() }
                settle()
            }
            // Open, cancel/reopen, scroll again, then return to the top.
            repeat(4) { attempt ->
                enterTouchMode(scenario)
                if (attempt != 1) {
                    scenario.onActivity { host ->
                        (host.feed.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(if (attempt == 3) 0 else 12 + attempt * 5,
                                if (attempt == 3) 0 else -37)
                        host.replies?.let {
                            (it.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(if (attempt == 3) 0 else 8 + attempt * 5,
                                    if (attempt == 3) 0 else -53)
                        }
                    }
                }
                settle()
                lateinit var host: ReplyScrollTestActivity
                var before = emptyList<Pair<Int, Int>>()
                scenario.onActivity {
                    host = it
                    before = anchors(it)
                    assertTrue("Background must start in touch mode", it.feed.isInTouchMode)
                }
                val monitor = instrumentation.addMonitor(ReplyActivity::class.java.name, null, false)
                var reply: ReplyActivity? = null
                try {
                    instrumentation.runOnMainSync {
                        host.startActivity(Intent(host, ReplyActivity::class.java)
                            .putExtra("type", if (entry == "feed") "feed" else "reply")
                            .putExtra("rid", "test-reply")
                            .putExtra("username", "Test user"))
                    }
                    reply = instrumentation.waitForMonitorWithTimeout(monitor, 5000) as? ReplyActivity
                    val opened = checkNotNull(reply) { "ReplyActivity did not open" }
                    settle()
                    instrumentation.runOnMainSync {
                        assertEquals("Reply must keep the device orientation",
                            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE,
                            opened.resources.configuration.orientation)
                        assertEquals("Background moved on $entry open #$attempt", before, anchors(host))
                        assertTrue("Opening reply must preserve touch mode", host.feed.isInTouchMode)
                        assertTrue("Editor must receive focus", opened.binding.editText.hasFocus())
                    }
                    // A cold emulator can reject the existing 150ms auto-show request before
                    // the editor is served. Test keyboard input after a real tap, independently
                    // of the opening-position assertion above.
                    if (!isKeyboardVisible(opened)) {
                        onView(withId(R.id.editText)).perform(click())
                    }
                    awaitKeyboard(opened)
                    instrumentation.runOnMainSync {
                        assertEquals("Keyboard moved the background", before, anchors(host))
                        assertTrue("Keyboard must open", ViewCompat.getRootWindowInsets(
                            opened.binding.root
                        )?.isVisible(WindowInsetsCompat.Type.ime()) == true)
                        (opened.binding.main as? SmoothInputLayout)?.showEmojiPanel(true)
                    }
                    settle()
                    instrumentation.runOnMainSync {
                        assertEquals("Emoji panel moved the background", before, anchors(host))
                        assertTrue("Emoji panel must preserve touch mode", host.feed.isInTouchMode)
                        (opened.binding.main as? SmoothInputLayout)?.showKeyboard()
                    }
                    settle()
                    instrumentation.runOnMainSync {
                        assertEquals("Returning to keyboard moved the background", before, anchors(host))
                    }
                } finally {
                    instrumentation.runOnMainSync { reply?.finish() }
                    instrumentation.removeMonitor(monitor)
                    settle()
                }
                scenario.onActivity {
                    assertEquals("Cancel must preserve position", before, anchors(it))
                }
            }
        }
    }

    private fun anchors(host: ReplyScrollTestActivity) =
        listOfNotNull(host.feed, host.replies).map { list ->
            val manager = list.layoutManager as LinearLayoutManager
            val position = manager.findFirstVisibleItemPosition()
            check(position != RecyclerView.NO_POSITION)
            position to checkNotNull(manager.findViewByPosition(position)).top
        }

    private fun enterTouchMode(scenario: ActivityScenario<ReplyScrollTestActivity>) {
        var x = 0f
        var y = 0f
        scenario.onActivity { host ->
            val list = host.replies ?: host.feed
            val location = IntArray(2)
            list.getLocationOnScreen(location)
            x = location[0] + list.width / 2f
            y = location[1] + list.height / 2f
        }
        val downTime = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        settle()
    }

    private fun awaitKeyboard(reply: ReplyActivity) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < deadline) {
            if (isKeyboardVisible(reply)) return
            SystemClock.sleep(100)
        }
        // The assertion below reports failure after allowing a cold IME to start.
    }

    private fun isKeyboardVisible(reply: ReplyActivity): Boolean {
        var visible = false
        instrumentation.runOnMainSync {
            visible = ViewCompat.getRootWindowInsets(reply.binding.root)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        return visible
    }

    private fun settle() {
        // Includes ReplyActivity's delayed keyboard request and window/IME transitions.
        SystemClock.sleep(700)
        instrumentation.waitForIdleSync()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "orientation={0}, entry={1}")
        fun cases() = listOf(
            arrayOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, "feed"),
            arrayOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, "comment"),
            arrayOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, "nested"),
            arrayOf(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, "feed"),
            arrayOf(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, "comment"),
            arrayOf(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, "nested"),
        )
    }
}
