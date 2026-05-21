package com.example.greenfocus.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hàm hỗ trợ giải mã mọi loại Drawable (WebP, PNG, Vector XML) sang Bitmap để xử lý biến dạng Mesh.
 */
fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    // Nếu là VectorDrawable (ví dụ dead_tree.xml), vẽ vector đó lên Bitmap
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

/**
 * Lớp hạt mô phỏng vật lý cho lá cây rơi lơ lửng xung quanh cây.
 * Tính toán hoàn toàn bằng các định luật cơ học cổ điển: Trọng lực, Gió thổi điều hòa, Ma sát.
 */
class LeafParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    var maxLife: Float,
    var life: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val color: Color
) {
    fun update(width: Float, height: Float, windX: Float, gravityY: Float) {
        // Áp dụng gia tốc trọng lực và lực gió thổi ngang
        vx += windX
        vy += gravityY

        // Áp dụng ma sát không khí (Drag) để hạt không rơi quá nhanh
        vx *= 0.96f
        vy *= 0.96f

        // Cập nhật tọa độ
        x += vx
        y += vy

        // Xoay lá cây
        rotation += rotationSpeed
        
        // Giảm thời gian sống
        life -= 1f
        alpha = (life / maxLife).coerceIn(0f, 1f)

        // Respawn nếu hạt bay ra ngoài biên hoặc hết tuổi thọ
        if (life <= 0f || x < -20f || x > width + 20f || y > height + 20f) {
            respawn(width, height)
        }
    }

    fun respawn(width: Float, height: Float) {
        val rand = Random()
        // Tạo hạt mới tại khu vực thân cây/gốc cây (ở giữa phía dưới)
        x = width / 2f + (rand.nextFloat() - 0.5f) * 60f
        y = height / 2f + 20f + (rand.nextFloat() - 0.5f) * 30f
        
        // Vận tốc hướng lên trên và hơi sang ngang để tạo độ lan tỏa
        vx = (rand.nextFloat() - 0.5f) * 2.5f
        vy = -rand.nextFloat() * 2.0f - 0.8f
        
        size = rand.nextFloat() * 10f + 6f
        maxLife = rand.nextFloat() * 100f + 100f
        life = maxLife
        alpha = 1.0f
        rotation = rand.nextFloat() * 360f
        rotationSpeed = (rand.nextFloat() - 0.5f) * 6f
    }
}

@Composable
fun Interactive3DCard(
    treeImageRes: Int,
    modifier: Modifier = Modifier,
    cardColor: Color = Color(0xFFFFFFFF),
    glowColor: Color = Color(0xFF81C784)
) {
    // Trạng thái theo dõi người dùng có đang kéo vuốt hay không
    var isDragging by remember { mutableStateOf(false) }

    // Góc nghiêng 3D thực tế của card
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    // Kích thước của Card để tính toán phần trăm kéo vuốt
    var cardSize by remember { mutableStateOf(IntSize(0, 0)) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Biến lưu thời gian chạy mô phỏng vật lý
    var frameTime by remember { mutableStateOf(0f) }
    
    // State trigger để thông báo cho Compose redraw Canvas mỗi frame
    var tickTrigger by remember { mutableStateOf(0L) }

    // Khởi tạo hệ thống hạt tự code (18 hạt lá cây/hạt sáng)
    val particles = remember {
        mutableStateListOf<LeafParticle>().apply {
            val rand = Random()
            repeat(18) {
                add(
                    LeafParticle(
                        x = 0f, y = 0f, vx = 0f, vy = 0f,
                        size = 0f, alpha = 0f, maxLife = 1f, life = 0f,
                        rotation = 0f, rotationSpeed = 0f,
                        color = if (rand.nextBoolean()) Color(0xFF4CAF50) else Color(0xFF81C784)
                    )
                )
            }
        }
    }

    // Vòng lặp cập nhật vật lý hạt liên tục (60 FPS game-loop)
    LaunchedEffect(cardSize) {
        if (cardSize.width <= 0 || cardSize.height <= 0) return@LaunchedEffect
        
        val width = cardSize.width.toFloat()
        val height = cardSize.height.toFloat()

        // Khởi tạo vị trí hạt ban đầu
        particles.forEach { it.respawn(width, height) }

        while (isActive) {
            withFrameNanos { timeNanos ->
                frameTime = timeNanos / 1_000_000_000f
                tickTrigger = timeNanos // Cập nhật để kích hoạt redraw canvas
                
                // Lực gió thay đổi điều hòa theo hàm Sin của thời gian
                val windX = sin(frameTime * 1.5f) * 0.08f
                // Trọng lực kéo nhẹ xuống dưới
                val gravityY = 0.03f

                // Cập nhật từng hạt một
                particles.forEach { particle ->
                    particle.update(width, height, windX, gravityY)
                }
            }
        }
    }

    // Cường độ đung đưa tự động (chỉ đung đưa khi KHÔNG kéo vuốt)
    // Dùng transition mượt 400ms để tránh giật khi bắt đầu vuốt hoặc buông tay
    val swayIntensity by animateFloatAsState(
        targetValue = if (isDragging) 0f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "swayIntensity"
    )

    // Góc đung đưa tự động khi đứng yên (Idle Sway)
    val idleSwayX = sin(frameTime * 1.2f) * 2.2f * swayIntensity
    val idleSwayY = cos(frameTime * 1.5f) * 2.2f * swayIntensity

    val finalRotationX = rotationX.value + idleSwayX
    val finalRotationY = rotationY.value + idleSwayY

    // Quy đổi góc nghiêng thành phần trăm di chuyển lệch giữa các layer (Parallax)
    val tiltPercentX = finalRotationY / 20f  // Từ -1.0 đến 1.0
    val tiltPercentY = -finalRotationX / 20f // Từ -1.0 đến 1.0

    // Coroutine scope để chạy animation song song với drag gesture
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val bitmap = remember(treeImageRes) {
        getBitmapFromDrawable(context, treeImageRes)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(190.dp)
            .onGloballyPositioned { coordinates ->
                cardSize = coordinates.size
            }
            // Lắng nghe sự kiện kéo vuốt của người dùng
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        
                        // Tính toán góc nghiêng mục tiêu dựa trên lượng kéo chuột/tay
                        val targetRotY = (rotationY.value + dragAmount.x * 0.15f).coerceIn(-20f, 20f)
                        val targetRotX = (rotationX.value - dragAmount.y * 0.15f).coerceIn(-20f, 20f)
                        
                        // Cập nhật mượt mà góc xoay bằng Spring Animation
                        val springSpec = spring<Float>(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                        
                        // Chạy coroutine song song để xoay card mượt mà
                        coroutineScope.launch { rotationY.animateTo(targetRotY, springSpec) }
                        coroutineScope.launch { rotationX.animateTo(targetRotX, springSpec) }
                    },
                    onDragEnd = {
                        isDragging = false
                        // Khi thả tay ra, tự động trả Card về vị trí thăng bằng ban đầu
                        val springReturn = spring<Float>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                        coroutineScope.launch { rotationY.animateTo(0f, springReturn) }
                        coroutineScope.launch { rotationX.animateTo(0f, springReturn) }
                    },
                    onDragCancel = {
                        isDragging = false
                        val springReturn = spring<Float>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                        coroutineScope.launch { rotationY.animateTo(0f, springReturn) }
                        coroutineScope.launch { rotationX.animateTo(0f, springReturn) }
                    }
                )
            }
            // Áp dụng phép chiếu xoay không gian 3D trên card
            .graphicsLayer {
                this.rotationX = finalRotationX
                this.rotationY = finalRotationY
                this.cameraDistance = 18f * density.density // Tạo hiệu ứng xa gần rõ rệt
            }
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(cardColor)
    ) {
        // LAYER 1: Shadow lớp nền xoay nhẹ ngược hướng nghiêng để tạo độ sâu
        Box(
            modifier = Modifier
                .size(130.dp)
                .offset(
                    x = with(density) { (-tiltPercentX * 12f).dp },
                    y = with(density) { (-tiltPercentY * 12f).dp }
                )
                .graphicsLayer {
                    alpha = 0.22f
                    scaleX = 1.05f
                    scaleY = 1.05f
                }
                .background(glowColor, shape = RoundedCornerShape(100.dp))
        )

        // LAYER 2: Physics Engine - Vẽ hệ thống hạt lá bay xung quanh cây
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    // Đọc tickTrigger ở đây để Compose biết cần redraw mỗi khi game-loop chạy
                    val tick = tickTrigger
                    drawContent()
                    // Vẽ các lá cây dựa trên tọa độ vật lý được tính toán liên tục
                    particles.forEach { particle ->
                        drawContext.canvas.save()
                        drawContext.canvas.translate(particle.x, particle.y)
                        drawContext.canvas.rotate(particle.rotation)
                        
                        drawCircle(
                            color = particle.color.copy(alpha = particle.alpha),
                            radius = particle.size / 2f,
                            center = Offset.Zero
                        )
                        
                        drawContext.canvas.restore()
                    }
                }
        )

        // LAYER 2.5: Bóng chân cây (Contact Shadow) rõ nét trực quan dưới gốc
        Box(
            modifier = Modifier
                .size(width = 65.dp, height = 10.dp)
                .offset(
                    x = with(density) { (tiltPercentX * 10f).dp },
                    y = with(density) { (tiltPercentY * 10f + 40f).dp } // đặt ở chân cây
                )
                .graphicsLayer {
                    alpha = 0.35f
                    scaleX = 1f + (tiltPercentY * 0.15f)
                    scaleY = 1f - (tiltPercentY * 0.15f)
                }
                .background(Color(0xFF1E281E).copy(alpha = 0.7f), shape = RoundedCornerShape(100.dp))
        )

        // LAYER 3: Cây 3D nổi lên phía trước với hiệu ứng uốn lượn lá cây (Mesh Deformation)
        if (bitmap != null) {
            Canvas(
                modifier = Modifier
                    .size(110.dp)
                    .offset(
                        x = with(density) { (tiltPercentX * 16f).dp },
                        y = with(density) { (tiltPercentY * 16f).dp }
                    )
                    .graphicsLayer {
                        // Cây hơi nghiêng nhẹ độc lập tạo cảm giác 3D tách biệt khỏi thẻ bài
                        this.rotationY = tiltPercentX * 6f
                        this.rotationX = -tiltPercentY * 6f
                        // Lắc lư trục Z nhẹ nhàng cả cây
                        this.rotationZ = sin(frameTime * 2.0f) * 1.5f
                    }
            ) {
                // Đăng ký redraw theo game-loop bằng cách đọc tickTrigger
                val tick = tickTrigger
                
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    
                    // Thiết lập lưới Mesh ô vuông (6 cột x 6 hàng = 49 đỉnh)
                    val meshWidth = 6
                    val meshHeight = 6
                    val totalVertices = (meshWidth + 1) * (meshHeight + 1)
                    
                    val w = size.width
                    val h = size.height
                    val bmpW = bitmap.width.toFloat()
                    val bmpH = bitmap.height.toFloat()
                    
                    val scaleX = w / bmpW
                    val scaleY = h / bmpH
                    
                    // Lưu trạng thái canvas để scale về kích thước của Box Compose (110.dp)
                    nativeCanvas.save()
                    nativeCanvas.scale(scaleX, scaleY)
                    
                    // Khởi tạo tọa độ đỉnh trong không gian Pixel gốc của Bitmap
                    val bmpVerts = FloatArray(totalVertices * 2)
                    
                    for (r in 0..meshHeight) {
                        val progressY = r.toFloat() / meshHeight
                        // heightFactor = 1.0 ở ngọn cây (progressY=0), và bằng 0.0 ở gốc cây (progressY=1.0)
                        val heightFactor = 1f - progressY
                        
                        // Độ uốn lượn ngang (Gió thổi xô lệch lá cây)
                        // progressY * 2.5f tạo ra độ trễ pha pha sóng theo chiều dọc (wavy wind effect)
                        val swayX = sin(frameTime * 3.0f + progressY * 2.5f) * (18f / scaleX) * heightFactor
                        // Nhấp nhô nhẹ trục đứng của lá
                        val swayY = cos(frameTime * 2.5f) * (2f / scaleY) * heightFactor
                        
                        val targetY = bmpH * progressY
                        
                        for (c in 0..meshWidth) {
                            val progressX = c.toFloat() / meshWidth
                            val targetX = bmpW * progressX
                            
                            val index = (r * (meshWidth + 1) + c) * 2
                            bmpVerts[index] = targetX + swayX
                            bmpVerts[index + 1] = targetY + swayY
                        }
                    }
                    
                    // Vẽ bitmap biến dạng bằng thuật toán Mesh gốc của Android OS
                    nativeCanvas.drawBitmapMesh(
                        bitmap,
                        meshWidth,
                        meshHeight,
                        bmpVerts,
                        0,
                        null,
                        0,
                        null
                    )
                    
                    nativeCanvas.restore()
                }
            }
        }

        // LAYER 4: Dynamic Specular Sheen (Luồng sáng lướt qua thẻ bài khi xoay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()

                    // Tính toán luồng sáng quét theo tọa độ kéo thẻ bài
                    val sheenPosition = (tiltPercentX + tiltPercentY) * size.width
                    val startOffset = Offset(sheenPosition - 80f, -80f)
                    val endOffset = Offset(sheenPosition + 120f, size.height + 80f)

                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        start = startOffset,
                        end = endOffset
                    )

                    // Vẽ luồng sáng quét chồng lên với BlendMode đặc biệt
                    drawRect(
                        brush = brush,
                        blendMode = BlendMode.Overlay
                    )
                }
        )
    }
}
