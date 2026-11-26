package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.PassengerType
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class FareCalculationResult(
    val baseFare: Double,
    val finalFare: Double,
    val discountAmount: Double,
    val isManualOverride: Boolean,
    val cashTendered: Double?,
    val changeAmount: Double,
    val shortfallAmount: Double,
    val isInsufficientCash: Boolean,
    val canIssueTicket: Boolean
)

class CalculateFareUseCase @Inject constructor() {

    operator fun invoke(
        baseFare: Double,
        passengerType: PassengerType,
        manualFareOverride: Double? = null,
        cashTendered: Double? = null
    ): FareCalculationResult {
        val sanitizedBase = roundCurrency(baseFare.coerceAtLeast(0.0))
        val isManualOverride = manualFareOverride != null

        val finalFare: Double
        val discountAmount: Double
        if (manualFareOverride != null) {
            finalFare = roundCurrency(manualFareOverride.coerceAtLeast(0.0))
            discountAmount = 0.0
        } else if (passengerType.isDiscounted) {
            finalFare = roundCurrency(sanitizedBase * DISCOUNT_MULTIPLIER)
            discountAmount = roundCurrency((sanitizedBase - finalFare).coerceAtLeast(0.0))
        } else {
            finalFare = sanitizedBase
            discountAmount = 0.0
        }

        val isValidFare = finalFare > 0.0
        val roundedCash = cashTendered?.let { roundCurrency(it) }
        val effectiveCash = roundedCash ?: 0.0
        val rawDifference = roundCurrency(effectiveCash - finalFare)

        val isInsufficientCash = roundedCash != null && rawDifference < 0.0
        val shortfallAmount = if (isValidFare && rawDifference < 0.0) {
            roundCurrency(finalFare - effectiveCash)
        } else {
            0.0
        }
        val changeAmount = if (roundedCash != null && rawDifference >= 0.0 && isValidFare) {
            rawDifference
        } else {
            0.0
        }
        val canIssueTicket = isValidFare && roundedCash != null && rawDifference >= 0.0

        return FareCalculationResult(
            baseFare = sanitizedBase,
            finalFare = finalFare,
            discountAmount = discountAmount,
            isManualOverride = isManualOverride,
            cashTendered = roundedCash,
            changeAmount = changeAmount,
            shortfallAmount = shortfallAmount,
            isInsufficientCash = isInsufficientCash,
            canIssueTicket = canIssueTicket
        )
    }

    companion object {
        const val DISCOUNT_MULTIPLIER = 0.80

        fun roundCurrency(value: Double): Double {
            if (!value.isFinite()) return 0.0
            return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
        }
    }
}
