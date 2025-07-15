package com.menhaz.senvia.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.menhaz.senvia.ui.component.PreferenceEntry
import com.menhaz.senvia.viewmodel.SettingsViewModel
import com.menhaz.senvia.viewmodel.SettingsViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDestinations: () -> Unit,
    onNavigateToFilter: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    val filterKeywords by viewModel.filterKeywords.collectAsState()
    val botToken by viewModel.botToken.collectAsState()
    val chatId by viewModel.chatId.collectAsState()
    val currentDestination by viewModel.currentDestination.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()

    val isTelegramConfigured = botToken.isNotBlank() && chatId.isNotBlank()
    val isPhoneConfigured = phoneNumber.isNotBlank()
    val hasActiveDestination = when (currentDestination) {
        "telegram" -> isTelegramConfigured
        "phone" -> isPhoneConfigured
        else -> false
    }

    Column(
        modifier = modifier
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
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Configure your SMS forwarding preferences",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Main Settings Card
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
                // General Settings
                PreferenceEntry(
                    title = {
                        Text(
                            "General",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    description = {
                        Text(
                            "Service settings and battery optimization",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = onNavigateToGeneral
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Destinations Setting
                PreferenceEntry(
                    title = {
                        Text(
                            "Destinations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    description = {
                        Text(
                            "Configure where to forward SMS messages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = onNavigateToDestinations
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Filters Setting
                PreferenceEntry(
                    title = {
                        Text(
                            "Filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    description = {
                        Text(
                            if (filterKeywords.isEmpty())
                                "All messages will be forwarded"
                            else
                                "${filterKeywords.size} active filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (filterKeywords.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = null,
                            tint = if (filterKeywords.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        if (filterKeywords.isNotEmpty()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    filterKeywords.size.toString(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    },
                    onClick = onNavigateToFilter
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // About Setting
                PreferenceEntry(
                    title = {
                        Text(
                            "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    description = {
                        Text(
                            "App info, privacy, and features",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = onNavigateToAbout
                )
            }
        }

        // Configuration Status Card (if not fully configured)
        if (!hasActiveDestination) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = "Setup Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Text(
                        text = "Configure at least one destination to start forwarding SMS messages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )

                    Button(
                        onClick = onNavigateToDestinations,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Setup Destinations")
                    }
                }
            }
        }
    }
}
