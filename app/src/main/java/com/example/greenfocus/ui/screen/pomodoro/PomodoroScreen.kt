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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.theme.GreenFocusTheme
import com.example.greenfocus.R
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

    val timerScale by animateFloatAsState(
        targetValue = if (pomodoroUiState.isTimerRunning) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 600),
        label = "timerScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7EC))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Username Row
        AnimatedVisibility(
            visible = !pomodoroUiState.isTimerRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pomodoro_username, pomodoroUiState.currentUserName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
                CoinContainer(coins = pomodoroUiState.userMoneyAmount)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 2. Timer with Circular Progress, Image, and Countdown
        TimerBlock(
            currentTime = pomodoroUiState.formattedTime,
            currentTree = pomodoroUiState.selectedTreeImage,
            currentTreeName = stringResource(id = pomodoroUiState.selectedTree.name),
            currentProgress = pomodoroUiState.currentPercentage,
            isHalfDone = pomodoroUiState.isHalfDone,
            seedImage = pomodoroUiState.selectedTree.imageStaticSeed,
            bigImage = pomodoroUiState.selectedTree.imageStaticBig,
            onTimerClick = { pomodoroViewModel.toggleTimeDialog() },
            isTimerRunning = pomodoroUiState.isTimerRunning,
            modifier = Modifier.scale(timerScale)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 3. Reward Info Card
        AnimatedVisibility(
            visible = !pomodoroUiState.isTimerRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Reward Icon",
                    tint = Color(0xFFFBC02D),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Hoàn thành để kiếm được: ${pomodoroUiState.dialogTimeValue} xu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Deep Focus Mode Toggle Block
        AnimatedVisibility(
            visible = !pomodoroUiState.isTimerRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = Color(0xFF388E3C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.pomodoro_deep_mode_button),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = pomodoroUiState.isDeepFocusEnabled,
                        onCheckedChange = null, // null because the Row handles the toggleable click
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF388E3C),
                            checkedTrackColor = Color(0xFFC8E6C9),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFFE0E0E0)
                        )
                    )
                }
            }
        }

        // 5. Scrollable Row of Tree Buttons
        AnimatedVisibility(
            visible = !pomodoroUiState.isTimerRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                TreeSelectionRow(
                    isTimerRunning = pomodoroUiState.isTimerRunning,
                    selectedTree = pomodoroUiState.selectedTree,
                    unlockedTrees = pomodoroUiState.unlockedTrees,
                    changeSelectedTree = { pomodoroViewModel.updateSelectedTree(it) }
                )
            }
        }

        // 6. Start Timer Button
        AnimatedVisibility(
            visible = !pomodoroUiState.isTimerRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
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
                                    pomodoroViewModel.toggleRationaleDialog() // This triggers the AlertDialog above
                                }

                                // SCENARIO 3: First Time Ask
                                else -> {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        } else {
                            // Android 12 and below don't require this specific permission
                            pomodoroViewModel.startTimerService(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pomodoro_start_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Give Up / Cancel Button
        AnimatedVisibility(
            visible = pomodoroUiState.isTimerRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            TextButton(
                onClick = { pomodoroViewModel.stopTimerService(context) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.pomodoro_stop_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
    currentTreeName: String,
    isHalfDone: Boolean = false,
    seedImage: Int = currentTree,
    bigImage: Int = currentTree,
    onTimerClick: () -> Unit,
    isTimerRunning: Boolean,
    modifier: Modifier = Modifier
) {
    // Track glow animation: pulses once when isHalfDone becomes true
    var glowVisible by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (glowVisible) 0.55f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "glowAlpha"
    )

    LaunchedEffect(isHalfDone) {
        if (isHalfDone) {
            glowVisible = true
            kotlinx.coroutines.delay(900)
            glowVisible = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(250.dp)
    ) {
        // Circular Progress Bar
        CircularProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            color = Color(0xFF388E3C),
            trackColor = Color(0xFFC8E6C9)
        )

        // Glow ring that pulses once at the halfway point
        if (glowAlpha > 0f) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize().alpha(glowAlpha),
                strokeWidth = 12.dp,
                color = Color(0xFF66BB6A),
                trackColor = Color.Transparent
            )
        }

        // Image and Text inside the circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTreeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // AnimatedContent: swap seed → big with scale-bounce when isHalfDone
            AnimatedContent(
                targetState = isHalfDone,
                transitionSpec = {
                    // Entering big tree: scale from 40% with bouncy spring + fade in
                    (scaleIn(
                        initialScale = 0.4f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(animationSpec = tween(350))) togetherWith
                    // Leaving small tree: scale up to 160% + quick fade out
                    (scaleOut(
                        targetScale = 1.6f,
                        animationSpec = tween(280)
                    ) + fadeOut(animationSpec = tween(200)))
                },
                label = "treeGrowAnimation"
            ) { halfDone ->
                val imageRes = if (halfDone) bigImage else seedImage
                // Thay thế Image phẳng cũ bằng Interactive3DCard
                com.example.greenfocus.ui.components.Interactive3DCard(
                    treeImageRes = imageRes,
                    modifier = Modifier.size(110.dp),
                    cardColor = Color(0xFFFAF7EC), // Màu kem đồng bộ màu nền ứng dụng
                    glowColor = Color(0x334CAF50)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentTime,
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFF2E7D32),
                modifier = Modifier.clickable(
                    enabled = !isTimerRunning,
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
    unlockedTrees: List<TreeType>,
    changeSelectedTree: (TreeType) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(unlockedTrees) { tree ->
            val isSelected = selectedTree.id == tree.id

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Using Surface instead of IconButton for better background and shadow control
                Surface(
                    onClick = { changeSelectedTree(tree) },
                    enabled = !isTimerRunning,
                    modifier = Modifier.size(80.dp).alpha(if (isTimerRunning && !isSelected) 0.5f else 1f), // Slightly smaller to leave space for text
                    shape = RoundedCornerShape(20.dp), // Smooth, modern rounded corners

                    // Background Color: Use soft green when selected, white when not
                    color = if (isSelected) Color(0xFFE8F5E9) else Color.White,

                    // Border: Add a hard green outline to the selected item to make it stand out instantly
                    border = if (isSelected) BorderStroke(3.dp, Color(0xFF388E3C)) else null,

                    // Shadow: Drops a subtle shadow so the button lifts off the white background
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
                                .padding(12.dp) // Keeps the tree image safely inside the borders
                        )
                    }
                }
                Text(
                    text = stringResource(id = tree.name),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF2E7D32) else Color.Gray
                )
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