package com.avicennasis.bluepaper.protocol

/** Readable text for 0xDB print-error codes (codes from niimbluelib's PrinterErrorCode). */
object PrinterErrors {
    private val messages = mapOf(
        0x01 to "Lid is open",
        0x02 to "Out of labels",
        0x03 to "Battery is low",
        0x05 to "Print cancelled on the printer",
        0x06 to "Printer rejected the print data",
        0x07 to "Printer is overheated",
        0x09 to "Printer is busy",
        0x10 to "Wrong label type for this printer",
        0x14 to "Printer couldn't update the label roll's RFID tag — the roll's tag may say all its labels are used",
        0x34 to "Printer timed out waiting for print data",
    )

    fun describe(code: Int): String =
        messages[code] ?: "Printer reported error 0x${code.toString(16).padStart(2, '0')}"
}
