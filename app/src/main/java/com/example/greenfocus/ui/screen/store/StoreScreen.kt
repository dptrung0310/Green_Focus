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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.greenfocus.data.model.StoreTreeItem
import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.data.model.TreeStatus
import com.example.greenfocus.ui.components.CoinContainer
import com.example.greenfocus.ui.components.TreeCard
import com.example.greenfocus.ui.theme.*

import com.example.greenfocus.R
import com.example.greenfocus.data.repository.PurchaseResult
import com.example.greenfocus.ui.navigation.Screen

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
                text = "Confirm Purchase?",
                fontWeight = FontWeight.Bold,
                color = BannerGreen,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Buy ${stringResource(item.tree.name)} for ${item.tree.price} coins?",
                    fontSize = 15.sp,
                    color = TextMuted
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰", fontSize = 14.sp)
                    Text(
                        text = "Your balance: $userCoins coins",
                        fontSize = 13.sp,
                        color = TextMuted.copy(alpha = 0.7f)
                    )
                }
            }
        },

        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Buy", color = BannerGreen, fontWeight = FontWeight.Bold)
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
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
                text = "Not Enough Coins",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE57373),
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "Complete more focus sessions to earn more coins",
                fontSize = 15.sp,
                color = TextMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = BannerGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    /**
    val mockTrees = listOf(
        StoreTreeItem(
            tree = TreeType(id = "1", name = R.string.tree_oak, description = "Classic", imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.OWNED
        ),
        StoreTreeItem(
            tree = TreeType(id = "2", name = R.string.tree_pine, description = "Evergreen focus", imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.OWNED
        ),
        StoreTreeItem(
            tree = TreeType(id = "3", name = R.string.tree_cherry, description = "Beautiful and calm", imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.OWNED
        ),
        StoreTreeItem(
            tree = TreeType(id = "4", name = R.string.tree_maple, description = "Autumn vibes", price = 500, imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.BUYABLE
        ),
        StoreTreeItem(
            tree = TreeType(id = "5", name = R.string.tree_palm, description = "Tropical paradise", price = 750, imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.BUYABLE
        ),
        StoreTreeItem(
            tree = TreeType(id = "6", name = R.string.tree_cactus, description = "Desert warrior", price = 1000, imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.BUYABLE
        ),
        StoreTreeItem(
            tree = TreeType(id = "7", name = R.string.tree_bamboo, description = "Zen master", price = 1250, imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.BUYABLE
        ),
        StoreTreeItem(
            tree = TreeType(id = "8", name = R.string.tree_sakura, description = "Legendary beauty", imageStaticSeed = R.drawable.tree, imageStaticSmall = R.drawable.tree, imageStaticBig = R.drawable.tree),
            status = TreeStatus.LOCKED
        )
    )
    **/

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
                        Text("Seed Shop", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BannerGreen)
                        Text("Unlock new trees", fontSize = 16.sp, color = TextMuted)
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
                        .background(BannerGreen)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Earn More Coins", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Complete sessions to earn rewards", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
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