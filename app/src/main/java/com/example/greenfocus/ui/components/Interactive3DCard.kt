package com.example.greenfocus.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

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
    // Góc nghiêng 3D thực tế của card
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    // Kích thước của Card để tính toán phần trăm kéo vuốt
    var cardSize by remember { mutableStateOf(IntSize(0, 0)) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Biến lưu thời gian chạy mô phỏng vật lý
    var frameTime by remember { mutableStateOf(0f) }

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

    // Quy đổi góc nghiêng thành phần trăm di chuyển lệch giữa các layer (Parallax)
    val tiltPercentX = rotationY.value / 20f  // Từ -1.0 đến 1.0
    val tiltPercentY = -rotationX.value / 20f // Từ -1.0 đến 1.0

    // Coroutine scope để chạy animation song song với drag gesture
    val coroutineScope = rememberCoroutineScope()

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
                        // Khi thả tay ra, tự động trả Card về vị trí thăng bằng ban đầu
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
                this.rotationX = rotationX.value
                this.rotationY = rotationY.value
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

        // LAYER 3: Cây 3D nổi lên phía trước (Dịch chuyển thuận chiều với góc nghiêng)
        Image(
            painter = painterResource(id = treeImageRes),
            contentDescription = "3D Interactive Tree",
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
                }
        )

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
