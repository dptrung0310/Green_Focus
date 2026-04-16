package com.example.greenfocus.ui.screen.pomodoro

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.theme.GreenFocusTheme
import com.example.greenfocus.R

// Sample data class for the tree list
data class TreeItem(val id: Int, val drawableRes: Int)

@Composable
fun PomodoroScreen(
    modifier: Modifier = Modifier,
    pomodoroViewModel: PomodoroViewModel = viewModel()
) {
    // In a real app, these would come from pomodoroViewModel.pomodoroUiState
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
                text = "Enable Deep Focus Mode",
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
                onClick = { pomodoroViewModel.startTimer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Start", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = { pomodoroViewModel.pauseTimer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Pause", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Dialog to change time
    if (pomodoroUiState.showTimeDialog) {
        AlertDialog(
            onDismissRequest = { pomodoroViewModel.toggleTimeDialog() },
            title = { Text("Change Duration") },
            text = { Text("Time picker UI goes here") },
            confirmButton = {
                TextButton(onClick = { pomodoroViewModel.toggleTimeDialog() }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { pomodoroViewModel.toggleTimeDialog() }) {
                    Text("Cancel")
                }
            }
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
                modifier = Modifier.clickable { onTimerClick }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPomodoroScreen(){
    GreenFocusTheme() {
        PomodoroScreen()
    }
}