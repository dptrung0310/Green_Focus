package com.example.greenfocus.ui.screen.pomodoro

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.theme.GreenFocusTheme
import com.example.greenfocus.R

// Sample data class for the tree list
data class TreeItem(val id: Int, val drawableRes: Int)

@Composable
fun PomodoroScreen(
    modifier: Modifier = Modifier,
    pomodoroViewModel: PomodoroViewModel = viewModel(factory = PomodoroViewModel.Factory)
) {

    val context = LocalContext.current
    val pomodoroUiState by pomodoroViewModel.pomodoroUiState.collectAsState()

    // Sample list of trees
    val trees = listOf(
        TreeItem(1, R.drawable.ic_launcher_background), // Replace with your drawables
        TreeItem(2, R.drawable.ic_launcher_background),
        TreeItem(3, R.drawable.ic_launcher_background)
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
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Xin chào X",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // 2. Timer with Circular Progress, Image, and Countdown
        TimerBlock(
            currentTime = pomodoroUiState.formattedTime,
            currentTree = R.drawable.ic_launcher_background,
            currentProgress = pomodoroUiState.currentPercentage,
            onTimerClick = { pomodoroViewModel.toggleTimeDialog() }
        )

        // 3. Deep Focus Mode Toggle Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .toggleable(
                    value = pomodoroUiState.isDeepFocusEnabled,
                    onValueChange = { pomodoroViewModel.toggleDeepFocus() },
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
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(trees) { tree ->
                IconButton(
                    onClick = { /* Select tree action */ },
                    modifier = Modifier.size(64.dp)
                ) {
                    Image(
                        painter = painterResource(id = tree.drawableRes),
                        contentDescription = "Select tree ${tree.id}"
                    )
                }
            }
        }

        // 5. Start Timer Button
        if (!pomodoroUiState.isTimerRunning) {
            Button (
                onClick = { pomodoroViewModel.startTimerService(context) },
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
}

@Composable
fun TimerBlock(
    currentTime: String,
    currentProgress: Float,
    currentTree: Int,
    onTimerClick: () -> Unit
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
                text = currentTime, // Replace with formatted time from state
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.clickable { onTimerClick() }
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
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            enabled = dialogTimeValue > 0,
                            onClick = {
                                onConfirm()
                                onDismiss() // Dismiss the dialog after confirming
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }

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