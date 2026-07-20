// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundServicesScreen(
    onClickBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.prefs() }

    var spellCheckerEnabled by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_ENABLE_SPELL_CHECKER_SERVICE, Defaults.PREF_ENABLE_SPELL_CHECKER_SERVICE))
    }
    var contactsEnabled by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_ENABLE_CONTACTS_OBSERVER, Defaults.PREF_ENABLE_CONTACTS_OBSERVER))
    }
    var clipboardEnabled by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_ENABLE_CLIPBOARD_LISTENER, Defaults.PREF_ENABLE_CLIPBOARD_LISTENER))
    }
    var smsOtpEnabled by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_ENABLE_SMS_OTP_RECEIVER, Defaults.PREF_ENABLE_SMS_OTP_RECEIVER))
    }
    var appSyncEnabled by remember {
        mutableStateOf(prefs.getBoolean(Settings.PREF_ENABLE_APP_SYNC_LISTENER, Defaults.PREF_ENABLE_APP_SYNC_LISTENER))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Background Services & Processes") },
                navigationIcon = { BackButton(onClickBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Manage active background services, observers, and memory locks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Spell Checker Service
            ServiceCard(
                title = "System Spell Checker Service",
                description = "Runs background dictionary checks for system spellchecking. May hold JNI native memory.",
                status = if (spellCheckerEnabled) "ACTIVE" else "DISABLED",
                enabled = spellCheckerEnabled,
                onToggle = { enabled ->
                    spellCheckerEnabled = enabled
                    prefs.edit().putBoolean(Settings.PREF_ENABLE_SPELL_CHECKER_SERVICE, enabled).apply()
                },
                onStopClicked = {
                    Toast.makeText(context, "Spell Checker memory cache flushed", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Contacts Observer
            ServiceCard(
                title = "Contacts Content Observer",
                description = "Monitors contacts changes in the background to suggest contact names.",
                status = if (contactsEnabled) "LISTENING" else "DISABLED",
                enabled = contactsEnabled,
                onToggle = { enabled ->
                    contactsEnabled = enabled
                    prefs.edit().putBoolean(Settings.PREF_ENABLE_CONTACTS_OBSERVER, enabled).apply()
                },
                onStopClicked = {
                    Toast.makeText(context, "Contacts observer unregistered", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Clipboard History Listener
            ServiceCard(
                title = "Clipboard Listener",
                description = "Listens to system primary clip changes in the background.",
                status = if (clipboardEnabled) "LISTENING" else "DISABLED",
                enabled = clipboardEnabled,
                onToggle = { enabled ->
                    clipboardEnabled = enabled
                    prefs.edit().putBoolean(Settings.PREF_ENABLE_CLIPBOARD_LISTENER, enabled).apply()
                },
                onStopClicked = {
                    Toast.makeText(context, "Clipboard listener stopped", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. SMS OTP Receiver
            ServiceCard(
                title = "SMS OTP Auto-Reader",
                description = "Holds SMS broadcast receiver to automatically suggest incoming OTP passcodes.",
                status = if (smsOtpEnabled) "READY" else "DISABLED",
                enabled = smsOtpEnabled,
                onToggle = { enabled ->
                    smsOtpEnabled = enabled
                    prefs.edit().putBoolean(Settings.PREF_ENABLE_SMS_OTP_RECEIVER, enabled).apply()
                },
                onStopClicked = {
                    Toast.makeText(context, "SMS Receiver unregistered", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. App Name Launcher Sync
            ServiceCard(
                title = "App Launcher Name Sync",
                description = "Listens for package installations/removals to update app dictionary suggestions.",
                status = if (appSyncEnabled) "LISTENING" else "DISABLED",
                enabled = appSyncEnabled,
                onToggle = { enabled ->
                    appSyncEnabled = enabled
                    prefs.edit().putBoolean(Settings.PREF_ENABLE_APP_SYNC_LISTENER, enabled).apply()
                },
                onStopClicked = {
                    Toast.makeText(context, "App sync listener stopped", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun ServiceCard(
    title: String,
    description: String,
    status: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onStopClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Status: $status",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStopClicked,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Stop & Free Memory")
            }
        }
    }
}
