// Android 版作者：南盺
// Small text/number helpers shared across Compose feature files.
package com.nanxin.hrtrecorder

import java.util.Locale

fun AppLanguage.t(zh: String, en: String): String =
    if (this == AppLanguage.English) en else zh

fun formatNumber(value: Double, decimals: Int): String {
    val safeDecimals = decimals.coerceAtLeast(0)
    val formatted = "%.${safeDecimals}f".format(Locale.US, value)
    return if (safeDecimals == 0) formatted else formatted.trimEnd('0').trimEnd('.')
}
