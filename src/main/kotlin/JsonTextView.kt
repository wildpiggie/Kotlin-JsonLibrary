import javax.swing.JTextArea

class JsonTextView(private val model: JsonObject) : JTextArea() {
    init {
        tabSize = 2
        text = model.getStructure()
        isEditable = false
    }

    fun refresh() {
        text = model.getStructure()
    }
}