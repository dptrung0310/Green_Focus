package com.example.greenfocus.ui.screen.pomodoro

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.theme.GreenFocusTheme
import com.example.greenfocus.R
import com.example.greenfocus.data.DataSource
import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.ui.components.CoinContainer

@Composable
fun PomodoroScreen(
    modifier: Modifier = Modifier,
    pomodoroViewModel: PomodoroViewModel = viewModel(factory = PomodoroViewModel.Factory)
) {

    val context = LocalContext.current
    val activity = context as? Activity
    val pomodoroUiState by pomodoroViewModel.pomodoroUiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                pomodoroViewModel.startTimerService(context)
            } else {
                Log.d("WARNING", "User denied Post Perm, now they can't run the app :(")
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), // Added padding to prevent UI from touching screen edges
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // Distributes space evenly
    ) {
        // 1. Username Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pomodoro_username, pomodoroUiState.currentUserName),
                style = MaterialTheme.typography.headlineSmall
            )
            CoinContainer(coins = pomodoroUiState.userMoneyAmount)
        }

        // 2. Timer with Circular Progress, Image, and Countdown
        TimerBlock(
            currentTime = pomodoroUiState.formattedTime,
            currentTree = pomodoroUiState.selectedTreeImage,
            currentProgress = pomodoroUiState.currentPercentage,
            onTimerClick = { pomodoroViewModel.toggleTimeDialog() },
            isTimerRunning = pomodoroUiState.isTimerRunning
        )

        // 3. Deep Focus Mode Toggle Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .toggleable(
                    value = pomodoroUiState.isDeepFocusEnabled,
                    enabled = !pomodoroUiState.isTimerRunning,
                    onValueChange = {
                        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                        val mode = appOps.checkOpNoThrow(
                            AppOpsManager.OPSTR_GET_USAGE_STATS,
                            Process.myUid(),
                            context.packageName
                        )
                        // SCENARIO 1: Green Light
                        if (mode == AppOpsManager.MODE_ALLOWED) {
                            pomodoroViewModel.toggleDeepFocus()
                        }
                        // SCENARIO 2: First Time Ask
                        else {
                            pomodoroViewModel.toggleUsageStatsRationaleDialog()
                        }
                        },
                    role = Role.Switch
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.pomodoro_deep_mode_button),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = pomodoroUiState.isDeepFocusEnabled,
                onCheckedChange = null // null because the Row handles the toggleable click
            )
        }

        // 4. Scrollable Row of Tree Buttons
        TreeSelectionRow(
            isTimerRunning = pomodoroUiState.isTimerRunning,
            selectedTree = pomodoroUiState.selectedTree,
            changeSelectedTree = { pomodoroViewModel.updateSelectedTree(it) }
        )

        // 5. Start Timer Button
        if (!pomodoroUiState.isTimerRunning) {
            Button (
                onClick = {
                    // 3-step permission check shenanigan
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        // SCENARIO 1: Green Light
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            pomodoroViewModel.startTimerService(context)
                        }

                        // SCENARIO 2: Needs Explanation
                        activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) -> {
                            pomodoroViewModel.toggleRationaleDialog()// This triggers the AlertDialog above
                        }

                        // SCENARIO 3: First Time Ask
                        else -> {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    // Android 12 and below don't require this specific permission
                    pomodoroViewModel.startTimerService(context)
                } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = stringResource(R.string.pomodoro_start_button), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = { pomodoroViewModel.stopTimerService(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = stringResource(R.string.pomodoro_stop_button), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Dialog to change time
    if (pomodoroUiState.showTimeDialog) {
        TimerDialog(
            dialogTimeValue = pomodoroUiState.dialogTimeValue,
            onUserInputChange = { pomodoroViewModel.updateTimeDialogValue(it)},
            onConfirm = { pomodoroViewModel.setTimer() },
            onDismiss = { pomodoroViewModel.toggleTimeDialog() }
        )
    }
    if (pomodoroUiState.showRationaleDialog) {
        RationaleDialog(
            onDismiss = { pomodoroViewModel.toggleRationaleDialog() },
            permissionLauncher = permissionLauncher
        )
    }
    if (pomodoroUiState.showUsageStatsRationaleDialog) {
        UsageStatsRationaleDialog(
            onDismiss = { pomodoroViewModel.toggleUsageStatsRationaleDialog() },
            context = context
        )
    }
}

@Composable
fun TimerBlock(
    currentTime: String,
    currentProgress: Float,
    currentTree: Int,
    onTimerClick: () -> Unit,
    isTimerRunning: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(250.dp)
    ) {
        // Circular Progress Bar
        CircularProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Image and Text inside the circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(currentTree), // Replace with tree image
                contentDescription = "Current growing tree",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentTime,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.clickable(
                    enabled = !isTimerRunning, // Only clickable when the timer is NOT running
                    onClick = { onTimerClick() }
                )
            )
        }
    }
}

@Composable
fun TimerDialog(
    dialogTimeValue: Int,
    onDismiss: () -> Unit,
    onUserInputChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        content = {
            Box(modifier = Modifier
                .width(400.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp) // 1. Rounded corners
                )
                .padding(16.dp) // 2. Inner padding))
            )
            {
                Column() {
                    Text(
                        text = stringResource(R.string.pomodoro_timer_dialog),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        label = { Text(stringResource(R.string.pomodoro_timer_dialog)) },
                        value = dialogTimeValue.toString(),
                        onValueChange = onUserInputChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done // Shows a "Done" checkmark on keyboard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ADDED BUTTONS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End // Aligns buttons to the right
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.dialog_cancel))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            enabled = dialogTimeValue > 0,
                            onClick = {
                                onConfirm()
                                onDismiss() // Dismiss the dialog after confirming
                            }
                        ) {
                            Text(stringResource(R.string.dialog_confirm))
                        }
                    }
                }
            }

        }
    )
}

@Composable
fun TreeSelectionRow(
    isTimerRunning: Boolean,
    selectedTree: TreeType,
    changeSelectedTree: (TreeType) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(DataSource.plants) { tree ->
            val isSelected = selectedTree.id == tree.id

            // Using Surface instead of IconButton for better background and shadow control
            Surface(
                onClick = { changeSelectedTree(tree) },
                enabled = !isTimerRunning,
                modifier = Modifier.size(96.dp).alpha(if (isTimerRunning && !isSelected) 0.5f else 1f), // 1. Noticeably bigger size!
                shape = RoundedCornerShape(24.dp), // 2. Smooth, modern rounded corners

                // 3. Background Color: Use your app's primary color when selected, soft grey when not
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F5F5),

                // 4. Border: Add a hard outline to the selected item to make it stand out instantly
                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,

                // 5. Shadow: Drops a subtle shadow so the button lifts off the white background
                shadowElevation = if (isSelected) 8.dp else 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = tree.imageStaticBig),
                        contentDescription = "Select tree ${tree.id}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp) // Keeps the tree image safely inside the borders
                    )
                }
            }
        }
    }
}


@Composable
fun RationaleDialog(
    onDismiss: () -> Unit,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.rationale_dialog_title)) },
        text = { Text(stringResource(R.string.rationale_dialog_content)) },
        confirmButton = {
            TextButton(onClick = {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                onDismiss()
            }) { Text(stringResource(R.string.dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.dialog_dismiss)) }
        }
    )
}

@Composable
fun UsageStatsRationaleDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.rationale_dialog_title)) },
        text = { Text(stringResource(R.string.usage_stats_rationale_dialog_content)) },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                onDismiss()
            }) { Text(stringResource(R.string.dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.dialog_dismiss)) }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPomodoroScreen(){
    GreenFocusTheme() {
        PomodoroScreen()
    }
}