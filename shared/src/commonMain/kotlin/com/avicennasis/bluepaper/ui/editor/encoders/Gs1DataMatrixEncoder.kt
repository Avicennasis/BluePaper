package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard

class Gs1DataMatrixEncoder : DataEncoder {

    override val standard = DataStandard.GS1_DATAMATRIX

    private val delegate = Gs1128Encoder()

    override fun fields(): List<DataField> = delegate.fields()

    override fun encode(fields: Map<String, String>): String = delegate.encode(fields)

    override fun decode(data: String): Map<String, String> = delegate.decode(data)
}
