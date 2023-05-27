import java.awt.BorderLayout
import java.awt.Button
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*


fun main() {
    val model = JsonObject()
    val studentArray = JsonArray()

    model.addElement("uc", JsonString("PA"))
    model.addElement("ects", JsonNumber(6.0))
    model.addElement("data-exame", JsonNull())

    model.addElement("inscritos", studentArray)

    val student1 = JsonObject()
    studentArray.addElement(student1)
    student1.addElement("numero", JsonNumber(101101))
    student1.addElement("nome", JsonString("Dave Farley"))
    student1.addElement("internacional", JsonBoolean(true))

    val student2 = JsonObject()
    studentArray.addElement(student2)
    student2.addElement("numero", JsonNumber(101102))
    student2.addElement("nome", JsonString("Martin Fowler"))
    student2.addElement("internacional", JsonBoolean(true))

    student1.addElement("A", student2)

    Editor(model).open()

    //GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames().forEach { print(it) }
}

class Editor(private val model: JsonObject) {
    private val commandStack = mutableListOf<Command>()
    private var commandIndex = 0

    private val frame = JFrame("JSON Object Editor").apply {

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(800, 800)

        layout = BorderLayout()

        //font = Font("MS Consolas", Font.PLAIN, 50) //não funciona


        val textView = JsonTextView(model)
        val editorView = JsonEditorView(model)

        val editorAndViewerPanel = object: JPanel() {
            init {
                layout = GridLayout(0, 2)
                val left = JPanel()
                left.layout = GridLayout()
                val scrollPane = JScrollPane(editorView).apply {
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                }
                left.add(scrollPane)

                val right = JPanel()
                right.layout = GridLayout()
                right.add(textView)

                add(left)
                add(right)
            }
        }

        add(editorAndViewerPanel, BorderLayout.CENTER)

        val undoButton = Button("undo")
        val redoButton = Button("redo")

        undoButton.addActionListener {
            commandStack[--commandIndex].undo()
            if(commandIndex == 0) undoButton.isEnabled = false
            redoButton.isEnabled = commandIndex < commandStack.size

            textView.refresh()
        }
        undoButton.isEnabled = false

        redoButton.addActionListener {
            commandStack[commandIndex++].run()
            if(commandIndex == commandStack.size) redoButton.isEnabled = false
            undoButton.isEnabled = commandIndex > 0

            textView.refresh()
        }
        redoButton.isEnabled = false

        val toolbar = JToolBar()

        toolbar.add(undoButton)
        toolbar.add(redoButton)
        add(toolbar, BorderLayout.PAGE_START)

        fun runCommandAndUpdateStack(cmd: Command){
            if(commandIndex < commandStack.size){
                commandStack.subList(commandIndex, commandStack.size).clear()
            }
            commandIndex = commandStack.size
            commandStack.add(commandIndex++, cmd)
            undoButton.isEnabled = true
            redoButton.isEnabled = false
            cmd.run()

            textView.refresh()

            revalidate()
            repaint()
        }

        editorView.addObserver(object : JsonEditorViewObserver {
            override fun elementAddedToObject(modelObject: JsonObject, name: String, value: JsonElement, index: Int) {
                if(!modelObject.elements.containsKey(name)) {
                    val cmd = AddToObjectCommand(modelObject, name, value, index)
                    runCommandAndUpdateStack(cmd)
                }
            }

            override fun elementAddedToArray(modelArray: JsonArray, value: JsonElement, index: Int) {
                val cmd = AddToArrayCommand(modelArray, value, index)
                runCommandAndUpdateStack(cmd)
            }

            override fun elementRemovedFromObject(modelObject: JsonObject, name: String, index: Int) {
                val removedValue = modelObject.elements[name]
                if(removedValue != null){
                    val cmd = RemoveFromObjectCommand(modelObject, name, removedValue, index)
                    runCommandAndUpdateStack(cmd)
                }
            }

            override fun elementRemovedFromArray(modelArray: JsonArray, index: Int) {
                val removedValue = modelArray.elements.getOrNull(index)
                if(removedValue != null){
                    val cmd = RemoveFromArrayCommand(modelArray, index, removedValue)
                    runCommandAndUpdateStack(cmd)
                }
            }

            override fun elementModifiedFromObject(name: String, newValue: JsonLeaf<*>, parent: JsonObject, index: Int) {
                parent.modifyElement(name, newValue, index)
                textView.refresh()
            }


            override fun elementModifiedFromArray(index: Int, newValue: JsonLeaf<*>, parent: JsonArray) {
                parent.modifyElement(index, newValue)
                textView.refresh()
            }
        })
    }

    fun open() {
        frame.isVisible = true
    }

    interface Command {
        fun run()
        fun undo()
    }

    class AddToObjectCommand(private val model: JsonObject, private val name: String, private val value: JsonElement, private val index: Int): Command {
        override fun run() {
            model.addElement(name, value)
        }

        override fun undo() {
            model.removeElement(name, index)
        }
    }

    class AddToArrayCommand(private val model: JsonArray, private val value: JsonElement, private val index: Int): Command {
        override fun run() {
            model.addElement(value, index)
        }

        override fun undo() {
            model.removeElement(index)
        }
    }

    class RemoveFromObjectCommand(private val model: JsonObject, private val name: String, private val removedValue: JsonElement, private val index: Int): Command {

        override fun run() {
            model.removeElement(name, index)
        }

        override fun undo() {
            model.addElement(name, removedValue)
        }
    }

    class RemoveFromArrayCommand(private val model: JsonArray, private val index: Int, private val removedvalue: JsonElement): Command {
        override fun run() {
            model.removeElement(index)
        }

        override fun undo() {
            model.addElement(removedvalue, index)
        }

    }
}






