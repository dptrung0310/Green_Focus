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
import com.example.greenfocus.ui.theme.BannerGreen
import com.example.greenfocus.R

data class NavItem(val route: String, val iconRes: Int)
val navItems = listOf(
    NavItem("Home", R.drawable.home),
    NavItem("Forest", R.drawable.forest),
    NavItem("Stats", R.drawable.stats),
    NavItem("Store", R.drawable.store),
    NavItem("Social", R.drawable.social),
    NavItem("Profile", R.drawable.profile)
)

@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val isSelected = item.route == currentRoute
            val contentColor = if (isSelected) BannerGreen else Color.Gray

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onNavigate(item.route) }
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.route,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.route,
                    color = contentColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}