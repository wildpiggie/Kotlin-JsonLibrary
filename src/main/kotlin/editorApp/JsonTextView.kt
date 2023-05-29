package editorApp

import jsonLibrary.JsonObject
import jsonLibrary.getStructure
import java.awt.Font
import javax.swing.JTextArea

class JsonTextView(private val model: JsonObject) : JTextArea() {
    init {
        tabSize = 2
        text = model.getStructure()
        isEditable = false

        font = Font(Font.MONOSPACED, Font.PLAIN, 18)
    }

    fun refresh() {
        text = model.getStructure()
    }
}