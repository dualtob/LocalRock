package com.kodraliu.localrock.shared.notify

/** The Web App posts no system notifications; alerts are shown in the page while it is open. */
private object NoRobotNotifier : RobotNotifier {
    override fun post(notification: RobotNotification) = Unit
    override fun showProgress(progress: RobotProgress) = Unit
    override fun clearProgress(duid: String) = Unit
}

actual fun platformRobotNotifier(): RobotNotifier = NoRobotNotifier

/** A closed browser tab keeps no connection open. */
actual val backgroundAlertsSupported: Boolean = false
