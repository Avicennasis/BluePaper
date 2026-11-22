package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import com.avicennasis.bluepaper.ble.MockBleTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorStateImageFitTest {

    private val state = EditorState(MockBleTransport(), CoroutineScope(Dispatchers.Unconfined))
    private val labelW get() = state.selectedLabelSize.value.widthPx.toFloat()
    private val labelH get() = state.selectedLabelSize.value.heightPx.toFloat()

    private fun added() = state.elements.value.single() as LabelElement.ImageElement
    private fun near(expected: Float, actual: Float, msg: String) =
        assertTrue(abs(expected - actual) < 0.5f, "$msg: expected $expected, got $actual")

    @Test
    fun `large image shrinks to fit the label keeping aspect ratio and is centred`() {
        state.addImageElement(ImageBitmap(2528, 1696)) // size of the real ReplacementLabel.jpeg

        val el = added()
        assertTrue(el.width <= labelW + 0.5f && el.height <= labelH + 0.5f, "fits: ${el.width}x${el.height} in ${labelW}x$labelH")
        assertTrue(abs(el.width / el.height - 2528f / 1696f) < 0.01f, "aspect ratio kept")
        assertTrue(abs(el.width - labelW) < 0.5f || abs(el.height - labelH) < 0.5f, "fills one dimension")
        near((labelW - el.width) / 2f, el.x, "centred x")
        near((labelH - el.height) / 2f, el.y, "centred y")
        assertEquals(1f, el.scale)
    }

    @Test
    fun `small image keeps its natural size and is centred`() {
        state.addImageElement(ImageBitmap(40, 20))

        val el = added()
        assertEquals(40f, el.width)
        assertEquals(20f, el.height)
        near((labelW - 40f) / 2f, el.x, "centred x")
        near((labelH - 20f) / 2f, el.y, "centred y")
    }
}
