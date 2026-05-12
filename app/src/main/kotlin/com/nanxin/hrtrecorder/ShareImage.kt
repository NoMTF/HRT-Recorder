// Android author watermark: Nanxin
// Share poster style follows the original HRT Recorder card direction.
package com.nanxin.hrtrecorder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

fun buildSharePng(
    analyte: Analyte,
    concentration: Double,
    selectedTimeH: Double,
    language: AppLanguage = AppLanguage.ZhHans,
    antiAndrogenLabel: String? = null,
): ByteArray =
    buildResultPosterPng(
        title = analytePosterTitle(analyte, language, antiAndrogenLabel),
        reached = posterReached(language),
        resultText = formatPosterNumber(concentration),
        unitText = posterUnit(analyte),
        selectedTimeH = selectedTimeH,
        language = language,
    )

fun buildCupSharePng(
    result: CupCalculatorResult,
    selectedTimeH: Double,
    language: AppLanguage = AppLanguage.ZhHans,
): ByteArray =
    buildResultPosterPng(
        title = language.t("\u6211\u7684\u7f69\u676f\u53c2\u8003", "My cup-size reference"),
        reached = language.t("\u7b97\u51fa\u6765\u4e86", "is ready"),
        resultText = result.displaySize,
        unitText = language.t("\u5c3a\u7801\u53c2\u8003", "size reference"),
        selectedTimeH = selectedTimeH,
        language = language,
    )

private fun buildResultPosterPng(
    title: String,
    reached: String,
    resultText: String,
    unitText: String,
    selectedTimeH: Double,
    language: AppLanguage,
): ByteArray {
    val width = 1080
    val height = 1440
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val pink = Color.rgb(234, 143, 181)
    val vividPink = Color.rgb(223, 94, 148)
    val violet = Color.rgb(178, 91, 205)
    val softPink = Color.rgb(255, 231, 241)
    val blue = Color.rgb(91, 190, 230)
    val softBlue = Color.rgb(223, 245, 255)
    val ink = Color.rgb(17, 24, 39)
    val muted = Color.rgb(112, 122, 138)

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(248, 252, 255)
    }.also { canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), it) }

    val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val blockW = width / 5f
    listOf(pink, blue, Color.WHITE, blue, pink).forEachIndexed { index, color ->
        blockPaint.color = color
        canvas.drawRect(index * blockW, 0f, (index + 1) * blockW, 24f, blockPaint)
        canvas.drawRect(index * blockW, height - 24f, (index + 1) * blockW, height.toFloat(), blockPaint)
    }
    blockPaint.color = softPink
    canvas.drawOval(RectF(-245f, -70f, 365f, 410f), blockPaint)
    blockPaint.color = softBlue
    canvas.drawOval(RectF(665f, 1005f, 1280f, 1580f), blockPaint)

    val card = RectF(88f, 112f, 992f, 1328f)
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 92, 120, 150)
        setShadowLayer(34f, 0f, 18f, Color.argb(45, 92, 120, 150))
    }.also { canvas.drawRoundRect(card, 72f, 72f, it) }
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 255, 255) }
        .also { canvas.drawRoundRect(card, 72f, 72f, it) }

    val pill = RectF(144f, 178f, 494f, 238f)
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(254, 239, 246) }
        .also { canvas.drawRoundRect(pill, 29f, 29f, it) }
    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(146, 208, 236) }
        .also { canvas.drawCircle(168f, 207f, 10f, it) }
    val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(183, 83, 132)
        textSize = 27f
        letterSpacingCompat(0.04f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(posterKicker(language), 190f, 219f, pillText)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            250f,
            0f,
            830f,
            0f,
            intArrayOf(vividPink, violet, blue),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        textSize = 80f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    fitText(titlePaint, title, 800f, if (language == AppLanguage.English) 52f else 60f)
    drawCentered(canvas, title, 344f, titlePaint, width)

    val reachedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 76f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    drawCentered(canvas, reached, 438f, reachedPaint, width)

    val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            165f,
            0f,
            915f,
            0f,
            intArrayOf(vividPink, violet, Color.rgb(77, 166, 220)),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        textSize = 190f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    fitText(numberPaint, resultText, 880f, 120f)
    drawCentered(canvas, resultText, 666f, numberPaint, width)

    val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    drawCentered(canvas, unitText, 812f, unitPaint, width)

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            210f,
            0f,
            870f,
            0f,
            intArrayOf(Color.rgb(255, 232, 242), Color.rgb(194, 229, 244), Color.rgb(255, 232, 242)),
            null,
            Shader.TileMode.CLAMP,
        )
    }.also { canvas.drawRoundRect(RectF(210f, 892f, 870f, 897f), 2.5f, 2.5f, it) }

    val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(73, 81, 99)
        textSize = 64f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    drawCentered(canvas, posterSlogan(language), 992f, sloganPaint, width)

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(145, 153, 168)
        textSize = 30f
    }
    drawCentered(canvas, formatPosterTime(selectedTimeH), 1072f, timePaint, width)

    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(229, 233, 239) }
        .also { canvas.drawRoundRect(RectF(172f, 1134f, 908f, 1137f), 1.5f, 1.5f, it) }

    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        letterSpacingCompat(0.14f)
    }
    canvas.drawText("HRT RECORDER", 172f, 1214f, brandPaint)
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(145, 153, 168)
        textSize = 27f
    }
    canvas.drawText(posterAuthor(language), 172f, 1260f, authorPaint)

    val handle = RectF(704f, 1176f, 910f, 1226f)
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            handle.left,
            0f,
            handle.right,
            0f,
            intArrayOf(vividPink, violet, blue),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
    }.also { canvas.drawRoundRect(handle, 25f, 25f, it) }
    val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 25f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    drawCenteredInRect(canvas, "@xynMTFxyn", handle, handlePaint)

    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    bitmap.recycle()
    return out.toByteArray()
}

private fun posterKicker(language: AppLanguage): String =
    language.t("HRT \u6570\u636e \u00b7 \u4eca\u65e5\u68c0\u6d4b", "HRT Data \u00b7 Today's Check")

private fun posterReached(language: AppLanguage): String =
    language.t("\u8fbe\u5230\u4e86", "has reached")

private fun posterSlogan(language: AppLanguage): String =
    language.t("\u4f60\u4e5f\u6765\u8bd5\u8bd5\u5427\uff01", "Try HRT Recorder too!")

private fun posterAuthor(language: AppLanguage): String =
    language.t("\u4f5c\u8005\uff1a\u5357\u76fa", "Author: Nanxin")

private fun analytePosterTitle(analyte: Analyte, language: AppLanguage, antiAndrogenLabel: String?): String =
    when (analyte) {
        Analyte.E2 -> language.t("\u6211\u7684\u96cc\u4e8c\u9187\u6d53\u5ea6", "My E2 level")
        Analyte.CPA -> {
            val label = antiAndrogenLabel?.takeIf { it.isNotBlank() }
                ?: language.t("\u6297\u96c4", "antiandrogen")
            language.t("\u6211\u7684${label}\u6d53\u5ea6", "My $label level")
        }
        Analyte.Testosterone -> language.t("\u6211\u7684\u777e\u916e\u6d53\u5ea6", "My T level")
    }

private fun posterUnit(analyte: Analyte): String =
    when (analyte) {
        Analyte.E2 -> "pg / mL"
        Analyte.CPA -> "ng / mL"
        Analyte.Testosterone -> "ng / dL"
    }

private fun formatPosterNumber(value: Double): String =
    when {
        value >= 100.0 -> "%.0f".format(Locale.US, value)
        value >= 10.0 -> "%.1f".format(Locale.US, value)
        else -> "%.2f".format(Locale.US, value)
    }.trimEnd('0').trimEnd('.')

private val posterTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

private fun formatPosterTime(timeH: Double): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli((timeH * 3_600_000.0).toLong()), ZoneId.systemDefault())
        .format(posterTimeFormatter)

private fun drawCentered(canvas: Canvas, text: String, y: Float, paint: Paint, width: Int) {
    canvas.drawText(text, (width - paint.measureText(text)) / 2f, y, paint)
}

private fun drawCenteredInRect(canvas: Canvas, text: String, rect: RectF, paint: Paint) {
    val x = rect.centerX() - paint.measureText(text) / 2f
    val y = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(text, x, y, paint)
}

private fun fitText(paint: Paint, text: String, maxWidth: Float, minSize: Float) {
    while (paint.textSize > minSize && paint.measureText(text) > maxWidth) {
        paint.textSize = max(minSize, paint.textSize - 4f)
    }
}

private fun Paint.letterSpacingCompat(value: Float) {
    textScaleX = 1f + value
}
