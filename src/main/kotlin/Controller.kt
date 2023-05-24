import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*


fun main() {
    val model = JsonObject()
    Editor(model).open()

    model.addElement("uc", JsonString("PA"))
    model.addElement("ects", JsonNumber(6.0))
    model.addElement("data-exame", JsonNull())
    val student1 = JsonObject()
    model.addElement("aluno", student1)
    student1.addElement("numero", JsonNumber(101101))
    student1.addElement("nome", JsonString("Dave Farley"))
    student1.addElement("internacional", JsonBoolean(true))

    //GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames().forEach { print(it) }
}

class Editor(private val model: JsonObject) {
    val commandStack = mutableListOf<Command>()

    private val frame = JFrame("JSON Object Editor").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        layout = GridLayout(0, 2)
        size = Dimension(600, 600)
        //font = Font("MS Consolas", Font.PLAIN, 50) //não funciona

        val right = JPanel()
        right.layout = GridLayout()
        val textView = JsonTextView(model)
        right.add(textView)

        val left = JPanel()
        left.layout = GridLayout()
        val scrollPane = JScrollPane(JsonEditorView(model, textView)).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        left.add(scrollPane)

        add(left)
        add(right)
    }

    fun open() {
        frame.isVisible = true
    }

    interface Command {
        fun run()
        fun undo()
    }

    class AddToObjectCommand(val model: JsonObject, val name: String, val value: JsonElement): Command {
        override fun run() {
            model.addElement(name, value)
        }

        override fun undo() {
            //model.remove(name)
        }
    }

    class AddToArrayCommand(val model: JsonArray, val value: JsonElement): Command {
        override fun run() {
            model.addElement(value)
        }

        override fun undo() {
            //model.remove(value)
        }
    }
}






