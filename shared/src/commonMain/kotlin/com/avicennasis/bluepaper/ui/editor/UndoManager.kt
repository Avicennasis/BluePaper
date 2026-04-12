package com.avicennasis.bluepaper.ui.editor

class UndoManager(private val maxHistory: Int = 50) {

    private val undoStack = mutableListOf<List<LabelElement>>()
    private val redoStack = mutableListOf<List<LabelElement>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun save(state: List<LabelElement>) {
        undoStack.add(deepCopy(state))
        redoStack.clear()
        if (undoStack.size > maxHistory) undoStack.removeAt(0)
    }

    fun undo(current: List<LabelElement>): List<LabelElement>? {
        if (undoStack.isEmpty()) return null
        redoStack.add(deepCopy(current))
        return undoStack.removeLast()
    }

    fun redo(current: List<LabelElement>): List<LabelElement>? {
        if (redoStack.isEmpty()) return null
        undoStack.add(deepCopy(current))
        return redoStack.removeLast()
    }

    private fun deepCopy(elements: List<LabelElement>): List<LabelElement> =
        elements.map { element ->
            when (element) {
                is LabelElement.TextElement -> element.copy()
                is LabelElement.ImageElement -> element.copy()
            }
        }
}
