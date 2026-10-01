package com.legendoftheglassdragon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legendoftheglassdragon.R
import com.legendoftheglassdragon.audio.LocalSelectionSound
import com.legendoftheglassdragon.ui.theme.AnsiColor

@Immutable
data class AnsiSpan(
    val text: String,
    val color: Color = AnsiColor.LightGray,
    val bold: Boolean = false,
)

enum class TerminalFrameTone {
    GLASS,
    NEUTRAL,
    GOLD,
    DANGER,
}

enum class LocationArt {
    TOWN,
    FOREST,
    INN,
    TRAINING,
    HEALER,
    WEAPON_SHOP,
    ARMOR_SHOP,
    BANK,
}

enum class MajorEventArt {
    NEW_DAY,
    VICTORY,
    DEFEAT,
    LEVEL_UP,
}

@Composable
fun TerminalSurface(
    modifier: Modifier = Modifier,
    frameTone: TerminalFrameTone = TerminalFrameTone.GLASS,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AnsiColor.Black),
    ) {
        val horizontalPadding = if (maxWidth >= 600.dp) 28.dp else 10.dp
        val innerPadding = if (maxWidth >= 600.dp) 18.dp else 12.dp
        val scrollState = rememberScrollState()
        val frameColor = frameColor(frameTone)

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 920.dp)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = horizontalPadding, vertical = 10.dp)
                .ansiFrame(frameColor),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = innerPadding, vertical = 14.dp),
                verticalArrangement = Arrangement.Top,
                content = content,
            )
        }
    }
}

private fun Modifier.ansiFrame(color: Color): Modifier = drawBehind {
    val hairline = 1.dp.toPx()
    val corner = 18.dp.toPx()
    val inset = 4.dp.toPx()
    drawRect(color.copy(alpha = 0.48f), style = Stroke(width = hairline))
    drawRect(
        AnsiColor.DimGray.copy(alpha = 0.22f),
        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(
            (size.width - inset * 2).coerceAtLeast(0f),
            (size.height - inset * 2).coerceAtLeast(0f),
        ),
        style = Stroke(width = hairline),
    )
    listOf(
        0f to 0f,
        size.width to 0f,
        0f to size.height,
        size.width to size.height,
    ).forEach { (x, y) ->
        val horizontalEnd = if (x == 0f) corner else size.width - corner
        val verticalEnd = if (y == 0f) corner else size.height - corner
        drawLine(color, start = androidx.compose.ui.geometry.Offset(x, y), end = androidx.compose.ui.geometry.Offset(horizontalEnd, y), strokeWidth = 2.dp.toPx())
        drawLine(color, start = androidx.compose.ui.geometry.Offset(x, y), end = androidx.compose.ui.geometry.Offset(x, verticalEnd), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun TerminalText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AnsiColor.LightGray,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
    )
}

@Composable
fun TerminalMessageSlot(
    message: String?,
    modifier: Modifier = Modifier,
    color: Color = AnsiColor.Magenta,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        message?.let { TerminalText(it, color = color) }
    }
}

@Composable
fun AnsiText(
    spans: List<AnsiSpan>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign? = null,
) {
    val annotated: AnnotatedString = buildAnnotatedString {
        spans.forEach { span ->
            pushStyle(
                SpanStyle(
                    color = span.color,
                    fontWeight = if (span.bold) FontWeight.Bold else FontWeight.Normal,
                ),
            )
            append(span.text)
            pop()
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        textAlign = textAlign,
    )
}

@Composable
fun TerminalMenuRow(
    number: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    note: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val selectionSound = LocalSelectionSound.current
    val activeColor = if (isPressed) AnsiColor.White else AnsiColor.IceBlue
    val outlineColor = when {
        !enabled -> AnsiColor.DimGray.copy(alpha = 0.2f)
        isPressed -> AnsiColor.White
        else -> AnsiColor.Cyan.copy(alpha = 0.32f)
    }
    val pressColor = if (isPressed) AnsiColor.Cyan.copy(alpha = 0.2f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .heightIn(min = 52.dp)
            .background(pressColor)
            .border(1.dp, outlineColor, RectangleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    selectionSound.playSelection()
                    onClick()
                },
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        AnsiText(
            spans = buildList {
                add(AnsiSpan(if (isPressed) "► " else "◆ ", if (enabled) activeColor else AnsiColor.DimGray))
                add(AnsiSpan("[", AnsiColor.DimGray))
                add(AnsiSpan(number.toString(), if (enabled) AnsiColor.Gold else AnsiColor.DimGray, bold = true))
                add(AnsiSpan("] ", AnsiColor.DimGray))
                add(AnsiSpan(label, if (enabled) activeColor else AnsiColor.DimGray, bold = isPressed))
                note?.let { add(AnsiSpan("  $it", if (enabled) AnsiColor.Silver else AnsiColor.DimGray)) }
            },
        )
    }
}

@Composable
fun TerminalRule(
    modifier: Modifier = Modifier,
    color: Color = AnsiColor.DimGray,
    character: Char = '─',
) {
    Text(
        text = character.toString().repeat(80),
        modifier = modifier.fillMaxWidth(),
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

@Composable
fun TerminalBox(
    lines: List<String>,
    modifier: Modifier = Modifier,
    color: Color = AnsiColor.Cyan,
) {
    val interiorWidth = 28
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        TerminalText("╔${"═".repeat(interiorWidth + 2)}╗", color = color)
        lines.forEach { line ->
            val clipped = line.take(interiorWidth)
            TerminalText("║ ${clipped.padEnd(interiorWidth)} ║", color = color)
        }
        TerminalText("╚${"═".repeat(interiorWidth + 2)}╝", color = color)
    }
}

@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    tone: TerminalFrameTone = TerminalFrameTone.NEUTRAL,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, frameColor(tone).copy(alpha = 0.72f), RectangleShape)
            .background(AnsiColor.PanelBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

@Composable
fun GlassDragonCrest(
    modifier: Modifier = Modifier,
    sizeDp: Int = 92,
) {
    Image(
        bitmap = ImageBitmap.imageResource(R.drawable.glass_dragon_crest),
        contentDescription = "Glass Dragon crest",
        modifier = modifier.size(sizeDp.dp),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

@Composable
fun GlassDragonTitleBanner(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.5f),
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.ansi_title_ornament),
            contentDescription = "Pixel ANSI dragon, crystal, and sword title ornament",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
        )
        val compact = maxWidth < 480.dp
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.68f)
                .background(AnsiColor.Black.copy(alpha = 0.86f))
                .padding(vertical = if (compact) 2.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TerminalText(
                text = "LEGEND OF THE",
                color = AnsiColor.Gold,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 11.sp else 16.sp,
                    letterSpacing = if (compact) 1.sp else 2.sp,
                ),
                textAlign = TextAlign.Center,
            )
            TerminalText(
                text = "GLASS DRAGON",
                color = AnsiColor.IceBlue,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (compact) 20.sp else 32.sp,
                    lineHeight = if (compact) 23.sp else 36.sp,
                    letterSpacing = if (compact) 1.sp else 2.sp,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun LocationPlate(
    location: LocationArt,
    modifier: Modifier = Modifier,
) {
    val (lines, color) = when (location) {
        LocationArt.TOWN -> listOf("╔╦╗  ◇  ╔╦╗", "╚╩╩══╦══╩╩╝", "▒ VILLAGE SQUARE ▒") to AnsiColor.Gold
        LocationArt.FOREST -> listOf(" ▲  ▲ ◆ ▲  ▲ ", "/▓\\/▒\\│/▓\\/▒\\", "══ DEEP FOREST ══") to AnsiColor.Green
        LocationArt.INN -> listOf("┌─┐  ◆  ┌─┐", "└┬┘╔═══╗└┬┘", "▒ DRAGON'S REST ▒") to AnsiColor.Gold
        LocationArt.TRAINING -> listOf("  ╲╱ ◆ ╲╱  ", "══╳═══╳══", "  TRAINING HALL  ") to AnsiColor.Red
        LocationArt.HEALER -> listOf("   ◇ ╬ ◇   ", "╔══╩═╩══╗", "  HEALER'S LAMP  ") to AnsiColor.Cyan
        LocationArt.WEAPON_SHOP -> listOf("  ╲╱  ◆  ╲╱  ", "══╳═════╳══", "  WEAPON SMITH  ") to AnsiColor.Silver
        LocationArt.ARMOR_SHOP -> listOf("  ╔═◇═╗  ", "  ║ █ ║  ", "  ARMORER  ") to AnsiColor.IceBlue
        LocationArt.BANK -> listOf("╔═◇═╦═◇═╗", "║ ▓ GOLD ▓ ║", "╚═══╩═══╝") to AnsiColor.Gold
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        lines.forEach { line ->
            TerminalText(
                text = line,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MajorEventBanner(
    event: MajorEventArt,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when (event) {
        MajorEventArt.NEW_DAY -> "╔═══◇ NEW DAY ◇═══╗" to AnsiColor.IceBlue
        MajorEventArt.VICTORY -> "╔═══◆ VICTORY ◆═══╗" to AnsiColor.Gold
        MajorEventArt.DEFEAT -> "▓▓▓ YOU HAVE FALLEN ▓▓▓" to AnsiColor.BrightRed
        MajorEventArt.LEVEL_UP -> "╔═◆ LEVEL ASCENDED ◆═╗" to AnsiColor.Gold
    }
    TerminalText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = color,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 20.sp,
            lineHeight = 25.sp,
        ),
        textAlign = TextAlign.Center,
    )
}

@Composable
fun CrystalDivider(
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val color = if (danger) AnsiColor.Red else AnsiColor.Cyan
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalRule(modifier = Modifier.weight(1f), color = color.copy(alpha = 0.55f), character = '─')
        TerminalText(if (danger) " ◆!◆ " else " ◇◆◇ ", color = if (danger) AnsiColor.BrightRed else AnsiColor.IceBlue)
        TerminalRule(modifier = Modifier.weight(1f), color = color.copy(alpha = 0.55f), character = '─')
    }
}

@Composable
fun TerminalGap(lines: Int = 1) {
    Spacer(modifier = Modifier.height((12 * lines).dp))
}

private fun frameColor(tone: TerminalFrameTone): Color = when (tone) {
    TerminalFrameTone.GLASS -> AnsiColor.Cyan
    TerminalFrameTone.NEUTRAL -> AnsiColor.DimGray
    TerminalFrameTone.GOLD -> AnsiColor.Gold
    TerminalFrameTone.DANGER -> AnsiColor.Red
}
