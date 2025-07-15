package com.menhaz.senvia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.menhaz.senvia.ui.screen.*
import com.menhaz.senvia.ui.theme.SenviaTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private var permissionUpdateTrigger by mutableStateOf(0)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Trigger recomposition by updating the state
        permissionUpdateTrigger++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SenviaTheme {
                MainScreen(
                    permissionUpdateTrigger = permissionUpdateTrigger,
                    onRequestPermissions = { requestPermissions() }
                )
            }
        }
        
        // Defer permission check to avoid blocking UI
        checkPermissionsDeferred()
    }

    private fun checkAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        )

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkPermissionsDeferred() {
        // Use a coroutine to defer permission checking without blocking
        lifecycleScope.launch {
            checkAllPermissions()
        }
    }

    fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        )

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionUpdateTrigger: Int = 0,
    onRequestPermissions: () -> Unit = {}
) {
    val activity = LocalActivity.current as MainActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var navigationStack by remember { mutableStateOf(listOf(Screen.Home)) }

    fun navigateTo(screen: Screen) {
        navigationStack = navigationStack + screen
        currentScreen = screen
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
            currentScreen = navigationStack.last()
        }
    }

    // Handle back button
    BackHandler(enabled = navigationStack.size > 1) {
        navigateBack()
    }

    fun handleTestMessage() {
        scope.launch {
            snackbarHostState.showSnackbar("Ping sent!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (navigationStack.size > 1) {
                        IconButton(onClick = { navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.Home) {
                        IconButton(
                            onClick = { navigateTo(Screen.Settings) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onTestMessage = { handleTestMessage() },
                    onNavigateToDestinations = { navigateTo(Screen.Destinations) },
                    onRequestPermissions = { activity.requestPermissions() },
                    onNavigateToLogs = { navigateTo(Screen.Logs) },
                    permissionUpdateTrigger = permissionUpdateTrigger
                )
                Screen.Settings -> SettingsScreen(
                    onNavigateToDestinations = { navigateTo(Screen.Destinations) },
                    onNavigateToFilter = { navigateTo(Screen.Filter) },
                    onNavigateToAbout = { navigateTo(Screen.About) },
                    onNavigateToGeneral = { navigateTo(Screen.General) }
                )
                Screen.Destinations -> DestinationsScreen()
                Screen.Filter -> FilterScreen()
                Screen.About -> AboutScreen()
                Screen.Logs -> LogsScreenComposable()
                Screen.General -> GeneralSettingsScreen()
            }
        }
    }
}

enum class Screen {
    Home, Settings, Destinations, Filter, About, Logs, General
}

