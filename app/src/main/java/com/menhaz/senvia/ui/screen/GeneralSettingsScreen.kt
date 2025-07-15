package com.menhaz.senvia.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.menhaz.senvia.core.BatteryOptimizationManager
import com.menhaz.senvia.core.CriticalLevel
import com.menhaz.senvia.ui.component.PreferenceEntry
import com.menhaz.senvia.viewmodel.SettingsViewModel
import com.menhaz.senvia.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    val batteryManager = remember { BatteryOptimizationManager.getInstance(context) }
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val realServiceStatus by viewModel.realServiceStatus.collectAsState()
    val autoStartEnabled by viewModel.autoStartEnabled.collectAsState()
    val autoDeleteLogsEnabled by viewModel.autoDeleteLogsEnabled.collectAsState()

    // Battery optimization state
    var batteryOptimizationIgnored by remember { mutableStateOf(false) }
    var batteryGuidance by remember { mutableStateOf(batteryManager.getBatteryManagementGuidance()) }

    // Update battery status on composition
    LaunchedEffect(Unit) {
        batteryOptimizationIgnored = batteryManager.isBatteryOptimizationIgnored()
        batteryGuidance = batteryManager.getBatteryManagementGuidance()
    }

    // Battery optimization request launcher
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Update status after returning from settings
        batteryOptimizationIgnored = batteryManager.isBatteryOptimizationIgnored()
        batteryGuidance = batteryManager.getBatteryManagementGuidance()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "General Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure service behavior and system permissions",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Service Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !realServiceStatus.isConfigured -> MaterialTheme.colorScheme.errorContainer
                        realServiceStatus.needsSync -> MaterialTheme.colorScheme.tertiaryContainer
                        realServiceStatus.isRunning -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                !realServiceStatus.isConfigured -> Icons.Filled.Error
                                realServiceStatus.needsSync -> Icons.Filled.Warning
                                realServiceStatus.isRunning -> Icons.Filled.CheckCircle
                                else -> Icons.Filled.Pause
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    !realServiceStatus.isConfigured -> "Service Not Configured"
                                    realServiceStatus.needsSync -> "Service Sync Required"
                                    realServiceStatus.isRunning -> "Service Running"
                                    else -> "Service Stopped"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            realServiceStatus.configurationIssue?.let { issue ->
                                Text(
                                    text = issue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (realServiceStatus.needsSync) {
                        Button(
                            onClick = { viewModel.restartService() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restart Service")
                        }
                    }
                }
            }

            // Service Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Auto-start setting
                    PreferenceEntry(
                        title = {
                            Text(
                                "Auto-start after boot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        description = {
                            Text(
                                "Automatically start service when device boots",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.PowerSettingsNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = autoStartEnabled,
                                onCheckedChange = { viewModel.setAutoStartEnabled(it) }
                            )
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Auto-delete logs setting
                    PreferenceEntry(
                        title = {
                            Text(
                                "Auto-delete Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        description = {
                            Text(
                                "Automatically delete message logs older than 30 days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = autoDeleteLogsEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setAutoDeleteLogsEnabled(enabled)
                                    scope.launch {
                                        if (enabled) {
                                            // Immediately clean old logs when enabled
                                            viewModel.deleteOldLogs()
                                            snackbarHostState.showSnackbar("Auto-delete enabled and old logs cleaned")
                                        } else {
                                            snackbarHostState.showSnackbar("Auto-delete disabled")
                                        }
                                    }
                                }
                            )
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Service restart
                    PreferenceEntry(
                        title = {
                            Text(
                                "Restart Service",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        description = {
                            Text(
                                "Restart the SMS forwarding service",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            scope.launch {
                                viewModel.restartService()
                                snackbarHostState.showSnackbar("Service restarted")
                            }
                        }
                    )
                }
            }

            // Battery Optimization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (batteryGuidance.criticalLevel) {
                        CriticalLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
                        CriticalLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                        CriticalLevel.LOW -> MaterialTheme.colorScheme.secondaryContainer
                        CriticalLevel.NONE -> MaterialTheme.colorScheme.surfaceContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (batteryOptimizationIgnored) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (batteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (batteryOptimizationIgnored) "Disabled (Recommended)" else "Enabled (May affect service)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (batteryOptimizationIgnored) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (!batteryOptimizationIgnored) {
                        Text(
                            text = "Battery optimization may prevent the SMS forwarding service from running reliably. Disable it for best performance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )

                        Button(
                            onClick = {
                                batteryManager.getRequestBatteryOptimizationIntent()?.let { intent ->
                                    try {
                                        batteryOptimizationLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Unable to open battery settings")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.BatteryAlert, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disable Battery Optimization")
                        }
                    }
                }
            }

            // Manufacturer-specific guidance
            if (batteryGuidance.manufacturerInfo.hasIssues) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${batteryGuidance.manufacturerInfo.name} Device Detected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = batteryGuidance.manufacturerInfo.settingsHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (batteryGuidance.manufacturerInfo.additionalSteps.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Additional steps:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                batteryGuidance.manufacturerInfo.additionalSteps.forEach { step ->
                                    Text(
                                        text = "• $step",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    batteryOptimizationLauncher.launch(intent)
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Unable to open app settings")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open App Settings")
                        }
                    }
                }
            }

            // System Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "System Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                        InfoRow("Android Version", "API ${android.os.Build.VERSION.SDK_INT}")
                        InfoRow("Service Status", if (realServiceStatus.isRunning) "Running" else "Stopped")
                        InfoRow("Battery Optimization", if (batteryOptimizationIgnored) "Disabled" else "Enabled")
                        InfoRow("Auto-start", if (autoStartEnabled) "Enabled" else "Disabled")
                        InfoRow("Auto-delete Logs", if (autoDeleteLogsEnabled) "Enabled (30 days)" else "Disabled")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}