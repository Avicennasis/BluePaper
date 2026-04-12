package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UndoManagerTest {

    private fun textEl(id: String, text: String) =
        LabelElement.TextElement(id = id, text = text)

    @Test
    fun undoEmptyReturnsNull() {
        val mgr = UndoManager()
        assertNull(mgr.undo(emptyList()))
        assertFalse(mgr.canUndo)
    }

    @Test
    fun redoEmptyReturnsNull() {
        val mgr = UndoManager()
        assertNull(mgr.redo(emptyList()))
        assertFalse(mgr.canRedo)
    }

    @Test
    fun saveAndUndo() {
        val mgr = UndoManager()
        val state0 = listOf(textEl("t1", "A"))
        mgr.save(state0)
        assertTrue(mgr.canUndo)

        val state1 = listOf(textEl("t1", "B"))
        val restored = mgr.undo(state1)
        assertEquals("A", (restored!![0] as LabelElement.TextElement).text)
    }

    @Test
    fun undoThenRedo() {
        val mgr = UndoManager()
        val state0 = listOf(textEl("t1", "A"))
        mgr.save(state0)

        val state1 = listOf(textEl("t1", "B"))
        mgr.undo(state1)
        assertTrue(mgr.canRedo)

        val redone = mgr.redo(state0)
        assertEquals("B", (redone!![0] as LabelElement.TextElement).text)
    }

    @Test
    fun saveClearsRedoStack() {
        val mgr = UndoManager()
        mgr.save(listOf(textEl("t1", "A")))
        mgr.undo(listOf(textEl("t1", "B")))
        assertTrue(mgr.canRedo)

        mgr.save(listOf(textEl("t1", "C")))
        assertFalse(mgr.canRedo)
    }

    @Test
    fun maxHistoryEnforced() {
        val mgr = UndoManager(maxHistory = 3)
        mgr.save(listOf(textEl("t1", "A")))
        mgr.save(listOf(textEl("t1", "B")))
        mgr.save(listOf(textEl("t1", "C")))
        mgr.save(listOf(textEl("t1", "D")))

        val r1 = mgr.undo(listOf(textEl("t1", "E")))
        assertEquals("D", (r1!![0] as LabelElement.TextElement).text)
        val r2 = mgr.undo(r1)
        assertEquals("C", (r2!![0] as LabelElement.TextElement).text)
        val r3 = mgr.undo(r2)
        assertEquals("B", (r3!![0] as LabelElement.TextElement).text)
        val r4 = mgr.undo(r3)
        assertNull(r4)
    }

    @Test
    fun snapshotsAreDeepCopies() {
        val mgr = UndoManager()
        val elements = listOf(textEl("t1", "Original"))
        mgr.save(elements)

        val restored = mgr.undo(listOf(textEl("t1", "Modified")))
        assertEquals("Original", (restored!![0] as LabelElement.TextElement).text)
    }
}
