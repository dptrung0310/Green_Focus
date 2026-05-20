package com.example.greenfocus.ui.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.greenfocus.data.DataSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsUiState.collectAsStateWithLifecycle()

    // 1. Fetch apps from OS when the screen first opens
    LaunchedEffect(Unit) {
        viewModel.getLaunchableApps(context)
    }

    // 5. Intercept the Android System Back Button if there are unsaved changes
    BackHandler(enabled = uiState.isChanged) {
        viewModel.toggleUnsavedDialog()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isChanged) viewModel.toggleUnsavedDialog() else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Bottom Action Buttons
            Surface(tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { viewModel.resetToDefault() }) {
                        Text("Mặc định")
                    }

                    Button(
                        onClick = {
                            viewModel.saveAllData()
                        },
                        enabled = uiState.isChanged // Disable button if no changes
                    ) {
                        Text("Lưu cấu hình")
                    }
                }
            }
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                // --- DROPDOWN MENU FOR SOUND ---
                Text(
                    text = "Âm thanh hoàn thành",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                SoundDropdownMenu(
                    currentSound = uiState.currentFinishSound,
                    onSoundSelected = { viewModel.updateFinishSoundDraft(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- APP CHECKLIST ---
                Text(
                    text = "Bỏ qua chế độ Tập trung sâu (Deep Mode)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // LazyColumn with weight(1f) restricts it to the remaining middle area
                // so it scrolls perfectly between the Dropdown and the Bottom Bar!
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(
                        uiState.allApps,
                        key = { it.packageName }) { app ->
                        val isChecked = uiState.deepModeAllowedApps.contains(app.packageName)

                        AppListItem(
                            app = app,
                            isChecked = isChecked,
                            onCheckedChange = { checked ->
                                val newSet = uiState.deepModeAllowedApps.toMutableSet()
                                if (checked) newSet.add(app.packageName) else newSet.remove(app.packageName)
                                viewModel.updateAllowedAppsDraft(newSet)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleResetDialog() },
            title = { Text("Khôi phục mặc định?") },
            text = { Text("Mọi thay đổi của bạn sẽ bị xóa. Bạn có chắc chắn muốn khôi phục về cài đặt gốc không?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToDefault()
                    viewModel.toggleResetDialog()
                }) { Text("Khôi phục") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleResetDialog() }) { Text("Hủy") }
            }
        )
    }

    if (uiState.showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleUnsavedDialog() },
            title = { Text("Chưa lưu thay đổi") },
            text = { Text("Bạn có những thay đổi chưa được lưu. Bạn có chắc chắn muốn thoát mà không lưu lại không?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleUnsavedDialog()
                    onNavigateBack() // Exit without saving
                }) { Text("Thoát", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleUnsavedDialog() }) { Text("Ở lại") }
            }
        )
    }
}

// --- HELPER COMPOSABLES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundDropdownMenu(
    currentSound: Int,
    onSoundSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Map your custom Sound constants to readable names
    val soundOptions = DataSource.winRingtone

    val currentSoundName = soundOptions.first { it.id == currentSound }.name

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentSoundName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                soundOptions.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSoundSelected(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: InstalledApp,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coil natively supports displaying Android Drawable objects!
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = null // Handled by Row clickable
        )
    }
}