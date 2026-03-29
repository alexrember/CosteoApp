package com.mg.costeoapp.feature.onboarding.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val illustrationType: IllustrationType
)

private enum class IllustrationType {
    COINS,
    SCANNER,
    COMPARE,
    FLOW
}

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Filled.AttachMoney,
        title = "Costeo Real",
        subtitle = "Calcula el costo exacto de cada plato",
        description = "Deja de adivinar cuanto te cuesta preparar un platillo. Costeo te da el precio real desglosado por ingrediente.",
        accentColor = Color(0xFFB39DDB),
        illustrationType = IllustrationType.COINS
    ),
    OnboardingPage(
        icon = Icons.Filled.QrCodeScanner,
        title = "Escanea y registra",
        subtitle = "Precios de Walmart, PriceSmart y mas",
        description = "Escanea el codigo de barras de tus productos y registra sus precios por tienda. Siempre tendras el precio actualizado.",
        accentColor = Color(0xFF8BC34A),
        illustrationType = IllustrationType.SCANNER
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.CompareArrows,
        title = "Compara y ahorra",
        subtitle = "Recetas, platos y comparador de precios",
        description = "Arma tus recetas, compara precios entre tiendas y descubre donde te sale mas barato cada ingrediente.",
        accentColor = Color(0xFF4FC3F7),
        illustrationType = IllustrationType.COMPARE
    ),
    OnboardingPage(
        icon = Icons.Filled.Restaurant,
        title = "Asi de facil",
        subtitle = "Selecciona tienda, escanea, registra, compara",
        description = "En 4 pasos tendras control total de tus costos. Empecemos.",
        accentColor = Color(0xFFFFB74D),
        illustrationType = IllustrationType.FLOW
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Skip button at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = "Saltar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    isVisible = pagerState.currentPage == page
                )
            }

            // Bottom section: indicators + button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        PageIndicator(
                            isSelected = index == pagerState.currentPage,
                            color = pages[pagerState.currentPage].accentColor
                        )
                        if (index < pages.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action button
                if (isLastPage) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[pagerState.currentPage].accentColor
                        )
                    ) {
                        Text(
                            text = "Comenzar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF121212)
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Siguiente",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    isSelected: Boolean,
    color: Color
) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 32.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicator_width"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(300),
        label = "indicator_color"
    )

    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(indicatorColor)
    )
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isVisible: Boolean
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = tween(400),
        label = "content_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .alpha(contentAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration area
        OnboardingIllustration(
            type = page.illustrationType,
            accentColor = page.accentColor,
            icon = page.icon
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(500)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(500)
            )
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = page.accentColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 100)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(500, delayMillis = 100)
            )
        ) {
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(500, delayMillis = 200)
            )
        ) {
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun OnboardingIllustration(
    type: IllustrationType,
    accentColor: Color,
    icon: ImageVector
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background decorative canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (type) {
                IllustrationType.COINS -> drawCoinsIllustration(accentColor, surfaceVariant, outline)
                IllustrationType.SCANNER -> drawScannerIllustration(accentColor, surfaceVariant, outline)
                IllustrationType.COMPARE -> drawCompareIllustration(accentColor, surfaceVariant, outline)
                IllustrationType.FLOW -> drawFlowIllustration(accentColor, surfaceVariant, outline)
            }
        }

        // Central icon
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            color = accentColor.copy(alpha = 0.15f),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = accentColor
                )
            }
        }
    }
}

// --- Canvas illustration functions ---

private fun DrawScope.drawCoinsIllustration(
    accent: Color,
    surface: Color,
    outline: Color
) {
    val centerX = size.width / 2
    val centerY = size.height / 2

    // Outer ring
    drawCircle(
        color = accent.copy(alpha = 0.08f),
        radius = size.minDimension / 2,
        center = Offset(centerX, centerY)
    )

    // Middle ring
    drawCircle(
        color = accent.copy(alpha = 0.05f),
        radius = size.minDimension / 2.8f,
        center = Offset(centerX, centerY)
    )

    // Decorative coins (small circles) orbiting
    val coinPositions = listOf(
        Offset(centerX - 70f, centerY - 75f),
        Offset(centerX + 75f, centerY - 55f),
        Offset(centerX - 80f, centerY + 60f),
        Offset(centerX + 70f, centerY + 70f),
        Offset(centerX + 10f, centerY - 90f),
        Offset(centerX - 30f, centerY + 85f)
    )

    coinPositions.forEachIndexed { index, pos ->
        val coinSize = if (index % 2 == 0) 14f else 10f
        drawCircle(
            color = accent.copy(alpha = 0.3f + (index * 0.05f)),
            radius = coinSize,
            center = pos
        )
        // Dollar sign dash inside bigger coins
        if (coinSize > 12f) {
            drawLine(
                color = accent.copy(alpha = 0.6f),
                start = Offset(pos.x, pos.y - 6f),
                end = Offset(pos.x, pos.y + 6f),
                strokeWidth = 2f
            )
        }
    }

    // Sparkle dots
    val sparkles = listOf(
        Offset(centerX - 55f, centerY - 40f),
        Offset(centerX + 50f, centerY + 20f),
        Offset(centerX + 30f, centerY - 80f)
    )
    sparkles.forEach { pos ->
        drawCircle(color = accent.copy(alpha = 0.4f), radius = 3f, center = pos)
    }
}

private fun DrawScope.drawScannerIllustration(
    accent: Color,
    surface: Color,
    outline: Color
) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val frameSize = size.minDimension * 0.7f

    // Outer glow
    drawCircle(
        color = accent.copy(alpha = 0.06f),
        radius = size.minDimension / 2,
        center = Offset(centerX, centerY)
    )

    // Scanner frame corners
    val cornerLen = 30f
    val cornerStroke = 4f
    val halfFrame = frameSize / 2
    val left = centerX - halfFrame
    val top = centerY - halfFrame
    val right = centerX + halfFrame
    val bottom = centerY + halfFrame

    val cornerColor = accent.copy(alpha = 0.5f)

    // Top-left corner
    drawLine(cornerColor, Offset(left, top + cornerLen), Offset(left, top), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = cornerStroke)

    // Top-right corner
    drawLine(cornerColor, Offset(right - cornerLen, top), Offset(right, top), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = cornerStroke)

    // Bottom-left corner
    drawLine(cornerColor, Offset(left, bottom - cornerLen), Offset(left, bottom), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = cornerStroke)

    // Bottom-right corner
    drawLine(cornerColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(right, bottom - cornerLen), Offset(right, bottom), strokeWidth = cornerStroke)

    // Barcode lines
    val barcodeTop = centerY - 20f
    val barcodeBottom = centerY + 20f
    val barWidths = listOf(3f, 2f, 4f, 2f, 3f, 1f, 4f, 2f, 3f, 2f, 4f, 1f, 3f, 2f, 4f)
    var xOffset = centerX - 45f
    barWidths.forEach { w ->
        drawLine(
            color = outline.copy(alpha = 0.4f),
            start = Offset(xOffset, barcodeTop),
            end = Offset(xOffset, barcodeBottom),
            strokeWidth = w
        )
        xOffset += w + 3f
    }

    // Scan line
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                accent.copy(alpha = 0.6f),
                accent,
                accent.copy(alpha = 0.6f),
                Color.Transparent
            )
        ),
        start = Offset(left + 15f, centerY + 30f),
        end = Offset(right - 15f, centerY + 30f),
        strokeWidth = 2.5f
    )
}

private fun DrawScope.drawCompareIllustration(
    accent: Color,
    surface: Color,
    outline: Color
) {
    val centerX = size.width / 2
    val centerY = size.height / 2

    // Background circle
    drawCircle(
        color = accent.copy(alpha = 0.06f),
        radius = size.minDimension / 2,
        center = Offset(centerX, centerY)
    )

    // Left card
    drawRoundRect(
        color = surface.copy(alpha = 0.8f),
        topLeft = Offset(centerX - 90f, centerY - 55f),
        size = Size(70f, 90f),
        cornerRadius = CornerRadius(12f)
    )
    // Left card price lines
    drawRoundRect(
        color = accent.copy(alpha = 0.3f),
        topLeft = Offset(centerX - 82f, centerY - 42f),
        size = Size(54f, 6f),
        cornerRadius = CornerRadius(3f)
    )
    drawRoundRect(
        color = outline.copy(alpha = 0.3f),
        topLeft = Offset(centerX - 82f, centerY - 30f),
        size = Size(40f, 4f),
        cornerRadius = CornerRadius(2f)
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.5f),
        topLeft = Offset(centerX - 82f, centerY - 15f),
        size = Size(30f, 10f),
        cornerRadius = CornerRadius(5f)
    )

    // Right card
    drawRoundRect(
        color = surface.copy(alpha = 0.8f),
        topLeft = Offset(centerX + 20f, centerY - 55f),
        size = Size(70f, 90f),
        cornerRadius = CornerRadius(12f)
    )
    // Right card price lines
    drawRoundRect(
        color = accent.copy(alpha = 0.3f),
        topLeft = Offset(centerX + 28f, centerY - 42f),
        size = Size(54f, 6f),
        cornerRadius = CornerRadius(3f)
    )
    drawRoundRect(
        color = outline.copy(alpha = 0.3f),
        topLeft = Offset(centerX + 28f, centerY - 30f),
        size = Size(40f, 4f),
        cornerRadius = CornerRadius(2f)
    )
    drawRoundRect(
        color = Color(0xFF8BC34A).copy(alpha = 0.5f),
        topLeft = Offset(centerX + 28f, centerY - 15f),
        size = Size(30f, 10f),
        cornerRadius = CornerRadius(5f)
    )

    // Arrows between cards
    drawLine(
        color = accent.copy(alpha = 0.4f),
        start = Offset(centerX - 12f, centerY - 18f),
        end = Offset(centerX + 12f, centerY - 18f),
        strokeWidth = 2f
    )
    drawLine(
        color = accent.copy(alpha = 0.4f),
        start = Offset(centerX + 12f, centerY - 5f),
        end = Offset(centerX - 12f, centerY - 5f),
        strokeWidth = 2f
    )

    // Bottom bar chart
    val barColors = listOf(accent.copy(alpha = 0.6f), Color(0xFF8BC34A).copy(alpha = 0.6f), accent.copy(alpha = 0.4f))
    val barHeights = listOf(35f, 50f, 25f)
    val barStartX = centerX - 40f
    barColors.forEachIndexed { index, color ->
        val x = barStartX + index * 30f
        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY + 60f - barHeights[index]),
            size = Size(18f, barHeights[index]),
            cornerRadius = CornerRadius(4f)
        )
    }
}

private fun DrawScope.drawFlowIllustration(
    accent: Color,
    surface: Color,
    outline: Color
) {
    val centerX = size.width / 2
    val centerY = size.height / 2

    // Background circle
    drawCircle(
        color = accent.copy(alpha = 0.06f),
        radius = size.minDimension / 2,
        center = Offset(centerX, centerY)
    )

    // 4 step circles arranged in a diamond/flow pattern
    val stepPositions = listOf(
        Offset(centerX, centerY - 80f),
        Offset(centerX + 80f, centerY - 10f),
        Offset(centerX, centerY + 60f),
        Offset(centerX - 80f, centerY - 10f)
    )

    val stepColors = listOf(
        Color(0xFFB39DDB),
        Color(0xFF8BC34A),
        Color(0xFF4FC3F7),
        accent
    )

    // Connecting lines
    for (i in 0 until stepPositions.size) {
        val next = (i + 1) % stepPositions.size
        drawLine(
            color = outline.copy(alpha = 0.25f),
            start = stepPositions[i],
            end = stepPositions[next],
            strokeWidth = 2f
        )
    }

    // Step number circles
    stepPositions.forEachIndexed { index, pos ->
        drawCircle(
            color = stepColors[index].copy(alpha = 0.2f),
            radius = 22f,
            center = pos
        )
        drawCircle(
            color = stepColors[index].copy(alpha = 0.5f),
            radius = 14f,
            center = pos
        )
    }

    // Small decorative dots
    val decorDots = listOf(
        Offset(centerX - 40f, centerY - 65f),
        Offset(centerX + 55f, centerY - 50f),
        Offset(centerX + 50f, centerY + 40f),
        Offset(centerX - 55f, centerY + 35f)
    )
    decorDots.forEach { pos ->
        drawCircle(color = accent.copy(alpha = 0.15f), radius = 5f, center = pos)
    }
}
