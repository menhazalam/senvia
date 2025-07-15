package com.menhaz.senvia.ui.component.dialog

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSetupDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    initialPhoneNumber: String = "",
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    
    // Initialize phone number when dialog becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            phoneNumber = initialPhoneNumber
        }
    }
    
    val isValidPhone = phoneNumber.isBlank() || phoneNumber.matches(Regex("^\\+?[1-9]\\d{1,14}$"))
    val isConfigured = initialPhoneNumber.isNotBlank()
    val hasChanges = phoneNumber != initialPhoneNumber

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
                        text = if (isConfigured) "Edit Phone Number" else "Phone Number Setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = if (isConfigured) 
                            "Update or clear the phone number. Leave empty to remove forwarding."
                        else
                            "Forward SMS messages to another phone number via SMS. Standard SMS rates apply.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1234567890") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Phone, contentDescription = null)
                        },
                        isError = phoneNumber.isNotBlank() && !isValidPhone,
                        supportingText = {
                            Text(
                                text = when {
                                    phoneNumber.isBlank() -> if (isConfigured) "Leave empty to remove phone forwarding" else "Enter phone number with country code"
                                    !isValidPhone -> "Invalid format. Use: +1234567890"
                                    else -> "Valid phone number"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true,
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
                                    text = "Important",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Text(
                                text = "• SMS forwarding uses your device's messaging service\n" +
                                      "• Standard SMS rates from your carrier apply\n" +
                                      "• International forwarding may have higher costs",
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
                                phoneNumber = if (isConfigured) initialPhoneNumber else ""
                                onDismiss() 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                onSave(phoneNumber)
                                onDismiss()
                            },
                            enabled = isValidPhone && (!isConfigured || hasChanges),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (phoneNumber.isBlank() && isConfigured) "Remove" else "Save")
                        }
                    }
                }
            }
        }
    }
}