package com.avicennasis.bluepaper.printer

open class PrinterException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PrinterNotConnectedException(message: String = "Printer is not connected") : PrinterException(message)

class PrinterTimeoutException(message: String = "Printer response timed out") : PrinterException(message)

class PrinterProtocolException(message: String) : PrinterException(message)
