package com.aedev.aetranzit.ui.components

import java.util.Locale

fun formatPhp(amount: Double): String = String.format(Locale.US, "₱%,.2f", amount)
