package com.kodraliu.localrock.ui

import androidx.compose.runtime.Composable

/** The Web App posts no notifications, so there is nothing to grant. */
@Composable
actual fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit = {
    onResult(false)
}
