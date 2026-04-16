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

// Sample data class for the tree list
data class TreeItem(val id: Int, val drawableRes: Int)

@Composable
fun PomodoroScreen(
    pomodoroViewModel: PomodoroViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // In a real app, these would come from pomodoroViewModel.pomodoroUiState
    val pomodoroUiState by pomodoroViewModel.pomodoroUiState.collectAsState()

    // Local state for the dialog and switch (Move to ViewModel in production)
    var showTimeDialog by remember { mutableStateOf(false) }
    var isDeepFocusEnabled by remember { mutableStateOf(false) }

    // Sample list of trees
    val trees = listOf(
        TreeItem(1, android.R.drawable.ic_menu_gallery), // Replace with your drawables
        TreeItem(2, android.R.drawable.ic_menu_gallery),
        TreeItem(3, android.R.drawable.ic_menu_gallery)
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(250.dp)
        ) {
            // Circular Progress Bar
            CircularProgressIndicator(
                progress = { 0.75f }, // Replace with state: timeRemaining / totalTime
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Image and Text inside the circle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = android.R.drawable.ic_menu_gallery), // Replace with tree image
                    contentDescription = "Current growing tree",
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "25:00", // Replace with formatted time from state
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.clickable { showTimeDialog = true }
                )
            }
        }

        // 3. Deep Focus Mode Toggle Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .toggleable(
                    value = isDeepFocusEnabled,
                    onValueChange = { isDeepFocusEnabled = it },
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
                checked = isDeepFocusEnabled,
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
        Button(
            onClick = { /* Start timer action via ViewModel */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Start", style = MaterialTheme.typography.titleMedium)
        }
    }

    // Dialog to change time
    if (showTimeDialog) {
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = { Text("Change Duration") },
            text = { Text("Time picker UI goes here") },
            confirmButton = {
                TextButton(onClick = { showTimeDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPomodoroScreen(){
    GreenFocusTheme() {
        PomodoroScreen()
    }
}