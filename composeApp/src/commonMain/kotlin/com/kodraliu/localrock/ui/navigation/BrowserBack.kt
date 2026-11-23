package com.kodraliu.localrock.ui.navigation

/** The two things [BrowserBack] does to the browser tab's history. */
interface BrowserBackHistory {
    /** Adds a guard entry after the current one, with the same URL. */
    fun pushGuard()

    /** Goes back one entry; the browser reports it later through [BrowserBack.poppedTo]. */
    fun goBack()
}

/**
 * Web App only: makes the browser's back button do what the App's system back gesture does.
 */
class BrowserBack(
    private val history: BrowserBackHistory,
    startedOnGuard: Boolean,
    private val dispatchBack: () -> Unit,
) {
    private var onGuard = startedOnGuard
    private var available = false
    private var dropping = false // our own goBack(), whose pop is not the user's

    /** Whether the App would handle a back gesture right now. */
    fun backAvailable(available: Boolean) {
        this.available = available
        settle()
    }

    /** The browser moved to another history entry: a guard entry or not. */
    fun poppedTo(onGuard: Boolean) {
        this.onGuard = onGuard
        if (dropping) dropping = false else if (!onGuard) dispatchBack()
        settle()
    }

    private fun settle() {
        if (dropping) return
        if (available && !onGuard) {
            history.pushGuard()
            onGuard = true
        } else if (!available && onGuard) {
            dropping = true
            history.goBack()
        }
    }
}
