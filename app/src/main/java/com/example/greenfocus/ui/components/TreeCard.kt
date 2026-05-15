package com.example.greenfocus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.data.model.StoreTreeItem
import com.example.greenfocus.data.model.TreeStatus
import com.example.greenfocus.ui.theme.*

@Composable
fun TreeCard(tree: StoreTreeItem, modifier: Modifier = Modifier) {
    val bgColor = when (tree.status) {
        TreeStatus.OWNED -> CardOwnedBackGround
        TreeStatus.LOCKED -> CardLockedBackGround
        TreeStatus.BUYABLE -> CardNormal
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = tree.tree.imageStaticBig),
            contentDescription = androidx.compose.ui.res.stringResource(id = tree.tree.name),
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(id = tree.tree.name),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (tree.status == TreeStatus.LOCKED) TextMuted else TextDark
        )
        Text(
            text = tree.tree.description,
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when (tree.status) {
                        TreeStatus.OWNED -> ButtonOwned
                        TreeStatus.BUYABLE -> BuyButton
                        TreeStatus.LOCKED -> TextMuted.copy(alpha = 0.5f)
                    }
                )
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            when (tree.status) {
                TreeStatus.OWNED -> Text("Owned ✓", color = BannerGreen, fontWeight = FontWeight.Bold)
                TreeStatus.BUYABLE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💰 ", fontSize = 14.sp)
                    Text(tree.tree.price.toString(), fontWeight = FontWeight.Bold)
                }
                TreeStatus.LOCKED -> Text("Locked", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

}