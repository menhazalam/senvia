package com.menhaz.senvia.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.menhaz.senvia.viewmodel.SettingsViewModel
import com.menhaz.senvia.viewmodel.SettingsViewModelFactory
import com.menhaz.senvia.core.ServiceStatus
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTestMessage: () -> Unit,
    onNavigateToDestinations: (() -> Unit)? = null,
    onRequestPermissions: (() -> Unit)? = null,
    onNavigateToLogs: (() -> Unit)? = null,
    permissionUpdateTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Lazy ViewModel creation - defer until after initial render
    var viewModelReady by remember { mutableStateOf(false) }
    val viewModel: SettingsViewModel? = if (viewModelReady) {
        viewModel(factory = SettingsViewModelFactory(context))
    } else null
    
    // Defer ViewModel creation to next frame
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50) // Small delay to let UI render first
        viewModelReady = true
    }
    
    // Use safe state collection with default values
    val botToken by (viewModel?.botToken?.collectAsState() ?: mutableStateOf(""))
    val chatId by (viewModel?.chatId?.collectAsState() ?: mutableStateOf(""))
    val phoneNumber by (viewModel?.phoneNumber?.collectAsState() ?: mutableStateOf(""))
    val currentDestination by (viewModel?.currentDestination?.collectAsState() ?: mutableStateOf(""))
    val filterKeywords by (viewModel?.filterKeywords?.collectAsState() ?: mutableStateOf(emptySet<String>()))
    val serviceRunning by (viewModel?.serviceRunning?.collectAsState() ?: mutableStateOf(false))
    val realServiceStatus by (viewModel?.realServiceStatus?.collectAsState() ?: mutableStateOf(ServiceStatus(false, false, false)))

    val isTelegramConfigured = botToken.isNotBlank() && chatId.isNotBlank()
    val isPhoneConfigured = phoneNumber.isNotBlank()
    val hasActiveDestination = when (currentDestination) {
        "telegram" -> isTelegramConfigured
        "phone" -> isPhoneConfigured
        else -> false
    }

// Check permissions when trigger updates
val isSmsPermissionGranted by remember(permissionUpdateTrigger) {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    )
}

val isNotificationPermissionGranted by remember(permissionUpdateTrigger) {
    mutableStateOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }
    )
}

val hasAllPermissions = isSmsPermissionGranted && isNotificationPermissionGranted
    
// Show loading state while ViewModel is being created
if (!viewModelReady) {
    LoadingHomeScreen(
        onRequestPermissions = onRequestPermissions,
        isSmsPermissionGranted = isSmsPermissionGranted,
        modifier = modifier
    )
    return
}

    val currentDestinationName = when (currentDestination) {
        "telegram" -> "Telegram"
        "phone" -> "Phone"
        else -> "Not Set"
    }

    val isFullyConfigured = hasAllPermissions && hasActiveDestination
    val isActuallyRunning = realServiceStatus.isRunning && realServiceStatus.isConfigured

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Main Status Card - More prominent and expressive
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    !hasAllPermissions -> MaterialTheme.colorScheme.errorContainer
                    !hasActiveDestination -> MaterialTheme.colorScheme.secondaryContainer
                    !isActuallyRunning -> MaterialTheme.colorScheme.tertiaryContainer
                    realServiceStatus.needsSync -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Icon and Title
                Icon(
                    imageVector = when {
                        !hasAllPermissions -> Icons.Filled.Warning
                        !hasActiveDestination -> Icons.Outlined.Settings
                        !isActuallyRunning -> Icons.Filled.Pause
                        realServiceStatus.needsSync -> Icons.Filled.Warning
                        else -> Icons.Filled.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        !hasAllPermissions -> MaterialTheme.colorScheme.onErrorContainer
                        !hasActiveDestination -> MaterialTheme.colorScheme.onSecondaryContainer
                        !isActuallyRunning -> MaterialTheme.colorScheme.onTertiaryContainer
                        realServiceStatus.needsSync -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(48.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            !hasAllPermissions -> "Permissions Required"
                            !hasActiveDestination -> "Setup Required"
                            !isActuallyRunning -> "Service Stopped"
                            realServiceStatus.needsSync -> "Service Sync Required"
                            else -> "Active & Forwarding"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !hasAllPermissions -> MaterialTheme.colorScheme.onErrorContainer
                            !hasActiveDestination -> MaterialTheme.colorScheme.onSecondaryContainer
                            !isActuallyRunning -> MaterialTheme.colorScheme.onTertiaryContainer
                            realServiceStatus.needsSync -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        textAlign = TextAlign.Center
                    )

                    if (hasActiveDestination && hasAllPermissions) {
                        Text(
                            text = "Forwarding SMS to $currentDestinationName",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Primary Action Button
                when {
                    !hasAllPermissions -> {
                        Button(
                            onClick = { onRequestPermissions?.invoke() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (!isSmsPermissionGranted) "Grant SMS Permissions"
                                else "Grant Notification Permission"
                            )
                        }
                    }
                    !hasActiveDestination -> {
                        Button(
                            onClick = { onNavigateToDestinations?.invoke() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Setup Destinations")
                        }
                    }
                    realServiceStatus.needsSync -> {
                        Button(
                            onClick = { viewModel?.restartService() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restart Service")
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel?.setServiceRunning(!serviceRunning) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (isActuallyRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isActuallyRunning) "Stop Service" else "Start Service")
                        }
                    }
                }
            }
        }

        // Quick Actions - Only show when fully configured
        if (isFullyConfigured) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel?.sendTestMessage()
                                onTestMessage()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test")
                        }

                        OutlinedButton(
                            onClick = { onNavigateToDestinations?.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Setup")
                        }
                    }
                }
            }
        }

        // Recent Activity - Show message forwarding history
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (viewModel?.messageLogs?.collectAsState()?.value?.isNotEmpty() == true) {
                        TextButton(
                            onClick = { onNavigateToLogs?.invoke() }
                        ) {
                            Text("View All")
                        }
                    }
                }

                val recentLogs = viewModel?.let { vm ->
                    vm.messageLogs.collectAsState().value.takeLast(3).reversed()
                } ?: emptyList()
                
                if (recentLogs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No recent activity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        recentLogs.forEach { log ->
                            ActivityItem(
                                sender = log.sender,
                                destination = log.destination,
                                status = log.status,
                                timestamp = log.timestamp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingHomeScreen(
    onRequestPermissions: (() -> Unit)?,
    isSmsPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Loading status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                if (!isSmsPermissionGranted) {
                    Button(
                        onClick = { onRequestPermissions?.invoke() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant SMS Permission")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    sender: String,
    destination: String,
    status: String,
    timestamp: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = when (destination) {
                "telegram" -> Icons.AutoMirrored.Outlined.Send
                "phone" -> Icons.Outlined.Phone
                else -> Icons.AutoMirrored.Outlined.Message
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (status == "sent") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "From $sender",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "via $destination",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = if (status == "sent") Icons.Outlined.CheckCircle else Icons.Outlined.Error,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (status == "sent") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}