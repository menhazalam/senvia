package com.menhaz.senvia.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.menhaz.senvia.R
import androidx.core.net.toUri

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // App Info Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo - Currently using app icon
            // To use your custom PNG logo:
            // 1. Place your PNG file in senvia/app/src/main/res/drawable/app_logo.png
            // 2. Change R.mipmap.ic_launcher to R.drawable.app_logo below
            Image(
                painter = painterResource(id = R.drawable.senvia_logo),
                contentDescription = "Senvia App Logo",
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = "Senvia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Forward SMS seamlessly",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* No action needed */ }
                ) {
                    Text("v1.0.0")
                }

                OutlinedButton(
                    onClick = { /* No action needed */ }
                ) {
                    Text("arm64-v8a")
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        "".toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("GitHub")
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        "https://buymeacoffee.com/menhazalam".toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Donate")
            }
        }
    }
}