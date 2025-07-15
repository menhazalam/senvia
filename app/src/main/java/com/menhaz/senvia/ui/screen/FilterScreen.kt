package com.menhaz.senvia.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.menhaz.senvia.viewmodel.SettingsViewModel
import com.menhaz.senvia.viewmodel.SettingsViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    var newKeyword by rememberSaveable { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filterKeywords by viewModel.filterKeywords.collectAsState()

    fun addKeywords() {
        val input = newKeyword.trim()
        if (input.isNotBlank()) {
            val keywords = input.split(",").map { it.trim() }.filter { 
                it.isNotBlank() && !filterKeywords.contains(it) 
            }
            if (keywords.isNotEmpty()) {
                // Add all keywords at once to avoid race condition
                viewModel.addKeywords(keywords)
                newKeyword = ""
                keyboardController?.hide()
            }
        }
    }

    Column(
        modifier = modifier
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
                text = "Message Filters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Only forward messages that contain these keywords",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with count and clear action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (filterKeywords.isEmpty()) Icons.Outlined.FilterListOff else Icons.Filled.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (filterKeywords.isEmpty()) "No filters" else "${filterKeywords.size} active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (filterKeywords.isNotEmpty()) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("Clear all")
                        }
                    }
                }

                // Input field
                OutlinedTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    label = { Text("Add keywords") },
                    placeholder = { Text("bank, OTP, verification") },
                    supportingText = { Text("Separate multiple keywords with commas") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addKeywords() }),
                    trailingIcon = {
                        IconButton(
                            onClick = { addKeywords() },
                            enabled = newKeyword.trim().isNotBlank()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Keywords display or empty state
                if (filterKeywords.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterKeywords.toList()) { keyword ->
                            FilterChip(
                                onClick = { viewModel.removeKeyword(keyword) },
                                label = { Text(keyword) },
                                selected = true,
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "All messages will be forwarded without filtering",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                    text = "• Keywords match anywhere in the message (case-insensitive)\n" +
                           "• Messages with ANY keyword will be forwarded\n" +
                           "• No keywords = forward all messages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Clear All Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Clear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear all keywords?") },
            text = {
                Text("This will remove all ${filterKeywords.size} keywords. All messages will be forwarded.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearKeywords()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
