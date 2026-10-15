package com.bodla.parivar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.composeapp.generated.resources.Res
import com.bodla.parivar.composeapp.generated.resources.bodla_parivar_mark
import org.jetbrains.compose.resources.painterResource
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.features.main.MainScreen
import kotlinx.coroutines.delay

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.6f) }
    val motion = rememberInfiniteTransition(label = "brand-splash-motion")
    val logoPulse by motion.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100), repeatMode = RepeatMode.Reverse),
        label = "logo-pulse"
    )
    val orbitAngle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing)),
        label = "logo-orbit"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        delay(1100) // Show splash for 1.1 seconds
        showSplash = false
    }

    BodlaParivarTheme {
        Box(modifier = Modifier.fillMaxSize().background(CreamBackground)) {
            MainScreen()

            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(WarmWhiteSurface, CreamBackground, LightIconBg)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(210.dp).rotate(orbitAngle),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = GoldenAccent.copy(alpha = 0.45f),
                                    style = Stroke(width = 2.dp.toPx()),
                                    radius = size.minDimension * 0.43f
                                )
                                drawCircle(
                                    color = SaffronPrimary,
                                    radius = 6.dp.toPx(),
                                    center = Offset(x = size.width / 2f, y = size.height * 0.07f)
                                )
                                drawCircle(
                                    color = LeafGreenSecondary,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x = size.width * 0.15f, y = size.height * 0.78f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(142.dp)
                                    .scale(scale.value * logoPulse)
                                    .shadow(20.dp, CircleShape)
                                    .background(WarmWhiteSurface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.bodla_parivar_mark),
                                    contentDescription = "Bodla Parivar logo",
                                    modifier = Modifier.size(112.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(26.dp))
                        Text(
                            text = "બોદલા પરિવાર",
                            style = BodlaPageTitle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bodla Parivar",
                            style = BodlaCaption.copy(fontSize = 16.sp),
                            color = SaffronPrimary
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { index ->
                                val dotScale = if ((orbitAngle / 120f).toInt() % 3 == index) 1.3f else 0.8f
                                Box(
                                    Modifier.size((7 * dotScale).dp).background(
                                        if (index == 1) LeafGreenSecondary else SaffronPrimary.copy(alpha = 0.72f),
                                        CircleShape
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
