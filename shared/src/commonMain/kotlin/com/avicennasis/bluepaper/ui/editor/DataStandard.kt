package com.avicennasis.bluepaper.ui.editor

enum class FieldType { TEXT, NUMBER, EMAIL, PHONE, URL }

data class DataField(
    val key: String,
    val label: String,
    val required: Boolean = false,
    val hint: String = "",
    val fieldType: FieldType = FieldType.TEXT,
)

enum class DataStandard(
    val displayName: String,
    val description: String,
    val applicableFormats: Set<BarcodeFormat>,
) {
    RAW_TEXT("Raw Text", "Plain text data", BarcodeFormat.entries.toSet()),
    VCARD("vCard", "Contact card", setOf(BarcodeFormat.QR_CODE)),
    URL("URL", "Web link", setOf(BarcodeFormat.QR_CODE)),
    WIFI("WiFi", "Network credentials", setOf(BarcodeFormat.QR_CODE)),
    MECARD("MeCard", "Simplified contact", setOf(BarcodeFormat.QR_CODE)),
    SMS("SMS", "Text message", setOf(BarcodeFormat.QR_CODE)),
    EMAIL("Email", "Email message", setOf(BarcodeFormat.QR_CODE)),
    PHONE("Phone", "Phone number", setOf(BarcodeFormat.QR_CODE)),
    GEO("Geo Location", "GPS coordinates", setOf(BarcodeFormat.QR_CODE)),
    AAMVA("AAMVA", "Driver's license / ID", setOf(BarcodeFormat.PDF_417)),
    GS1_128("GS1-128", "Supply chain AIs", setOf(BarcodeFormat.CODE_128)),
    GS1_DATAMATRIX("GS1 DataMatrix", "Product ID AIs", setOf(BarcodeFormat.DATA_MATRIX)),
    HIBC("HIBC", "Health Industry Bar Code", setOf(BarcodeFormat.DATA_MATRIX, BarcodeFormat.CODE_128)),
    ;

    companion object {
        fun forFormat(format: BarcodeFormat): List<DataStandard> =
            entries.filter { format in it.applicableFormats }
    }
}
