import javax.swing.JLabel
import javax.swing.JTextArea

class TextView(private val model: JsonElement) : JTextArea() {
    init {
        tabSize = 2
        text = "${model.getStructure()}"
        model.addObserver(object : JsonElementObserver {
            override fun elementAdded(element: JsonElement) {
                text = "${model.getStructure()}"
            }
        })
    }
}