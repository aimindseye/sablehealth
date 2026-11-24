package io.github.aimindseye.sablehealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Visible Health Connect permission rationale. No analytics, network or record access. */
class M1PermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { M1PermissionRationale { finish() } }
    }
}

@Composable
private fun M1PermissionRationale(close: () -> Unit) {
    MaterialTheme {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Sable Health · Health Connect privacy", style = MaterialTheme.typography.titleMedium)
            Text(
                "Sable Health requests only the read categories you select. " +
                    "The M1-A check counts recent records and checks timestamps and writer apps.",
            )
            Text(
                "The app does not store health measurements, send them to a server, " +
                    "connect to a wearable over Bluetooth, sell data or run background collection.",
            )
            Text(
                "The optional coverage report includes categories, record counts, " +
                    "source app package names and earliest/latest observed UTC times. " +
                    "Review it privately before sharing; timestamps are personal metadata.",
            )
            Text(
                "Health Connect permissions can be revoked in Android Settings at any time. " +
                    "The Dashboard remains synthetic or empty during M1-A.",
            )
            FocusButton("Close", close)
        }
    }
}
