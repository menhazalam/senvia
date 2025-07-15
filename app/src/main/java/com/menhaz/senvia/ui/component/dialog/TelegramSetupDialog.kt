package com.menhaz.senvia.ui.component.dialog

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramSetupDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    initialBotToken: String = "",
    initialChatId: String = "",
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var botToken by rememberSaveable { mutableStateOf("") }
    var chatId by rememberSaveable { mutableStateOf("") }
    var botTokenError by rememberSaveable { mutableStateOf("") }
    var chatIdError by rememberSaveable { mutableStateOf("") }
    
    // Initialize values when dialog becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            botToken = initialBotToken
            chatId = initialChatId
        }
    }
    
    val isConfigured = initialBotToken.isNotBlank() && initialChatId.isNotBlank()
    val hasChanges = botToken != initialBotToken || chatId != initialChatId

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = tween(200),
            initialScale = 0.8f
        ) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(
            animationSpec = tween(150),
            targetScale = 0.8f
        ) + fadeOut(animationSpec = tween(150))
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isConfigured) "Edit Telegram Bot" else "Telegram Bot Setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = if (isConfigured)
                            "Update or clear the bot configuration. Leave empty to remove Telegram forwarding."
                        else
                            "Configure your Telegram bot to receive SMS messages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { 
                            botToken = it
                            botTokenError = if (it.isNotBlank() && !it.matches(Regex("\\d+:[A-Za-z0-9_-]+"))) "Invalid bot token format"
                                           else ""
                        },
                        label = { Text("Bot Token") },
                        placeholder = { Text("123456789:ABCdefGHIjklMNOpqrSTUvwxyz") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Key, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = botTokenError.isNotEmpty(),
                        supportingText = if (botTokenError.isNotEmpty()) {
                            { Text(botTokenError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { 
                            chatId = it
                            chatIdError = if (it.isNotBlank() && !it.matches(Regex("-?\\d+"))) "Chat ID must be numeric"
                                         else ""
                        },
                        label = { Text("Chat ID") },
                        placeholder = { Text("123456789") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true,
                        isError = chatIdError.isNotEmpty(),
                        supportingText = if (chatIdError.isNotEmpty()) {
                            { Text(chatIdError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Setup Instructions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Text(
                                text = if (isConfigured)
                                    "Leave both fields empty to remove Telegram forwarding"
                                else
                                    "1. Create a bot via @BotFather on Telegram\n" +
                                    "2. Get your bot token from BotFather\n" +
                                    "3. Find your Chat ID by messaging @userinfobot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Standard M3 two-button layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                botToken = if (isConfigured) initialBotToken else ""
                                chatId = if (isConfigured) initialChatId else ""
                                onDismiss() 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                onSave(botToken, chatId)
                                onDismiss()
                            },
                            enabled = botTokenError.isEmpty() && chatIdError.isEmpty() &&
                                     (!isConfigured || hasChanges) &&
                                     (botToken.isBlank() == chatId.isBlank()), // Both empty or both filled
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (botToken.isBlank() && chatId.isBlank() && isConfigured) "Remove" else "Save")
                        }
                    }
                }
            }
        }
    }
}