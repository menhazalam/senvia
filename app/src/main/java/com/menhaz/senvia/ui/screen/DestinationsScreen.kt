package com.menhaz.senvia.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.menhaz.senvia.ui.component.dialog.PhoneSetupDialog
import com.menhaz.senvia.ui.component.dialog.TelegramSetupDialog
import com.menhaz.senvia.viewmodel.SettingsViewModel
import com.menhaz.senvia.viewmodel.SettingsViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
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

    val botToken by (viewModel?.botToken?.collectAsState() ?: mutableStateOf(""))
    val chatId by (viewModel?.chatId?.collectAsState() ?: mutableStateOf(""))
    val savedPhoneNumber by (viewModel?.phoneNumber?.collectAsState() ?: mutableStateOf(""))
    val currentDestination by (viewModel?.currentDestination?.collectAsState() ?: mutableStateOf(""))

    val isTelegramConfigured = botToken.isNotBlank() && chatId.isNotBlank()
    val isPhoneConfigured = savedPhoneNumber.isNotBlank()

    // Dialog states
    var showPhoneDialog by rememberSaveable { mutableStateOf(false) }
    var showTelegramDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Forward Messages",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose where to send your SMS messages",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Integrated Destinations
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Telegram Card
                DestinationCard(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    title = "Telegram Bot",
                    subtitle = if (isTelegramConfigured) {
                        if (currentDestination == "telegram") "Active • Forwarding messages" else "Ready to use"
                    } else "Setup required",
                    isSelected = currentDestination == "telegram",
                    isConfigured = isTelegramConfigured,
                    onSelect = {
                        if (isTelegramConfigured) {
                            viewModel?.setCurrentDestination("telegram")
                        }
                    },
                    onSetup = { showTelegramDialog = true }
                )

                // Phone Card
                DestinationCard(
                    icon = Icons.Outlined.Phone,
                    title = "Phone Number",
                    subtitle = if (isPhoneConfigured) {
                        if (currentDestination == "phone") "Active • Forwarding to $savedPhoneNumber" else savedPhoneNumber
                    } else "Setup required",
                    isSelected = currentDestination == "phone",
                    isConfigured = isPhoneConfigured,
                    onSelect = {
                        if (isPhoneConfigured) {
                            viewModel?.setCurrentDestination("phone")
                        }
                    },
                    onSetup = { showPhoneDialog = true }
                )
            }

            // Info Section
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
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "How it works",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = "• Tap to select or setup destination\n• Use settings icon to edit configuration\n• Only one destination active at a time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Dialogs
        PhoneSetupDialog(
            isVisible = showPhoneDialog,
            onDismiss = { showPhoneDialog = false },
            onSave = { phoneNumber ->
                scope.launch {
                    viewModel?.savePhoneNumber(phoneNumber)
                    if (phoneNumber.isBlank()) {
                        if (currentDestination == "phone") {
                            viewModel?.setCurrentDestination("none")
                        }
                        snackbarHostState.showSnackbar("Phone number removed")
                    } else {
                        // Smart activation: auto-activate when saving valid phone number
                        viewModel?.setCurrentDestination("phone")
                        snackbarHostState.showSnackbar("Phone number saved and activated!")
                    }
                }
                showPhoneDialog = false
            },
            initialPhoneNumber = savedPhoneNumber
        )

        TelegramSetupDialog(
            isVisible = showTelegramDialog,
            onDismiss = { showTelegramDialog = false },
            onSave = { token, chatId ->
                scope.launch {
                    viewModel?.saveBotToken(token)
                    viewModel?.saveChatId(chatId)
                    if (token.isBlank() && chatId.isBlank()) {
                        if (currentDestination == "telegram") {
                            viewModel?.setCurrentDestination("none")
                        }
                        snackbarHostState.showSnackbar("Telegram bot removed")
                    } else {
                        // Smart activation: auto-activate when saving valid bot config
                        viewModel?.setCurrentDestination("telegram")
                        snackbarHostState.showSnackbar("Telegram bot saved and activated!")
                    }
                }
                showTelegramDialog = false
            },
            initialBotToken = botToken,
            initialChatId = chatId
        )
    }
}

@Composable
private fun DestinationCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isConfigured: Boolean,
    onSelect: () -> Unit,
    onSetup: () -> Unit
) {
    Card(
        onClick = if (isConfigured) onSelect else onSetup,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action indicator
            if (!isConfigured) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Setup required",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(
                    onClick = onSetup,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Edit",
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

        }
    }
}
