package com.example.greenfocus.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun PermissionTestScreen(
    modifier: Modifier = Modifier,
    statuses: Map<String, Boolean>,
    onCheckAll: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestUsage: () -> Unit,
    onRequestNotification: () -> Unit
) {
    //Check right after opened app
    LaunchedEffect(Unit) { onCheckAll() }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("BẢNG KIỂM TRA QUYỀN ỨNG DỤNG", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        //Permissions' State
        PermissionItem("Camera (AR)", statuses["Camera"] ?: false, onRequestCamera)
        PermissionItem("Overlay (Chặn app)", statuses["Overlay"] ?: false, onRequestOverlay)
        PermissionItem("Usage Stats (Theo dõi)", statuses["Usage Stats"] ?: false, onRequestUsage)
        PermissionItem("Notification (Timer)", statuses["Notification"] ?: false, onRequestNotification)

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        Button(onClick = onCheckAll, modifier = Modifier.fillMaxWidth()) {
            Text("REFRESH")
        }
    }
}

@Composable
fun PermissionItem(name: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .clickable { if (!isGranted) onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, fontSize = 16.sp)
        Text(
            text = if (isGranted) "Đã được cấp quyền" else "Chưa được cấp quyền",
            color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontWeight = FontWeight.Bold
        )
    }
}