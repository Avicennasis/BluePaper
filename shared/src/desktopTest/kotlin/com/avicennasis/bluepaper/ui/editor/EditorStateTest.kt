package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ble.MockBleTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorStateTest {

    private fun createEditorState(): EditorState {
        val transport = MockBleTransport()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        return EditorState(transport, scope)
    }

    @Test
    fun undoAfterAddElement() {
        val state = createEditorState()
        assertTrue(state.elements.value.isEmpty())

        state.addTextElement()
        assertEquals(1, state.elements.value.size)

        state.undo()
        assertTrue(state.elements.value.isEmpty())
    }

    @Test
    fun undoAfterMoveRestoresOriginalPosition() {
        val state = createEditorState()
        state.addTextElement()
        val element = state.elements.value.first()
        val originalX = element.x
        val originalY = element.y

        // moveElement saves undo snapshot on first call, then moves + clamps
        state.moveElement(element.id, originalX + 50f, originalY + 50f)
        state.moveElementDone(element.id)

        // Undo should restore the pre-move snapshot
        state.undo()
        val restored = state.elements.value.first()
        assertEquals(originalX, restored.x)
        assertEquals(originalY, restored.y)
    }

    @Test
    fun redoAfterUndo() {
        val state = createEditorState()
        state.addTextElement()
        val addedId = state.elements.value.first().id

        state.undo()
        assertTrue(state.elements.value.isEmpty())

        state.redo()
        assertEquals(1, state.elements.value.size)
        assertEquals(addedId, state.elements.value.first().id)
    }

    @Test
    fun saveAsTemplatePreservesElementCount() {
        val state = createEditorState()
        state.addTextElement()
        state.addTextElement()

        // saveAsTemplate converts elements but should not modify the element list
        state.saveAsTemplate("Test Template", "A test")
        assertEquals(2, state.elements.value.size)
    }

    @Test
    fun multipleUndosThenRedos() {
        val state = createEditorState()
        state.addTextElement()
        state.addTextElement()
        state.addTextElement()
        assertEquals(3, state.elements.value.size)

        state.undo()
        assertEquals(2, state.elements.value.size)

        state.undo()
        assertEquals(1, state.elements.value.size)

        state.undo()
        assertTrue(state.elements.value.isEmpty())

        // Redo all three
        state.redo()
        assertEquals(1, state.elements.value.size)

        state.redo()
        assertEquals(2, state.elements.value.size)

        state.redo()
        assertEquals(3, state.elements.value.size)
    }

    @Test
    fun undoOnEmptyHistoryIsNoOp() {
        val state = createEditorState()
        state.undo()
        assertTrue(state.elements.value.isEmpty())
        assertFalse(state.canUndo.value)
    }

    @Test
    fun redoOnEmptyHistoryIsNoOp() {
        val state = createEditorState()
        state.redo()
        assertTrue(state.elements.value.isEmpty())
        assertFalse(state.canRedo.value)
    }

    @Test
    fun newActionClearsRedoStack() {
        val state = createEditorState()
        state.addTextElement()
        state.undo()
        assertTrue(state.canRedo.value)

        // Adding a new element should clear the redo stack
        state.addTextElement()
        assertFalse(state.canRedo.value)
    }

    @Test
    fun undoRemoveElementRestoresIt() {
        val state = createEditorState()
        state.addTextElement()
        val id = state.elements.value.first().id

        state.removeElement(id)
        assertTrue(state.elements.value.isEmpty())

        state.undo()
        assertEquals(1, state.elements.value.size)
        assertEquals(id, state.elements.value.first().id)
    }

    @Test
    fun canUndoCanRedoTrackState() {
        val state = createEditorState()
        assertFalse(state.canUndo.value)
        assertFalse(state.canRedo.value)

        state.addTextElement()
        assertTrue(state.canUndo.value)
        assertFalse(state.canRedo.value)

        state.undo()
        assertFalse(state.canUndo.value)
        assertTrue(state.canRedo.value)

        state.redo()
        assertTrue(state.canUndo.value)
        assertFalse(state.canRedo.value)
    }
}
