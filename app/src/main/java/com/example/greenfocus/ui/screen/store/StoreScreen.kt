package com.example.greenfocus.ui.screen.store


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.greenfocus.data.model.StoreTreeItem
import com.example.greenfocus.ui.components.CoinContainer
import com.example.greenfocus.ui.components.TreeCard
import com.example.greenfocus.ui.theme.*
import com.example.greenfocus.R

@Composable
private fun PurchaseConfirmDialog(
    item: StoreTreeItem,
    userCoins: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StoreBackGround,
        title = {
            Text(
                text = stringResource(R.string.store_confirm_title),
                fontWeight = FontWeight.Bold,
                color = BannerGreen,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.store_confirm_message,
                        stringResource(item.tree.name), item.tree.price),
                    fontSize = 15.sp,
                    color = TextMuted
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰", fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.store_balance, userCoins),
                        fontSize = 13.sp,
                        color = TextMuted.copy(alpha = 0.7f)
                    )
                }
            }
        },

        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.store_btn_buy), color = BannerGreen, fontWeight = FontWeight.Bold)
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = TextMuted)
            }
        }
    )
}

@Composable
private fun InsufficientFundsDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StoreBackGround,
        title = {
            Text(
                text = stringResource(R.string.store_insufficient_title),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE57373),
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = stringResource(R.string.store_insufficient_message),
                fontSize = 15.sp,
                color = TextMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_confirm), color = BannerGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val gradientBanner = Brush.linearGradient(
        colors = listOf(
            BannerGreen_Start,
            BannerGreen_End
        )
    )

    if (uiState.showPurchaseConfirmDialog && uiState.pendingItem != null) {
        PurchaseConfirmDialog(
            item = uiState.pendingItem!!,
            userCoins = uiState.userCoins,
            onConfirm = { viewModel.onConfirmPurchase() },
            onDismiss = { viewModel.onDismissDialog() }
        )
    }

    if (uiState.showInsufficientFundsDialog) {
        InsufficientFundsDialog(
            onDismiss = { viewModel.onDismissDialog() }
        )
    }

    uiState.errorMessage?.let { errorMsg ->
        LaunchedEffect(errorMsg) {
            viewModel.onErrorDismissed()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(StoreBackGround)
            .padding(horizontal = 16.dp)
            .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = {GridItemSpan(2)}) {
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.store_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BannerGreen)
                        Text(stringResource(R.string.store_subtitle), fontSize = 16.sp, color = TextMuted)
                    }

                    //User total coins will be updated here
                    CoinContainer(coins = uiState.userCoins) //Mock data
                }
            }

            items(uiState.trees) { item ->
                TreeCard(
                    tree = item,
                    onBuyClicked = {viewModel.onBuyClicked(item)}
                )
            }

            item(span = {GridItemSpan(2)}) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(gradientBanner)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.store_banner_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.store_banner_subtitle), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛍️", fontSize = 24.sp)
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
}