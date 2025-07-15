package com.menhaz.senvia.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Battery optimization status information
 */
data class BatteryOptimizationStatus(
    val isIgnored: Boolean,
    val canRequestExemption: Boolean,
    val requiresAction: Boolean,
    val sdkVersion: Int
)

/**
 * Manufacturer-specific information
 */
data class ManufacturerInfo(
    val hasIssues: Boolean,
    val name: String,
    val settingsHint: String,
    val additionalSteps: List<String>
)

/**
 * Critical level for battery optimization issues
 */
enum class CriticalLevel {
    NONE,    // No issues
    LOW,     // Minor issues, app should work
    MEDIUM,  // Moderate issues, may affect reliability
    HIGH     // Severe issues, app may not work properly
}

/**
 * Comprehensive battery management guidance
 */
data class BatteryManagementGuidance(
    val status: BatteryOptimizationStatus,
    val manufacturerInfo: ManufacturerInfo,
    val recommendations: List<String>,
    val criticalLevel: CriticalLevel
)

class BatteryOptimizationManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "BatteryOptimizationManager"
        
        @Volatile
        private var INSTANCE: BatteryOptimizationManager? = null
        
        fun getInstance(context: Context): BatteryOptimizationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BatteryOptimizationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Check if battery optimization is ignored for this app
     */
    fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                Log.d(TAG, "Battery optimization ignored: $isIgnored")
                isIgnored
            } catch (e: Exception) {
                Log.e(TAG, "Error checking battery optimization status", e)
                false
            }
        } else {
            // Battery optimization doesn't exist before Android M
            true
        }
    }
    
    /**
     * Get intent to request battery optimization exemption
     */
    fun getRequestBatteryOptimizationIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating battery optimization intent", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Get intent to open battery optimization settings
     */
    fun getBatteryOptimizationSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Try specific app settings first
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating battery settings intent", e)
                try {
                    // Fallback to general battery settings
                    Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error creating fallback battery settings intent", e2)
                    null
                }
            }
        } else {
            null
        }
    }
    
    /**
     * Get battery optimization status with detailed information
     */
    fun getBatteryOptimizationStatus(): BatteryOptimizationStatus {
        val isIgnored = isBatteryOptimizationIgnored()
        val canRequestExemption = true
        
        return BatteryOptimizationStatus(
            isIgnored = isIgnored,
            canRequestExemption = canRequestExemption,
            requiresAction = canRequestExemption && !isIgnored,
            sdkVersion = Build.VERSION.SDK_INT
        )
    }
    
    /**
     * Check if the device manufacturer has known battery optimization issues
     */
    fun hasKnownBatteryOptimizationIssues(): ManufacturerInfo {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") -> ManufacturerInfo(
                hasIssues = true,
                name = "Xiaomi",
                settingsHint = "Go to Settings > Apps > Manage apps > senvia > Battery saver > No restrictions",
                additionalSteps = listOf(
                    "Enable 'Autostart' in Security app",
                    "Add to 'Memory cleanup whitelist'",
                    "Disable 'Battery optimization' in Settings"
                )
            )
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> ManufacturerInfo(
                hasIssues = true,
                name = "Huawei/Honor",
                settingsHint = "Go to Settings > Apps > senvia > Battery > App launch > Manage manually",
                additionalSteps = listOf(
                    "Enable 'Auto-launch'",
                    "Enable 'Secondary launch'",
                    "Enable 'Run in background'"
                )
            )
            manufacturer.contains("oppo") -> ManufacturerInfo(
                hasIssues = true,
                name = "Oppo",
                settingsHint = "Go to Settings > Battery > Power Saving Mode > High Performance",
                additionalSteps = listOf(
                    "Add senvia to 'Startup Manager'",
                    "Enable 'Background App Management'"
                )
            )
            manufacturer.contains("vivo") -> ManufacturerInfo(
                hasIssues = true,
                name = "Vivo",
                settingsHint = "Go to Settings > Battery > Background App Management",
                additionalSteps = listOf(
                    "Enable 'High Background App Limit'",
                    "Add senvia to whitelist"
                )
            )
            manufacturer.contains("oneplus") -> ManufacturerInfo(
                hasIssues = true,
                name = "OnePlus",
                settingsHint = "Go to Settings > Battery > Battery Optimization",
                additionalSteps = listOf(
                    "Set senvia to 'Don't optimize'",
                    "Enable 'Advanced Optimization' > 'Deep Optimization' for senvia"
                )
            )
            manufacturer.contains("samsung") -> ManufacturerInfo(
                hasIssues = true,
                name = "Samsung",
                settingsHint = "Go to Settings > Apps > senvia > Battery > Optimize battery usage",
                additionalSteps = listOf(
                    "Turn OFF 'Optimize battery usage'",
                    "Add to 'Never sleeping apps' in Device Care"
                )
            )
            manufacturer.contains("meizu") -> ManufacturerInfo(
                hasIssues = true,
                name = "Meizu",
                settingsHint = "Go to Settings > Apps > senvia > Battery",
                additionalSteps = listOf(
                    "Enable 'Background freeze' exemption",
                    "Enable 'Standby intelligent clearup' exemption"
                )
            )
            else -> ManufacturerInfo(
                hasIssues = false,
                name = Build.MANUFACTURER,
                settingsHint = "Use standard Android battery optimization settings",
                additionalSteps = emptyList()
            )
        }
    }
    
    /**
     * Get comprehensive battery management guidance
     */
    fun getBatteryManagementGuidance(): BatteryManagementGuidance {
        val status = getBatteryOptimizationStatus()
        val manufacturerInfo = hasKnownBatteryOptimizationIssues()
        
        val recommendations = mutableListOf<String>()
        
        if (status.requiresAction) {
            recommendations.add("Disable battery optimization for senvia")
        }
        
        if (manufacturerInfo.hasIssues) {
            recommendations.add("Configure ${manufacturerInfo.name}-specific battery settings")
            recommendations.addAll(manufacturerInfo.additionalSteps)
        }
        
        recommendations.addAll(listOf(
            "Keep the app running in background",
            "Avoid force-closing the app",
            "Ensure sufficient storage space"
        ))
        
        return BatteryManagementGuidance(
            status = status,
            manufacturerInfo = manufacturerInfo,
            recommendations = recommendations,
            criticalLevel = when {
                !status.isIgnored && manufacturerInfo.hasIssues -> CriticalLevel.HIGH
                !status.isIgnored -> CriticalLevel.MEDIUM
                manufacturerInfo.hasIssues -> CriticalLevel.LOW
                else -> CriticalLevel.NONE
            }
        )
    }
}