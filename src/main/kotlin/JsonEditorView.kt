import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class JsonEditorView(model: JsonObject) : JPanel() {
    private val observers: MutableList<JsonEditorViewObserver> = mutableListOf()

    // no futuro adicionar a lista de observers e a interface jsoneditorview observer
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.TOP_ALIGNMENT

        add(JsonObjectWidget(model))
    }

    fun addObserver(observer: JsonEditorViewObserver) {
        observers.add(observer)
    }

    inner class JsonLeafWidget(value: JsonLeaf<*>): JsonWidget() {
        init {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.RIGHT_ALIGNMENT
            alignmentY = Component.CENTER_ALIGNMENT
            border = BorderFactory.createLineBorder(
                Color.GREEN, 2, true)
            when (value) {

                is JsonBoolean -> {
                    val checkBox = JCheckBox()
                    checkBox.isSelected = value.value
                    add(checkBox)
                }
                //se preferirmos pode ser um textfield vazio
                //mas para isso temos que verificar no observer se estiver vazio para dizer que é null
                // e não string
                is JsonNull -> {
                    add(JLabel("null"))
                }
                is JsonString -> {
                    val textField = JTextField(7)
                    textField.maximumSize = Dimension(textField.maximumSize.width, textField.preferredSize.height)
                    textField.text = value.value
                    add(textField)
                }
                else -> {
                    val textField = JTextField(7)
                    textField.maximumSize = Dimension(textField.maximumSize.width, textField.preferredSize.height)
                    textField.text = value.toString()
                    add(textField)
                }
            }
            add(Box.createHorizontalGlue())

             // refresecar atraves do controller
        }
    }
    inner class JsonObjectWidget(modelObject: JsonObject): JsonWidget() {
        private val widgets = mutableMapOf<String, JsonWidget>()
        init {

            modelObject.elements.forEach{
                addObjectWidgets(it.key, it.value)
            }

            border = BorderFactory.createLineBorder(
                Color.BLACK, 2, true)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            modelObject.addObserver(object : JsonObjectObserver {
                override fun elementAdded(name: String, value: JsonElement) {
                    addObjectWidgets(name, value)
                }
            })

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val menu = JPopupMenu("Object Button")
                        val buttonAddObject = JButton("Add Object")
                        val buttonAddArray = JButton("Add Array")
                        val buttonAddLeaf = JButton("Add Leaf")

                        buttonAddObject.addActionListener {
                            val objectName = JOptionPane.showInputDialog("Object name")
                            objectName?.let {
                                observers.forEach {
                                    it.elementAddedToObject(modelObject, objectName, JsonObject())
                                }
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }

                        buttonAddArray.addActionListener {
                            val arrayName = JOptionPane.showInputDialog("Array name")
                            arrayName?.let {
                                observers.forEach {
                                    it.elementAddedToObject(modelObject, arrayName, JsonArray())
                                }
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }

                        buttonAddLeaf.addActionListener {
                            val leafName = JOptionPane.showInputDialog("Leaf name")
                            leafName?.let {
                                observers.forEach {
                                    it.elementAddedToObject(modelObject, leafName, JsonNull())
                                }
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }

                        val del = JButton("delete all")
                        del.addActionListener {
                            println("del no objeto")
                            // components.forEach { remove(it) }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }
                        menu.add(buttonAddObject)
                        menu.add(buttonAddArray)
                        menu.add(buttonAddLeaf)
                        menu.add(del)
                        menu.show(e.component, 100, 100)
                    }
                }
            })
        }

        fun addObjectWidgets(name: String, value: JsonElement) {
            val panel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                alignmentY = Component.CENTER_ALIGNMENT

                add(Box.createHorizontalStrut(10))
                val label = JLabel(name)
                add(label)
                add(Box.createHorizontalStrut(10))

                var widget: JsonWidget = JsonLeafWidget(JsonNull())
                when(value) {
                    is JsonLeaf<*> -> {
                        widget = JsonLeafWidget(value)
                    }
                    is JsonObject -> {
                        widget = JsonObjectWidget(value)
                    }
                    is JsonArray -> {
                        widget = JsonArrayWidget(value)
                    }
                }
                add(widget)
                widgets[name] = widget

                add(Box.createHorizontalStrut(10))
                revalidate()
                repaint()
            }
            add(panel)
        }
    }
    inner class JsonArrayWidget(modelArray: JsonArray): JsonWidget() {
        val widgets = mutableListOf<JsonWidget>()
        init {
            border = BorderFactory.createLineBorder(
                Color.BLUE, 2, true)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            modelArray.addObserver(object : JsonArrayObserver {
                override fun elementAdded(value: JsonElement) {
                    val panel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        alignmentX = Component.LEFT_ALIGNMENT
                        alignmentY = Component.CENTER_ALIGNMENT

                        add(Box.createHorizontalStrut(10))
                        add(Box.createHorizontalStrut(10))

                        var widget: JsonWidget = JsonLeafWidget(JsonNull())
                        when(value) {
                            is JsonLeaf<*> -> {
                                widget = JsonLeafWidget(value)
                            }
                            is JsonObject -> {
                                widget = JsonObjectWidget(value)
                            }
                            is JsonArray -> {
                                widget = JsonArrayWidget(value)
                            }
                        }
                        add(widget)
                        widgets.add(widget)

                        add(Box.createHorizontalStrut(10))
                        revalidate()
                        repaint()
                    }
                    add(panel)
                }
            })

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val menu = JPopupMenu("Array Button")
                        val buttonAddObject = JButton("Add Object")
                        val buttonAddArray = JButton("Add Array")
                        val buttonAddLeaf = JButton("Add Leaf")

                        buttonAddObject.addActionListener {
                            observers.forEach {
                                it.elementAddedToArray(modelArray, JsonObject())
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }

                        buttonAddArray.addActionListener {
                            observers.forEach {
                                it.elementAddedToArray(modelArray, JsonArray())
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }

                        buttonAddLeaf.addActionListener {
                            observers.forEach {
                                it.elementAddedToArray(modelArray, JsonNull())
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }



                        val del = JButton("delete all")

                        del.addActionListener {
                            println("del no array")
                            //components.forEach {remove(it) }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }

                        menu.add(buttonAddObject)
                        menu.add(buttonAddArray)
                        menu.add(buttonAddLeaf)
                        menu.add(del)
                        menu.show(e.component, 100, 100)
                    }
                }
            })
        }
    }
}
abstract class JsonWidget : JPanel()

interface JsonEditorViewObserver {
    fun elementAddedToObject(modelObject: JsonObject, name: String, value: JsonElement)
    fun elementAddedToArray(modelArray: JsonArray, value: JsonElement)

    //fun widgetModified() {}
    //fun widgetRemoved() {}
}

