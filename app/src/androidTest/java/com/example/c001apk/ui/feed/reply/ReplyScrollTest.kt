package com.example.c001apk.ui.feed.reply

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.testing.ReplyScrollTestActivity
import com.example.c001apk.view.SmoothInputLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReplyScrollTest(private val orientation: Int, private val entry: String) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun openingAndReopeningReplyPreservesBackgroundPosition() {
        val context = instrumentation.targetContext
        val intent = Intent(context, ReplyScrollTestActivity::class.java)
            .putExtra("orientation", orientation)
        ActivityScenario.launch<ReplyScrollTestActivity>(intent).use { scenario ->
            settle()
            if (entry == "nested") {
                scenario.onActivity { it.showReplies() }
                settle()
            }
            // Open, cancel/reopen without scrolling, then scroll and open again.
            repeat(3) { attempt ->
                instrumentation.setInTouchMode(true)
                if (attempt != 1) {
                    scenario.onActivity { host ->
                        (host.feed.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(12 + attempt * 5, -37)
                        host.replies?.let {
                            (it.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(8 + attempt * 5, -53)
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
                    checkNotNull(reply) { "ReplyActivity did not open" }
                    settle()
                    val opened = reply
                    instrumentation.runOnMainSync {
                        assertEquals("Background moved on $entry open #$attempt", before, anchors(host))
                        assertTrue("Opening reply must preserve touch mode", host.feed.isInTouchMode)
                        assertTrue("Editor must receive focus", opened.binding.editText.hasFocus())
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
