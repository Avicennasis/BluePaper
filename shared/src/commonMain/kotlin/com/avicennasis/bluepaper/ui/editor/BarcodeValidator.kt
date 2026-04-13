package com.avicennasis.bluepaper.ui.editor

data class ValidationResult(
    val isValid: Boolean,
    val fixedData: String? = null,
    val error: String? = null,
)

object BarcodeValidator {

    fun validate(format: BarcodeFormat, data: String): ValidationResult {
        if (data.isEmpty()) return ValidationResult(false, error = "Data cannot be empty")
        return when (format) {
            BarcodeFormat.EAN_13 -> validateNumericWithLength(data, "EAN-13", 13)
            BarcodeFormat.EAN_8 -> validateNumericWithLength(data, "EAN-8", 8)
            BarcodeFormat.UPC_A -> validateNumericWithLength(data, "UPC-A", 12)
            BarcodeFormat.UPC_E -> validateNumericWithLength(data, "UPC-E", 8)
            BarcodeFormat.ITF -> validateItf(data)
            BarcodeFormat.RSS_14 -> validateNumericWithLength(data, "RSS-14", 14)
            BarcodeFormat.CODE_39 -> validateCode39(data)
            BarcodeFormat.CODABAR -> validateCodabar(data)
            else -> ValidationResult(true)
        }
    }

    fun autoFix(format: BarcodeFormat, data: String): String = when (format) {
        BarcodeFormat.EAN_13 -> fixEan13(data)
        BarcodeFormat.EAN_8 -> fixEan8(data)
        BarcodeFormat.UPC_A -> fixUpcA(data)
        BarcodeFormat.UPC_E -> fixUpcE(data)
        BarcodeFormat.CODE_39 -> data.uppercase().filter { it in CODE_39_CHARS }
        BarcodeFormat.ITF -> fixItf(data.filter { it.isDigit() })
        BarcodeFormat.RSS_14 -> fixRss14(data)
        BarcodeFormat.CODABAR -> fixCodabar(data)
        else -> data
    }

    fun computeCheckDigit(digits: String): Int {
        // EAN/UPC mod-10: weights alternate 1,3 from the RIGHT
        // For the digits before the check digit, the rightmost gets ×3, next ×1, etc.
        var sum = 0
        for (i in digits.indices) {
            val d = digits[i] - '0'
            val fromRight = digits.length - 1 - i
            sum += if (fromRight % 2 == 0) d * 3 else d
        }
        return (10 - (sum % 10)) % 10
    }

    private fun fixEan13(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(12, '0').take(12)
        return padded + computeCheckDigit(padded)
    }

    private fun fixEan8(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(7, '0').take(7)
        return padded + computeCheckDigit(padded)
    }

    private fun fixUpcA(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(11, '0').take(11)
        return padded + computeCheckDigit(padded)
    }

    /**
     * Expands a 7-digit UPC-E (number system + 6 middle digits) to an 11-digit UPC-A payload
     * (without check digit). The expansion is based on the last of the 6 middle digits (d6).
     */
    fun expandUpcE(digits: String): String {
        require(digits.length == 7) { "expandUpcE requires exactly 7 digits" }
        val d0 = digits[0] // number system digit
        val d1 = digits[1]
        val d2 = digits[2]
        val d3 = digits[3]
        val d4 = digits[4]
        val d5 = digits[5]
        val d6 = digits[6]
        return when (d6) {
            '0', '1', '2' -> "$d0$d1$d2${d6}0000$d3$d4$d5"
            '3' -> "${d0}${d1}${d2}${d3}00000$d4$d5"
            '4' -> "${d0}${d1}${d2}${d3}${d4}00000$d5"
            else -> "${d0}${d1}${d2}${d3}${d4}${d5}0000$d6"
        }
    }

    private fun fixUpcE(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(7, '0').take(7)
        val upcAPayload = expandUpcE(padded)
        val checkDigit = computeCheckDigit(upcAPayload)
        return padded + checkDigit
    }

    private fun fixRss14(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(13, '0').take(13)
        return padded + computeCheckDigit(padded)
    }

    private fun fixItf(data: String): String {
        if (data.isEmpty()) return "00"
        return if (data.length % 2 != 0) "0$data" else data
    }

    private fun fixCodabar(data: String): String {
        val upper = data.uppercase()
        val body = upper.filter { it in CODABAR_CHARS }
        val starts = setOf('A', 'B', 'C', 'D')
        val hasStart = body.isNotEmpty() && body.first() in starts
        val hasStop = body.isNotEmpty() && body.last() in starts
        return when {
            hasStart && hasStop -> body
            hasStart -> body + "A"
            hasStop -> "A" + body
            else -> "A${body}A"
        }
    }

    private fun validateNumeric(data: String, formatName: String): ValidationResult =
        if (data.all { it.isDigit() }) ValidationResult(true)
        else ValidationResult(false, error = "$formatName requires numeric data only")

    private fun validateNumericWithLength(
        data: String,
        formatName: String,
        expectedLength: Int,
    ): ValidationResult {
        val numericCheck = validateNumeric(data, formatName)
        if (!numericCheck.isValid) return numericCheck
        if (data.length != expectedLength) {
            return ValidationResult(false, error = "Expected $expectedLength digits for $formatName")
        }
        return ValidationResult(true)
    }

    private fun validateItf(data: String): ValidationResult {
        val numericCheck = validateNumeric(data, "ITF")
        if (!numericCheck.isValid) return numericCheck
        if (data.length < 2) {
            return ValidationResult(false, error = "Expected minimum 2 digits for ITF")
        }
        if (data.length % 2 != 0) {
            return ValidationResult(false, error = "Expected even number of digits for ITF")
        }
        return ValidationResult(true)
    }

    private fun validateCode39(data: String): ValidationResult {
        val upper = data.uppercase()
        return if (upper.all { it in CODE_39_CHARS }) ValidationResult(true)
        else ValidationResult(false, error = "Code 39 allows: A-Z 0-9 - . $ / + % SPACE")
    }

    private fun validateCodabar(data: String): ValidationResult {
        val upper = data.uppercase()
        return if (upper.all { it in CODABAR_CHARS }) ValidationResult(true)
        else ValidationResult(false, error = "Codabar allows: 0-9 - $ : / . + A B C D")
    }

    private val CODE_39_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-. $/+%".toSet()
    private val CODABAR_CHARS = "0123456789-\$:/.+ABCD".toSet()
}
