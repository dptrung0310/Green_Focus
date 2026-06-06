package com.example.greenfocus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.ui.theme.AuthGreenLight
import com.example.greenfocus.R

data class NavItem(val route: String, val label: String, val iconRes: Int)

val navItems = listOf(
    NavItem(route = "home",    label = "Trang chủ",    iconRes = R.drawable.home),
    NavItem(route = "forest",  label = "Khu rừng",  iconRes = R.drawable.forest),
    NavItem(route = "stats",   label = "Thống kê",   iconRes = R.drawable.stats),
    NavItem(route = "store",   label = "Cửa hàng",   iconRes = R.drawable.store),
    NavItem(route = "social",  label = "Cộng đồng",  iconRes = R.drawable.social),
    NavItem(route = "profile", label = "Cá nhân", iconRes = R.drawable.profile),
)

@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val isSelected = item.route == currentRoute
            val contentColor = if (isSelected) AuthGreenLight else Color.Gray

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(item.route) }
                    .padding(vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    color = contentColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}