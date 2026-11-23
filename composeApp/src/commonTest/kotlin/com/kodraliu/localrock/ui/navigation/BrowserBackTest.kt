package com.kodraliu.localrock.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The browser's back button does what the App's system back gesture does. */
class BrowserBackTest {

    /**
     * A browser tab's history: the page's own entry, plus whatever [BrowserBack] pushes. Going back
     * from the page's own entry leaves the page. Pop events arrive later, as in a browser.
     */
    private class FakeBrowser(startOnGuard: Boolean = false) : BrowserBackHistory {
        val entries = mutableListOf(false).apply { if (startOnGuard) add(true) } // true = a guard entry
        var index = entries.lastIndex
        var leftPage = false
        private val pendingPops = ArrayDeque<Boolean>()
        lateinit var back: BrowserBack

        override fun pushGuard() {
            while (entries.lastIndex > index) entries.removeAt(entries.lastIndex)
            entries.add(true)
            index = entries.lastIndex
        }

        override fun goBack() = moveBy(-1)

        fun userBack() { moveBy(-1); deliverPops() }
        fun userForward() { moveBy(1); deliverPops() }

        private fun moveBy(delta: Int) {
            val target = index + delta
            if (target < 0) { leftPage = true; return }
            if (target > entries.lastIndex) return
            index = target
            pendingPops.addLast(entries[index])
        }

        fun deliverPops() {
            while (pendingPops.isNotEmpty()) back.poppedTo(onGuard = pendingPops.removeFirst())
        }
    }

    private class Setup(startOnGuard: Boolean = false) {
        val browser = FakeBrowser(startOnGuard)
        var appBacks = 0
        val back = BrowserBack(browser, startedOnGuard = startOnGuard, dispatchBack = { appBacks++ })
            .also { browser.back = it }
    }

    @Test
    fun browser_back_goes_back_in_the_app_when_the_app_can_go_back() {
        val s = Setup()
        s.back.backAvailable(true)

        s.browser.userBack()

        assertEquals(1, s.appBacks)
        assertFalse(s.browser.leftPage)
    }

    @Test
    fun browser_back_keeps_going_back_while_the_app_can() {
        val s = Setup()
        s.back.backAvailable(true)

        s.browser.userBack()
        s.browser.userBack()
        s.browser.userBack()

        assertEquals(3, s.appBacks)
        assertFalse(s.browser.leftPage)
    }

    @Test
    fun once_the_app_cannot_go_back_browser_back_leaves_the_page() {
        val s = Setup()
        s.back.backAvailable(true)
        s.back.backAvailable(false) // the in-app back arrow went back to the first screen
        s.browser.deliverPops()

        s.browser.userBack()

        assertEquals(0, s.appBacks)
        assertTrue(s.browser.leftPage)
    }

    @Test
    fun a_reload_on_a_guard_entry_starts_on_the_first_screen_without_a_stale_guard() {
        val s = Setup(startOnGuard = true) // the browser restores the guard entry on reload
        s.back.backAvailable(false)
        s.browser.deliverPops()

        s.browser.userBack()

        assertEquals(0, s.appBacks)
        assertTrue(s.browser.leftPage)
    }

    @Test
    fun browser_forward_never_leaves_a_stale_guard() {
        val s = Setup()
        s.back.backAvailable(true)
        s.back.backAvailable(false)
        s.browser.deliverPops()

        s.browser.userForward() // back onto the dropped guard entry
        s.browser.userBack()

        assertEquals(0, s.appBacks)
        assertTrue(s.browser.leftPage)
    }
}
