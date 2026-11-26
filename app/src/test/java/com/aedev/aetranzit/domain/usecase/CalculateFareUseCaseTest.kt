package com.aedev.aetranzit.domain.usecase

import com.aedev.aetranzit.domain.model.PassengerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateFareUseCaseTest {

    private val calculateFare = CalculateFareUseCase()

    @Test
    fun `regular passenger pays full base fare and computes over-payment change`() {
        val result = calculateFare(
            baseFare = 195.0,
            passengerType = PassengerType.REGULAR,
            cashTendered = 200.0
        )

        assertEquals(195.0, result.finalFare, 0.001)
        assertEquals(0.0, result.discountAmount, 0.001)
        assertEquals(5.0, result.changeAmount, 0.001)
        assertFalse(result.isInsufficientCash)
        assertTrue(result.canIssueTicket)
    }

    @Test
    fun `discounted passenger types apply 20 percent discount with clean 2-decimal rounding`() {
        val studentResult = calculateFare(
            baseFare = 65.33,
            passengerType = PassengerType.STUDENT,
            cashTendered = 100.0
        )
        // 65.33 * 0.80 = 52.264 -> rounds HALF_UP to 52.26
        assertEquals(52.26, studentResult.finalFare, 0.001)
        assertEquals(13.07, studentResult.discountAmount, 0.001)
        assertEquals(47.74, studentResult.changeAmount, 0.001)
        assertTrue(studentResult.canIssueTicket)

        val seniorResult = calculateFare(
            baseFare = 143.79,
            passengerType = PassengerType.SENIOR,
            cashTendered = 115.03
        )
        // 143.79 * 0.80 = 115.032 -> rounds HALF_UP to 115.03
        assertEquals(115.03, seniorResult.finalFare, 0.001)
        assertEquals(0.0, seniorResult.changeAmount, 0.001)
        assertFalse(seniorResult.isInsufficientCash)
        assertTrue(seniorResult.canIssueTicket)

        val pwdResult = calculateFare(
            baseFare = 195.0,
            passengerType = PassengerType.PWD,
            cashTendered = 200.0
        )
        assertEquals(156.0, pwdResult.finalFare, 0.001)
        assertEquals(39.0, pwdResult.discountAmount, 0.001)
        assertEquals(44.0, pwdResult.changeAmount, 0.001)
    }

    @Test
    fun `manual fare override supersedes automatic 20 percent discount`() {
        val result = calculateFare(
            baseFare = 195.0,
            passengerType = PassengerType.STUDENT,
            manualFareOverride = 120.50,
            cashTendered = 150.0
        )

        assertTrue(result.isManualOverride)
        assertEquals(120.50, result.finalFare, 0.001)
        assertEquals(0.0, result.discountAmount, 0.001)
        assertEquals(29.50, result.changeAmount, 0.001)
        assertTrue(result.canIssueTicket)
    }

    @Test
    fun `exact cash yields zero change and allows ticket issuance`() {
        val result = calculateFare(
            baseFare = 140.0,
            passengerType = PassengerType.REGULAR,
            cashTendered = 140.0
        )

        assertEquals(0.0, result.changeAmount, 0.001)
        assertEquals(0.0, result.shortfallAmount, 0.001)
        assertFalse(result.isInsufficientCash)
        assertTrue(result.canIssueTicket)
    }

    @Test
    fun `insufficient or missing cash flags shortfall and blocks ticket issuance`() {
        val insufficient = calculateFare(
            baseFare = 195.0,
            passengerType = PassengerType.REGULAR,
            cashTendered = 100.0
        )
        assertTrue(insufficient.isInsufficientCash)
        assertEquals(95.0, insufficient.shortfallAmount, 0.001)
        assertEquals(0.0, insufficient.changeAmount, 0.001)
        assertFalse(insufficient.canIssueTicket)

        val noCashYet = calculateFare(
            baseFare = 195.0,
            passengerType = PassengerType.REGULAR,
            cashTendered = null
        )
        assertFalse(noCashYet.isInsufficientCash)
        assertFalse(noCashYet.canIssueTicket)
    }
}
