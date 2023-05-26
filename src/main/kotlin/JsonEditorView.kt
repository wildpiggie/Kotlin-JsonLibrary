import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class JsonEditorView(model: JsonObject) : JPanel() {
    private val observers: MutableList<JsonEditorViewObserver> = mutableListOf()

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
        }
    }
    inner class JsonObjectWidget(private val modelObject: JsonObject): JsonWidget() {
        private val widgets = mutableMapOf<String, JsonWidget>()
        init {
            modelObject.elements.forEach{
                addElementWidget(it.key, it.value)
            }

            border = BorderFactory.createLineBorder(
                Color.BLACK, 5)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            modelObject.addObserver(object : JsonObjectObserver {
                override fun elementAdded(name: String, value: JsonElement) {
                    addElementWidget(name, value)
                    revalidate()
                    repaint()
                }

                override fun elementRemoved(name: String) {
                    remove(widgets.remove(name)?.parent)
                    revalidate()
                    repaint()
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
                        }

                        buttonAddArray.addActionListener {
                            val arrayName = JOptionPane.showInputDialog("Array name")
                            arrayName?.let {
                                observers.forEach {
                                    it.elementAddedToObject(modelObject, arrayName, JsonArray())
                                }
                            }
                            menu.isVisible = false
                        }

                        buttonAddLeaf.addActionListener {
                            val leafName = JOptionPane.showInputDialog("Leaf name")
                            leafName?.let {
                                observers.forEach {
                                    it.elementAddedToObject(modelObject, leafName, JsonNull())
                                }
                            }
                            menu.isVisible = false
                        }

                        //De momento este botão causa problemas porque ao apagar um elemento do array enquanto estás a percorre-lo da porcaria
                        /*
                        val del = JButton("delete all")
                        del.addActionListener {
                            widgets.forEach{widget ->
                                observers.forEach{
                                    it.elementRemovedFromObject(modelObject, widget.key)
                                }
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }
                        menu.add(del)
                         */

                        menu.add(buttonAddObject)
                        menu.add(buttonAddArray)
                        menu.add(buttonAddLeaf)
                        menu.show(e.component, e.x, e.y)
                    }
                }
            })
        }

        private fun addElementWidget(name: String, value: JsonElement) {
            val panel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                alignmentY = Component.CENTER_ALIGNMENT

                add(Box.createHorizontalStrut(10))
                val label = JLabel(name)
                add(label)
                add(Box.createHorizontalStrut(10))

                label.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            val menu = JPopupMenu("Element")
                            val removeButton = JButton("remove element")
                            removeButton.addActionListener {
                                observers.forEach {
                                    it.elementRemovedFromObject(modelObject, name)
                                }
                                menu.isVisible = false
                            }
                            menu.add(removeButton)
                            menu.show(e.component, e.x, e.y)
                        }
                    }
                })

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
            }
            add(panel)
        }
    }
    inner class JsonArrayWidget(private val modelArray: JsonArray): JsonWidget() {
        init {
            modelArray.elements.forEach {
                add(ArrayElementWidget(it))
            }

            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createLineBorder(Color.DARK_GRAY, 5)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    onMouseClicked(e)
                }
            })

            modelArray.addObserver(object : JsonArrayObserver {
                override fun elementAdded(value: JsonElement) {
                    add(ArrayElementWidget(value))
                    updateBorders()
                    revalidate()
                    repaint()
                }

                override fun elementAdded(value: JsonElement, index: Int) {
                    add(ArrayElementWidget(value), index)
                    updateBorders()
                    revalidate()
                    repaint()
                }

                override fun elementRemoved(index: Int) {
                    remove(this@JsonArrayWidget.getComponent(index))
                    updateBorders()
                    revalidate()
                    repaint()
                }
            })

            updateBorders()
        }

        private fun updateBorders() {
            this.components.forEach {
                if (it is ArrayElementWidget) {
                    val borderComponent = BorderFactory.createLineBorder(
                        if (this.components.indexOf(it) % 2 == 0) Color(220,220,220) else Color(190,190,190), 10
                    )
                    it.border = borderComponent
                }
            }
        }

        private fun onMouseClicked(e: MouseEvent, widget: ArrayElementWidget? = null){
            var index = 0

            if(widget != null)
                index = this@JsonArrayWidget.components.indexOf(widget) + 1

            if (SwingUtilities.isRightMouseButton(e)) {
                val menu = JPopupMenu("Array Button")
                val buttonAddObject = JButton("Add Object")
                val buttonAddArray = JButton("Add Array")
                val buttonAddLeaf = JButton("Add Leaf")

                buttonAddObject.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, JsonObject(), index)
                    }
                    menu.isVisible = false
                }

                buttonAddArray.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, JsonArray(), index)
                    }
                    menu.isVisible = false
                }

                buttonAddLeaf.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, JsonNull(), index)
                    }
                    menu.isVisible = false
                }

                menu.add(buttonAddObject)
                menu.add(buttonAddArray)
                menu.add(buttonAddLeaf)

                if(widget!=null){
                    val remove = JButton("remove from array")
                    remove.addActionListener {
                        observers.forEach {
                            it.elementRemovedFromArray(modelArray, index - 1)
                        }
                        menu.isVisible = false
                    }
                    menu.add(remove)
                }

                menu.show(e.component, e.x, e.y)
            }
        }

        inner class ArrayElementWidget(value: JsonElement) : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                alignmentY = Component.CENTER_ALIGNMENT

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        onMouseClicked(e, this@ArrayElementWidget)
                    }
                })

                var widget: JsonWidget = JsonLeafWidget(JsonNull())
                when (value) {
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
            }
        }
    }
}
abstract class JsonWidget : JPanel()

interface JsonEditorViewObserver {
    fun elementAddedToObject(modelObject: JsonObject, name: String, value: JsonElement)
    fun elementAddedToArray(modelArray: JsonArray, value: JsonElement, index: Int)

    fun elementRemovedFromObject(modelObject: JsonObject, name: String)
    fun elementRemovedFromArray(modelArray: JsonArray, index: Int)

    //fun widgetModified() {}
}

