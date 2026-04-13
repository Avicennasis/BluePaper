package com.avicennasis.bluepaper.ui.editor

enum class BarcodeLibrary {
    QROSE, // Reserved — requires Kotlin 2.3.0+, currently unused
    ZXING,
}

enum class ErrorCorrection(val displayName: String) {
    L("Low (7%)"),
    M("Medium (15%)"),
    Q("Quartile (25%)"),
    H("High (30%)"),
}

enum class BarcodeFormat(
    val displayName: String,
    val category: String,
    val is2D: Boolean,
    val library: BarcodeLibrary,
) {
    QR_CODE("QR Code", "2D Codes", true, BarcodeLibrary.ZXING),
    PDF_417("PDF417", "2D Codes", true, BarcodeLibrary.ZXING),
    DATA_MATRIX("Data Matrix", "2D Codes", true, BarcodeLibrary.ZXING),
    AZTEC("Aztec", "2D Codes", true, BarcodeLibrary.ZXING),
    MAXICODE("MaxiCode", "2D Codes", true, BarcodeLibrary.ZXING),
    RSS_EXPANDED("RSS Expanded", "2D Codes", true, BarcodeLibrary.ZXING),

    CODE_128("Code 128", "Linear Codes", false, BarcodeLibrary.ZXING),
    CODE_39("Code 39", "Linear Codes", false, BarcodeLibrary.ZXING),
    CODE_93("Code 93", "Linear Codes", false, BarcodeLibrary.ZXING),
    CODABAR("Codabar", "Linear Codes", false, BarcodeLibrary.ZXING),
    ITF("ITF", "Linear Codes", false, BarcodeLibrary.ZXING),
    RSS_14("RSS-14", "Linear Codes", false, BarcodeLibrary.ZXING),

    EAN_13("EAN-13", "Retail", false, BarcodeLibrary.ZXING),
    EAN_8("EAN-8", "Retail", false, BarcodeLibrary.ZXING),
    UPC_A("UPC-A", "Retail", false, BarcodeLibrary.ZXING),
    UPC_E("UPC-E", "Retail", false, BarcodeLibrary.ZXING),
    ;

    companion object {
        fun byCategory(): Map<String, List<BarcodeFormat>> =
            entries.groupBy { it.category }
    }
}
