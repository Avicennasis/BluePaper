package com.avicennasis.bluepaper.ui.editor

fun bringElementToFront(elements: List<LabelElement>, id: String): List<LabelElement> {
    val el = elements.find { it.id == id } ?: return elements
    return elements.filter { it.id != id } + el
}

fun sendElementToBack(elements: List<LabelElement>, id: String): List<LabelElement> {
    val el = elements.find { it.id == id } ?: return elements
    return listOf(el) + elements.filter { it.id != id }
}

fun moveElementUp(elements: List<LabelElement>, id: String): List<LabelElement> {
    val list = elements.toMutableList()
    val idx = list.indexOfFirst { it.id == id }
    if (idx < 0 || idx >= list.lastIndex) return elements
    val tmp = list[idx]
    list[idx] = list[idx + 1]
    list[idx + 1] = tmp
    return list
}

fun moveElementDown(elements: List<LabelElement>, id: String): List<LabelElement> {
    val list = elements.toMutableList()
    val idx = list.indexOfFirst { it.id == id }
    if (idx <= 0) return elements
    val tmp = list[idx]
    list[idx] = list[idx - 1]
    list[idx - 1] = tmp
    return list
}
