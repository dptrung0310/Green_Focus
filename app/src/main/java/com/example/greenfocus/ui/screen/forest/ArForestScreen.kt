package com.example.greenfocus.ui.screen.forest

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.data.model.TreeResourceMapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.greenfocus.R
import com.example.greenfocus.data.model.TreeType
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

data class PlacedTree(
    val anchor: Anchor,
    val treeId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArForestScreen(
    onNavigateBack: () -> Unit,
    forestViewModel: ForestViewModel = viewModel(factory = ForestViewModel.Factory)
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAF7EC)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Quyền truy cập Camera",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "Ứng dụng cần quyền Camera để sử dụng tính năng Thực tế tăng cường (AR) hiển thị cây trồng.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Cấp quyền Camera")
                }
            }
        }
    } else {
        ArSceneContent(
            onNavigateBack = onNavigateBack,
            forestViewModel = forestViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArSceneContent(
    onNavigateBack: () -> Unit,
    forestViewModel: ForestViewModel
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    val sessionState = remember { mutableStateOf<Session?>(null) }
    val frameState = remember { mutableStateOf<Frame?>(null) }

    val placedTrees = remember { mutableStateListOf<PlacedTree>() }

    var planesTracked by remember { mutableStateOf(0) }
    var trackingStateTxt by remember { mutableStateOf("CHƯA BẮT ĐẦU") }
    var failureReasonTxt by remember { mutableStateOf("-") }
    var featurePointCount by remember { mutableStateOf(0) }
    var debugLog by remember { mutableStateOf("Di chuyển camera lên mặt sàn/bàn...") }
    var hitInfo by remember { mutableStateOf("Chưa đặt") }
    var sessionError by remember { mutableStateOf<String?>(null) }

    val uiState by forestViewModel.forestUiState.collectAsState()
    val liveSessions = remember(uiState.treeList) {
        uiState.treeList.filter { it.status == "ALIVE" }
    }

    var forestPlaced by remember { mutableStateOf(false) }
    var showPlaneRenderer by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (sessionError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("❌ AR không khởi động được", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(sessionError ?: "", color = Color.White, fontSize = 13.sp)
                    Button(
                        onClick = { onNavigateBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Quay lại") }
                }
            }
            return
        }

        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            planeRenderer = showPlaneRenderer,
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }
            },
            onSessionUpdated = { session, frame ->
                sessionState.value = session
                frameState.value = frame
                val planes = session.getAllTrackables(Plane::class.java)
                planesTracked = planes.count { it.trackingState == TrackingState.TRACKING }

                val cam = frame.camera
                trackingStateTxt = cam.trackingState.name
                failureReasonTxt = when (cam.trackingFailureReason) {
                    TrackingFailureReason.NONE             -> if (cam.trackingState == TrackingState.TRACKING) "OK" else "PAUSED"
                    TrackingFailureReason.INSUFFICIENT_LIGHT   -> "⚠️ TỐI QUÁ"
                    TrackingFailureReason.EXCESSIVE_MOTION     -> "⚠️ DI CHUYỂN NHANH"
                    TrackingFailureReason.INSUFFICIENT_FEATURES -> "⚠️ BỀ MẶT ĐƠN ĐIỆU"
                    TrackingFailureReason.BAD_STATE             -> "⚠️ KHỞI ĐẦU SAI"
                    TrackingFailureReason.CAMERA_UNAVAILABLE   -> "⚠️ CAM BỊ CHIẾM"
                    else                                       -> cam.trackingFailureReason.name
                }
                try {
                    frame.acquirePointCloud().use { cloud ->
                        featurePointCount = cloud.points.limit() / 4
                    }
                } catch (_: Exception) {}

                if (!forestPlaced && uiState.isLoaded && planesTracked > 0 && liveSessions.isNotEmpty()) {
                    val plane = planes.firstOrNull { it.trackingState == TrackingState.TRACKING }
                    if (plane != null) {
                        val centerPose = plane.centerPose
                        val N = liveSessions.size
                        val cols = when {
                            N <= 3 -> N
                            N <= 8 -> 3
                            else -> 4
                        }
                        val spacing = 0.5f
                        
                        liveSessions.forEachIndexed { index, sessionItem ->
                            val row = index / cols
                            val col = index % cols
                            val dx = (col - (cols - 1) / 2.0f) * spacing
                            val dz = (row - (N / cols) / 2.0f) * spacing
                            val angleRad = (Math.random() * 2 * Math.PI).toFloat()
                            val qy = Math.sin(angleRad / 2.0).toFloat()
                            val qw = Math.cos(angleRad / 2.0).toFloat()
                            val rotation = Pose.makeRotation(0f, qy, 0f, qw)
                            val relativePose = Pose.makeTranslation(dx, 0f, dz).compose(rotation)
                            val finalPose = centerPose.compose(relativePose)
                            val anchor = plane.createAnchor(finalPose)
                            placedTrees.add(PlacedTree(anchor, sessionItem.treeId))
                        }
                        forestPlaced = true
                        showPlaneRenderer = false
                        hitInfo = "Tự động sắp xếp $N cây thành công"
                        debugLog = "Đã tự động hiển thị khu rừng"
                        Toast.makeText(context, "Đã hiển thị khu rừng thành công! 🌳", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onTouchEvent = { _, _ -> false },
            onSessionFailed = { exception ->
                sessionError = exception.message ?: exception.javaClass.simpleName
            }
        ) {
            for (placedTree in placedTrees) {
                key(placedTree.anchor) {
                    AnchorNode(anchor = placedTree.anchor) {
                        val modelInstance = rememberModelInstance(
                            modelLoader = modelLoader,
                            fileLocation = getModelPath(placedTree.treeId)
                        )
                        if (modelInstance != null) {
                            ModelNode(
                                modelInstance = modelInstance,
                                scaleToUnits = 0.35f
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = when {
                    !uiState.isLoaded ->
                        "⏳ Đang tải dữ liệu khu rừng..."
                    liveSessions.isEmpty() ->
                        "⚠️ Bạn chưa có cây sống nào trong lịch sử. Hãy tập trung trồng cây trước!"
                    forestPlaced ->
                        "🌟 Đã hiển thị khu rừng thành công!"
                    planesTracked > 0 ->
                        "✅ Phát hiện mặt phẳng – Đang dựng cây..."
                    featurePointCount > 20 ->
                        "🔍 Đang nhận diện mặt sàn... hãy quét camera xung quanh"
                    else ->
                        "🔄 Quét camera chậm xuống nền gạch hoặc mặt bàn có vân..."
                },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (planesTracked == 0 && failureReasonTxt.contains("BỀ MẶT")) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 200.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xFFE65100).copy(alpha = 0.9f), shape = RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    "💡 Tường trơn hoặc bề mặt quá tối khó quét.\nHãy hướng xuống sàn nhà hoặc mặt bàn có vân.",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        var showDebug by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 150.dp, start = 12.dp)
        ) {
            if (showDebug) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.70f), shape = RoundedCornerShape(12.dp))
                        .border(width = 1.dp, color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .width(200.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Debug Console", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Tracking: $trackingStateTxt",
                            color = if (trackingStateTxt == "TRACKING") Color.Green else Color(0xFFFFAB40),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Reason: $failureReasonTxt",
                            color = if (failureReasonTxt == "OK") Color.Green else Color(0xFFFF6E40),
                            fontSize = 11.sp)
                        Text("Features: $featurePointCount",
                            color = if (featurePointCount > 30) Color.Green else Color(0xFFFFAB40),
                            fontSize = 11.sp)
                        Text("Planes: $planesTracked",
                            color = if (planesTracked > 0) Color.Green else Color.Red,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Live Trees: ${liveSessions.size}", color = Color.Cyan, fontSize = 11.sp)
                        Text("Rendered: ${placedTrees.size}", color = Color.Magenta, fontSize = 11.sp)
                        Text(hitInfo, color = Color.Yellow, fontSize = 10.sp)
                        Text(debugLog, color = Color.White, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ẩn Console",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showDebug = false }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp))
                        .clickable { showDebug = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Hiện Debug", color = Color.White, fontSize = 10.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (forestPlaced) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .border(width = 1.dp, color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .clickable {
                            placedTrees.forEach { it.anchor.detach() }
                            placedTrees.clear()
                            forestPlaced = false
                            showPlaneRenderer = true
                            debugLog = "Vui lòng quét mặt phẳng mới..."
                            hitInfo = "Đã dọn dẹp"
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sắp xếp lại",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Sắp xếp lại", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Khu Rừng Của Bạn",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Danh sách cây sống đang hiển thị trong AR",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2E7D32), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${liveSessions.size} Cây",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!uiState.isLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF81C784),
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (liveSessions.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFB71C1C).copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Bạn chưa có cây sống nào. Hãy quay lại trồng cây để ngắm nhìn thành quả nhé!",
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        val treeCounts = remember(liveSessions) {
                            liveSessions.groupBy { it.treeId }.mapValues { it.value.size }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(treeCounts.toList()) { (treeId, count) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                                        .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = TreeResourceMapper.getBigDrawable(treeId)),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(id = TreeResourceMapper.getNameResId(treeId)),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "x$count",
                                            color = Color(0xFF81C784),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Float.format() = "%.2f".format(this)

private fun getModelPath(treeId: String): String {
    return when (treeId) {
        "default_oak" -> "models/oak.glb"
        "pine"        -> "models/pine.glb"
        "maple"       -> "models/mapple.glb"
        "palm"        -> "models/palm.glb"
        "cactus"      -> "models/cactus.glb"
        "bamboo"      -> "models/bamboo.glb"
        "sakura"      -> "models/sakura.glb"
        else          -> "models/oak.glb"
    }
}
