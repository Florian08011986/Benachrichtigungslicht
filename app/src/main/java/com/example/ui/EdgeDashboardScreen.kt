package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AppEdgeRule
import com.example.model.LineStyle
import com.example.model.PulseCurve
import com.example.model.ContactEdgeRule
import com.example.model.EdgeColorPalette
import com.example.model.NotchType
import com.example.ui.components.AppRuleCard
import com.example.ui.components.ColorPresetItem
import com.example.ui.components.ContactRuleCard
import com.example.ui.components.CustomColorEditor
import com.example.ui.components.LivePreviewCard
import com.example.ui.components.ColorSwatches
import com.example.ui.components.AppColorRow
import com.example.ui.components.AppColorDialog
import com.example.model.LightWhen
import com.example.model.singleColor
import com.example.util.InstalledApp
import com.example.model.AnimationMode
import com.example.model.LightProfiles
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkOledBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EdgeDashboardScreen(
    viewModel: EdgeDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val notifSettings by viewModel.notificationSettings.collectAsState()
    val appRules by viewModel.appRules.collectAsState()
    val contactRules by viewModel.contactRules.collectAsState()
    val scheduleConfig by viewModel.scheduleConfig.collectAsState()
    val batterySaverConfig by viewModel.batterySaverConfig.collectAsState()
    val batteryStatus by viewModel.batteryStatus.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val installedApps by viewModel.installedApps.collectAsState()
    var appSearch by remember { mutableStateOf("") }
    var colorDialogApp by remember { mutableStateOf<InstalledApp?>(null) }
    var colorDialogForCalls by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) viewModel.loadInstalledApps()
    }

    // Farbauswahl-Dialoge
    colorDialogApp?.let { app ->
        val current = appRules.firstOrNull { it.packageName == app.packageName }?.singleColor()
        AppColorDialog(
            title = app.label,
            current = current,
            allowReset = true,
            onSelect = { viewModel.setAppColor(app, it) },
            onReset = { viewModel.clearAppColor(app.packageName) },
            onTest = { viewModel.testAppColor(it) },
            onDismiss = { colorDialogApp = null }
        )
    }
    if (colorDialogForCalls) {
        AppColorDialog(
            title = "Anrufe",
            current = config.callColor,
            allowReset = false,
            onSelect = { viewModel.setCallColor(it) },
            onReset = {},
            onTest = { viewModel.testAppColor(it) },
            onDismiss = { colorDialogForCalls = false }
        )
    }
    var selectedCategory by remember { mutableStateOf("Alle") }
    var testingPulse by remember { mutableStateOf(false) }

    // Dialog: Neue App-Regel
    var showAddAppDialog by remember { mutableStateOf(false) }
    var newAppName by remember { mutableStateOf("") }
    var newAppPackage by remember { mutableStateOf("") }

    // Dialog: Neue VIP-Kontakt-Regel
    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPaletteId by remember { mutableIntStateOf(41) }

    // Overlay-Berechtigungs-Launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val hasOverlay = Settings.canDrawOverlays(context)
        if (hasOverlay) {
            Toast.makeText(context, "Berechtigung für Kantenlicht erteilt!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Überlagerungs-Berechtigung abgelehnt.", Toast.LENGTH_SHORT).show()
        }
    }

    // Benachrichtigungszugriff-Launcher
    val notifAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.isNotificationListenerEnabled()) {
            Toast.makeText(context, "Benachrichtigungszugriff erfolgreich aktiviert!", Toast.LENGTH_SHORT).show()
        }
    }

    // Telefonstatus- und Benachrichtigungsberechtigungs-Launcher für Anruferkennung
    val callerPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val phoneGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: false
        if (phoneGranted) {
            viewModel.setCallerIdAlertEnabled(true)
            Toast.makeText(context, "Anruferkennung aktiviert!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Telefonberechtigung für Anruferkennung erforderlich.", Toast.LENGTH_SHORT).show()
        }
    }

    val hasNotifAccess = viewModel.isNotificationListenerEnabled()
    val hasOverlayAccess = Settings.canDrawOverlays(context)

    // Live-Vorschau nur solange die App im Vordergrund ist
    LifecycleResumeEffect(config.isLivePreviewEnabled) {
        if (config.isLivePreviewEnabled) viewModel.setPreviewActive(true)
        onPauseOrDispose { viewModel.setPreviewActive(false) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (hasNotifAccess && hasOverlayAccess) NeonCyan else Color(0xFFFF9800))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EdgeLighting Pro",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, (if (hasNotifAccess && hasOverlayAccess) NeonCyan else Color(0xFFFF9800)).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasNotifAccess && hasOverlayAccess) "LICHT BEREIT" else "EINRICHTUNG",
                            color = if (hasNotifAccess && hasOverlayAccess) NeonCyan else Color(0xFFFF9800),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkOledBg,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkOledBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live-Vorschau am echten Display (ersetzt Mini-Handy)
            item {
                LivePreviewCard(
                    livePreviewEnabled = config.isLivePreviewEnabled,
                    hasOverlayAccess = hasOverlayAccess,
                    onToggle = { viewModel.setLivePreviewEnabled(it) },
                    onRequestOverlayAccess = {
                        overlayPermissionLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    }
                )
            }

            // Status- & Berechtigungs-Banner
            item {
                StatusAndPermissionsCard(
                    config = config,
                    hasNotificationAccess = hasNotifAccess,
                    hasOverlayAccess = hasOverlayAccess,
                    onOpenNotificationAccessSettings = {
                        try {
                            notifAccessLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Einstellungen konnten nicht geöffnet werden", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenOverlaySettings = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        overlayPermissionLauncher.launch(intent)
                    },
                    onTriggerTestAlert = {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        } else {
                            viewModel.triggerCurrentTestAlert(5)
                            Toast.makeText(context, "Kantenlicht-Test aktiv (5 Sekunden)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleCallerId = { enable ->
                        if (enable) {
                            val hasPhone = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED

                            val permissions = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }

                            if (!hasPhone) {
                                callerPermissionsLauncher.launch(permissions.toTypedArray())
                            } else {
                                viewModel.setCallerIdAlertEnabled(true)
                            }
                        } else {
                            viewModel.setCallerIdAlertEnabled(false)
                        }
                    },
                    onTestCallerPulse = {
                        testingPulse = !testingPulse
                        viewModel.triggerTestCallerPulse(testingPulse)
                    },
                    isTestingPulse = testingPulse
                )
            }

            // 5 Haupt-Tabs in deutscher Sprache
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = NeonCyan,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("1. Farben") },
                        icon = { Icon(Icons.Default.ColorLens, contentDescription = "Farben") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("2. Stil & Bewegung") },
                        icon = { Icon(Icons.Default.ShapeLine, contentDescription = "Formen") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("3. Geometrie & Helligkeit") },
                        icon = { Icon(Icons.Default.RoundedCorner, contentDescription = "Geometrie & Helligkeit") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("4. Apps & VIP") },
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Apps & VIP") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("5. Ruhe & Akku") },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = "Ruhe & Akku") }
                    )
                }
            }

            // TAB 0: FARBEN (48 PRESETS + EIGENER FARBDESIGNER)
            if (selectedTab == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eigener Farbdesigner",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = config.useCustomColors,
                            onCheckedChange = { use ->
                                if (use) {
                                    viewModel.setCustomColors(config.customColors)
                                } else {
                                    viewModel.selectPreset(config.selectedPaletteId)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }

                if (config.useCustomColors) {
                    item {
                        CustomColorEditor(
                            colors = config.customColors,
                            onColorsChanged = { viewModel.setCustomColors(it) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "48 kuratierte Neon- & RGB-Farbverläufe",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Kategorie-Filter-Chips auf Deutsch
                item {
                    val categories = listOf("Alle", "KI", "Neon", "Cosmic", "Elements", "Pastel", "Vibrant")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (selectedCategory == cat) NeonCyan else DarkBorder,
                                    enabled = true,
                                    selected = selectedCategory == cat
                                )
                            )
                        }
                    }
                }

                val filteredPresets = if (selectedCategory == "Alle") {
                    EdgeColorPalette.PRESETS
                } else {
                    EdgeColorPalette.PRESETS.filter { it.category == selectedCategory }
                }

                items(filteredPresets.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (preset in pair) {
                            ColorPresetItem(
                                preset = preset,
                                isSelected = !config.useCustomColors && config.selectedPaletteId == preset.id,
                                onSelect = { viewModel.selectPreset(preset.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // TAB 1: LINIENSTIL & BEWEGUNG
            if (selectedTab == 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Linienstil",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Alle Stile laufen am Bildschirmrand entlang.",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            LineStyle.entries.forEach { style ->
                                val isSelected = config.lineStyle == style
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) NeonCyan else DarkBorder, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.updateLineStyle(lineStyle = style) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("line_style_${style.name}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.displayName,
                                            color = if (isSelected) NeonCyan else TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = style.description,
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Aktiv", tint = NeonCyan)
                                    }
                                }
                            }

                            if (config.lineStyle == LineStyle.AURA) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Leucht-Reichweite ins Display: ${config.glowReach.toInt()} px",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Wie weit Aura und Leuchtschein vom Rand nach innen ziehen",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = config.glowReach.coerceIn(20f, 400f),
                                onValueChange = { viewModel.updateLineStyle(glowReach = it) },
                                valueRange = 20f..400f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                                modifier = Modifier.testTag("glow_reach_slider")
                            )
                            }
                        }
                    }
                }

                // Ring: nur Farbe und Stärke
                if (config.lineStyle == LineStyle.RING) item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Ring",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Farbe",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Gilt für alle Apps ohne eigene Farbe (App-Farben im Tab „4. Apps & VIP“)",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ColorSwatches(
                                selected = config.ringColor,
                                onSelect = { viewModel.setRingColor(it) },
                                modifier = Modifier.testTag("ring_color_swatches")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Stärke: ${config.borderThickness.toInt()} px",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = config.borderThickness.coerceIn(2f, 60f),
                                onValueChange = { viewModel.updateGeometry(borderThickness = it) },
                                valueRange = 2f..60f,
                                colors = SliderDefaults.colors(thumbColor = Color(config.ringColor), activeTrackColor = Color(config.ringColor)),
                                modifier = Modifier.testTag("ring_thickness_slider")
                            )
                        }
                    }
                }

                // Wann leuchten (gilt für Ring und KI-Aura)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Wann leuchten",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LightWhen.entries.forEach { option ->
                                    FilterChip(
                                        selected = notifSettings.lightWhen == option,
                                        onClick = { viewModel.updateNotificationSettings(notifSettings.copy(lightWhen = option)) },
                                        label = { Text(option.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = NeonCyan,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("light_when_${option.name}")
                                    )
                                }
                            }
                            Text(
                                text = when (notifSettings.lightWhen) {
                                    LightWhen.SCREEN_ACTIVE -> "Nur wenn das Handy entsperrt ist und du es benutzt"
                                    LightWhen.LOCKED -> "Nur wenn das Handy gesperrt oder der Bildschirm aus ist"
                                    LightWhen.BOTH -> "Immer – entsperrt und gesperrt"
                                },
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                // Deckkraft (nur KI-Aura)
                if (config.lineStyle == LineStyle.AURA) item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Deckkraft",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${config.opacityPercent} %",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "100 % = Rand verdeckt den Inhalt darunter vollständig. Weniger = durchscheinend.",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = config.opacityPercent.toFloat(),
                                onValueChange = { viewModel.updateOpacity(it.toInt()) },
                                valueRange = 10f..100f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                                modifier = Modifier.testTag("opacity_slider")
                            )
                        }
                    }
                }

                // Bewegung & Puls (nur KI-Aura)
                if (config.lineStyle == LineStyle.AURA) item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Bewegung & Puls",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimationMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = config.animationMode == mode,
                                        onClick = { viewModel.updateMotion(animationMode = mode) },
                                        label = { Text(mode.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricPurple,
                                            selectedLabelColor = TextPrimary,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("motion_mode_${mode.name}")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            val moveEnabled = config.animationMode != AnimationMode.PULSE
                            Text(
                                text = "Bewegungstempo: ${String.format("%.1f", config.animationSpeed)}x" +
                                        "  (eine Runde in ${String.format("%.1f", 10f / config.animationSpeed)} s)",
                                color = if (moveEnabled) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = config.animationSpeed.coerceIn(0.2f, 20f),
                                onValueChange = { viewModel.updateGeometry(animationSpeed = it) },
                                valueRange = 0.2f..20f,
                                enabled = moveEnabled,
                                colors = SliderDefaults.colors(thumbColor = ElectricPurple, activeTrackColor = ElectricPurple),
                                modifier = Modifier.testTag("rotation_speed_slider")
                            )

                            val pulseEnabled = config.animationMode != AnimationMode.ROTATE
                            Text(
                                text = "Pulskurve",
                                color = if (pulseEnabled) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PulseCurve.entries.forEach { curve ->
                                    FilterChip(
                                        selected = config.pulseCurve == curve,
                                        enabled = pulseEnabled,
                                        onClick = { viewModel.updateMotion(pulseCurve = curve) },
                                        label = { Text(curve.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = NeonCyan,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("pulse_curve_${curve.name}")
                                    )
                                }
                            }
                            Text(
                                text = config.pulseCurve.description,
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Pulstempo: ein Atemzug alle ${String.format("%.1f", 1f / config.pulseSpeed)} s",
                                color = if (pulseEnabled) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = config.pulseSpeed.coerceIn(0.125f, 2f),
                                onValueChange = { viewModel.updateMotion(pulseSpeed = it) },
                                valueRange = 0.125f..2f,
                                enabled = pulseEnabled,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                                modifier = Modifier.testTag("pulse_speed_slider")
                            )
                            Text(
                                text = "Pulsstärke: ${(config.pulseStrength * 100).toInt()} %",
                                color = if (pulseEnabled) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Wie stark das Licht beim Ausatmen abtaucht – niedrig = ruhiger",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = config.pulseStrength.coerceIn(0.1f, 1f),
                                onValueChange = { viewModel.updateMotion(pulseStrength = it) },
                                valueRange = 0.1f..1f,
                                enabled = pulseEnabled,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                                modifier = Modifier.testTag("pulse_strength_slider")
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Zwei-Stufen-Start",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Beim Auslösen kurz ein buntes Spektrum, dann weicher Übergang in deine Farben",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Switch(
                                    checked = config.isTwoStageStartEnabled,
                                    onCheckedChange = { viewModel.setTwoStageStartEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = DarkSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("two_stage_switch")
                                )
                            }
                            TextButton(
                                onClick = { viewModel.replayIntro() },
                                modifier = Modifier.testTag("two_stage_replay")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start-Effekt am Display ansehen", color = NeonCyan)
                            }
                        }
                    }
                }
            }

            // TAB 2: GEOMETRIE & NOTCH
            if (selectedTab == 2) {
                // Komplettprofile: Farbe + Bewegung + Rand mit einem Tipp
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Licht- & Pulsprofile",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Setzt Stil, Farben, Bewegung, Puls und Randstärke auf einmal – danach frei anpassbar.",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LightProfiles.ALL.forEach { profile ->
                                val preset = EdgeColorPalette.getPreset(profile.paletteId)
                                val isActive = !config.useCustomColors &&
                                        config.selectedPaletteId == profile.paletteId &&
                                        config.animationMode == profile.animationMode &&
                                        config.lineStyle == profile.lineStyle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isActive) NeonCyan else DarkBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { viewModel.applyLightProfile(profile) }
                                        .padding(12.dp)
                                        .testTag("light_profile_${profile.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 10.dp, height = 40.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    preset.colors.map { Color(it) }
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.name,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = profile.description,
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (isActive) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Aktiv",
                                            tint = NeonCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Rand-Geometrie",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Randstärke: ${config.borderThickness.toInt()} px",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = config.borderThickness,
                                onValueChange = { viewModel.updateGeometry(borderThickness = it) },
                                valueRange = 4f..60f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                            )

                            Text(
                                text = "Eckenradius Oben: ${config.cornerRadiusTop.toInt()} px",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = config.cornerRadiusTop,
                                onValueChange = { viewModel.updateGeometry(cornerRadiusTop = it) },
                                valueRange = 0f..150f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                            )

                            Text(
                                text = "Eckenradius Unten: ${config.cornerRadiusBottom.toInt()} px",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = config.cornerRadiusBottom,
                                onValueChange = { viewModel.updateGeometry(cornerRadiusBottom = it) },
                                valueRange = 0f..150f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Aura-Glow & Unschärfe-Filter",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Sanfter strahlender Neonschein um den Rand",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Switch(
                                    checked = config.isGlowEnabled,
                                    onCheckedChange = { viewModel.updateGeometry(glowEnabled = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = DarkSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Notch- & Kamera-Aussparungen",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                NotchType.values().forEach { notch ->
                                    val isSelected = config.notchConfig.notchType == notch
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateNotchConfig(config.notchConfig.copy(notchType = notch))
                                        },
                                        label = { Text(notch.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonMagenta.copy(alpha = 0.2f),
                                            selectedLabelColor = NeonMagenta,
                                            containerColor = DarkSurfaceVariant,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = if (isSelected) NeonMagenta else DarkBorder,
                                            enabled = true,
                                            selected = isSelected
                                        )
                                    )
                                }
                            }

                            if (config.notchConfig.notchType != NotchType.NONE) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Notch-Breite: ${config.notchConfig.notchWidth.toInt()} px",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = config.notchConfig.notchWidth,
                                    onValueChange = {
                                        viewModel.updateNotchConfig(config.notchConfig.copy(notchWidth = it))
                                    },
                                    valueRange = 40f..400f,
                                    colors = SliderDefaults.colors(thumbColor = NeonMagenta, activeTrackColor = NeonMagenta)
                                )

                                Text(
                                    text = "Notch-Höhe: ${config.notchConfig.notchHeight.toInt()} px",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = config.notchConfig.notchHeight,
                                    onValueChange = {
                                        viewModel.updateNotchConfig(config.notchConfig.copy(notchHeight = it))
                                    },
                                    valueRange = 20f..250f,
                                    colors = SliderDefaults.colors(thumbColor = NeonMagenta, activeTrackColor = NeonMagenta)
                                )

                                Text(
                                    text = "Horizontaler Versatz X: ${config.notchConfig.notchOffsetX.toInt()} px",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = config.notchConfig.notchOffsetX,
                                    onValueChange = {
                                        viewModel.updateNotchConfig(config.notchConfig.copy(notchOffsetX = it))
                                    },
                                    valueRange = -150f..150f,
                                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                )

                                Text(
                                    text = "Vertikaler Versatz Y: ${config.notchConfig.notchOffsetY.toInt()} px",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = config.notchConfig.notchOffsetY,
                                    onValueChange = {
                                        viewModel.updateNotchConfig(config.notchConfig.copy(notchOffsetY = it))
                                    },
                                    valueRange = 0f..80f,
                                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                )

                                Text(
                                    text = "Kurven- / Lochradius: ${config.notchConfig.notchRadius.toInt()} px",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = config.notchConfig.notchRadius,
                                    onValueChange = {
                                        viewModel.updateNotchConfig(config.notchConfig.copy(notchRadius = it))
                                    },
                                    valueRange = 6f..60f,
                                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                )
                            }
                        }
                    }
                }
            }

            // TAB 3: APPS & VIP-BENACHRICHTIGUNGEN
            if (selectedTab == 3) {
                if (!viewModel.isNotificationListenerEnabled()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF331600)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warnung",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Benachrichtigungszugriff erforderlich",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Damit das Kantenlicht bei eingehenden Nachrichten auf dem Sperrbildschirm und in Apps leuchtet, wird der Android-Benachrichtigungszugriff benötigt.",
                                    color = Color(0xFFFFD599),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("In Android-Einstellungen freigeben", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Benachrichtigungen & Sperrbildschirm",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Kantenlicht bei Benachrichtigung",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Leuchtet auf bei neuen Mitteilungen",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = notifSettings.isNotificationAlertEnabled,
                                    onCheckedChange = {
                                        viewModel.updateNotificationSettings(notifSettings.copy(isNotificationAlertEnabled = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = DarkSurfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bildschirm bei Sperre aufwecken",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Aktiviert den Rand kurz im gesperrten Zustand",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = notifSettings.wakeScreenOnNotification,
                                    onCheckedChange = {
                                        viewModel.updateNotificationSettings(notifSettings.copy(wakeScreenOnNotification = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonMagenta,
                                        checkedTrackColor = DarkSurfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bis gelesen weiterpulsieren",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Rand atmet, bis die Mitteilung weggewischt oder geöffnet wurde (max. 60 min)",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = notifSettings.pulseUntilDismissed,
                                    onCheckedChange = {
                                        viewModel.updateNotificationSettings(notifSettings.copy(pulseUntilDismissed = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = DarkSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("until_dismissed_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (notifSettings.pulseUntilDismissed)
                                    "Leuchtdauer: bis gelesen (Regler gilt, wenn aus)"
                                else
                                    "Standard-Leuchtdauer: ${notifSettings.defaultAlertDurationSeconds} Sekunden",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = notifSettings.defaultAlertDurationSeconds.toFloat(),
                                onValueChange = {
                                    viewModel.updateNotificationSettings(notifSettings.copy(defaultAlertDurationSeconds = it.toInt()))
                                },
                                valueRange = 2f..20f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                            )
                        }
                    }
                }

                // Blaues Pulsieren bei laufenden Vorgängen
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Blaues Pulsieren bei laufenden Vorgängen",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Der Rand atmet blau, solange eine App einen Fortschritt anzeigt – z. B. Download, Upload, Backup, Export oder Installation.",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = config.isProcessingPulseEnabled,
                                    onCheckedChange = { viewModel.setProcessingPulseEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF1E6BFF),
                                        checkedTrackColor = DarkSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("processing_pulse_switch")
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tempo folgt der Pulsgeschwindigkeit unter „3. Geometrie & Helligkeit“. Benötigt Benachrichtigungszugriff.",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (!Settings.canDrawOverlays(context)) {
                                        overlayPermissionLauncher.launch(
                                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        )
                                    } else {
                                        viewModel.testProcessingPulse(8L)
                                        Toast.makeText(context, "Verarbeitungs-Puls: 8 Sekunden Test", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6BFF), contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("processing_pulse_test")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Puls jetzt testen", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // App-Farben: je App eine Farbe; Anrufe extra
                item {
                    Column {
                        Text(
                            text = "App-Farben",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tippe auf eine App und wähle ihre Farbe. Ohne eigene Farbe leuchtet die Ring-Grundfarbe.",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppColorRow(
                            packageName = null,
                            label = "Anrufe",
                            color = config.callColor,
                            fallbackColor = config.ringColor,
                            onClick = { colorDialogForCalls = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = appSearch,
                            onValueChange = { appSearch = it },
                            placeholder = { Text("App suchen …") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_search")
                        )
                    }
                }

                val rulesByPackage = appRules.associateBy { it.packageName }
                val visibleApps = installedApps.filter {
                    appSearch.isBlank() || it.label.contains(appSearch, ignoreCase = true)
                }.sortedByDescending { rulesByPackage.containsKey(it.packageName) } // Apps mit Farbe zuerst
                items(visibleApps, key = { it.packageName }) { app ->
                    AppColorRow(
                        packageName = app.packageName,
                        label = app.label,
                        color = rulesByPackage[app.packageName]?.singleColor(),
                        fallbackColor = config.ringColor,
                        onClick = { colorDialogApp = app }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VIP-Kontakte (Persönlicher Glow) (${contactRules.size})",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Filtert nach Absendernamen in Mitteilungen",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = { showAddContactDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Hinzufügen", tint = NeonMagenta, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VIP hinzufügen", color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (contactRules.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Noch keine VIP-Kontakte angelegt. Tippe auf '+ VIP hinzufügen', um z. B. Mama oder Partner mit leuchtenden Herzen zu belegen!",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(contactRules) { rule ->
                        ContactRuleCard(
                            rule = rule,
                            onUpdate = { viewModel.saveContactRule(it) },
                            onDelete = { viewModel.removeContactRule(rule.id) },
                            onTestAlert = {
                                viewModel.triggerTestNotification(
                                    paletteId = rule.paletteId,
                                    useCustomColors = rule.useCustomColors,
                                    customColors = rule.customColors,
                                    lineStyle = rule.lineStyle,
                                    durationSeconds = rule.durationSeconds
                                )
                            }
                        )
                    }
                }
            }

            // TAB 4: RUHEZEIT & AKKUSPARMODUS (ZEITPLAN / DND & BATTERY SAVER)
            if (selectedTab == 4) {
                // 1. Zeitplan & Ruhezeiten (Nicht stören / DND)
                item {
                    val isInDndNow = scheduleConfig.isCurrentlyInDnd()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Ruhezeiten (Nicht stören)",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = scheduleConfig.isScheduleEnabled,
                                    onCheckedChange = {
                                        viewModel.updateScheduleConfig(scheduleConfig.copy(isScheduleEnabled = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = DarkSurfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Deaktiviert Kantenleuchten automatisch zu festgelegten Uhrzeiten (z. B. nachts).",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )

                            // Status-Badge
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isInDndNow) Color(0xFF331600) else DarkSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (!scheduleConfig.isScheduleEnabled) "Zeitplan ist ausgeschaltet"
                                    else if (isInDndNow) "🌙 Ruhephase aktiv – Kantenlicht ist stummgeschaltet"
                                    else "☀️ Außerhalb der Ruhezeit – Normalbetrieb",
                                    color = if (isInDndNow) Color(0xFFFF9800) else NeonGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (scheduleConfig.isScheduleEnabled) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // Zeitspanne-Auswahl
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Startzeit (Nachtruhe)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "%02d:%02d Uhr".format(scheduleConfig.startHour, scheduleConfig.startMinute),
                                            color = NeonCyan,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                val newHour = (scheduleConfig.startHour - 1 + 24) % 24
                                                viewModel.updateScheduleConfig(scheduleConfig.copy(startHour = newHour))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) { Text("-1h", color = TextPrimary) }

                                        Button(
                                            onClick = {
                                                val newHour = (scheduleConfig.startHour + 1) % 24
                                                viewModel.updateScheduleConfig(scheduleConfig.copy(startHour = newHour))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) { Text("+1h", color = TextPrimary) }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Endzeit (Aufwachen)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "%02d:%02d Uhr".format(scheduleConfig.endHour, scheduleConfig.endMinute),
                                            color = NeonCyan,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                val newHour = (scheduleConfig.endHour - 1 + 24) % 24
                                                viewModel.updateScheduleConfig(scheduleConfig.copy(endHour = newHour))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) { Text("-1h", color = TextPrimary) }

                                        Button(
                                            onClick = {
                                                val newHour = (scheduleConfig.endHour + 1) % 24
                                                viewModel.updateScheduleConfig(scheduleConfig.copy(endHour = newHour))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) { Text("+1h", color = TextPrimary) }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Wochentage-Auswahl
                                Text("Aktive Wochentage", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val days = listOf(
                                        "Mo" to Calendar.MONDAY,
                                        "Di" to Calendar.TUESDAY,
                                        "Mi" to Calendar.WEDNESDAY,
                                        "Do" to Calendar.THURSDAY,
                                        "Fr" to Calendar.FRIDAY,
                                        "Sa" to Calendar.SATURDAY,
                                        "So" to Calendar.SUNDAY
                                    )
                                    days.forEach { (label, dayConst) ->
                                        val isSel = scheduleConfig.activeDays.contains(dayConst)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isSel) NeonCyan.copy(alpha = 0.25f) else DarkSurfaceVariant)
                                                .border(1.dp, if (isSel) NeonCyan else Color.Transparent, CircleShape)
                                                .clickable {
                                                    val updated = scheduleConfig.activeDays.toMutableSet()
                                                    if (isSel) updated.remove(dayConst) else updated.add(dayConst)
                                                    viewModel.updateScheduleConfig(scheduleConfig.copy(activeDays = updated))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) NeonCyan else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Intelligenter Akkusparmodus (Battery Saver)
                item {
                    val isLowBatActive = batterySaverConfig.shouldApplyBatterySaver(
                        batteryStatus.levelPercent,
                        batteryStatus.isPowerSaveMode,
                        batteryStatus.isCharging
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (batteryStatus.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                                        contentDescription = null,
                                        tint = if (isLowBatActive) Color(0xFFFF9800) else NeonGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Intelligenter Akkusparmodus",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = batterySaverConfig.isBatterySaverEnabled,
                                    onCheckedChange = {
                                        viewModel.updateBatterySaverConfig(batterySaverConfig.copy(isBatterySaverEnabled = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonGreen,
                                        checkedTrackColor = DarkSurfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Drosselt bei niedrigem Akkustand automatisch die Bildrate auf 30 FPS und dimmt die Leuchtstärke, um den Akku zu schonen.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )

                            // Akkustatus-Anzeige
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Akkustand: ${batteryStatus.levelPercent} %" +
                                            if (batteryStatus.isCharging) " ⚡ (Wird geladen)"
                                            else if (batteryStatus.isPowerSaveMode) " ⚠️ (System-Energiesparer an)"
                                            else "",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = if (isLowBatActive) "SPARMODUS AKTIV" else "NORMALER BETRIEB",
                                    color = if (isLowBatActive) Color(0xFFFF9800) else NeonGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (batterySaverConfig.isBatterySaverEnabled) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // Schwellenwert-Slider
                                Text(
                                    text = "Sparmodus aktivieren ab: unter ${batterySaverConfig.triggerThresholdPercent} % Akku",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = batterySaverConfig.triggerThresholdPercent.toFloat(),
                                    onValueChange = {
                                        viewModel.updateBatterySaverConfig(batterySaverConfig.copy(triggerThresholdPercent = it.toInt()))
                                    },
                                    valueRange = 5f..50f,
                                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // FPS Reduzierung Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Bildwiederholrate auf 30 FPS drosseln", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Reduziert die CPU-/GPU-Last um über 50 %", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(
                                        checked = batterySaverConfig.reduceFpsTo30,
                                        onCheckedChange = {
                                            viewModel.updateBatterySaverConfig(batterySaverConfig.copy(reduceFpsTo30 = it))
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // GPU Glow Deaktivierung Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Aura-Glow & Unschärfe pausieren", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Spart Shader-Berechnungen bei schwachem Akku", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(
                                        checked = batterySaverConfig.disableGlowFilter,
                                        onCheckedChange = {
                                            viewModel.updateBatterySaverConfig(batterySaverConfig.copy(disableGlowFilter = it))
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dimm-Intensität Slider
                                Text(
                                    text = "Leuchtintensität dimmen auf: ${batterySaverConfig.dimIntensityPercent} %",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = batterySaverConfig.dimIntensityPercent.toFloat(),
                                    onValueChange = {
                                        viewModel.updateBatterySaverConfig(batterySaverConfig.copy(dimIntensityPercent = it.toInt()))
                                    },
                                    valueRange = 20f..80f,
                                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: App-Regel hinzufügen
    if (showAddAppDialog) {
        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            title = { Text("App-Kantenlicht hinzufügen", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("App-Namen und Paketnamen eintragen, um personalisiertes Kantenleuchten einzurichten:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("App-Name (z. B. Signal)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAppPackage,
                        onValueChange = { newAppPackage = it },
                        label = { Text("Paketname (z. B. org.thoughtcrime.securesms)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAppName.isNotBlank() && newAppPackage.isNotBlank()) {
                            viewModel.saveAppRule(
                                AppEdgeRule(
                                    packageName = newAppPackage.trim(),
                                    appName = newAppName.trim(),
                                    paletteId = 1,
                                    durationSeconds = 6
                                )
                            )
                            newAppName = ""
                            newAppPackage = ""
                            showAddAppDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Hinzufügen", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAppDialog = false }) {
                    Text("Abbrechen", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog: VIP-Kontakt-Regel hinzufügen
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("VIP-Kontakt anlegen", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Sobald eine Nachricht oder ein Anruf von diesem Absendernamen eingeht, leuchtet der Bildschirm im gewählten Muster auf:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Kontaktname (z. B. Mama, Schatz, Chef)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Persönliche Leuchtfarbe auswählen:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(EdgeColorPalette.PRESETS.take(12)) { p ->
                            val isSel = newContactPaletteId == p.id
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(p.colors.map { Color(it.toInt()) }))
                                    .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { newContactPaletteId = p.id }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContactName.isNotBlank()) {
                            viewModel.saveContactRule(
                                ContactEdgeRule(
                                    id = UUID.randomUUID().toString(),
                                    contactIdentifier = newContactName.trim(),
                                    paletteId = newContactPaletteId,
                                    lineStyle = LineStyle.RING,
                                    durationSeconds = 8
                                )
                            )
                            newContactName = ""
                            showAddContactDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                ) {
                    Text("VIP Speichern", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Abbrechen", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun StatusAndPermissionsCard(
    config: com.example.model.EdgeConfig,
    hasNotificationAccess: Boolean,
    hasOverlayAccess: Boolean,
    onOpenNotificationAccessSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onTriggerTestAlert: () -> Unit,
    onToggleCallerId: (Boolean) -> Unit,
    onTestCallerPulse: () -> Unit,
    isTestingPulse: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_permissions_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Benachrichtigungslicht-Status",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Leuchtet ausschließlich bei neuen Benachrichtigungen & Anrufen",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasNotificationAccess && hasOverlayAccess) NeonCyan.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f))
                        .border(1.dp, if (hasNotificationAccess && hasOverlayAccess) NeonCyan.copy(alpha = 0.4f) else Color(0xFFFF9800).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (hasNotificationAccess && hasOverlayAccess) "BEREIT" else "EINRICHTUNG",
                        color = if (hasNotificationAccess && hasOverlayAccess) NeonCyan else Color(0xFFFF9800),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Benachrichtigungs-Zugriff Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (hasNotificationAccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasNotificationAccess) NeonGreen else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Benachrichtigungszugriff",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hasNotificationAccess) "Erteilt – erkennt neue Nachrichten" else "Erforderlich für App-Benachrichtigungen",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (!hasNotificationAccess) {
                    Button(
                        onClick = onOpenNotificationAccessSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("activate_notif_permission_button")
                    ) {
                        Text("Aktivieren", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Displayrand-Überlagerung Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (hasOverlayAccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasOverlayAccess) NeonGreen else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Displayrand-Überlagerung",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hasOverlayAccess) "Erteilt – Kantenlicht über Apps & Sperrbildschirm" else "Erforderlich für Randbeleuchtung",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (!hasOverlayAccess) {
                    Button(
                        onClick = onOpenOverlaySettings,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("activate_overlay_permission_button")
                    ) {
                        Text("Erteilen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Probe-Alarm abspielen Button
            Button(
                onClick = onTriggerTestAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_alert_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Probe-Alarm abspielen (5 Sek. Leuchttest)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Anruferkennung (Caller ID)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Anruf-Alarm",
                            tint = ElectricPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bei eingehendem Anruf leuchten",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Kantenlicht pulsiert während das Telefon klingelt",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Switch(
                    checked = config.isCallerIdAlertEnabled,
                    onCheckedChange = onToggleCallerId,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ElectricPurple,
                        checkedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("caller_id_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onTestCallerPulse,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_pulse_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTestingPulse) NeonMagenta else DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (isTestingPulse) Color.White else NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTestingPulse) "Anruf-Puls beenden" else "Anruf-Leuchtpuls testen",
                    color = if (isTestingPulse) Color.White else NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
