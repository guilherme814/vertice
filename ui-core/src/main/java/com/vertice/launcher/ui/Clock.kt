package com.vertice.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.ClockStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Hora atual, atualizada só na virada do minuto (zero custo entre uma e outra). */
@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(60_000L - value % 60_000L + 40L)
    }
}

private val textShadow = Shadow(Color(0x66000000), Offset(0f, 3f), 10f)

/**
 * Relógio da tela inicial. Quatro estilos; a hora só muda a cada minuto
 * (sem ponteiro de segundos de propósito: segundos custam bateria sem agregar muito).
 */
@Composable
fun ClockWidget(
    now: Long,
    style: ClockStyle,
    format: ClockFormat,
    showDate: Boolean,
    tint: Color = Snow,
    scale: Float = 1f,
    align: Alignment.Horizontal = Alignment.Start,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val locale = Locale.getDefault()
    val is24 = when (format) {
        ClockFormat.System -> DateFormat.is24HourFormat(ctx)
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
    }
    val date = Date(now)
    val hour = remember(is24, locale) { SimpleDateFormat(if (is24) "HH" else "h", locale) }.format(date)
    val minute = remember(locale) { SimpleDateFormat("mm", locale) }.format(date)
    val suffix = if (is24) "" else remember(locale) { SimpleDateFormat("a", locale) }.format(date).lowercase()
    // Padrão de data correto para o idioma do aparelho (em pt-BR: "segunda-feira, 23 de novembro").
    val dateText = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
    }.format(date).replaceFirstChar { it.uppercase() }

    val clickClock = Modifier.pointerInput(Unit) { detectTapGestures { onClockClick() } }
    val clickDate = Modifier.pointerInput(Unit) { detectTapGestures { onDateClick() } }

    when (style) {
        ClockStyle.Classic -> Column(modifier, horizontalAlignment = align) {
            Text(
                "$hour:$minute" + if (suffix.isEmpty()) "" else " $suffix",
                color = tint, fontSize = (64 * scale).sp, lineHeight = (68 * scale).sp, fontWeight = FontWeight.Light,
                style = TextStyle(shadow = textShadow), modifier = clickClock
            )
            if (showDate) Text(dateText, color = Snow.copy(alpha = 0.8f), fontSize = 15.sp, modifier = clickDate)
        }

        ClockStyle.Stacked -> Column(modifier, horizontalAlignment = align) {
            if (showDate) Text(
                dateText, color = Snow.copy(alpha = 0.85f), fontSize = 15.sp,
                fontWeight = FontWeight.Medium, modifier = clickDate
            )
            Text(
                hour, color = tint, fontSize = (92 * scale).sp, lineHeight = (84 * scale).sp, fontWeight = FontWeight.ExtraLight,
                style = TextStyle(shadow = textShadow), modifier = clickClock
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = clickClock) {
                Text(
                    minute, color = if (tint == Snow) Mint else tint, fontSize = (92 * scale).sp, lineHeight = (84 * scale).sp, fontWeight = FontWeight.ExtraLight,
                    style = TextStyle(shadow = textShadow)
                )
                if (suffix.isNotEmpty()) Text(
                    " $suffix", color = Snow.copy(alpha = 0.8f), fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }

        ClockStyle.Minimal -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$hour:$minute" + if (suffix.isEmpty()) "" else " $suffix",
                color = tint, fontSize = (30 * scale).sp, fontWeight = FontWeight.Light,
                style = TextStyle(shadow = textShadow), modifier = clickClock
            )
            if (showDate) Text(
                "   $dateText", color = Snow.copy(alpha = 0.8f), fontSize = 14.sp, modifier = clickDate
            )
        }

        ClockStyle.Analog -> {
            val cal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
            val minutes = cal.get(Calendar.MINUTE)
            val hours = cal.get(Calendar.HOUR) // 0..11
            Column(modifier, horizontalAlignment = align) {
                AnalogFace(hours, minutes, clickClock, scale, tint)
                if (showDate) {
                    Spacer(Modifier.height(10.dp))
                    Text(dateText, color = Snow.copy(alpha = 0.85f), fontSize = 15.sp, modifier = clickDate)
                }
            }
        }
    }
}

@Composable
private fun AnalogFace(hours: Int, minutes: Int, modifier: Modifier, scale: Float, tint: Color) {
    Canvas(modifier.size(150.dp * scale)) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(Color(0x59141C29), radius = r, center = c)
        drawCircle(Color(0x33FFFFFF), radius = r, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        // Marcas: 12 horas (as de 12/3/6/9 são mais fortes).
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            val major = i % 3 == 0
            val outer = r * 0.92f
            val inner = r * if (major) 0.80f else 0.86f
            drawLine(
                if (major) Snow else Snow.copy(alpha = 0.55f),
                Offset(c.x + cos(a).toFloat() * inner, c.y + sin(a).toFloat() * inner),
                Offset(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer),
                strokeWidth = if (major) 4f else 2.5f, cap = StrokeCap.Round
            )
        }
        fun hand(angleDeg: Double, length: Float, width: Float, color: Color) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(
                color, c,
                Offset(c.x + cos(a).toFloat() * r * length, c.y + sin(a).toFloat() * r * length),
                strokeWidth = width, cap = StrokeCap.Round
            )
        }
        hand(hours * 30.0 + minutes * 0.5, 0.48f, 9f, tint)
        hand(minutes * 6.0, 0.72f, 6f, Mint)
        drawCircle(Mint, radius = 7f, center = c)
    }
}
