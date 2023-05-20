import javax.swing.JLabel
import javax.swing.JTextArea

class TextView(private val model: JsonObject) : JTextArea() {
    init {
        tabSize = 2
        text = "${model.getStructure()}"
        model.addObserver(object : JsonObjectObserver {
            override fun elementAdded(name: String, value: JsonElement) {
               text = "${model.getStructure()}"
            }
        })
    }
}