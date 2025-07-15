package com.menhaz.senvia.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceEntry(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val clickableModifier = if (onClick != null && enabled) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Leading icon
        if (icon != null) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }

        // Title and description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.bodyLarge.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                title()
            }

            if (description != null) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    description()
                }
            }
        }

        // Trailing content
        if (trailingContent != null) {
            Box {
                trailingContent()
            }
        }
    }
}