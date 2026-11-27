package com.aedev.aetranzit.domain.usecase

import java.security.SecureRandom
import java.util.Locale
import javax.inject.Inject

class CompanyJoinCodeGenerator @Inject constructor() {

    private val random = SecureRandom()

    fun generate(
        companyName: String,
        digitSupplier: () -> Int = { 1000 + random.nextInt(9000) }
    ): String {
        val prefix = extractPrefix(companyName)
        val digits = digitSupplier().coerceIn(0, 9999)
        return String.format(Locale.US, "%s-%04d", prefix, digits)
    }

    fun extractPrefix(companyName: String): String {
        val lettersOnly = companyName
            .uppercase(Locale.US)
            .filter { it in 'A'..'Z' }

        val base = when {
            lettersOnly.length >= PREFIX_LENGTH -> lettersOnly.take(PREFIX_LENGTH)
            lettersOnly.isNotEmpty() -> lettersOnly.padEnd(PREFIX_LENGTH, 'X')
            else -> DEFAULT_PREFIX
        }
        return base
    }

    fun normalize(rawCode: String): String =
        rawCode.trim().uppercase(Locale.US)

    fun isValidFormat(rawCode: String): Boolean =
        JOIN_CODE_REGEX.matches(normalize(rawCode))

    companion object {
        private const val PREFIX_LENGTH = 3
        private const val DEFAULT_PREFIX = "BUS"
        val JOIN_CODE_REGEX = Regex("^[A-Z0-9]{3}-[0-9]{4}$")
    }
}
