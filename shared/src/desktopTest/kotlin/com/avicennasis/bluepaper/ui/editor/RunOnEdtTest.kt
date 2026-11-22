package com.avicennasis.bluepaper.ui.editor

import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunOnEdtTest {

    @Test
    fun `runs inline when already on the EDT`() {
        // Compose Desktop's LaunchedEffect runs on the EDT, which is where the
        // file dialogs are opened from ("Cannot call invokeAndWait from the event dispatcher thread").
        var result: Int? = null
        SwingUtilities.invokeAndWait { result = runOnEdt { 42 } }
        assertEquals(42, result)
    }

    @Test
    fun `hops onto the EDT from another thread`() {
        assertTrue(runOnEdt { SwingUtilities.isEventDispatchThread() })
    }
}
