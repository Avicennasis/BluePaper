package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ble.MockBleTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorStateModelForPrinterTest {
    private val state = EditorState(MockBleTransport(), CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `connecting a D110_M selects the D110_M model`() {
        // The editor used to stay on its D110 default, so a D110_M got the D110 sequence.
        state.selectModelForPrinter("D110_M-HC23072563")
        assertEquals("d110_m", state.selectedModel.value.model)
    }

    @Test
    fun `an unknown printer name keeps the current model`() {
        state.selectModelForPrinter("Some Speaker")
        assertEquals("d110", state.selectedModel.value.model)
    }
}
