package com.kodraliu.localrock.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kodraliu.localrock.resources.Res
import com.kodraliu.localrock.resources.web_app_no_server_body
import com.kodraliu.localrock.resources.web_app_no_server_title
import com.kodraliu.localrock.ui.adaptive.CenteredContent
import org.jetbrains.compose.resources.stringResource

/**
 * Shown instead of the App when the Web App's deployment named no Server in `config.json`, so a
 * setup problem doesn't look like a failed sign-in. There is no login form: there's nowhere to send it.
 */
@Composable
fun NoServerScreen() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) VacDarkColorScheme else VacLightColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CenteredContent {
                Column(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Text(stringResource(Res.string.web_app_no_server_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(Res.string.web_app_no_server_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
