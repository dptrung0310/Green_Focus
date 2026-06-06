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
    NavItem(route = "home",    label = "Home",    iconRes = R.drawable.home),
    NavItem(route = "forest",  label = "Forest",  iconRes = R.drawable.forest),
    NavItem(route = "stats",   label = "Stats",   iconRes = R.drawable.stats),
    NavItem(route = "store",   label = "Store",   iconRes = R.drawable.store),
    NavItem(route = "social",  label = "Social",  iconRes = R.drawable.social),
    NavItem(route = "profile", label = "Profile", iconRes = R.drawable.profile),
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
                    .clickable { onNavigate(item.route) }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(if (isSelected) 28.dp else 24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    color = contentColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}