package com.example.greenfocus.ui.screen.forest

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.R
import com.example.greenfocus.data.model.FocusSession
import com.example.greenfocus.data.model.TreeResourceMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ForestScreen(
    modifier: Modifier = Modifier,
    onArForestNavigate: () -> Unit,
    forestViewModel: ForestViewModel = viewModel(factory = ForestViewModel.Factory)
) {
    val uiState by forestViewModel.forestUiState.collectAsState()
    val screenBackgroundColor = Color(0xFFFAF7EC)
    val filteredSessions = uiState.treeList.filter { session ->
        when (uiState.currentFilter) {
            ForestFilter.ALL -> true
            ForestFilter.ALIVE -> session.status == "ALIVE"
            ForestFilter.DEAD -> session.status == "DEAD" || session.status == "WITHERED"
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = modifier
            .fillMaxSize()
            .background(screenBackgroundColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    text = "Khu rừng của tôi",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF266E46),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                ArBanner(onClick = onArForestNavigate)

                Spacer(modifier = Modifier.height(20.dp))

                FilterTabs(
                    selectedFilter = uiState.currentFilter,
                    totalCount = uiState.currentTotalTree,
                    aliveCount = uiState.currentAliveTree,
                    deadCount = uiState.currentDeadTree,
                    onFilterSelected = { forestViewModel.setFilter(it) }
                )
            }
        }

        items(filteredSessions) { session ->
            TreeCard(
                treeId = session.treeId,
                status = session.status,
                duration = session.durationMinutes,
                timestamp = session.startTime
            )
        }
    }
}

@Composable
fun ArBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(Color(0xFF388E3C))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Xem chế độ AR",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Ngắm khu rừng dưới dạng 3D",
                color = Color(0xCCFFFFFF),
                fontSize = 14.sp
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "AR Camera",
                tint = Color.White
            )
        }
    }
}

@Composable
fun FilterTabs(
    selectedFilter: ForestFilter,
    totalCount: Int,
    aliveCount: Int,
    deadCount: Int,
    onFilterSelected: (ForestFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilterTabItem(
            text = "Tất cả ($totalCount)",
            isSelected = selectedFilter == ForestFilter.ALL,
            modifier = Modifier.weight(1f)
        ) { onFilterSelected(ForestFilter.ALL) }

        FilterTabItem(
            text = "Cây sống ($aliveCount)",
            isSelected = selectedFilter == ForestFilter.ALIVE,
            modifier = Modifier.weight(1f)
        ) { onFilterSelected(ForestFilter.ALIVE) }

        FilterTabItem(
            text = "Cây héo ($deadCount)",
            isSelected = selectedFilter == ForestFilter.DEAD,
            modifier = Modifier.weight(1f)
        ) { onFilterSelected(ForestFilter.DEAD) }
    }
}

@Composable
fun FilterTabItem(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF388E3C) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF4B5563)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TreeGrid(selectedFilter: ForestFilter, sessions: List<FocusSession>) {
    val filteredSessions = sessions.filter { session ->
        when (selectedFilter) {
            ForestFilter.ALL -> true
            ForestFilter.ALIVE -> session.status == "ALIVE"
            ForestFilter.DEAD -> session.status == "DEAD" || session.status == "WITHERED"
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filteredSessions) { session ->
            TreeCard(
                treeId = session.treeId,
                status = session.status,
                duration = session.durationMinutes,
                timestamp = session.startTime
            )
        }
    }
}

@Composable
fun TreeCard(
    treeId: String,
    status: String,
    duration: Int,
    timestamp: Long
) {
    val isAlive = status == "ALIVE"
    val treeImage = TreeResourceMapper.getDrawableResId(treeId)
    val backgroundColor = if (isAlive) Color(0xFF98D19F) else Color(0xFFB3BAC4)
    val textColor = if (isAlive) Color(0xFF266E46) else Color(0xFF4B5563)
    val dateString = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))

    Column(
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .aspectRatio(0.85f)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                if (isAlive) {
                    treeImage
                } else {
                    R.drawable.dead_tree
                }
            ),
            contentDescription = "Tree",
            modifier = Modifier.size(50.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(id = TreeResourceMapper.getNameResId(treeId)),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${duration} phút • ${dateString}",
            color = textColor.copy(alpha = 0.8f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
