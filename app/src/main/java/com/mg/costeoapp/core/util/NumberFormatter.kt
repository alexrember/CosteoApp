package com.mg.costeoapp.core.util

import java.math.RoundingMode

/**
 * Formatea un Double para mostrar en UI.
 * Muestra hasta 3 decimales si son relevantes, sin ceros innecesarios.
 * Ejemplos:
 *   946.0 → "946"
 *   13.75 → "13.75"
 *   44.999999999997 → "45"
 *   1.499999999999 → "1.5"
 *   9.200000000001 → "9.2"
 *   768.5999999998 → "768.6"
 *   0.0 → "0"
 */
fun Double.formatDisplay(): String {
    val bd = toBigDecimal().setScale(3, RoundingMode.HALF_UP)
    return bd.stripTrailingZeros().toPlainString()
}
