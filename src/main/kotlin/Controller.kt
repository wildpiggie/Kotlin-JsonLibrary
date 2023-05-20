import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*


fun main() {
    Editor().open()
}

class Editor {
    val frame = JFrame("JSON Object Editor").apply {

        val model = JsonObject()
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        layout = GridLayout(0, 2)
        size = Dimension(600, 600)

        val left = JPanel()
        left.layout = GridLayout()
        val scrollPane = JScrollPane(JsonEditorView(model)).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        }
        left.add(scrollPane)
        add(left)

        val right = JPanel()
        right.layout = GridLayout()
        val srcArea = TextView(model)
        right.add(srcArea)
        add(right)

        model.addElement("uc", JsonString("PA"))
        model.addElement("ects", JsonNumber(6.0))
        model.addElement("data-exame", JsonNull())
        val student1 = JsonObject()
        model.addElement("aluno", student1)
        student1.addElement("numero", JsonNumber(101101))
        student1.addElement("nome", JsonString("Dave Farley"))
        student1.addElement("internacional", JsonBoolean(true))


    }

    fun open() {
        frame.isVisible = true
    }
}






